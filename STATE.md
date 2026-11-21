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
- [ ] [M] Snapshot alarm time: comments say 23:50, code sets 23:00 (`SnapshotReceiver.kt:69`); the inexact daily alarm has an 18 h
  window on the Pixel 8, so a "midnight" snapshot can land at 17:00 next day. Read-only finding. Risky code (score history).
  - [x] Design run 2026-11-21: `docs/decisions/snapshot-alarm-time.md`; recommends Option B after a one-night Doze check. Waits on Oliver's choice.
  - [x] Oliver chose B plus C for the snapshot fix (21 Nov), after the design paper docs/decisions/snapshot-alarm-time.md (2026-11-21).
  - [x] Built on `fix/snapshot-every-scan` (2026-11-21, `8b778cf`), unit tests pass; waits on the Pixel 8 check (run below), then external review.
  - [x] External review round 1: no High; Medium and Low 2 fixed, Low 1 moved to the device check, Low 3 rejected (2026-11-21, review fixes run). Waits on the Pixel 8 check, then merge.
- [ ] [S] `PackageReplacedReceiver` says an update clears the app's alarms; AOSP keeps them on a replace. Tonight's `dumpsys alarm` right after `install -r` shows which; fix the comment then.
- [ ] [S] Trusting or untrusting an app recomputes the score without a scan, so today's history point keeps the old score until the next scan (the widget's follow-up scan records it when a widget is placed).
  - [x] Oliver decided (21 Nov): keep it so, today's point stays until the next scan.
- [ ] [M] Widget opacity on Android 8–11 does nothing (review round 1 M1, rejected for this release by Oliver). `setImageAlpha` acts on
  the ImageView's image, but the card is its background (also on `master`). Card draws opaque, so readable. Confirmed on the S8 (2026-11-07: 76 and 100 draw identically), so it can be fixed and verified there; no API 30 emulator needed.
- [ ] [M] Legacy-rules and real-D2D check (optional): same bmgr cycle on an API 30 emulator, plus one phone-to-phone transfer.
- [ ] [S] device-verify can't restore a named wallpaper: the picker has no recents list. Reapplying the lock screen's photo to Home works
  while the lock screen still has it.

## Blocked on me

  See HANDOFF.md: HQ's command keeps this list there.

## Red flags

  See HANDOFF.md: HQ's command keeps this list there.

Done items moved from Next on 2026-10-25: `docs/state-archive/2026-10.md`.

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

### 2026-11-21 — snapshot fix, B plus C (`fix/snapshot-every-scan`)

- [x] Roadmap block applied (pasted by Oliver; inbox empty). No phone this run: the Pixel 8 is in use by another project, no adb command sent.
- [x] How history is stored: Room `privacy_history.db`, table `privacy_snapshots` (`id` autoincrement, `timestamp`, `score`, `deductionsJson`), version 1, no migrations. Only the History screen reads it. "One point per local day, replaced by a later scan" fits as is: find the day's row and update it by `id`. No schema change, migration, table or column; one `@Update` and one select added to the DAO.
- [x] Where a scan finishes: the app scan (`PrivacyViewModel.onScanSuccess`: start, pull to refresh, return from settings, widget tap via app start); the widget scan (`PrivacyWidgetProvider.onUpdate`: 6 h alarm, boot, config Save and the app's follow-up via `requestImmediateUpdate`); the snapshot alarm (`SnapshotReceiver`). No single existing point: the scanner returns the raw score, and the trust adjuster also runs on trust changes without a scan. So one shared call, `PrivacySnapshotRepository.recordScan(adjustedScore)`, is called at those three places. No new scans.
- [x] Old bug found by test: the day ended at midnight + 24 h, so on the 25 h day (25 Oct) a scan after 23:00 counted as a new day.
- [x] Failing tests first (`PrivacySnapshotRepositoryTest`, fake DAO, Europe/Berlin): 4 of 6 failed on the old logic (first scan of the day won, e.g. expected 61 but was 53). With replace but the old 24 h day: the 25 h day test failed (3 points, not 2). With local-midnight day ends: all pass.
- [x] Alarm: one `setAndAllowWhileIdle(RTC_WAKEUP, next 22:00 local)`, re-armed first thing on delivery; app start, boot and update call `ensureSnapshotAlarm`, which arms only when the PendingIntent is missing (`FLAG_NO_CREATE`). The old 23:00 repeating alarm, same PendingIntent, is replaced at its first delivery after the update. Unused `cancelMidnightSnapshot` removed. `SnapshotAlarmTest`: 22:00 tonight / tomorrow, both DST nights, the pending rule.
- [x] Room wraps the new `setPointForDay` in `withTransaction` (generated `PrivacySnapshotDao_Impl`), so the app scan and the widget scan it triggers can't both insert.
- [x] `compileDebugKotlin`, `testDebugUnitTest --rerun` 211/211, `assembleDebug` pass (APK 15:03, sha256 `26539635afcd…`). Manifest, entity and database unchanged against `master`.
- [x] Own diff review against `master`: nothing to block the merge.
- [x] STATE: Oliver decided (21 Nov): History empty text is now "Your history starts with your first scan." (`label_history_no_data`, `values/strings.xml`, the only locale: no translation to flag); trust/untrust keeps today's point until the next scan. Spec line 32 updated. Pasted block had no other lines.
- [x] After the text change: `compileDebugKotlin`, `testDebugUnitTest --rerun` 211/211 (0 failures).
- [x] External review round 1 files written (`docs/reviews/fix-snapshot-every-scan*`); stopped at "external review", branch stays checked out. No adb command this run.
- [x] Review round 1 fixes (Grok 4.7 High, `docs/reviews/fix-snapshot-every-scan-findings-1.md`; no High, so no round 2). Roadmap block applied first (LG G2 line). No adb command.
  - Medium (reboot after 22:00 skips the night's scan): failing test first (re-arm at 22:15 landed 23 h 45 m later), then fixed: armed after 22:00 with no point today, the alarm fires now; tomorrow's is armed first and replaced off the main thread.
  - Low 2 (overlap saves the earlier scan): failing test first (expected 61, was 58), then fixed in `setPointForDay`: an older stamp never replaces the day's point.
  - Low 1 moved to the device check (step 3); Low 3 rejected as a check expectation (step 4 updated).
  - `compileDebugKotlin`, `testDebugUnitTest --rerun` 216/216 (0 failures), `assembleDebug` (APK 18:25, sha256 `dab43615b997…`).
- [ ] Not verified on a device: all of it. Pixel 8 check for tonight and tomorrow morning below.

**Pixel 8 check, tonight, best before 21:30** (`<test serial>` from `.claude/devices.local`; read the crash buffer before any `logcat -c`):
1. Before: `adb -s <test serial> shell dumpsys alarm`, note the `ACTION_MIDNIGHT_SNAPSHOT` alarm (expected: old one, `origWhen` 23:00, `window=+18h`, `repeatInterval=86400000`). History "All": note the point count, oldest (71, 4 Oct 09:27) and today's point (53, 21 Nov 03:52).
2. `adb -s <test serial> install -r app/build/outputs/apk/debug/app-debug.apk`, then `dumpsys alarm` again before any start: does the old snapshot alarm survive the update? (settles the Next item on `PackageReplacedReceiver`.)
3. `adb -s <test serial> shell am force-stop com.techtrest.privamatic.debug`, then cold start (`am start -n com.techtrest.privamatic.debug/com.techtrest.privamatic.MainActivity`). `dumpsys alarm`: exactly one snapshot alarm, `origWhen=2026-11-21 22:00:00`, `window=+1h0m0s0ms`, no `repeatInterval` (0), `flags` with the allow-while-idle bit (0x4). Then two more cold starts without force-stop: after each, the snapshot alarm's `origWhen` and `whenElapsed` are unchanged (left alone; review Low 1). If this step runs after 22:00, a force-stop then cold start arms tomorrow 22:00 when today already has a point, and fires within minutes when it has none.
4. History "All": same count, every point before 21 Nov unchanged; the 21 Nov point now shows the cold start's scan time and score (it replaced 03:52). Pull to refresh on the Dashboard a minute later, reopen History: still one 21 Nov point, now at the refresh time. With a widget placed, the point lands a few seconds after the cold start or refresh: the widget update each app scan triggers scans and records too (review Low 3).
5. `adb -s <test serial> logcat -b crash -d`: empty. Leave the phone idle, screen off, unplugged if possible, with this debug build.

**Tomorrow morning:** `dumpsys alarm`: one snapshot alarm, `origWhen=2026-11-22 22:00:00`, `window=+1h` (so tonight's fired and re-armed); the app's alarm history in the same dump: snapshot delivery time last night, expected 22:00–23:00. History 1W: the 21 Nov point is the day's last scan (the alarm, or a later widget update before midnight); a 22 Nov point exists only from a scan after midnight (the 6 h widget update). Crash buffer empty.

**Optional, review Medium:** on a later evening after 22:00, before any scan that day (no app start; with a widget placed its boot scan records first, so this needs no widget), reboot: `dumpsys alarm` shows the snapshot alarm due within minutes, not tomorrow 22:00; after it fires, History has a point for that day and the alarm is at tomorrow 22:00.

The 2026-11-21 snapshot alarm time design run, the 2026-11-21 48 h change cap run, the 2026-11-20 HQ section v1.3 run, the 2026-11-18 runs (wrap-up, review branch stays checked out, widget hardening review), the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
