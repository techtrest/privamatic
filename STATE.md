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
  - [x] Pixel 8 evening check passed (2026-11-21, run below; snapshot alarm flag is 0x20, accepted by Oliver). Waits on tomorrow morning's readout, then merge.
- [ ] [S] `PackageReplacedReceiver` says an update clears the app's alarms; AOSP keeps them on a replace. Fix the comment.
  - [x] Settled 2026-11-21 (Pixel 8): after `install -r` both alarms were still there, unchanged. The comment is wrong.
- [ ] [S] Each app start pushes the widget's 6 h alarm back 6 h (Pixel 8, 2026-11-21: new alarm object at +5h59m after each `am kill` cold start, log "Periodic widget updates scheduled" each time), although `rearmPeriodicUpdatesIfPlaced` should leave a live alarm alone. Read-only finding, not this branch's scope.
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

- [x] Build, unit tests, review round 1 and its fixes: `docs/state-archive/2026-11.md` (run moved there 2026-11-21).
- [ ] Overnight result pending (read tomorrow morning, below). Not verified yet: the 22:00 alarm firing in Doze and its scan recording.

### 2026-11-21 — Pixel 8 check, evening (`fix/snapshot-every-scan`, device-verify)

- [x] Inbox empty, no roadmap block. APK: the 18:25 APK (`dab43615…`) is not byte-identical to a forced fresh build of HEAD (`284d01c1…`): every zip entry has the same bytes, only the zip/signing bytes differ. Installed the fresh build. Evidence files in the session scratchpad (not tracked).
- [x] 1. Before (19:34): old snapshot alarm `origWhen=2026-11-21 23:00`, `window=+18h`, `repeatInterval=86400000`, `flags=0x0`. History DB: 34 points, oldest 71 (4 Oct 09:27:09), today 53 (21 Nov 03:52:07, id 34). Widget placed (id 26).
- [x] 2. `install -r` (19:35:05): the old snapshot alarm **survived the update** unchanged (same alarm object), as did the widget alarm. The update started the app: `PackageReplacedReceiver` logged "restoring…", then "Snapshot alarm already pending, left alone"; the widget also scanned on `APPWIDGET_UPDATE`. Answer for the Next item: an update does not clear the alarms on the Pixel 8; comment not changed in this run.
- [x] 3. Force-stop, cold start (19:35:34): exactly one snapshot alarm, `origWhen=2026-11-21 22:00:00`, `window=+1h0m0s0ms`, `repeatInterval=0`, `flags=0x20`, `whenElapsed 181387761`, with the allow-while-idle `idle-options` bundle. Expected 0x4 was wrong: the system stores an inexact `setAndAllowWhileIdle` as 0x20 (allow-while-idle, inexact form; Android source, not device-proven). Oliver accepted 0x20 (21 Nov). Two more cold starts (`am kill` from HOME, pid gone, LaunchState COLD, 19:38:19 and 19:38:51): same alarm object, `origWhen` and `whenElapsed` unchanged, still one; log "already pending, left alone" (review Low 1: pass).
- [x] 4. History: after the starts, 34 points, rows 1–33 identical, row 34 (21 Nov) now 19:38:51, score 53. Pull to refresh at 19:40:20: still 34, row 34 now 19:40:21.893 (the widget follow-up scan, under 1 s after). History "All" screenshot: 4 Oct – 21 Nov, one point per day, last on 21 Nov.
- [x] 5. Crash buffer empty (19:31 and 19:40). Phone left at 19:41 on the home screen, widget shows 53 Fair, debug build installed.
- [x] Final alarms (19:41): snapshot 22:00 tonight (as above); widget alarm next at about 01:38, window +4h30m (so 01:38–06:08, all after midnight). Phone is on USB power (status "not charging", 80 %), deep idle ACTIVE: unless unplugged, tonight doesn't test Doze.
- [x] The database copies used for the check are deleted from the Pixel 8 (device-verify, 20:01); the snapshot alarm unchanged at 20:01:32.
- [x] Found: a force-stop clears the app's `Alarm Stats` and `App Alarm history` entries in `dumpsys alarm`; at 19:41 the package has none, so tomorrow's entries are all from tonight.

**Tomorrow morning — what to read, in this order, before opening the app** (no force-stop, no reboot before step 2):
1. `adb -s <test serial> shell date`, then `adb -s <test serial> shell dumpsys alarm`, saved whole:
   - pending snapshot alarm: `origWhen=2026-11-22 22:00:00`, `window=+1h0m0s0ms` means tonight's fired and re-armed (the re-arm happens only on delivery; an unfired one still shows 21 Nov 22:00).
   - `Alarm Stats`, entry `u0a338:com.techtrest.privamatic.debug`: the line above `*walarm*:…ACTION_MIDNIGHT_SNAPSHOT` gives "N alarms, last -T". Delivery time = the `date` minus T. Expected 1 alarm, 22:00–23:00. Same for `…ACTION_UPDATE_WIDGET`: its deliveries should all be after midnight.
2. History DB (copy as device-verify did on 21 Nov; or tap the 21 Nov point): the 21 Nov row's (id 34) timestamp and score; count 34 or 35.
3. Crash buffer: `logcat -d -b crash`, empty.

**How to tell the alarm's scan from a widget refresh:** only the DB and `dumpsys alarm` survive the night; the Pixel 8's main log rotates within minutes, so logcat isn't evidence. The widget alarm can't fire before about 01:38, so it can't touch the 21 Nov point. So:
- row 34 timestamp within a few seconds after the snapshot delivery time → the alarm's scan recorded it (pass);
- row 34 still 19:40:21 → the alarm's scan recorded nothing (fail, if step 1 shows it fired);
- any other 21 Nov time → a scan the alarm doesn't explain (launcher `APPWIDGET_UPDATE`, which `Alarm Stats` doesn't list). Then whether the alarm's own scan recorded can't be shown: report that, don't guess.
- A 22 Nov point (id 35) comes from a widget alarm scan after midnight; it must not change row 34.

**Optional, review Medium:** on a later evening after 22:00, before any scan that day (no app start; with a widget placed its boot scan records first, so this needs no widget), reboot: `dumpsys alarm` shows the snapshot alarm due within minutes, not tomorrow 22:00; after it fires, History has a point for that day and the alarm is at tomorrow 22:00.

The 2026-11-21 snapshot fix build and review runs, the 2026-11-21 snapshot alarm time design run, the 2026-11-21 48 h change cap run, the 2026-11-20 HQ section v1.3 run, the 2026-11-18 runs (wrap-up, review branch stays checked out, widget hardening review), the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
