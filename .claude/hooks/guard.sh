#!/usr/bin/env bash
# PreToolUse guard for Privamatic: enforces the CLAUDE.md safety rules even when the
# permission lists are bypassed or approved by mistake.
#
# Silent by default: it prints a decision only for known risky patterns. Anything it does
# not recognise (heredocs, long scripts, odd syntax) gets no decision, so the normal
# permission flow applies. Internal errors block (exit 2) instead of letting a call through.
# Rules: STATE.md "Guard hook". Tests: scripts/test-hook.sh.

set -u
export LC_ALL=C
trap '__rc=$?; if [ "$__rc" -ne 0 ] && [ "$__rc" -ne 2 ]; then
  echo "guard.sh: internal error (exit $__rc), call blocked to be safe. Check .claude/hooks/guard.sh." >&2
  exit 2; fi' EXIT

fail() { echo "guard.sh: $*; call blocked to be safe." >&2; exit 2; }

DEBUG_PKG=com.techtrest.privamatic.debug
GUARD_COMMIT=c837e59              # branches without it still track .claude/settings.local.json
MAX_DEPTH=8                       # nesting of $(...), bash -c, heredocs, adb shell
US=$'\x1f'                        # word separator inside a token record
RD=$'\x1e'                        # prefix of a redirection-operator word
PLAIN_RE='^[A-Za-z0-9_./:=,+@%~-]+'
HOOK_DIR=${BASH_SOURCE[0]%/*}
[[ $HOOK_DIR == "${BASH_SOURCE[0]}" ]] && HOOK_DIR=.
LOG=${GUARD_LOG:-$HOOK_DIR/../hook.log}

# ---------------------------------------------------------------- device serials
#
# The serials live in the gitignored .claude/devices.local (CLAUDE.md, "Devices").
# GUARD_DEVICES_FILE overrides the path for scripts/test-hook.sh only; the hook sees Claude
# Code's environment, so a prefix on a Bash command can't change it.
# The file is parsed, never sourced: nothing in it can run or end the hook before a decision.
# Anything wrong sets DEVICES_PROBLEM, and every adb call or Gradle task that targets a device
# is then denied. An empty P8 must never reach a comparison: ser="" would match it.

P8=""                             # TEST_DEVICE_SERIAL: Pixel 8, test device
P9=""                             # DAILY_PHONE_SERIAL: Pixel 9, Oliver's daily phone
DEVICES_PROBLEM=""

serial_value() {  # serial_value VALUE: sets SERIAL to the cleaned value, or "" if it is not a serial
  local v=$1
  v=${v%%[[:space:]]#*}                  # trailing comment
  v=${v#"${v%%[![:space:]]*}"}           # leading space
  v=${v%"${v##*[![:space:]]}"}           # trailing space, CR included
  if [[ $v == \"*\" || $v == \'*\' ]]; then v=${v:1:${#v}-2}; fi
  if [[ $v =~ ^[A-Za-z0-9._:-]+$ ]]; then SERIAL=$v; else SERIAL=""; fi
}

load_devices() {
  local f=${GUARD_DEVICES_FILE:-$HOOK_DIR/../devices.local} line
  if [[ ! -f $f || ! -r $f ]]; then DEVICES_PROBLEM="the devices file is missing"; return; fi
  while IFS= read -r line || [[ -n $line ]]; do
    line=${line#export }
    case $line in
      TEST_DEVICE_SERIAL=*) serial_value "${line#*=}"; P8=$SERIAL ;;
      DAILY_PHONE_SERIAL=*) serial_value "${line#*=}"; P9=$SERIAL ;;
    esac
  done < "$f"
  if [[ -z $P8 ]]; then DEVICES_PROBLEM="TEST_DEVICE_SERIAL is empty or invalid in the devices file"
  elif [[ -z $P9 ]]; then DEVICES_PROBLEM="DAILY_PHONE_SERIAL is empty or invalid in the devices file"
  elif [[ $P8 == "$P9" ]]; then DEVICES_PROBLEM="both serials in the devices file are the same"
  fi
}
SERIAL=""
load_devices

# ---------------------------------------------------------------- decisions

DEC="" DEC_RULE="" DEC_REASON=""
# decide LEVEL RULE REASON: keeps the strongest decision (deny > ask) and its first reason.
decide() {
  local n=0 o=0
  case $1 in deny) n=3 ;; ask) n=2 ;; esac
  case $DEC in deny) o=3 ;; ask) o=2 ;; esac
  if (( n > o )); then DEC=$1 DEC_RULE=$2 DEC_REASON=$3; fi
}

# ---------------------------------------------------------------- tokenizer
#
# tok TEXT DEPTH splits shell text into simple commands and appends each to TOK_OUT as
# US-joined words. Quotes and backslashes are removed. $(...), `...`, <(...) and heredoc
# body lines are tokenized as commands of their own; the outer word keeps "$(...)".
# Redirections become words "RD<fd><op>", followed by their target word.
# It is an approximation of bash: good enough to find commands, not to run them.

TOK_OUT=()
TOO_DEEP=0
BAL_END=0

# _bal S I: S[I-1] is an opening "("; sets BAL_END to the index of the matching ")".
_bal() {
  local s=$1 i=$2 n=${#1} d=1 c r
  while (( i < n )); do
    c=${s:i:1}
    case $c in
      \\) ((i += 2)); continue ;;
      "'") r=${s:i+1}
           if [[ $r == *"'"* ]]; then r=${r%%"'"*}; ((i += ${#r} + 2)); else i=$n; fi
           continue ;;
      '"') ((i++))
           while (( i < n )); do
             c=${s:i:1}
             if [[ $c == \\ ]]; then ((i += 2)); continue; fi
             [[ $c == '"' ]] && break
             ((i++))
           done ;;
      '(') ((d++)) ;;
      ')') ((d--)); if (( d == 0 )); then BAL_END=$i; return; fi ;;
    esac
    ((i++))
  done
  BAL_END=$n
}

# Helpers below use tok's locals (bash scoping is dynamic).
_ew() {  # end the current word
  if (( inw )); then
    if (( nw )); then rec+=$US; fi
    rec+=$w; ((nw++)); w="" inw=0
  fi
}
_ec() {  # end the current simple command
  _ew
  if (( nw )); then TOK_OUT+=("$rec"); fi
  rec="" nw=0
}
_word() { _ew; w=$1 inw=1; _ew; }

_backtick() {  # s[i] is "`": tokenize up to the closing backtick
  local j=$((i + 1)) r=""
  while (( j < n )) && [[ ${s:j:1} != '`' ]]; do
    if [[ ${s:j:1} == \\ ]]; then r+=${s:j+1:1}; ((j += 2)); else r+=${s:j:1}; ((j++)); fi
  done
  tok "$r" $((depth + 1))
  w+="\$(${r})" inw=1 i=$((j + 1))
}

_subst() {  # s[i] is "$" and s[i+1] is "("
  local inner
  if [[ ${s:i+2:1} == '(' ]]; then  # $(( arithmetic ))
    _bal "$s" $((i + 2)); w+=${s:i:BAL_END-i+1}; inw=1 i=$((BAL_END + 1)); return
  fi
  _bal "$s" $((i + 2)); inner=${s:i+2:BAL_END-i-2}
  tok "$inner" $((depth + 1))
  w+="\$(${inner})" inw=1 i=$((BAL_END + 1))
}

_dq() {  # s[i] is '"': read a double-quoted string
  local ch x
  ((i++)); inw=1
  while (( i < n )); do
    ch=${s:i:1}
    case $ch in
      '"') ((i++)); return ;;
      \\) x=${s:i+1:1}
          case $x in
            '"'|\\|'$'|'`') w+=$x; ((i += 2)) ;;
            $'\n') ((i += 2)) ;;
            *) w+=\\; ((i++)) ;;
          esac ;;
      '$') if [[ ${s:i+1:1} == '(' ]]; then _subst; else w+='$'; ((i++)); fi ;;
      '`') _backtick ;;
      *) w+=$ch; ((i++)) ;;
    esac
  done
}

_heredocs() {  # s[i] starts the first pending heredoc body; tokenize each body line
  local k line t li=0
  local -a lines
  mapfile -t lines <<< "${s:i}"  # one copy; slicing per line is quadratic on long scripts
  for k in "${!hd[@]}"; do
    while (( i < n && li < ${#lines[@]} )); do
      line=${lines[li]}; ((li++))
      ((i += ${#line} + 1))
      t=$line
      if [[ ${hds[k]} == 1 ]]; then t=${t#"${t%%[!$'\t']*}"}; fi
      [[ $t == "${hd[k]}" ]] && break
      tok "$line" $((depth + 1))
    done
  done
  hd=() hds=()
}

tok() {
  local s=$1 depth=$2
  if (( depth > MAX_DEPTH )); then TOO_DEEP=1; return; fi
  local n=${#s} i=0 c nx w="" inw=0 rec="" nw=0 fd op r j inner strip dl
  local -a hd=() hds=()
  while (( i < n )); do
    c=${s:i:1}
    case $c in
      ' '|$'\t') _ew ;;
      $'\n') _ec; ((i++)); if (( ${#hd[@]} )); then _heredocs; fi; continue ;;
      ';'|'|') _ec ;;
      '&')
        if [[ ${s:i+1:1} == '>' ]]; then
          op='&>'; ((i += 2))
          if [[ ${s:i:1} == '>' ]]; then op='&>>'; ((i++)); fi
          _word "$RD$op"; continue
        fi
        _ec ;;
      '(')
        if (( inw )) && [[ $w == *= ]]; then  # array assignment a=(...)
          _bal "$s" $((i + 1)); w+=${s:i:BAL_END-i+1}; i=$((BAL_END + 1)); continue
        fi
        _ec ;;
      ')') _ec ;;
      '<'|'>')
        if [[ ${s:i+1:1} == '(' ]]; then  # process substitution
          _bal "$s" $((i + 2)); inner=${s:i+2:BAL_END-i-2}
          tok "$inner" $((depth + 1))
          w+="${c}(${inner})" inw=1 i=$((BAL_END + 1)); continue
        fi
        fd=""
        if (( inw )) && [[ $w =~ ^[0-9]+$ ]]; then fd=$w w="" inw=0; else _ew; fi
        op=$c; ((i++)); nx=${s:i:1}
        if [[ $c == '<' && $nx == '<' ]]; then
          ((i++))
          if [[ ${s:i:1} == '<' ]]; then
            ((i++)); _word "$RD$fd<<<"; continue
          fi
          strip=0
          if [[ ${s:i:1} == '-' ]]; then strip=1; ((i++)); fi
          while [[ ${s:i:1} == ' ' || ${s:i:1} == $'\t' ]]; do ((i++)); done
          dl=""
          while (( i < n )); do
            nx=${s:i:1}
            case $nx in
              ' '|$'\t'|$'\n'|';'|'|'|'&'|'<'|'>'|'('|')') break ;;
              "'"|'"') ((i++)); r=${s:i}; r=${r%%"$nx"*}; dl+=$r; ((i += ${#r} + 1)) ;;
              \\) dl+=${s:i+1:1}; ((i += 2)) ;;
              *) dl+=$nx; ((i++)) ;;
            esac
          done
          hd+=("$dl") hds+=("$strip")
          _word "$RD$fd<<"; _word "$dl"; continue
        fi
        if [[ $c == '>' && ( $nx == '>' || $nx == '&' || $nx == '|' ) ]] ||
           [[ $c == '<' && ( $nx == '>' || $nx == '&' ) ]]; then
          op+=$nx; ((i++))
        fi
        _word "$RD$fd$op"; continue ;;
      "'")
        r=${s:i+1}
        if [[ $r == *"'"* ]]; then r=${r%%"'"*}; w+=$r; ((i += ${#r} + 2)); else w+=$r; i=$n; fi
        inw=1; continue ;;
      '"') _dq; continue ;;
      \\)
        nx=${s:i+1:1}
        if [[ $nx != $'\n' ]]; then w+=$nx; inw=1; fi
        ((i += 2)); continue ;;
      '$')
        nx=${s:i+1:1}
        if [[ $nx == '(' ]]; then _subst; continue; fi
        if [[ $nx == "'" ]]; then  # $'...' ANSI-C quoting
          j=$((i + 2)) r=""
          while (( j < n )) && [[ ${s:j:1} != "'" ]]; do
            if [[ ${s:j:1} == \\ ]]; then r+=${s:j:2}; ((j += 2)); else r+=${s:j:1}; ((j++)); fi
          done
          printf -v r '%b' "$r"; w+=$r; inw=1 i=$((j + 1)); continue
        fi
        w+='$'; inw=1 ;;
      '`') _backtick; continue ;;
      '#')
        if (( ! inw )); then r=${s:i}; r=${r%%$'\n'*}; ((i += ${#r})); continue; fi
        w+=$c ;;
      *)
        if [[ ${s:i:64} =~ $PLAIN_RE ]]; then
          w+=${BASH_REMATCH[0]}; inw=1; ((i += ${#BASH_REMATCH[0]})); continue
        fi
        w+=$c; inw=1 ;;
    esac
    ((i++))
  done
  _ec
}

SPLIT=()
_split() {  # _split RECORD: sets SPLIT to its words
  local r=$1
  SPLIT=()
  while [[ $r == *"$US"* ]]; do SPLIT+=("${r%%"$US"*}"); r=${r#*"$US"}; done
  SPLIT+=("$r")
}

# run_text TEXT DEPTH CTX: tokenize TEXT and analyse its commands in context CTX.
run_text() {
  local -a saved=("${TOK_OUT[@]}") mine
  TOK_OUT=()
  tok "$1" "$2"
  mine=("${TOK_OUT[@]}")
  TOK_OUT=("${saved[@]}")
  local rec
  for rec in "${mine[@]}"; do analyze_rec "$rec" "$2" "$3"; done
}

# analyze_words DEPTH CTX WORD...: analyse an already split command (find -exec, git alias).
analyze_words() {
  local depth=$1 ctx=$2; shift 2
  (( $# )) || return 0
  local r
  printf -v r "%s$US" "$@"
  analyze_rec "${r%"$US"}" "$depth" "$ctx"
}

# ---------------------------------------------------------------- shared state

SIM_DIR=$PWD            # working directory as the command line would see it (cd is tracked)
SIM_BRANCH=""           # branch after a git switch/checkout earlier in the same command
SIM_BRANCH_DIR=""
declare -A ASSIGN=()    # NAME=VALUE seen earlier in the command, for "$x push"
declare -A SIM_ALIAS=() # git aliases defined earlier in the command
EXPORTED_SERIAL=""
P9_SEEN=0               # a command addressed the Pixel 9
P9_SAFE=1               # every part so far is on the Pixel 9 read-only allow list
PART_OK=0
GIT_ALIAS_DEPTH=0

note_assign() {  # note_assign NAME=VALUE
  local nm=${1%%=*} v=${1#*=}
  nm=${nm%+}
  ASSIGN[$nm]=$v
  if [[ $nm == ANDROID_SERIAL ]]; then EXPORTED_SERIAL=$v; fi
}

is_cfg_path() {  # .claude/settings*.json, .claude/hooks/**, .claude/devices.local, or .claude itself
  local p=$1
  [[ $p == '~'* ]] && p=${HOME-}${p#'~'}
  [[ $p != /* ]] && p=$SIM_DIR/$p
  case $p in
    */.claude/*settings*|*/.claude/*hooks*|*/.claude/*devices.local*|*/.claude|*/.claude/) return 0 ;;
  esac
  return 1
}

is_keystore() {
  local p=${1,,}
  case $p in *.jks|*.keystore|*.jks.*|*.keystore.*) return 0 ;; esac
  return 1
}

is_secret_name() {
  local nm=${1^^}
  [[ $nm == *PASSWORD* || $nm == *KEY* || $nm == *SECRET* || $nm == *TOKEN* ]]
}

has_secret_ref() {  # does the word expand a variable with a secret-looking name?
  local s=$1
  while [[ $s =~ \$\{?!?([A-Za-z_][A-Za-z0-9_]*) ]]; do
    is_secret_name "${BASH_REMATCH[1]}" && return 0
    s=${s#*"${BASH_REMATCH[0]}"}
  done
  return 1
}

# ---------------------------------------------------------------- per-command analysis

# analyze_rec RECORD DEPTH CTX. CTX is "local" or "remote:<class>" (inside adb shell).
analyze_rec() {
  local depth=$2 ctx=$3 k
  _split "$1"
  local -a W=("${SPLIT[@]}") A=() R=() E=()
  for ((k = 0; k < ${#W[@]}; k++)); do
    if [[ ${W[k]} == "$RD"* ]]; then
      R+=("${W[k]#"$RD"}" "${W[k+1]-}"); ((k++))
    else
      A+=("${W[k]}")
    fi
  done
  if [[ $ctx == local ]]; then analyze_local; else analyze_remote; fi
}

strip_prefix() {  # leading assignments into E, then shell keywords
  while (( ${#A[@]} )) && [[ ${A[0]} =~ ^[A-Za-z_][A-Za-z0-9_]*\+?= ]]; do
    E+=("${A[0]}"); A=("${A[@]:1}")
  done
  while (( ${#A[@]} )); do
    case ${A[0]} in
      '!'|'{'|'}'|then|do|else|elif|if|while|until|time|fi|done|esac) A=("${A[@]:1}") ;;
      *) break ;;
    esac
  done
}

unwrap() {  # strip wrapper commands (env, timeout, xargs, ...) from A
  local nm guard=0 v
  local -a sp
  while (( ${#A[@]} && guard++ < 20 )); do
    nm=${A[0]##*/}
    case $nm in
      command|exec|nohup|setsid|time|nice|ionice|stdbuf|timeout|xargs|unbuffer|chronic|env|busybox|toybox) ;;
      *) return ;;
    esac
    A=("${A[@]:1}")
    case $nm in
      exec) while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -a ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done ;;
      nice|ionice) while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -[ncp] ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done ;;
      stdbuf) while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -[ioe] ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done ;;
      time) while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -[of] ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done ;;
      xargs) while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -[adEILnPs] ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done ;;
      timeout)
        while [[ ${A[0]-} == -* ]]; do [[ ${A[0]} == -[sk] ]] && A=("${A[@]:1}"); A=("${A[@]:1}"); done
        A=("${A[@]:1}") ;;  # the duration
      command|nohup|setsid|unbuffer|chronic)
        while [[ ${A[0]-} == -* ]]; do A=("${A[@]:1}"); done ;;
      env)
        while (( ${#A[@]} )); do
          case ${A[0]} in
            -u|-C|--unset|--chdir) A=("${A[@]:2}") ;;
            -S|--split-string) read -r -a sp <<< "${A[1]-}"; A=("${sp[@]}" "${A[@]:2}") ;;
            -S*|--split-string=*) v=${A[0]#-S}; v=${v#--split-string=}
                                  read -r -a sp <<< "$v"; A=("${sp[@]}" "${A[@]:1}") ;;
            --) A=("${A[@]:1}"); break ;;
            -*) A=("${A[@]:1}") ;;
            *=*) E+=("${A[0]}"); A=("${A[@]:1}") ;;
            *) break ;;
          esac
        done
        if (( ! ${#A[@]} )); then A=(env); return; fi ;;  # bare env prints the environment
    esac
  done
}

analyze_local() {
  local k op tg a name
  for ((k = 0; k < ${#R[@]}; k += 2)); do
    op=${R[k]} tg=${R[k+1]}
    case $op in
      *'<<'*) ;;
      *'<'|*'<&') if is_keystore "$tg"; then decide deny secret-keystore "reads keystore $tg; keystores are never read (CLAUDE.md Secrets)."; fi ;;
      *) if [[ $op == *'>&' && $tg =~ ^[0-9-]+$ ]]; then continue; fi
         if is_cfg_path "$tg"; then decide ask claude-config-write "writes $tg (Claude settings or guard hook); confirm this change."; fi ;;
    esac
  done

  strip_prefix
  if (( ! ${#A[@]} )); then
    for a in "${E[@]}"; do note_assign "$a"; done
    return
  fi
  # "$x push" after x=git earlier in the command
  if [[ ${A[0]} =~ ^\$\{?([A-Za-z_][A-Za-z0-9_]*)\}?$ ]] && [[ -n ${ASSIGN[${BASH_REMATCH[1]}]+x} ]]; then
    local -a sp
    read -r -a sp <<< "${ASSIGN[${BASH_REMATCH[1]}]}"
    A=("${sp[@]}" "${A[@]:1}")
  fi
  unwrap
  (( ${#A[@]} )) || return 0
  name=${A[0]##*/}

  PART_OK=0
  case $name in
    git) analyze_git "${A[@]:1}" ;;
    gh) analyze_gh "${A[@]:1}" ;;
    adb) analyze_adb "${A[@]:1}" ;;
    gradlew|gradle) analyze_gradle "${A[@]:1}" ;;
    ssh|scp|sftp|rsync)
      decide deny remote-access "$name is not allowed; no server belongs to this project (CLAUDE.md Secrets)." ;;
    sudo|su|doas|pkexec)
      decide deny root "$name is not allowed." ;;
    keytool)
      decide deny secret-keytool "keytool reads keystores; never read keystores (CLAUDE.md Secrets)." ;;
    printenv)
      local named=0
      for a in "${A[@]:1}"; do
        [[ $a == -* ]] && continue
        named=1
        if is_secret_name "$a"; then decide deny secret-env "printenv $a would print a secret; report names only (CLAUDE.md Secrets)."; fi
      done
      if (( ! named )); then decide deny secret-env "printenv without a name prints every variable, secrets included; use printenv NAME."; fi ;;
    env)
      decide deny secret-env "env without a command prints every variable, secrets included; use printenv NAME." ;;
    set)
      if (( ${#A[@]} == 1 )); then decide deny secret-env "set without arguments prints every variable, secrets included."; fi ;;
    export|declare|typeset|readonly)
      local names=0
      for a in "${A[@]:1}"; do
        [[ $a == -* ]] && continue
        names=1
        if [[ $a == *=* ]]; then note_assign "$a"
        elif [[ $name != export ]] && is_secret_name "$a"; then decide deny secret-env "$name would print secret variable $a."
        fi
      done
      if (( ! names )); then decide deny secret-env "$name without names prints every variable, secrets included."; fi ;;
    echo|printf)
      for a in "${A[@]:1}"; do
        if has_secret_ref "$a"; then decide deny secret-env "$name would print a secret variable ($a); report names only (CLAUDE.md Secrets)."; break; fi
      done ;;
    bash|sh|zsh|dash|ksh|mksh|ash)
      for ((k = 1; k < ${#A[@]}; k++)); do
        a=${A[k]}
        case $a in
          --) break ;;
          --*) ;;
          -*c*) run_text "${A[k+1]-}" $((depth + 1)) local; break ;;
          -o|+o|-O|+O) ((k++)) ;;
          -*|+*) ;;
          *) break ;;  # a script file: not inspected
        esac
      done ;;
    eval)
      run_text "${A[*]:1}" $((depth + 1)) local ;;
    watch)
      k=1
      while [[ ${A[k]-} == -* ]]; do [[ ${A[k]} == -n ]] && ((k++)); ((k++)); done
      run_text "${A[*]:k}" $((depth + 1)) local ;;
    find)
      for ((k = 1; k < ${#A[@]}; k++)); do
        case ${A[k]} in
          -exec|-execdir|-ok|-okdir)
            local -a sub=()
            ((k++))
            while (( k < ${#A[@]} )) && [[ ${A[k]} != ';' && ${A[k]} != '+' ]]; do sub+=("${A[k]}"); ((k++)); done
            analyze_words $((depth + 1)) local "${sub[@]}" ;;
        esac
      done ;;
    cd|pushd)
      local t=${A[1]-${HOME-/}}
      [[ $t == -[LPe@] ]] && t=${A[2]-${HOME-/}}
      [[ $t == '~'* ]] && t=${HOME-}${t#'~'}
      [[ $t != /* ]] && t=$SIM_DIR/$t
      if [[ $t != *'$'* && -d $t ]]; then SIM_DIR=$t SIM_BRANCH=""; fi ;;
    grep|egrep|fgrep|head|tail|wc)
      PART_OK=1
      for ((k = 0; k < ${#R[@]}; k += 2)); do
        case ${R[k]} in 2'>') [[ ${R[k+1]} == /dev/null ]] || PART_OK=0 ;; *) PART_OK=0 ;; esac
      done ;;
  esac

  # Checks that apply whatever the command is.
  case $name in
    ls|find|stat|file|test|'['|'[['|echo|printf|basename|dirname|realpath|readlink|which|type|du|wc) ;;
    *)
      for a in "${A[@]:1}"; do
        if is_keystore "$a"; then decide deny secret-keystore "$name with keystore $a; keystores are never read (CLAUDE.md Secrets)."; break; fi
        if [[ $a == /proc/*/environ ]]; then decide deny secret-env "$a holds the environment, secrets included."; break; fi
      done ;;
  esac
  local wr=0 inplace=0
  case $name in
    cp|mv|rm|rmdir|unlink|tee|truncate|touch|chmod|chown|chgrp|ln|install|dd|rsync|shred|patch|sponge|vi|vim|nvim|nano|emacs|ed|ex) wr=1 ;;
    sed) for a in "${A[@]:1}"; do [[ $a == --in-place* || ( $a == -[a-zA-Z]* && $a == *i* ) ]] && inplace=1; done
         wr=$inplace ;;
    python|python3|perl|ruby|node)
      for a in "${A[@]:1}"; do
        if [[ $a == *.claude/*settings* || $a == *.claude/*hooks* || $a == *.claude/*devices.local* ]]; then
          decide ask claude-config-write "$name touches $a (Claude settings or guard hook); confirm."; break
        fi
      done ;;
  esac
  if (( wr )); then
    for a in "${A[@]:1}"; do
      if is_cfg_path "${a#*=}"; then decide ask claude-config-write "$name changes $a (Claude settings or guard hook); confirm this change."; break; fi
    done
  fi

  if (( ! PART_OK )); then P9_SAFE=0; fi
  PART_OK=0
}

# ---------------------------------------------------------------- git

resolve_dir() {  # resolve_dir BASE REL: sets RES_DIR
  local r=$2
  [[ $r == '~'* ]] && r=${HOME-}${r#'~'}
  if [[ $r == /* ]]; then RES_DIR=$r; else RES_DIR=$1/$r; fi
}

git_q() { git "${GQ[@]}" "$@" 2>/dev/null; }  # read-only query in the target repo

cur_branch() {
  if [[ -n $SIM_BRANCH && $SIM_BRANCH_DIR == "$dir" ]]; then CUR_BRANCH=$SIM_BRANCH; return; fi
  CUR_BRANCH=$(git_q symbolic-ref --short -q HEAD) || CUR_BRANCH=""
}
on_master() { cur_branch; [[ $CUR_BRANCH == master ]]; }

# check_target REV: 0 contains GUARD_COMMIT (or not this repo), 1 lacks it, 2 unresolvable.
check_target() {
  local rev=$1 sha
  [[ $rev == - ]] && rev='@{-1}'
  [[ $rev == *'$'* || -z $rev ]] && return 2
  sha=$(git_q rev-parse --verify -q "${rev}^{commit}") ||
    sha=$(git_q rev-parse --verify -q "refs/remotes/origin/${rev}^{commit}") || return 2
  git_q cat-file -e "${GUARD_COMMIT}^{commit}" || return 0
  git_q merge-base --is-ancestor "$GUARD_COMMIT" "$sha" && return 0
  return 1
}

# switch_to TARGET NEWBRANCH DETACH UNRESOLVED_MEANS: shared check for switch/checkout.
switch_to() {
  local target=$1 new=$2 detach=$3 rc
  check_target "$target"; rc=$?
  case $rc in
    1) decide deny git-switch-pre-guard "'$target' does not contain $GUARD_COMMIT; switching there overwrites or deletes the untracked .claude/settings.local.json (STATE.md). Back the file up and ask Oliver."
       return 1 ;;
    2) return 2 ;;
  esac
  if [[ -n $new ]]; then SIM_BRANCH=$new
  elif (( detach )); then SIM_BRANCH="(detached)"
  elif [[ $target == - ]]; then SIM_BRANCH=$(git_q rev-parse --abbrev-ref '@{-1}') || SIM_BRANCH=""
  else SIM_BRANCH=$target
  fi
  SIM_BRANCH_DIR=$dir
  return 0
}

is_git_builtin() {
  case $1 in
    add|am|apply|archive|bisect|blame|branch|bundle|cat-file|check-ignore|checkout|cherry|cherry-pick|\
    citool|clean|clone|commit|commit-tree|config|count-objects|describe|diff|diff-tree|difftool|fetch|\
    for-each-ref|format-patch|fsck|gc|grep|gui|hash-object|help|http-push|init|log|ls-files|ls-remote|\
    ls-tree|maintenance|merge|merge-base|mergetool|mv|name-rev|notes|prune|pull|push|range-diff|rebase|\
    reflog|remote|repack|replace|request-pull|reset|restore|rev-list|rev-parse|revert|rm|send-email|\
    send-pack|shortlog|show|show-branch|show-ref|sparse-checkout|stash|status|submodule|subtree|switch|\
    symbolic-ref|tag|update-index|update-ref|var|verify-commit|verify-tag|version|whatchanged|worktree) return 0 ;;
  esac
  return 1
}

analyze_git() {
  local -a args=("$@") GQ=() CFG=() rest=() P=()
  local dir=$SIM_DIR i=0 n=$# a k sub val
  while (( i < n )); do
    a=${args[i]}
    case $a in
      -C) resolve_dir "$dir" "${args[i+1]-}"; dir=$RES_DIR; ((i += 2)) ;;
      -C?*) resolve_dir "$dir" "${a#-C}"; dir=$RES_DIR; ((i++)) ;;
      -c) CFG+=("${args[i+1]-}"); ((i += 2)) ;;
      -c?*) CFG+=("${a#-c}"); ((i++)) ;;
      --git-dir|--work-tree) GQ+=("$a=${args[i+1]-}"); ((i += 2)) ;;
      --git-dir=*|--work-tree=*) GQ+=("$a"); ((i++)) ;;
      --namespace|--super-prefix|--config-env|--exec-path) ((i += 2)) ;;
      --) ((i++)); break ;;
      -*) ((i++)) ;;
      *) break ;;
    esac
  done
  GQ=(-C "$dir" "${GQ[@]}")
  sub=${args[i]-}
  rest=("${args[@]:i+1}")
  [[ -z $sub ]] && return

  if ! is_git_builtin "$sub"; then  # an alias? (aliases cannot shadow built-ins)
    val=""
    for a in "${CFG[@]}"; do
      if [[ ${a,,} == "alias.${sub,,}="* ]]; then val=${a#*=}; fi
    done
    [[ -z $val ]] && val=${SIM_ALIAS[$sub]-}
    [[ -z $val ]] && { val=$(git_q config --get "alias.$sub") || val=""; }
    if [[ -n $val ]] && (( GIT_ALIAS_DEPTH < 5 )); then
      if [[ $val == '!'* ]]; then
        run_text "${val#!} ${rest[*]}" $((depth + 1)) local
      else
        local -a av cf=()
        read -r -a av <<< "$val"
        for a in "${CFG[@]}"; do cf+=(-c "$a"); done
        ((GIT_ALIAS_DEPTH++))
        analyze_git -C "$dir" "${cf[@]}" "${av[@]}" "${rest[@]}"
        ((GIT_ALIAS_DEPTH--))
      fi
    fi
    return
  fi

  case $sub in
    push|send-pack|http-push)
      decide deny git-push "git push is never allowed; Oliver pushes (CLAUDE.md Git)." ;;
    subtree)
      [[ ${rest[0]-} == push ]] && decide deny git-push "git subtree push is a push; Oliver pushes (CLAUDE.md Git)." ;;
    tag) git_tag ;;
    clean) git_clean ;;
    reset)
      for a in "${rest[@]}"; do
        if [[ $a == --hard ]] && on_master; then
          decide deny git-reset-hard-master "git reset --hard while master is checked out is never allowed (CLAUDE.md Git)."; break
        fi
      done ;;
    commit)
      if on_master; then decide deny git-commit-master "git commit on master is never allowed; use a feature branch (CLAUDE.md Git)."; fi ;;
    merge)
      for a in "${rest[@]}"; do
        if [[ $a == --continue ]] && on_master; then
          decide deny git-commit-master "git merge --continue commits on master; if a merge into master conflicts, stop and ask Oliver."; break
        fi
      done ;;
    cherry-pick|revert|am)
      local stop=0
      for a in "${rest[@]}"; do [[ $a == --abort || $a == --quit || $a == --skip || $a == --show-current-patch* ]] && stop=1; done
      if (( ! stop )) && on_master; then decide deny git-commit-master "git $sub creates a commit on master; use a feature branch (CLAUDE.md Git)."; fi ;;
    switch) git_switch ;;
    checkout) git_checkout ;;
    branch)
      local D=0 del=0 force=0
      for a in "${rest[@]}"; do
        case $a in
          --delete) del=1 ;;
          --force) force=1 ;;
          --*) ;;
          -*) [[ $a == *D* ]] && D=1; [[ $a == *d* ]] && del=1; [[ $a == *f* ]] && force=1 ;;
        esac
      done
      if (( D || (del && force) )); then decide ask git-branch-D "git branch -D deletes a branch even when it is not merged; confirm."; fi ;;
    config)
      local -a cr=()
      for a in "${rest[@]}"; do [[ $a == -* || $a == set ]] && continue; cr+=("$a"); done
      if [[ ${cr[0]-} == alias.* && -n ${cr[1]-} ]]; then SIM_ALIAS[${cr[0]#alias.}]=${cr[1]}; fi ;;
  esac
  case $sub in
    checkout|restore|rm|mv)
      for a in "${rest[@]}"; do
        if is_cfg_path "$a"; then decide ask claude-config-write "git $sub changes $a (Claude settings or guard hook); confirm."; break; fi
      done ;;
  esac
}

git_tag() {
  local list=0 create=0 pos=0 skip=0 a
  for a in "${rest[@]}"; do
    if (( skip )); then skip=0; [[ $a != -* ]] && continue; fi
    case $a in
      -l|--list|-v|--verify|-i|--ignore-case|--omit-empty) list=1 ;;
      --contains|--no-contains|--merged|--no-merged|--points-at|--sort|--format) list=1; skip=1 ;;
      --contains=*|--no-contains=*|--merged=*|--no-merged=*|--points-at=*|--sort=*|--format=*|--column*|--no-column|--color*) list=1 ;;
      -n|-n[0-9]*) list=1 ;;
      -d|--delete|-a|--annotate|-s|--sign|--no-sign|-u|--local-user*|-m|--message*|-F|--file*|-f|--force|-e|--edit|--cleanup*|--create-reflog|--trailer*) create=1 ;;
      --) ;;
      --*) ;;
      -*) [[ $a == *[dasumFfe]* ]] && create=1; [[ $a == *l* ]] && list=1 ;;
      *) pos=1 ;;
    esac
  done
  if (( create || (pos && !list) )); then
    decide deny git-tag-write "creating or deleting tags is never allowed; Oliver tags (CLAUDE.md Git). Listing (git tag, -l, --contains) is fine."
  fi
}

git_clean() {
  local force=0 dry=0 skip=0 a nf=0
  for a in "${rest[@]}"; do
    if (( skip )); then skip=0; continue; fi
    case $a in
      --force) force=1 ;;
      --dry-run) dry=1 ;;
      -e|--exclude) skip=1 ;;
      --) break ;;
      --*) ;;
      -*) [[ $a == *f* ]] && force=1; [[ $a == *n* ]] && dry=1 ;;
    esac
  done
  for a in "${CFG[@]}"; do [[ ${a,,} == clean.requireforce=false || ${a,,} == clean.requireforce=0 ]] && nf=1; done
  if (( ! nf )) && [[ $(git_q config --type=bool --get clean.requireForce) == false ]]; then nf=1; fi
  if (( force || (nf && !dry) )); then
    decide deny git-clean-force "git clean -f deletes untracked files (settings.local.json among them); never allowed (CLAUDE.md Git)."
  fi
}

git_switch() {
  local create="" orphan=0 detach=0 a k target
  local -a P=()
  for ((k = 0; k < ${#rest[@]}; k++)); do
    a=${rest[k]}
    case $a in
      -c|-C|--create|--force-create) create=${rest[k+1]-}; ((k++)) ;;
      --create=*|--force-create=*) create=${a#*=} ;;
      -c?*|-C?*) create=${a:2} ;;
      --orphan) orphan=1; ((k++)) ;;
      --orphan=*) orphan=1 ;;
      -d|--detach) detach=1 ;;
      --) P+=("${rest[@]:k+1}"); break ;;
      -*) ;;
      *) P+=("$a") ;;
    esac
  done
  if (( orphan )); then
    decide deny git-switch-pre-guard "an orphan branch has no $GUARD_COMMIT; it drops the tracked guard hook and settings."; return
  fi
  if [[ -n $create ]]; then target=${P[0]-HEAD}; else target=${P[0]-}; fi
  [[ -z $target ]] && return
  switch_to "$target" "$create" "$detach"
  if (( $? == 2 )); then
    decide ask git-switch-unverified "cannot resolve '$target' (locally or as origin/$target) to check that it contains $GUARD_COMMIT; confirm."
  fi
}

git_checkout() {
  local create="" orphan=0 dd=0 restore=0 detach=0 a k
  local -a P=()
  for ((k = 0; k < ${#rest[@]}; k++)); do
    a=${rest[k]}
    case $a in
      -b|-B) create=${rest[k+1]-}; ((k++)) ;;
      --orphan) orphan=1; ((k++)) ;;
      --detach) detach=1 ;;
      -p|--patch|--ours|--theirs|--pathspec-from-file=*|--pathspec-file-nul|--no-overlay|--overlay) restore=1 ;;
      --) dd=1; break ;;
      -*) ;;
      *) P+=("$a") ;;
    esac
  done
  (( restore )) && return
  if (( orphan )); then
    decide deny git-switch-pre-guard "an orphan branch has no $GUARD_COMMIT; it drops the tracked guard hook and settings."; return
  fi
  if [[ -n $create ]]; then
    switch_to "${P[0]-HEAD}" "$create" 0
    if (( $? == 2 )); then
      decide ask git-switch-unverified "cannot resolve start point '${P[0]-}' to check that it contains $GUARD_COMMIT; confirm."
    fi
    return
  fi
  (( dd )) && return             # git checkout [<rev>] -- <paths>: a file restore
  (( ${#P[@]} == 1 )) || return  # nothing, or <tree-ish> <paths>: a file restore
  switch_to "${P[0]}" "" "$detach"  # unresolvable means git treats it as a path
}

# ---------------------------------------------------------------- gh

analyze_gh() {
  local -a P=()
  local a skip=0 w=0
  for a in "$@"; do
    if (( skip )); then skip=0; continue; fi
    case $a in
      -R|--repo|--hostname) skip=1 ;;
      -*) ;;
      *) P+=("$a"); (( ${#P[@]} >= 2 )) && break ;;
    esac
  done
  local g=${P[0]-} act=${P[1]-}
  case $g in
    "") return ;;
    api|release) w=1 ;;
    issue) case $act in comment|close|create|new|edit|reopen|delete|transfer|pin|unpin|lock|unlock|develop) w=1 ;; esac ;;
    pr) case $act in create|new|merge|close|comment|edit|review|ready|reopen|checkout|lock|unlock|revert|update-branch) w=1 ;; esac ;;
    repo) case $act in create|new|edit|delete|rename|archive|unarchive|fork|sync|set-default|deploy-key|autolink) w=1 ;; esac ;;
    secret|variable) case $act in set|delete|remove) w=1 ;; esac ;;
    label) case $act in create|edit|delete|clone) w=1 ;; esac ;;
    workflow) case $act in run|enable|disable) w=1 ;; esac ;;
    run) case $act in cancel|rerun|delete) w=1 ;; esac ;;
    cache) case $act in delete) w=1 ;; esac ;;
    gist) case $act in create|new|edit|delete|rename) w=1 ;; esac ;;
    ssh-key|gpg-key) case $act in add|delete) w=1 ;; esac ;;
    alias) case $act in set|import|delete) w=1 ;; esac ;;
    extension|extensions|ext) case $act in install|upgrade|remove|create|exec) w=1 ;; esac ;;
    config) case $act in set|clear-cache) w=1 ;; esac ;;
    auth) case $act in
            token|login|logout|refresh|setup-git|switch) w=1 ;;
            status) for a in "$@"; do [[ $a == --show-token || $a == -t ]] && w=1; done ;;
          esac ;;
    codespace|cs) case $act in list|ls|view) ;; *) w=1 ;; esac ;;
    project) case $act in list|view|field-list|item-list) ;; *) w=1 ;; esac ;;
    browse|search|status|help|version|completion|attestation|org|ruleset|agent-task|copilot|preview|accessibility) ;;
    *) decide ask gh-unknown "'gh $g' is not a known gh command (an alias or extension?); gh is read-only here, confirm."; return ;;
  esac
  if (( w )); then
    decide deny gh-write "gh $g${act:+ $act} writes to GitHub or prints a credential; gh is read-only here (CLAUDE.md GitHub)."
  fi
}

# ---------------------------------------------------------------- adb

has_user0() {
  local prev=""
  for a in "$@"; do
    [[ $prev == --user && $a == 0 ]] && return 0
    prev=$a
  done
  return 1
}

logcat_pid_only() {  # logcat args that only read, filtered by --pid
  local a has=0 skip=""
  for a in "$@"; do
    if [[ -n $skip ]]; then [[ $skip == --pid ]] && has=1; skip=""; continue; fi
    case $a in
      --pid=?*) has=1 ;;
      --pid|-v|-t|-T|-b|-e|-m|--format|--regex|--max-count|--buffer) skip=$a ;;
      -d|-D|--dividers|--format=*|--regex=*|--max-count=*|--buffer=*|-v?*|-t?*|-T?*|-b?*|-m?*) ;;
      -*) return 1 ;;
      *:[VDIWEFS]) ;;
      *) return 1 ;;
    esac
  done
  (( has ))
}

logcat_writes() {  # logcat options that clear or change buffers or write files
  local a
  for a in "$@"; do
    case $a in -c|--clear|-G*|--buffer-size*|-P|--prune*|-f|--file*|-r|--rotate-kbytes*|-n|--rotate-count*) return 0 ;; esac
  done
  return 1
}

shell_rest() {  # strip adb shell options; sets REM to the remote command text
  local -a r=("$@")
  while [[ ${r[0]-} == -* ]]; do
    [[ ${r[0]} == -e ]] && r=("${r[@]:1}")
    r=("${r[@]:1}")
  done
  local IFS=' '
  REM="${r[*]}"
}

# p9_remote_ok TEXT: every remote command is on the read-only allow list.
p9_remote_ok() {
  local -a saved=("${TOK_OUT[@]}") recs
  TOK_OUT=(); tok "$1" $((depth + 1)); recs=("${TOK_OUT[@]}"); TOK_OUT=("${saved[@]}")
  (( ${#recs[@]} )) || return 1
  local rec real=0 x
  for rec in "${recs[@]}"; do
    _split "$rec"
    local -a V=("${SPLIT[@]}")
    for x in "${V[@]}"; do [[ $x == "$RD"* ]] && return 1; done
    case ${V[0]} in
      getprop)
        if (( ${#V[@]} == 2 )); then [[ ${V[1]} =~ ^[A-Za-z0-9._-]+$ ]] || return 1
        elif (( ${#V[@]} != 1 )); then return 1; fi
        real=1 ;;
      dumpsys) [[ ${#V[@]} == 3 && ${V[1]} == package && ${V[2]} == "$DEBUG_PKG" ]] || return 1; real=1 ;;
      pidof) [[ ${#V[@]} == 2 && ${V[1]} == "$DEBUG_PKG" ]] || return 1; real=1 ;;
      logcat) logcat_pid_only "${V[@]:1}" || return 1; real=1 ;;
      grep|egrep|fgrep|head|tail|wc) for x in "${V[@]:1}"; do [[ $x == */* ]] && return 1; done ;;
      *) return 1 ;;
    esac
  done
  (( real ))
}

# p9_allowed SUB ARGS...: the whole adb call is on the Pixel 9 read-only allow list.
p9_allowed() {
  local sub=$1 k png=0 other=0
  shift
  for ((k = 0; k < ${#R[@]}; k += 2)); do
    case ${R[k]} in
      2'>') [[ ${R[k+1]} == /dev/null ]] || other=1 ;;
      2'>&') [[ ${R[k+1]} == 1 ]] || other=1 ;;
      '>'|'>|'|1'>') if [[ ${R[k+1]} == *.png && ${R[k+1]} != *'$'* ]]; then ((png++)); else other=1; fi ;;
      *) other=1 ;;
    esac
  done
  (( other )) && return 1
  case $sub in
    exec-out) [[ $# -eq 2 && $1 == screencap && $2 == -p && $png -eq 1 ]] ;;
    logcat) (( ! png )) && logcat_pid_only "$@" ;;
    shell) (( png )) && return 1; shell_rest "$@"; p9_remote_ok "$REM" ;;
    *) return 1 ;;
  esac
}

analyze_adb() {
  local -a args=("$@") S=() rest
  local i=0 n=$# a sub cls=p8 s label
  while (( i < n )); do
    a=${args[i]}
    case $a in
      -s) S+=("${args[i+1]-}"); ((i += 2)) ;;
      -s?*) S+=("${a#-s}"); ((i++)) ;;
      -t|-H|-P|-L) ((i += 2)) ;;
      -*) ((i++)) ;;
      *) break ;;
    esac
  done
  sub=${args[i]-}
  rest=("${args[@]:i+1}")
  if (( ! ${#S[@]} )); then
    case "$sub ${rest[*]}" in
      "devices "|"devices -l"|"start-server "|"kill-server "|"version ") return ;;
    esac
  fi
  if [[ -n $DEVICES_PROBLEM ]]; then
    decide deny devices-file "adb $sub targets a device, but $DEVICES_PROBLEM: create .claude/devices.local, see CLAUDE.md."
    return
  fi
  if (( ! ${#S[@]} )); then
    decide deny adb-no-serial "adb needs -s <serial> (CLAUDE.md Devices); the test device is adb -s $P8."
    return
  fi
  for s in "${S[@]}"; do
    if [[ -z $s || $s == *'$'* || $s == *'`'* ]]; then cls=var; break
    elif [[ $s == "$P9" ]]; then cls=p9
    elif [[ $s != "$P8" ]]; then cls=unknown
    fi
  done
  [[ $cls == p8 ]] && return
  case $cls in
    p9) P9_SEEN=1; label="the Pixel 9 ($P9, Oliver's daily phone)" ;;
    var) label="a device whose serial comes from a variable ($s)" ;;
    *) label="unknown device $s" ;;
  esac
  if [[ $cls == p9 ]] && p9_allowed "$sub" "${rest[@]}"; then PART_OK=1; return; fi

  local deny_msg="adb $sub on $label can change data, packages or settings; not allowed (CLAUDE.md Devices)."
  local ask_msg="adb $sub on $label is not on the read-only allow list; confirm."
  case $sub in
    uninstall|push|sync|root|unroot|remount|reboot|reboot-bootloader|disable-verity|enable-verity|sideload|restore|tcpip|usb|emu)
      decide deny adb-protected-write "$deny_msg" ;;
    install|install-multiple|install-multi-package)
      if has_user0 "${rest[@]}"; then decide ask adb-protected-install "$ask_msg"
      else decide deny adb-protected-write "adb $sub on $label without --user 0 installs for every user; not allowed (CLAUDE.md Devices)."; fi ;;
    logcat)
      if logcat_writes "${rest[@]}"; then decide deny adb-protected-write "$deny_msg"
      else decide ask adb-protected-other "$ask_msg"; fi ;;
    shell|exec-out)
      shell_rest "${rest[@]}"
      decide ask adb-protected-other "$ask_msg"
      if [[ -n $REM ]]; then run_text "$REM" $((depth + 1)) "remote:$cls|$label"; fi ;;
    *) decide ask adb-protected-other "$ask_msg" ;;
  esac
}

# A command running inside adb shell on a protected device (ctx "remote:<class>|<label>").
analyze_remote() {
  local label=${ctx#*|} k op tg name
  local deny_msg ask_msg
  for ((k = 0; k < ${#R[@]}; k += 2)); do
    op=${R[k]} tg=${R[k+1]}
    case $op in
      *'<<'*|*'<'|*'<&') ;;
      *) [[ $op == *'>&' || $tg == /dev/null ]] && continue
         decide deny adb-protected-write "writes $tg on $label; not allowed (CLAUDE.md Devices)." ;;
    esac
  done
  strip_prefix
  unwrap
  (( ${#A[@]} )) || return 0
  name=${A[0]##*/}
  deny_msg="adb shell $name on $label can change data, packages or settings; not allowed (CLAUDE.md Devices)."
  ask_msg="adb shell $name on $label is not on the read-only allow list; confirm."
  case $name in
    sh|bash|mksh|ash)
      for ((k = 1; k < ${#A[@]}; k++)); do
        case ${A[k]} in -*c*) run_text "${A[k+1]-}" $((depth + 1)) "$ctx"; return ;; -*) ;; *) break ;; esac
      done
      decide ask adb-protected-other "$ask_msg" ;;
    su|pm|cmd|rm|rmdir|mv|cp|dd|run-as|settings|input|sendevent|setprop|svc|wm|appops|bmgr|dpm|\
    locksettings|reboot|sm|device_config|sqlite3|monkey|chmod|chown|chcon|touch|mkdir|ln|truncate|tee|\
    wipe|vdc|ime|telecom|restorecon|setenforce|start|stop|uiautomator|screenrecord)
      decide deny adb-protected-write "$deny_msg" ;;
    screencap)  # with a file argument it writes to the device; to stdout it only reads
      local x f=0
      for x in "${A[@]:1}"; do [[ $x != -* ]] && f=1; done
      if (( f )); then decide deny adb-protected-write "$deny_msg"; else decide ask adb-protected-other "$ask_msg"; fi ;;
    am)
      case ${A[1]-} in
        start|start-activity|startservice|start-service|monitor|get-*|stack|task|help|"") decide ask adb-protected-other "$ask_msg" ;;
        *) decide deny adb-protected-write "adb shell am ${A[1]} on $label changes app or system state; not allowed (CLAUDE.md Devices)." ;;
      esac ;;
    content)
      if [[ ${A[1]-} == query ]]; then decide ask adb-protected-other "$ask_msg"
      else decide deny adb-protected-write "$deny_msg"; fi ;;
    logcat)
      if logcat_writes "${A[@]:1}"; then decide deny adb-protected-write "$deny_msg"
      else decide ask adb-protected-other "$ask_msg"; fi ;;
    *) decide ask adb-protected-other "$ask_msg" ;;
  esac
}

# ---------------------------------------------------------------- gradle

analyze_gradle() {
  local a skip=0 dev="" ser="" t
  for a in "$@"; do
    if (( skip )); then skip=0; continue; fi
    case $a in
      -x|--exclude-task|-p|--project-dir|-b|--build-file|-c|--settings-file|-g|--gradle-user-home|-I|--init-script|--console|--warning-mode|--priority|--include-build) skip=1 ;;
      -*) ;;
      *) t=${a##*:}; case $t in install*|uninstall*|connected*) dev=$a ;; esac ;;
    esac
  done
  [[ -z $dev ]] && return
  if [[ -n $DEVICES_PROBLEM ]]; then
    decide deny devices-file "Gradle task $dev targets a device, but $DEVICES_PROBLEM: create .claude/devices.local, see CLAUDE.md."
    return
  fi
  for a in "${E[@]}"; do [[ $a == ANDROID_SERIAL=* ]] && ser=${a#*=}; done
  [[ -z $ser ]] && ser=$EXPORTED_SERIAL
  if [[ $ser != "$P8" ]]; then
    decide deny gradle-device-task "Gradle task $dev talks to a device; prefix ANDROID_SERIAL=$P8 in the same command (CLAUDE.md Devices)."
  fi
}

# ---------------------------------------------------------------- main

IFS= read -r -d '' INPUT || true
command -v jq >/dev/null 2>&1 || fail "jq is not installed or not on PATH"
TOOL="" CWD="" CMD="" FPATH="" NEWTEXT=""
VARS=$(jq -r '@sh "TOOL=\(.tool_name // "") CWD=\(.cwd // "") CMD=\(.tool_input.command // "") FPATH=\(.tool_input.file_path // .tool_input.notebook_path // "") NEWTEXT=\([.tool_input.content, .tool_input.new_string, .tool_input.new_source, (.tool_input.edits // [] | .[]? | .new_string)] | map(select(type == "string")) | join("\n"))"' <<< "$INPUT") ||
  fail "could not parse the hook input as JSON"
eval "$VARS"
[[ -z $CWD ]] && CWD=$PWD
SIM_DIR=$CWD
SUBJECT=""

case $TOOL in
  Bash)
    SUBJECT=$CMD
    tok "$CMD" 0
    for rec in "${TOK_OUT[@]}"; do analyze_rec "$rec" 0 local; done
    if (( TOO_DEEP )); then decide ask parse-too-deep "command nests deeper than the guard inspects; confirm."; fi
    if [[ -z $DEC ]] && (( P9_SEEN )); then
      if (( P9_SAFE )); then
        DEC=allow DEC_RULE=pixel9-readonly DEC_REASON="read-only Pixel 9 command on the allow list"
      else
        decide ask pixel9-mixed "the Pixel 9 command is chained with other commands; only the read-only allow list runs without asking."
      fi
    fi ;;
  Edit|Write|MultiEdit|NotebookEdit)
    SUBJECT="$TOOL $FPATH"
    p=$FPATH
    [[ $p != /* ]] && p=$CWD/$p
    case $p in
      */.claude/hooks/*|*/.claude/settings*.json|*/.claude/devices.local)
        decide ask edit-claude-config "$TOOL changes $FPATH (Claude settings or guard hook); confirm." ;;
    esac
    case $p in
      *.gradle|*.gradle.kts)
        if [[ $NEWTEXT == *signingConfig* ]]; then
          decide ask edit-gradle-signing "$TOOL adds signingConfig to $FPATH; signing changes need Oliver."
        fi ;;
    esac ;;
esac

if [[ -n $DEC ]]; then
  if [[ $DEC != allow ]]; then
    snippet=${SUBJECT:0:200}
    snippet=${snippet//$'\n'/\\n}
    printf '%(%Y-%m-%dT%H:%M:%S%z)T\t%s\t%s\t%s\n' -1 "$DEC" "$DEC_RULE" "$snippet" >> "$LOG" 2>/dev/null || true
  fi
  jq -n --arg d "$DEC" --arg r "[guard: $DEC_RULE] $DEC_REASON" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: $d, permissionDecisionReason: $r}}' ||
    fail "could not write the decision"
fi
exit 0
