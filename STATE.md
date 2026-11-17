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
Claude Code runs Bash in a sandbox since 2026-11-10 (2026-11-10 run). Builds are offline from the sandbox Gradle home: after a new dependency, Oliver builds once outside and re-seeds it.
Inside the sandbox `.claude/hooks`, `.claude/agents`, `.claude/skills`, the settings files, `.git/hooks` and `.git/config` are read-only, so a checkout or merge that changes them is Oliver's.

## Next

- [ ] [S] External review round 1 of `fix/widget-refresh-hardening` (Oliver, in Cursor; prompt and diff are on that branch in `docs/reviews/`).
  Then a Claude Code run works through `fix-widget-refresh-hardening-findings-1.md`: prove or reject each finding, fix, re-run the gate, merge.
- [ ] [S] LG G2 as a third test phone: waits for HQ's shared guard engine.
- [ ] [S] Live check of the serials-from-file hook: a Pixel 9 `pm clear` is denied (still open; so far proven only by feeding JSON to the hook, 2026-11-05).
  - [x] Pixel 8 half: 8 literal Pixel 8 adb commands ran without a prompt or refusal, no new `hook.log` line (2026-11-17 device-verify gate).
- [ ] [S] Widget update action is open to other apps: `PrivacyWidgetProvider` is exported with `ACTION_UPDATE_WIDGET` (full scan, battery only).
  Fixed on `fix/widget-refresh-hardening` (not merged; verified on the Pixel 8 and S8), waits for the external review above.
- [ ] [S] Re-arm the 6-hour widget alarm on app start and `MY_PACKAGE_REPLACED`. A force-stop clears both alarms; only the snapshot is
  re-armed (`PackageReplacedReceiver.kt:22`, `MainActivity.kt:26`). Pixel 8: 0 alarms after two force-stops, both back after a reboot;
  three `install -r` in a row kept both, so an update alone doesn't clear them. Whether an F-Droid update force-stops: not checked.
  Fixed on `fix/widget-refresh-hardening` (not merged; verified on the Pixel 8 and S8), waits for the external review above.
- [ ] [M] 48 h change cap edge: a score change again across midnight keeps the first change's timestamp, so the cap can hide a fresh
  change (`ScoreHistoryRepository.kt:93-97`). Code reading only; failing test first. Risky code (score history).
- [ ] [M] Snapshot alarm time: comments say 23:50, code sets 23:00 (`SnapshotReceiver.kt:69`); the inexact daily alarm has an 18 h
  window on the Pixel 8, so a "midnight" snapshot can land at 17:00 next day. Read-only finding. Risky code (score history).
- [ ] [M] Widget opacity on Android 8–11 does nothing (review round 1 M1, rejected for this release by Oliver). `setImageAlpha` acts on
  the ImageView's image, but the card is its background (also on `master`). Card draws opaque, so readable; fix needs an API 30 emulator.
- [ ] [M] Legacy-rules and real-D2D check (optional): same bmgr cycle on an API 30 emulator, plus one phone-to-phone transfer.
- [ ] [S] device-verify can't restore a named wallpaper: the picker has no recents list. Reapplying the lock screen's photo to Home works
  while the lock screen still has it.

## Blocked on me

- External review round 1 of `fix/widget-refresh-hardening`: run `docs/reviews/fix-widget-refresh-hardening-prompt-1.md` (on that branch) in Cursor
  with a non-Claude model, save `docs/reviews/fix-widget-refresh-hardening-findings-1.md`.
- Create `.claude/devices.local` on the Framework laptop before the next session there, or the hook denies every device command (format: CLAUDE.md, Devices).
  Also copy the 9 Pixel 9 deny rules into that machine's `.claude/settings.local.json`; the file is gitignored, so a pull won't bring them.
  The sandbox comes to that machine later, once it has its own `devices.local`.
- Push `master` with the 2026-11-17 wrap-up merge.

Done items from this list moved to `docs/state-archive/2026-11.md` (2026-11-17).

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

### 2026-11-17 — device-verify under the sandbox (`chore/device-verify-sandbox`)

- [x] STATE: Oliver approved the HQ section v1.2 merge; merged and pushed (`c232c12`) (2026-11-17). It had no line in Blocked on me.
- [x] STATE: Local settings with HQ's stop-now allow rule installed by Oliver; live file identical to the proposal (2026-11-17).
- [x] Merged branch `chore/hq-status-contract` deleted with `git branch -d` (no commits outside `master`). Git warned it couldn't lock `.git/config` (read-only in the sandbox); no config section for the branch is left.
- [x] `.claude/agents/device-verify.md`: new Sandbox section listing the excluded adb forms from `settings.local.json`; one literal adb command per call, no pipes, redirects, chains or `$(...)`, no script files; `install -r` without `-t`; crash buffer cleared via `shell logcat -c -b crash`; screenshots via `shell screencap` to `/sdcard/Download/PV_shot.png`, the fixed pull, then `cp` into `docs/screens/<run>/`; stop and report on a refusal or prompt. Edited with the Edit tool.
- [x] Gate after a restart (2026-11-17, Pixel 8): device-verify registered with tools Bash and Read only (session agent list). assembleDebug fresh (APK 20:19:06 > start 20:18:55);
  `install -r` Success; force-stop; `shell logcat -c -b crash`; `am start -W` ok, COLD, 707 ms; screencap, fixed pull and `cp` gave `docs/screens/device-verify-sandbox/launch.png`
  (Dashboard, score 53, no dialog); crash buffer empty; `pidof` alive. The agent listed 8 adb commands, each one call, none prompted or refused;
  `hook.log` stayed at 55 lines (no new deny or ask).
- [x] Merge: `.claude/agents` is read-only in the sandbox, so the `--no-ff` merge was Oliver's.
- [x] STATE: Oliver merged `chore/device-verify-sandbox` with `--no-ff` and pushed (`origin/master` at `2ca7d39`) (2026-11-17).
- [x] Wrap-up (`chore/wrap-up-2026-11-17`): Next and Blocked on me made current; next up is the widget fix external review.
  Own diff review against `master`: no blocking problems (agent file, STATE, archive move only).
- Found: the guard hook logs only deny and ask decisions to `hook.log`, so allowed adb calls never appear there. Proof that commands ran one by one comes from the agent's transcript, not from `hook.log`.

### 2026-11-17 — HQ managed section v1.2

- [x] CLAUDE.md: HQ managed section v1 replaced by v1.2 word for word (`86c30b9`); nothing else in the file changed.
  Status and handoff are now written by HQ's command, never by hand.
- [x] Stale hand-off items checked against git: the tiny-pass merge is done (`191cf71` on `origin/master`); the widget fix review and the Framework laptop setup are still open.
- [x] The stop-now allow rule goes into the local settings as a proposal file for Oliver to install; no settings file edited.

The 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
