#!/usr/bin/env bash
# Feeds sample PreToolUse JSON to .claude/hooks/guard.sh and checks each decision.
# Usage: bash scripts/test-hook.sh   — one line per case, exits non-zero on any failure.
# Decisions: none (no output), allow, ask, deny, block (exit 2).
# Needs jq and git. Touches no device and nothing outside a temp dir.
# Uses fake serials from a temp devices file; it never reads the real .claude/devices.local.

set -u
ROOT=$(cd "$(dirname "$0")/.." && pwd)
HOOK=$ROOT/.claude/hooks/guard.sh
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
export GUARD_LOG=$TMP/hook.log
P8=FAKE0TEST0SERIAL
P9=FAKE0DAILY0SERIAL
export GUARD_DEVICES_FILE=$TMP/devices.local
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\n' "$P8" "$P9" > "$GUARD_DEVICES_FILE"
DBG=com.techtrest.privamatic.debug
APK=app/build/outputs/apk/debug/app-debug.apk
pass=0 fail=0

# Throwaway repos: MREPO has master checked out, FREPO a feature branch.
G=(git -c user.name=test -c user.email=test@example.invalid -c init.defaultBranch=master)
MREPO=$TMP/master-repo
FREPO=$TMP/feature-repo
"${G[@]}" init -q "$MREPO"
"${G[@]}" -C "$MREPO" commit -q --allow-empty -m init
"${G[@]}" -C "$MREPO" branch feature
"${G[@]}" clone -q "$MREPO" "$FREPO" 2>/dev/null
"${G[@]}" -C "$FREPO" switch -q -c feature

# run JSON [ENV...]: prints the hook's decision.
run() {
  local json=$1 out rc
  shift
  out=$(printf '%s' "$json" | env "$@" "$BASH" "$HOOK" 2>"$TMP/err"); rc=$?
  if (( rc == 2 )); then echo block; return; fi
  if (( rc != 0 )); then echo "exit$rc"; return; fi
  if [[ -z $out ]]; then echo none; return; fi
  jq -r '.hookSpecificOutput.permissionDecision // "invalid"' <<< "$out" 2>/dev/null || echo invalid
}

check() {  # check EXPECTED GOT LABEL
  local label=${3//$'\n'/⏎}
  (( ${#label} > 100 )) && label="${label:0:97}..."
  if [[ $2 == "$1" ]]; then
    ((pass++)); printf 'PASS  %-5s  %s\n' "$1" "$label"
  else
    ((fail++)); printf 'FAIL  want %-5s got %-5s  %s\n' "$1" "$2" "$label"
  fi
}

bash_json() { jq -n --arg c "$1" --arg d "$2" '{hook_event_name: "PreToolUse", tool_name: "Bash", cwd: $d, tool_input: {command: $c}}'; }

b() {  # b EXPECTED COMMAND [CWD]
  check "$1" "$(run "$(bash_json "$2" "${3:-$ROOT}")")" "$2"
}

e() {  # e EXPECTED TOOL PATH [NEW_TEXT]
  local json
  case $2 in
    Write) json=$(jq -n --arg p "$3" --arg t "${4:-x}" '{tool_name: "Write", cwd: "'"$ROOT"'", tool_input: {file_path: $p, content: $t}}') ;;
    Edit) json=$(jq -n --arg p "$3" --arg t "${4:-x}" '{tool_name: "Edit", cwd: "'"$ROOT"'", tool_input: {file_path: $p, old_string: "a", new_string: $t}}') ;;
    MultiEdit) json=$(jq -n --arg p "$3" --arg t "${4:-x}" '{tool_name: "MultiEdit", cwd: "'"$ROOT"'", tool_input: {file_path: $p, edits: [{old_string: "a", new_string: "b"}, {old_string: "c", new_string: $t}]}}') ;;
    NotebookEdit) json=$(jq -n --arg p "$3" --arg t "${4:-x}" '{tool_name: "NotebookEdit", cwd: "'"$ROOT"'", tool_input: {notebook_path: $p, new_source: $t}}') ;;
  esac
  check "$1" "$(run "$json")" "$2 $3${4:+ [${4:0:40}]}"
}

echo "== git push"
b deny 'git push'
b deny 'git push --dry-run origin master'
b deny 'git -C . push'
b deny 'bash -c "git push"'
b deny "sh -c 'git push origin HEAD'"
b deny 'cd app && git push'
b deny 'FOO=1 git push'
b deny '/usr/bin/git push'
b deny 'git status; git push'
b deny 'git status || git push origin master'
b deny 'git status | git push'
b deny 'echo $(git push)'
b deny 'echo "result: `git push`"'
b deny '(git push)'
b deny '{ git push; }'
b deny 'if true; then git push; fi'
b deny 'command git push'
b deny 'env GIT_TRACE=1 git push'
b deny 'nohup git push &'
b deny 'timeout 10 git push'
b deny 'exec git push'
b deny 'git -c core.pager=cat --no-pager push'
b deny 'g""it push'
b deny '\git push'
b deny "\$'\\x67it' push"
b deny 'eval "git push"'
b deny 'x=git; $x push'
b deny 'git -c alias.yolo=push yolo'
b deny 'git config alias.yolo push && git yolo'
b deny 'echo origin | xargs git push'
b deny 'find . -maxdepth 0 -exec git push \;'
b deny 'bash -c "bash -c \"git push\""'
b deny $'bash <<\'EOF\'\ngit status\ngit push origin master\nEOF'
b none $'cat > notes.txt <<\'EOF\'\nOliver\'s call: build the default.\nNothing risky in here.\nEOF'
b none 'git status && git log --oneline -5 && git diff master...HEAD'
b none 'git commit -m "docs: never git push from Claude"' "$FREPO"

echo "== git tag"
b none 'git tag'
b none 'git tag -l'
b none "git tag --list 'v1.*'"
b none 'git tag --contains f8221e1'
b none 'git tag -n'
b deny 'git tag v9.9.9'
b deny 'git tag -a v9.9.9 -m "release"'
b deny 'git tag -d v1.5.1'
b deny 'git -C . tag -f v1.5.1'
b deny 'git tag --delete v1.5.1'

echo "== git clean"
b deny 'git clean -f'
b deny 'git clean -fdx'
b deny 'git clean -d -x -f'
b deny 'git clean --force'
b deny 'git -c clean.requireForce=false clean -d'
b none 'git clean -n'
b none 'git clean -nd'

echo "== git reset --hard / commit on master"
b deny 'git reset --hard HEAD' "$MREPO"
b none 'git reset --hard HEAD' "$FREPO"
b none 'git reset --soft HEAD' "$MREPO"
b deny 'git switch master && git reset --hard' "$FREPO"
b deny 'git commit -m x' "$MREPO"
b deny 'git commit --amend --no-edit' "$MREPO"
b deny "git -C \"$MREPO\" commit -m x"
b deny "cd \"$MREPO\" && git commit -m x"
b none 'git commit -m x' "$FREPO"
b deny 'git checkout master && git commit -m x' "$FREPO"
b none 'git switch -c fix/x && git commit -m x' "$MREPO"
b none 'git merge --no-ff feature' "$MREPO"
b deny 'git merge --continue' "$MREPO"

echo "== git switch / checkout (needs c837e59)"
b none 'git switch master'
b none 'git checkout master'
b none 'git checkout -b feat/new'
b none 'git switch -c feat/new master'
b deny 'git checkout 549e47b'
b deny 'git switch --detach 549e47b'
b deny 'git switch -c old 549e47b'
b deny 'git checkout -b old 549e47b'
b deny 'git -C . switch -C old c837e59^1'
b deny 'git switch fix/privacy-tip-selection'
b deny 'git checkout --orphan empty'
b none 'git checkout -- STATE.md'
b none 'git checkout 549e47b -- STATE.md'
b none 'git restore STATE.md'
b none 'git checkout no-such-file.txt'
b ask  'git switch no-such-branch-xyz'
b ask  'git branch -D old'
b ask  'git branch -d -f old'
b none 'git branch -d merged'

echo "== gh"
b deny 'gh pr create --fill'
b deny 'gh issue comment 12 -b thanks'
b deny 'gh -R techtrest/privamatic issue close 5'
b deny 'gh pr merge 3 --squash'
b deny 'gh release create v1.5.2'
b deny 'gh api repos/techtrest/privamatic/issues'
b deny 'gh repo edit --description x'
b deny 'gh auth token'
b none 'gh issue view 12'
b none 'gh pr list --state open'
b none 'gh pr diff 3'
b none 'gh repo view'
b ask  'gh co 12'

echo "== ssh / scp / rsync / sftp / sudo"
b deny 'ssh fdroid.example.org'
b deny 'scp app.apk host:/tmp/'
b deny 'rsync -a build/ backup/'
b deny 'sftp host'
b deny 'echo hi && sudo -n true'

echo "== adb without -s"
b deny 'adb shell pm list packages'
b deny "adb install $APK"
b deny "ANDROID_SERIAL=$P8 adb shell ls"
b none 'adb devices'
b none 'adb devices -l'
b none 'adb start-server'
b none 'adb kill-server'
b none 'adb version'

echo "== adb: Pixel 8 (no decision)"
b none "adb -s $P8 install -r $APK"
b none "adb -s $P8 shell pm clear $DBG"
b none "adb -s $P8 shell am force-stop $DBG && adb -s $P8 shell am start -n $DBG/com.techtrest.privamatic.MainActivity"

echo "== adb: Pixel 9 and other serials, deny"
b deny "adb -s $P9 shell \"pm clear $DBG\""
b deny "adb -s $P9 shell pm clear $DBG"
b deny "adb -s $P9 uninstall com.techtrest.privamatic"
b deny "adb -s $P9 shell rm /sdcard/x"
b deny "adb -s $P9 shell run-as $DBG ls"
b deny "adb -s $P9 shell settings put global x 1"
b deny "adb -s $P9 shell input tap 1 1"
b deny "adb -s $P9 shell am clear-debug-app"
b deny "adb -s $P9 push x /sdcard/"
b deny "adb -s $P9 install-multiple a.apk b.apk"
b deny "adb -s $P9 install -r $APK"
b deny "adb -s $P9 shell cmd package list packages"
b deny "adb -s $P9 shell 'ls; rm -rf /sdcard/x'"
b deny "adb -s $P9 shell sh -c \"pm clear $DBG\""
b deny "adb -s $P9 shell \"getprop > /sdcard/x\""
b deny "adb -s $P9 logcat -c"
b deny 'adb -s $D shell rm x'
b deny 'adb -s ${X} shell pm clear com.example'
b deny 'adb -s "$(cat serial.txt)" uninstall com.example'
b deny 'adb -s emulator-5554 shell pm clear com.example'

echo "== adb: Pixel 9 and other serials, ask"
b ask  "adb -s $P9 install -r -t --user 0 $APK"
b ask  "adb -s $P9 shell ls /sdcard"
b ask  "adb -s $P9 shell"
b ask  "adb -s $P9 pull /sdcard/x.txt"
b ask  "adb -s $P9 shell dumpsys package com.techtrest.privamatic"
b ask  "adb -s $P9 shell dumpsys package $DBG > /tmp/p9.txt"
b ask  "adb -s $P9 exec-out screencap -p > shot.txt"
b deny "adb -s $P9 shell screencap /sdcard/shot.png"
b ask  "adb -s $P9 logcat -d"
b ask  'adb -s $D shell getprop'
b ask  'adb -s emulator-5554 shell ls'

echo "== adb: Pixel 9 read-only allow list"
b allow "adb -s $P9 exec-out screencap -p > docs/screens/p9.png"
b allow "adb -s $P9 shell getprop"
b allow "adb -s $P9 shell getprop ro.build.version.release"
b allow "adb -s $P9 shell dumpsys package $DBG | grep -E 'versionName|versionCode'"
b allow "adb -s $P9 shell pidof $DBG"
b allow "adb -s $P9 logcat -d --pid=\$(adb -s $P9 shell pidof $DBG) | tail -n 200"
b allow "adb -s $P9 shell 'getprop ro.product.model | head -1'"
b ask   "adb -s $P9 shell getprop && rm -rf build"
b ask   "adb -s $P9 shell getprop ro.build.id; cat /etc/hostname"
b deny  "adb -s $P9 shell getprop; adb -s $P9 shell pm clear $DBG"
b deny  "adb -s $P9 exec-out screencap -p > p9.png && git push"

echo "== Gradle device tasks"
b deny './gradlew :app:installDebug'
b deny './gradlew connectedAndroidTest'
b deny './gradlew uninstallAll'
b deny "ANDROID_SERIAL=$P9 ./gradlew installDebug"
b none "ANDROID_SERIAL=$P8 ./gradlew :app:installDebug"
b none "export ANDROID_SERIAL=$P8 && ./gradlew installDebug"
b none 'JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew :app:assembleDebug'

echo "== secrets"
b deny 'cat release.jks'
b deny 'xxd app/upload.keystore | head'
b deny 'base64 < keys/release.jks'
b deny 'cp ~/keys/privamatic.jks /tmp/'
b deny 'strings x.KEYSTORE'
b deny 'keytool -list -v -keystore x.jks'
b deny 'printenv'
b deny 'printenv GITHUB_TOKEN'
b deny 'env'
b deny 'env | grep -i java'
b deny 'echo $API_KEY'
b deny "printf '%s' \"\${STORE_PASSWORD}\""
b deny 'cat /proc/self/environ'
b none 'ls *.jks'
b none "find . -name '*.jks'"
b none 'printenv JAVA_HOME'
b none 'echo $HOME'
b none 'env JAVA_HOME=/x ./gradlew :app:compileDebugKotlin'

echo "== Bash writes to Claude settings / hooks"
b ask  "echo '{}' > .claude/settings.json"
b ask  'cp /tmp/x .claude/settings.local.json'
b ask  "sed -i 's/a/b/' .claude/settings.local.json"
b ask  'jq . x.json | tee .claude/settings.json'
b ask  'rm .claude/hooks/guard.sh'
b ask  'mv .claude/hooks/guard.sh /tmp/'
b ask  'chmod -x .claude/hooks/guard.sh'
b ask  'rm -rf .claude'
b ask  'git checkout -- .claude/settings.json'
b ask  "python3 -c \"open('.claude/settings.json','w')\""
b ask  'cd .claude && rm settings.json'
b none 'cat .claude/settings.json'
b none 'jq . .claude/settings.local.json'
b none 'sed -n 1,5p .claude/settings.json'

echo "== Edit / Write / MultiEdit / NotebookEdit"
e ask  Write ".claude/settings.json"
e ask  Edit "$ROOT/.claude/settings.local.json"
e ask  Write "$ROOT/.claude/hooks/guard.sh"
e ask  MultiEdit "$ROOT/.claude/hooks/other.sh"
e ask  NotebookEdit "$ROOT/.claude/hooks/x.ipynb"
e ask  Edit "$ROOT/app/build.gradle.kts" 'signingConfig = signingConfigs.getByName("release")'
e ask  MultiEdit "$ROOT/app/build.gradle.kts" 'signingConfig signingConfigs.release'
e none Edit "$ROOT/app/build.gradle.kts" 'versionCode = 10'
e none Write "$ROOT/STATE.md" 'signingConfig is mentioned in prose here'
e none Write "$ROOT/.claude/agents/verify.md"
check none "$(run '{"tool_name":"Read","tool_input":{"file_path":".claude/settings.json"}}')" "Read .claude/settings.json"

echo "== silence, nesting and speed"
long=$'cat > big.txt <<EOF\n'
for ((k = 0; k < 2000; k++)); do long+="line $k: plain text with 'quotes', \"more\" and \$(echo sub)"$'\n'; done
long+='EOF'
start=$(date +%s%N)
b none "$long"
ms=$(( ($(date +%s%N) - start) / 1000000 ))
check fast "$( (( ms < 5000 )) && echo fast || echo "${ms}ms")" "2000-line heredoc took ${ms} ms"
b none 'for f in *.kt; do echo "$f"; done | sort | uniq -c'
nested=ls
for ((k = 0; k < 10; k++)); do printf -v nested 'bash -c %q' "$nested"; done
b ask  "$nested"

echo "== hook covers the deny entries dropped from settings.json (git tag, branch -D, commit --amend)"
b deny 'git tag v9'
b none 'git tag -l'
b ask  'git branch -D x'
b deny 'git commit --amend' "$MREPO"
b none 'git commit --amend' "$FREPO"
if [[ -f $ROOT/.claude/settings.json ]]; then
  for rule in 'Bash(git tag:*)' 'Bash(git branch -D:*)' 'Bash(git commit --amend:*)'; do
    check absent "$(jq -r --arg r "$rule" 'if (.permissions.deny | index($r)) then "present" else "absent" end' "$ROOT/.claude/settings.json")" "settings.json deny list has no $rule"
  done
fi

echo "== hook covers the Pixel 9 adb deny entries dropped from settings.json"
b deny "adb -s $P9 uninstall com.techtrest.privamatic"
b deny "adb -s $P9 shell pm list packages"
b deny "adb -s $P9 shell cmd package list packages"
b deny "adb -s $P9 shell rm /sdcard/x"
b deny "adb -s $P9 shell run-as $DBG ls"
b deny "adb -s $P9 shell settings put global x 1"
b deny "adb -s $P9 shell input tap 1 1"
b deny "adb -s $P9 shell am clear-debug-app"
b deny "adb -s $P9 push x /sdcard/"
if [[ -f $ROOT/.claude/settings.json ]]; then
  check 0 "$(jq '[.permissions.deny[] | select(startswith("Bash(adb -s"))] | length' "$ROOT/.claude/settings.json")" "settings.json deny list names no serial"
fi

echo "== devices file: fail closed"
# dev EXPECTED COMMAND DEVICES_FILE: runs COMMAND with that devices file.
dev() { check "$1" "$(run "$(bash_json "$2" "$ROOT")" GUARD_DEVICES_FILE="$3")" "[${3##*/}] $2"; }
DF=$TMP/df
mkdir -p "$DF"
: > "$DF/empty-file"
printf 'TEST_DEVICE_SERIAL=\nDAILY_PHONE_SERIAL=%s\n' "$P9" > "$DF/empty-test"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=\n' "$P8" > "$DF/empty-daily"
printf 'TEST_DEVICE_SERIAL=%s\n' "$P8" > "$DF/no-daily-line"
printf 'TEST_DEVICE_SERIAL="   "\nDAILY_PHONE_SERIAL=%s\n' "$P9" > "$DF/blank-test"
printf 'TEST_DEVICE_SERIAL=<serial>\nDAILY_PHONE_SERIAL=%s\n' "$P9" > "$DF/placeholder-test"
printf 'TEST_DEVICE_SERIAL=$(id)\nDAILY_PHONE_SERIAL=%s\n' "$P9" > "$DF/command-test"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\n' "$P8" "$P8" > "$DF/same-twice"
for f in "$TMP/no-such-file" "$DF" "$DF/empty-file" "$DF/empty-test" "$DF/empty-daily" "$DF/no-daily-line" "$DF/blank-test" "$DF/placeholder-test" "$DF/command-test" "$DF/same-twice"; do
  dev deny "adb -s $P8 install -r $APK" "$f"
  dev deny "adb -s $P8 shell getprop" "$f"
  dev deny "adb -s $P9 shell getprop" "$f"
  dev deny "adb -s $P9 exec-out screencap -p > p9.png" "$f"
  dev deny 'adb -s emulator-5554 shell ls' "$f"
  dev deny 'adb -s "" shell ls' "$f"
  dev deny 'adb -s $D shell getprop' "$f"
  dev deny 'adb shell ls' "$f"
  dev deny './gradlew :app:installDebug' "$f"
  dev deny "ANDROID_SERIAL=$P8 ./gradlew :app:installDebug" "$f"
  dev deny 'ANDROID_SERIAL= ./gradlew connectedAndroidTest' "$f"
  dev none 'adb devices' "$f"
  dev none 'adb devices -l' "$f"
  dev none 'adb start-server' "$f"
  dev none 'adb kill-server' "$f"
  dev none 'adb version' "$f"
  dev none './gradlew :app:compileDebugKotlin' "$f"
  dev none 'git status' "$f"
done
dev deny 'git push' "$TMP/no-such-file"
why=$(printf '%s' "$(bash_json "adb -s $P8 shell ls" "$ROOT")" | GUARD_DEVICES_FILE=$TMP/no-such-file "$BASH" "$HOOK" | jq -r '.hookSpecificOutput.permissionDecisionReason')
check yes "$([[ $why == *'create .claude/devices.local, see CLAUDE.md'* ]] && echo yes || echo no)" "deny reason says to create the file"

echo "== devices file: accepted forms"
printf '# comment\n\nexport TEST_DEVICE_SERIAL="%s"  # Pixel 8\nDAILY_PHONE_SERIAL='"'"'%s'"'"'\r\nOTHER=1\n' "$P8" "$P9" > "$DF/forms"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s' "$P8" "$P9" > "$DF/no-final-newline"
for f in "$DF/forms" "$DF/no-final-newline"; do
  dev none "adb -s $P8 shell pm clear $DBG" "$f"
  dev deny "adb -s $P9 shell pm clear $DBG" "$f"
  dev allow "adb -s $P9 shell getprop" "$f"
  dev none "ANDROID_SERIAL=$P8 ./gradlew :app:installDebug" "$f"
done

echo "== devices file: default path next to the hook; a command prefix can't redirect it"
mkdir -p "$TMP/proj/.claude/hooks"
cp "$HOOK" "$TMP/proj/.claude/hooks/guard.sh"
real_hook=$HOOK
HOOK=$TMP/proj/.claude/hooks/guard.sh
sibling() { check "$1" "$(run "$(bash_json "$2" "$ROOT")" -u GUARD_DEVICES_FILE)" "[sibling $3] $2"; }
sibling deny "adb -s $P8 shell ls" "absent"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\n' "$P8" "$P9" > "$TMP/proj/.claude/devices.local"
sibling none "adb -s $P8 shell ls" "present"
sibling deny "adb -s $P9 shell pm clear $DBG" "present"
HOOK=$real_hook
b deny "GUARD_DEVICES_FILE=/tmp/x adb -s $P9 shell pm clear $DBG"
b deny "export GUARD_DEVICES_FILE=/tmp/x; adb -s $P9 shell pm clear $DBG"

echo "== legacy test device (S8)"
LS=FAKE0LEGACY0SERIAL
LF=$TMP/legacy-set
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\nLEGACY_TEST_SERIAL=%s\n' "$P8" "$P9" "$LS" > "$LF"
# Free like the Pixel 8.
for c in "adb -s $LS install -r $APK" "adb -s $LS uninstall $DBG" "adb -s $LS push x /sdcard/" \
         "adb -s $LS shell pm clear $DBG" "adb -s $LS shell am force-stop $DBG" \
         "adb -s $LS shell am start -n $DBG/com.techtrest.privamatic.MainActivity" \
         "adb -s $LS shell input tap 1 1" "adb -s $LS shell uiautomator dump /sdcard/ui.xml" \
         "adb -s $LS shell settings put global x 1" "adb -s $LS shell dumpsys alarm" \
         "adb -s $LS logcat -c -b crash" "adb -s $LS exec-out screencap -p > shot.png" \
         "ANDROID_SERIAL=$LS ./gradlew :app:installDebug" "ANDROID_SERIAL=$LS ./gradlew connectedDebugAndroidTest" \
         "export ANDROID_SERIAL=$LS && ./gradlew installDebug" \
         "adb -s $LS shell am force-stop $DBG && adb -s $P8 shell am force-stop $DBG"; do
  dev none "$c" "$LF"
done
# Setting it changes nothing for the others.
dev none "adb -s $P8 install -r $APK" "$LF"
dev none "adb -s $P8 shell pm clear $DBG" "$LF"
dev none "ANDROID_SERIAL=$P8 ./gradlew installDebug" "$LF"
dev deny "adb -s $P9 shell pm clear $DBG" "$LF"
dev deny "adb -s $P9 uninstall com.techtrest.privamatic" "$LF"
dev deny "adb -s $P9 install -r $APK" "$LF"
dev ask  "adb -s $P9 install -r -t --user 0 $APK" "$LF"
dev ask  "adb -s $P9 shell ls /sdcard" "$LF"
dev allow "adb -s $P9 shell getprop" "$LF"
dev allow "adb -s $P9 exec-out screencap -p > p9.png" "$LF"
dev deny "adb -s $LS shell ls && adb -s $P9 shell pm clear $DBG" "$LF"
dev deny "adb -s OTHER0SERIAL shell pm clear $DBG" "$LF"
dev ask  "adb -s OTHER0SERIAL shell ls" "$LF"
dev deny "ANDROID_SERIAL=$P9 ./gradlew installDebug" "$LF"
dev deny "ANDROID_SERIAL=OTHER0SERIAL ./gradlew installDebug" "$LF"
dev deny './gradlew installDebug' "$LF"
dev deny "adb shell pm list packages" "$LF"
# Missing, empty or invalid: only the S8 turns into an unknown device.
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\n' "$P8" "$P9" > "$TMP/legacy-missing"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\nLEGACY_TEST_SERIAL=\n' "$P8" "$P9" > "$TMP/legacy-empty"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\nLEGACY_TEST_SERIAL=not a serial\n' "$P8" "$P9" > "$TMP/legacy-invalid"
for f in legacy-missing legacy-empty legacy-invalid; do
  dev deny "adb -s $LS shell pm clear $DBG" "$TMP/$f"
  dev deny "adb -s $LS shell input tap 1 1" "$TMP/$f"
  dev deny "adb -s $LS install -r $APK" "$TMP/$f"
  dev ask  "adb -s $LS shell ls" "$TMP/$f"
  dev ask  "adb -s $LS shell getprop" "$TMP/$f"
  dev ask  'adb -s "" shell ls' "$TMP/$f"
  dev deny "ANDROID_SERIAL=$LS ./gradlew installDebug" "$TMP/$f"
  dev deny 'ANDROID_SERIAL= ./gradlew installDebug' "$TMP/$f"
  dev deny './gradlew installDebug' "$TMP/$f"
  dev none "adb -s $P8 install -r $APK" "$TMP/$f"
  dev none "adb -s $P8 shell pm clear $DBG" "$TMP/$f"
  dev none "ANDROID_SERIAL=$P8 ./gradlew installDebug" "$TMP/$f"
  dev deny "adb -s $P9 shell pm clear $DBG" "$TMP/$f"
  dev ask  "adb -s $P9 shell ls /sdcard" "$TMP/$f"
  dev allow "adb -s $P9 shell getprop" "$TMP/$f"
  dev none 'adb devices -l' "$TMP/$f"
done
# The same as another serial: every device command denies.
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\nLEGACY_TEST_SERIAL=%s\n' "$P8" "$P9" "$P9" > "$TMP/legacy-same-daily"
printf 'TEST_DEVICE_SERIAL=%s\nDAILY_PHONE_SERIAL=%s\nLEGACY_TEST_SERIAL=%s\n' "$P8" "$P9" "$P8" > "$TMP/legacy-same-test"
for f in legacy-same-daily legacy-same-test; do
  dev deny "adb -s $P8 shell ls" "$TMP/$f"
  dev deny "adb -s $P8 install -r $APK" "$TMP/$f"
  dev deny "adb -s $P9 shell getprop" "$TMP/$f"
  dev deny "adb -s $P9 exec-out screencap -p > p9.png" "$TMP/$f"
  dev deny "adb -s $LS shell ls" "$TMP/$f"
  dev deny "adb -s OTHER0SERIAL shell ls" "$TMP/$f"
  dev deny "ANDROID_SERIAL=$P8 ./gradlew installDebug" "$TMP/$f"
  dev deny 'adb shell ls' "$TMP/$f"
  dev none 'adb devices -l' "$TMP/$f"
done
why=$(printf '%s' "$(bash_json "adb -s $P8 shell ls" "$ROOT")" | GUARD_DEVICES_FILE=$TMP/legacy-same-daily "$BASH" "$HOOK" | jq -r '.hookSpecificOutput.permissionDecisionReason')
check yes "$([[ $why == *'LEGACY_TEST_SERIAL is the same as another serial'* ]] && echo yes || echo no)" "deny reason names the legacy serial"

echo "== devices file: writes need confirmation, reads don't"
b ask  "echo 'TEST_DEVICE_SERIAL=x' > .claude/devices.local"
b ask  "echo x >> .claude/devices.local"
b ask  "sed -i 's/a/b/' .claude/devices.local"
b ask  'cp /tmp/x .claude/devices.local'
b ask  'mv .claude/devices.local /tmp/'
b ask  'rm .claude/devices.local'
b ask  'jq . x.json | tee .claude/devices.local'
b ask  "python3 -c \"open('.claude/devices.local','w')\""
e ask  Write ".claude/devices.local"
e ask  Edit "$ROOT/.claude/devices.local"
b none 'cat .claude/devices.local'
b none 'git check-ignore .claude/devices.local'

echo "== fail closed"
check block "$(run "$(bash_json 'git status' "$ROOT")" PATH=/nonexistent)" "jq not on PATH -> exit 2"
check block "$(run '{"tool_name": "Bash", "tool_input": {"command": ')" "malformed JSON -> exit 2"
if [[ -f $ROOT/.claude/settings.json ]]; then
  hookcmd=$(jq -r '.hooks.PreToolUse[] | select(.matcher == "Bash") | .hooks[0].command' "$ROOT/.claude/settings.json")
  mkdir -p "$TMP/empty-project"
  rc=0; printf '%s' "$(bash_json 'git status' "$ROOT")" | CLAUDE_PROJECT_DIR=$TMP/empty-project sh -c "$hookcmd" >/dev/null 2>&1 || rc=$?
  check block "$( (( rc == 2 )) && echo block || echo "exit$rc")" "settings.json hook command, guard.sh missing -> exit 2"
  out=$(printf '%s' "$(bash_json 'git push' "$ROOT")" | CLAUDE_PROJECT_DIR=$ROOT sh -c "$hookcmd" 2>/dev/null | jq -r '.hookSpecificOutput.permissionDecision')
  check deny "$out" "settings.json hook command runs guard.sh (git push)"
else
  echo "SKIP  settings.json hook command (no .claude/settings.json yet)"
fi

echo "== log"
: > "$GUARD_LOG"
run "$(bash_json 'git push' "$ROOT")" >/dev/null
run "$(bash_json 'git branch -D x' "$ROOT")" >/dev/null
run "$(bash_json 'git status' "$ROOT")" >/dev/null
run "$(bash_json "adb -s $P9 shell getprop" "$ROOT")" >/dev/null
lines=$(wc -l < "$GUARD_LOG")
check 2 "$lines" "log has one line per deny/ask and none for allow/no decision"
check yes "$(grep -q $'\tdeny\tgit-push\tgit push$' "$GUARD_LOG" && echo yes || echo no)" "log line: timestamp, decision, rule id, command"

echo
echo "SUMMARY: $pass passed, $fail failed, $((pass + fail)) cases"
(( fail == 0 ))
