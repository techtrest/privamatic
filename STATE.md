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

- [ ] [S] LG G2 as a third test phone: waits for HQ's shared guard engine.
- [ ] [S] Live check of the serials-from-file hook: a Pixel 9 `pm clear` is denied (still open; so far proven only by feeding JSON to the hook, 2026-11-05).
  - [x] Pixel 8 half: 8 literal Pixel 8 adb commands ran without a prompt or refusal, no new `hook.log` line (2026-11-17 device-verify gate).
- [ ] [S] Investigate `BootReceiver` running when no boot explains it. Pixel 8 (Android 15+): on cold starts right after a force-stop it ran too (2 alarm arms, 2 scans), a later cold start didn't. S8: it ran twice in two processes after a reboot, and only the second armed the widget alarm. Alarm count stayed correct in both.
  2026-11-18, Pixel 8: the system log shows a real `BOOT_COMPLETED` delivered on the first start after a force-stop (Android 15+ behaviour), 335 ms after the start's own arm.
- [ ] [S] First-run screen (S8): a widget tap or app start doesn't scan while the app is on that screen; once past it, both do. Check whether that is wanted. Path not changed by the widget hardening.
- [ ] [S] `device-verify`: read the crash buffer before any `logcat -c` (it also clears crash on the Pixel 8) and before a reboot; the main log buffer can rotate within 60 s on the Pixel 8, so read boot lines sooner.
- [ ] [M] 48 h change cap edge: a score change again across midnight keeps the first change's timestamp, so the cap can hide a fresh
  change (`ScoreHistoryRepository.kt:93-97`). Code reading only; failing test first. Risky code (score history).
- [ ] [M] Snapshot alarm time: comments say 23:50, code sets 23:00 (`SnapshotReceiver.kt:69`); the inexact daily alarm has an 18 h
  window on the Pixel 8, so a "midnight" snapshot can land at 17:00 next day. Read-only finding. Risky code (score history).
- [ ] [M] Widget opacity on Android 8–11 does nothing (review round 1 M1, rejected for this release by Oliver). `setImageAlpha` acts on
  the ImageView's image, but the card is its background (also on `master`). Card draws opaque, so readable. Confirmed on the S8 (2026-11-07: 76 and 100 draw identically), so it can be fixed and verified there; no API 30 emulator needed.
- [ ] [M] Legacy-rules and real-D2D check (optional): same bmgr cycle on an API 30 emulator, plus one phone-to-phone transfer.
- [ ] [S] device-verify can't restore a named wallpaper: the picker has no recents list. Reapplying the lock screen's photo to Home works
  while the lock screen still has it.

## Blocked on me

  See HANDOFF.md: HQ's command keeps this list there.

## Red flags

  See HANDOFF.md: HQ's command keeps this list there.

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

### 2026-11-20 — HQ managed section v1.3 (`chore/hq-section-v1.3`)

- [x] STATE: Oliver pushed `master` (`origin/master` at `a55b5fc`, `git merge-base --is-ancestor a55b5fc origin/master`); "Push master" left out of the new list (2026-11-20).
- [x] CLAUDE.md: HQ section v1.2 swapped for v1.3, word for word; nothing else in the file changed. Guard test 541/541; the guard lets HQ's `write` and `stop-now` commands through silently.
- [x] Blocked on me and Red flags now hold only the pointer line; the open item (Framework laptop setup) moved to HQ's list. Done items from Blocked on me moved to `docs/state-archive/2026-11.md` (2026-11-18). Docs only: no build, no external review.

### 2026-11-18 — wrap-up (`chore/wrap-up-2026-11-18`)

- [x] STATE: Oliver pushed `master` (`c90c608`) and deleted the five merged local branches (`chore/legacy-test-device`, `chore/sandbox-followup`, `chore/sandbox-rollout`, `chore/sandbox-tinypass`, `fix/mockito-agent`). It had no line in Blocked on me (2026-11-18).
- [x] Next and Blocked on me made current: ticked items moved to the November archive. No review is open; the widget hardening fixes are on `master` and unreleased by choice (ROADMAP, Later).

### 2026-11-18 — external review branch stays checked out (`chore/review-branch-checkout`)

- [x] STATE: Oliver merged `fix/widget-refresh-hardening` with `--no-ff` and pushed (`origin/master` at `3990262`); removed from Blocked on me (2026-11-18).
- [x] STATE: Oliver checked the cancel-gate screenshots before the push: nothing private (2026-11-18).
- [x] CLAUDE.md, External review: two bullets added (branch under review stays checked out when a run stops at "external review"; a later run switches back, or puts the exact `git switch` command first in Blocked on me and the handoff if the sandbox blocks it). Nothing else changed, HQ section untouched. Docs only: no build, no external review.

The 2026-11-18 widget hardening review run, the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
