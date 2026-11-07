# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.2 (versionCode 10), tagged on `2a95fa3`; live on F-Droid (Oliver, 2026-11-07).
The Pixel 8 runs the 1.5.2 debug build (versionCode 10), What's new already dismissed.
The Claude Code workflow files are merged into `master` with `--no-ff` and pushed. They are:
- CLAUDE.md and STATE.md;
- the narrowed `.claude/settings.local.json` (untracked; the allow list, plus the Pixel 9 adb deny rules since 2026-11-05);
- the inventory.

CONVENTIONS.md is the single rulebook: `.cursorrules` now points to it, and the duplicate docs are deleted.
The guard hook is merged into local `master` and passed its live smoke test (see Next).
`master` now also has a spec template, a `device-verify` subagent and an external-review step (CLAUDE.md), all merged from `fix/backup-exclusions`.
`device-verify` is registered only in sessions started after it was committed. Until then, a general-purpose agent reads its file.
`.claude/settings.local.json` is untracked since `c837e59`, so a `git pull` on the Framework laptop, or a switch to any
pre-`c837e59` branch, overwrites or deletes it. Back it up first. The guard hook now denies such switches.
Device serials are only in the gitignored `.claude/devices.local`, one per machine; tracked files use role names. Old commits keep the numbers (accepted).

## Next

- [ ] [S] Live check of the serials-from-file hook, on the next run with a phone: a Pixel 8 command runs without a prompt, a Pixel 9 `pm clear` is denied.
  So far proven only by feeding JSON to the hook (2026-11-05 run below).
- [ ] [S] Widget update action is open to other apps: `PrivacyWidgetProvider` is exported with `ACTION_UPDATE_WIDGET` (full scan, battery only).
- [ ] [S] Re-arm the 6-hour widget alarm on app start and `MY_PACKAGE_REPLACED`. A force-stop clears both alarms; only the snapshot is
  re-armed (`PackageReplacedReceiver.kt:22`, `MainActivity.kt:26`). Pixel 8: 0 alarms after two force-stops, both back after a reboot;
  three `install -r` in a row kept both, so an update alone doesn't clear them. Whether an F-Droid update force-stops: not checked.
- [ ] [M] 48 h change cap edge: a score change again across midnight keeps the first change's timestamp, so the cap can hide a fresh
  change (`ScoreHistoryRepository.kt:93-97`). Code reading only; failing test first. Risky code (score history).
- [ ] [M] Snapshot alarm time: comments say 23:50, code sets 23:00 (`SnapshotReceiver.kt:69`); the inexact daily alarm has an 18 h
  window on the Pixel 8, so a "midnight" snapshot can land at 17:00 next day. Read-only finding. Risky code (score history).
- [ ] [M] Widget opacity on Android 8–11 does nothing (review round 1 M1, rejected for this release by Oliver). `setImageAlpha` acts on
  the ImageView's image, but the card is its background (also on `master`). Card draws opaque, so readable; fix needs an API 30 emulator.
- [ ] [M] Legacy-rules and real-D2D check (optional): same bmgr cycle on an API 30 emulator, plus one phone-to-phone transfer.
- [ ] [S] device-verify ran its sweep from a scratchpad script, which hides each adb call from the guard hook (commands matched, Pixel 8
  only). Consider adding "run commands inline, never from a script file" to the agent file.
- [ ] [S] device-verify can't restore a named wallpaper: the picker has no recents list. Reapplying the lock screen's photo to Home works
  while the lock screen still has it.

## Blocked on me

- [x] Push `master` (STATE updates and the inbox `STATE:` rule in CLAUDE.md) (done by 2026-11-07: `origin/master` at `dcf6f0b`)
- Create `.claude/devices.local` on the Framework laptop before the next session there, or the hook denies every device command (format: CLAUDE.md, Devices).
  Also copy the 9 Pixel 9 deny rules into that machine's `.claude/settings.local.json`; the file is gitignored, so a pull won't bring them.
- [x] After the F-Droid build is live: check whether to nudge #7 / #11 (fixed in 1.4.1, waiting for confirmation) (done 2026-11-07: no reply since 16 Sep, Oliver closes both with a reopen note)

## Done (moved from Next, 2026-10-25)

- [x] [S] Default Keyboard row strings moved to `strings.xml`; `.gitignore` covers `docs/store/`, `docs/store-graphics/`, `docs/audits/` (2026-10-25, see archive).
- [x] [S] Step 1b — trial fix: Default Launcher row debug text. See archive.
- [x] [S] Same leak in browser, SMS, email rows (2026-10-21, `666a434`).
- [x] [S] `QuickWinsDetector.extractAppName` parsed UI copy; replaced by `PrivacyIssue.appName` (`666a434`).
- [x] [M] Guard hook (`015f699`); see Guard hook below.
- [x] [S] Guard hook live smoke test (2026-10-20): all 11 checks as intended (5 deny, 3 ask, 1 silent allow on the Pixel 9);
  `.claude/hook.log` had 8 lines, one per deny and ask. Closes the workflow-setup "rules match in a live session" item.
  Side effect: `adb devices` starts the adb daemon, so a "first" adb call is never fully read-only on the host.
- [x] [M] Backup-rules fix (`87b6f78`, external review round 1: no findings). The four suspicions:
  - Missing `.xml` was not a bug: `ad_id_prefs.xml` and `score_history.xml` already stayed out of backup (device, before-run).
  - `-shm` / `-wal` moot: the database domain is excluded and Room uses TRUNCATE mode.
  - `<cloud-backup>` and `<device-transfer>` fixed, mirrored, enforced by `BackupRulesTest`.
  - Room was already excluded with `domain="database"` (device, before-run).
- [x] [S] Widget readability design run: `docs/decisions/widget-readability.md`; see archive.
- [x] [L] Widget readability + widget pack (`feat/widget-pack`, merged `3f5a553` 2026-10-24). Rounds 1–2 parked; round 3 gate clean.
  "↓12" at font 2.0 fits 40 dp; `.gitignore` tracks `app/src/release/`; 28 dp sides, 18 sp score, rows aligned at 1.0 and 2.0.
  External review round 1: no High; L1 fixed (`e2093fc`), M1 rejected for this release.
- [x] [S] Release note for the backup change, agreed by Oliver 2026-10-21; use exactly:
  "Privamatic no longer copies any of its data into backups or to a new phone. On a new phone, set up your trusted apps and settings again."
- [x] [S] Koloe17 report closed: resolved by #19 in 1.4.1; no newer report exists (Oliver, 2026-10-25).
- [x] [S] Spec "Out" line corrected: a font-scale change does trigger widget updates (2026-10-24).

## Guard hook

`.claude/hooks/guard.sh` is a PreToolUse hook, registered in the tracked `.claude/settings.json` for `Bash` and for `Edit|Write|MultiEdit|NotebookEdit`.
It is the real guard. The allow and deny lists are a convenience.
- **Silent by default.** It outputs a decision only for the patterns below; anything else (heredocs, long scripts) gets none.
- **It sees through:** `&&`, `||`, `;`, `|`, subshells, `$(...)` and backticks, `bash -c` / `sh -c` / `eval`, quotes, env prefixes,
  `command`/`env`/`exec`/`nohup`/`timeout`/`xargs`, absolute paths, `git -C`/`-c`, git aliases, `find -exec`, heredoc lines and `adb shell "..."` strings.
- **Deny:**
  - git: `push` (any form), tag create or delete, `clean -f`, and `reset --hard` / `commit` / `merge --continue` / `cherry-pick` / `revert` / `am` while `master` is checked out;
  - a switch or checkout to anything without `c837e59` (resolved locally, then as `origin/<x>`), and `--orphan`;
  - gh writes, `ssh`/`scp`/`rsync`/`sftp`, `sudo`/`su`;
  - adb without `-s`, except `devices [-l]`, `start-server`, `kill-server`, `version`;
  - every adb call or Gradle task that targets a device, if `.claude/devices.local` is missing, the Pixel 8 or Pixel 9 serial is empty or invalid, or any two serials are the same;
  - on the Pixel 9, a variable serial or an unknown serial: data/package/settings changes (`pm`, `cmd`, `rm`, `run-as`, `settings`, `input`, `am` writes, `push`, `uninstall`, install without `--user 0`, `logcat -c`, device-side redirects);
  - Gradle `install*`/`uninstall*`/`connected*` without `ANDROID_SERIAL=<test serial or legacy serial>`;
  - keystore reads, `keytool`, and printing `*PASSWORD*`/`*KEY*`/`*SECRET*`/`*TOKEN*` variables or the whole environment.
- **Allow silently:** Pixel 9 `exec-out screencap -p > *.png`, `shell getprop [prop]`, `shell dumpsys package <debug>`,
  `shell pidof <debug>`, `logcat --pid=…`, optionally piped into `grep`/`head`/`tail`/`wc`. This applies only when every part of the command is on that list; otherwise ask.
- **Ask:**
  - any other Pixel 9 or unknown-serial command;
  - `git branch -D`, or a switch target it can't resolve;
  - unknown `gh` subcommands;
  - Bash writes to `.claude/settings*.json`, `.claude/hooks/**` or `.claude/devices.local` (swapping its values would free the Pixel 9);
  - Edit/Write on those paths, or on Gradle files whose new text contains `signingConfig`.
- **Fail closed:** missing `jq`, bad JSON or a script error exit 2, which blocks. The hook command ends in `|| exit 2`, so a missing `guard.sh` blocks too.
- **Log:** every deny and ask goes to `.claude/hook.log` (git-ignored) as timestamp, decision, rule id and the first 200 characters of the command.
- **Test:** `bash scripts/test-hook.sh` prints one line per case and exits non-zero on any failure. It needs no device and uses fake serials from a temp file (`GUARD_DEVICES_FILE`, test only).
- **Serials:** read from `.claude/devices.local`, parsed and never sourced. `LEGACY_TEST_SERIAL` (Samsung S8) is free like the Pixel 8; if it is missing, empty or invalid, only the S8 counts as unknown. The Pixel 9 adb deny rules moved from `settings.json` to the gitignored `settings.local.json` as a second layer, so each machine needs its own copy.
- **Hook contract** (code.claude.com/docs/en/hooks, via Context7, 2026-10-20):
  - **Input:** JSON on stdin with `tool_name`, `tool_input` (`command`, `file_path`, …) and `cwd`.
  - **Output:** `hookSpecificOutput.permissionDecision` is `allow`, `deny`, `ask` or `defer`, with a `permissionDecisionReason`. Precedence is deny > defer > ask > allow.
    Settings deny and ask rules still apply over a hook `allow`.
  - **Exit codes:** JSON is read only on exit 0. Exit 2 blocks, and its stderr goes to Claude. Any other exit code is a non-blocking error, and the call goes ahead; hence the trap and `|| exit 2`.
  - **Loading:** settings files are watched, so hook edits normally apply mid-session. `/hooks` is a read-only browser.
    Hooks run only after the workspace trust dialog is accepted. To be sure, start a new session after changing them.
  - **Auto mode:** a hook `ask` still shows the prompt. The classifier can deny the call but cannot approve it silently.
    Before v2.1.211 it could approve an unsandboxed Bash command. Installed is 2.1.304, so no ask rule needs to become deny.

## Open questions for Oliver

- None open. (Device name at large text: Oliver accepted the ellipsis; added to the spec 2026-10-24.)

## Runs

### 2026-11-07 — legacy test device and widget hardening

- [x] v1.5.2 live on F-Droid (Oliver, 2026-11-07)
- [x] Samsung Galaxy S8 (stock Android 9) is a free legacy test device, like the Pixel 8 (Oliver, 2026-11-07)
- [x] S8 registered (`chore/legacy-test-device`): `LEGACY_TEST_SERIAL` in `devices.local`, `guard.sh`, CLAUDE.md, `device-verify`. `test-hook.sh` 541/541 (was 441). 8 mutations each turn it red.
- [x] Hand-made JSON with the real serials: Pixel 8 and S8 no decision for install, force-stop, input, uiautomator and Gradle install. Pixel 9 unchanged: `pm clear`, uninstall, input, force-stop, plain install and Gradle install deny; `--user 0` install asks; getprop and screencap allow. Unknown serial: `pm clear` deny, `ls` ask.

### 2026-11-05 — inbox `STATE:` lines (`chore/inbox-state-lines`, merged into local `master`)

- [x] CLAUDE.md, Session: rules for `STATE:` lines and `ADD Open decisions: …[blocked]` lines. Rules only: no block with a `STATE:` line has been applied yet.
- Tracker contract support is a separate ROADMAP item, in `~/Documents/project-tracker`.

### 2026-11-05 — second layer and branch cleanup (`chore/state-second-layer`, merged into local `master`)

- [x] `settings.local.json`: the 9 Pixel 9 adb deny rules added, identical to those removed from `settings.json`; allow list unchanged. `git check-ignore` confirms it is ignored; `git grep` for the serials on `master` empty; `test-hook.sh` 441/441.
- [x] Local branches deleted, `master` only left. The other 15 were merged. `design/widget-centre-change` was unmerged, not on the remote, approved by Oliver: tip `86cb2db`, restorable until git prunes it. Remote untouched. Its design docs still name the branch and tip (`docs/decisions/widget-centre-change.md:5`, `docs/screens/widget-centre-change/measurements.md:3`).
- Not verified: Claude Code enforcing these deny rules live; trying one means attempting a write on the Pixel 9. Same rule text as before the move.

The 2026-11-05 private-device-details run: `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
