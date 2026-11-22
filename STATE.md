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
  2026-11-22: its harm is fixed (`fix/boot-receiver-rearm`: it re-arms only a missing widget alarm and refreshes only then). Still open only why Android keeps sending it after a force-stop; low priority.
- [ ] [S] First-run screen (S8): a widget tap or app start doesn't scan while the app is on that screen; once past it, both do. Check whether that is wanted. Path not changed by the widget hardening.
- [ ] [S] `device-verify`: read the crash buffer before any `logcat -c` (it also clears crash on the Pixel 8) and before a reboot; the main log buffer can rotate within 60 s on the Pixel 8, so read boot lines sooner.
- [ ] [S] Optional device check of review Medium: on a later evening after 22:00, before any scan that day (no app start; with a widget placed its boot scan records first, so this needs no widget), reboot: `dumpsys alarm` shows the snapshot alarm due within minutes, not tomorrow 22:00; after it fires, History has a point for that day and the alarm is at tomorrow 22:00.
- [ ] [S] `PackageReplacedReceiver` says an update clears the app's alarms; AOSP keeps them on a replace. Fix the comment.
  - [x] Settled 2026-11-21 (Pixel 8): after `install -r` both alarms were still there, unchanged. The comment is wrong.
- [x] [S] (decided 2026-11-21, kept as is) Trusting or untrusting an app recomputes the score without a scan, so today's history point keeps the old score until the next scan (the widget's follow-up scan records it when a widget is placed).
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
    the last assignment counts, and `env -i`/`-u` (also after `export`) forget it; env options it can't read safely (e.g. `-vu NAME`) are denied (`env-options-unparsed`, 2026-11-22);
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

### 2026-11-22 — overnight readout and widget push-back check (`fix/snapshot-every-scan`, device-verify)

- [x] Inbox: Oliver's block (2 STATE:, 2 ADD Next) applied. Pixel 8 unplugged overnight (Doze), plugged in again before the readout.
- [x] 1. Readout before any app start (phone 09:14:49): `Alarm Stats` snapshot "1 alarms, last -11h14m23s616ms" → **delivered 2026-11-21 22:00:26**, via the allow-while-idle path; pending snapshot alarm now `origWhen=2026-11-22 22:00:00`, `window=+1h`, no repeat, `flags=0x20` (re-armed on delivery). Widget alarm: 2 deliveries overnight, last 09:09:42.
- [x] 2. History (1W, screenshot `docs/screens/2026-11-22-overnight-readout/history-21nov.png`): 21 Nov point **22:00, score 53** (was 19:40 last night) → the alarm's scan recorded it (to the minute; the screen shows no seconds). 22 Nov point 09:09, score 53 = the widget alarm's scan. Crash buffer empty. Outcome: **alarm's scan recorded**.
- [x] 3. Widget push-back (Next item): code reading, then the same `am kill` start check on the branch and the `master` build (both in the Next item). Not this branch's; left in Next. The `master` APK had to be built in place: an `install -r` from the scratchpad runs inside the sandbox and can't reach the phone.
- [x] 4. Final state (09:30): branch build reinstalled (`install -r`, APK `00998bd6…`), force-stop + cold start; exactly one snapshot alarm `origWhen=2026-11-22 22:00:00`, `window=+1h`, no repeat, `flags=0x20`, and one widget alarm. Crash buffer empty. Home screen, widget 53 Fair (`docs/screens/2026-11-22-overnight-readout/home-final.png`). After the `install -r` over `master`, the update left master's 23:00 repeating alarm pending ("already pending, left alone"); by design its first delivery re-arms the 22:00 one-shot (`SnapshotReceiver.onReceive`), so users updating from 1.5.2 get one last late-window night. Not device-tested; the force-stop replaced it here.
- [x] Own diff review against `master`: no blocking problems. Merged with `--no-ff`, branch deleted, not pushed.


### 2026-11-22 — BootReceiver re-arms only a missing widget alarm (`fix/boot-receiver-rearm`, device-verify)

- [x] Inbox empty; Oliver: no roadmap decisions from chat.
- [x] Change: `BootReceiver` calls `rearmPeriodicUpdatesIfPlaced` (now returns whether it re-armed) and sends the immediate refresh only then. Snapshot path unchanged. `compileDebugKotlin`, `testDebugUnitTest --rerun` (216 tests, 0 failures), `assembleDebug` pass; APK `7ffb774f…`. No new test: the decision is `needsRearm`, already unit-tested.
- [x] Gate 1 (Pixel 8, `install -r`, force-stop, cold start): one widget alarm `471f08c` whenElapsed 280356102 (6 h), one snapshot alarm 22:00. App start armed both; `BootReceiver` ran 0.3 s later and logged "left alone".
- [x] Gate 2 (two `am kill` cold starts, `stopped=false`): widget alarm still `471f08c`, whenElapsed 280356102, only one; no "Periodic widget updates scheduled"; `BootReceiver` "left alone" each time. The one scan per start is the app's own refresh (`PrivacyViewModel`; read in code, not proven).
- [x] Gate 3 (real reboot, done twice; the first one's log lines rotated out): one widget alarm ~6 h after boot (`e807e4f`, armed 19:36:54), one scan, snapshot alarm still 2026-11-22 22:00. Broadcast history: `BootReceiver` got `BOOT_COMPLETED` ~3 s after the alarm was armed, and no `ACTION_UPDATE_WIDGET` was sent. Its own log lines: could not verify (rotated out).
- [x] Gate 4: crash buffer has no app entries (one system `init` SIGABRT per reboot). Home screen, widget 53 Fair (`docs/screens/2026-11-22-boot-receiver-rearm/home-widget-after-reboot.png`); 22:00 alarm armed.
- [x] Found: after a reboot the widget's enabled step (`APPWIDGET_ENABLED` → `onEnabled`) armed the alarm before `BootReceiver` ran, so `BootReceiver`'s own re-arm branch was not exercised on the device; that path is covered only by the `needsRearm` unit tests.
- [x] No external review (small receiver fix reusing the reviewed re-arm pattern). Own diff review against `master`: no blocking problems. Merged with `--no-ff`, branch deleted, not pushed.

### 2026-11-22 — guard serial assignments (`fix/guard-serial-assignments`, no phone)

- [x] Inbox (block pasted in chat): 1 CHANGE, 3 ADD, 1 STATE: applied to ROADMAP.md; the 22 Nov Next item on hanging device commands merged into the roadmap item.
- [x] Rule from HQ (22 Nov): any .proposed sandbox or hook file from a Privamatic run goes to the HQ chat for review before Oliver installs it.
- [x] Cull's 16 `ANDROID_SERIAL` cases (Cull `.claude/hooks/test-hook.sh:158-173`, read only) adapted into `scripts/test-hook.sh` (Cull's second test phone → Pixel 8, its P8 → S8, its unknown phone → `OTHER0SERIAL`), plus 6 for the `export` path. Cull's 5 `P8_SERIAL` cases are for its fixtures script, which Privamatic has no counterpart for.
- [x] Installed guard: 556 passed, **7 failed** of 563. Every last-assignment case passes (line 1086 keeps the last one). All 7 failures are env dropping the serial: `env -u ANDROID_SERIAL`, `env --unset=ANDROID_SERIAL`, `env -i`, `env -iv`, `env -` after a prefix, and `env -u` / `env -i` after `export ANDROID_SERIAL=`. The guard let through a Gradle install that, as written, runs with no serial.
- [x] Proposed fix `scripts/guard.sh.proposed` (sandbox: `.claude/hooks` is read-only): env `-u NAME`, `--unset=NAME`, `-uNAME` forget NAME; `-i`, `-`, `--ignore-environment` and `-iv`-style clusters forget all, and the exported serial no longer counts for that command. Nothing else changed. Full `test-hook.sh` against it (`GUARD_HOOK=…`): 563/563.
- [x] HQ chat review 1 (22 Nov): right direction, not yet OK to install. Each finding proven by test cases first.
  - [x] M1 (must), confirmed: `env -vu NAME`, `-0u NAME`, `-vC DIR` and unknown options skipped one word, so the gradle call wasn't analysed (both guards: none). Fix: env reads only known options and clusters of `0`/`i`/`v`; anything else is denied (`env-options-unparsed`, hint: write the options separately). `-CDIR`, `--chdir=DIR` and the signal options are read correctly.
  - [x] M2 (must), not a hole: adb without `-s` never falls back to `ANDROID_SERIAL`, so `env -u ANDROID_SERIAL adb …`, `env -i adb …` and `export ANDROID_SERIAL=… && adb shell ls` are already denied (`adb-no-serial`) by the installed guard. No change; cases added.
  - [x] Should: cases for M1, M2 and chained (`env -u ANDROID_SERIAL true && ./gradlew installDebug` denied; after an `export` it stays allowed). Full suite: installed guard 567/579 (the 7 env cases + 5 M1), proposed 579/579.
- [x] HQ chat approved the guard fix in round 2 (22 Nov); Oliver installed scripts/guard.sh.proposed as .claude/hooks/guard.sh (backup of the old guard at ~/guard.sh.bak-2026-11-22), committed it (0042a1a), merged (76d351d) and pushed; test-hook.sh 579/579 on the installed guard.

The 2026-11-21 snapshot fix build, review and evening Pixel 8 runs, the 2026-11-21 snapshot alarm time design run, the 2026-11-21 48 h change cap run, the 2026-11-20 HQ section v1.3 run, the 2026-11-18 runs (wrap-up, review branch stays checked out, widget hardening review), the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
