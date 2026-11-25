# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.2 (versionCode 10), tagged on `2a95fa3`; live on F-Droid (Oliver, 2026-11-07).
v1.5.3 (versionCode 11) is pushed and tagged `v1.5.3` on `00e9d4d` (Oliver, 2026-11-23); not yet confirmed live on F-Droid.
The Pixel 8 runs the run 2+3 debug build (versionCode 11, `92b4065a…`).
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

- [ ] [L] v1.6 "the loop" (spec `docs/decisions/v1.6-fix-to-result.md`, sections 6–7): runs 1, 2+3 as one branch, 4, 5, 6, 9, release; about 12.25 evenings. Run 1 is merged. Run 2+3 `feat/rescan-in-place` is built and through its Pixel 8 gate (24 Nov); review round 1 (2 High, 2 Medium, 4 Low) is fixed (one Low rejected) and re-checked on the Pixel 8 (25 Nov). It waits for external review round 2 in Cursor, then merge; after that run 4 `feat/fix-list` (needs the S8 for screen lock and the Pixel 8; ask Oliver which phone is free before any device step).
- [x] [S] (25 Nov, c07–c13) Run 2+3: on long Quick Win pages the result snackbar covers Check again (below the fold) for up to 10 s; fix in the review round, e.g. keep Check again in view after a result or lift the snackbar above it (Oliver, 24 Nov)
- [x] [S] (24 Nov, b18) Run 2+3 copy: when the uninstall Quick Win's trailing rescan sentence goes, make its steps consistent: step 2 "Tap 'Uninstall'" quoted like step 1, no full stops on the steps (Oliver, 23 Nov).
- [ ] [S] Tab tap while the pager still moves to another tab: it can stay stuck between two pages (seen 25 Nov with slow animations; same lines on `master`).
- [ ] [L] v1.6.1 "explaining": runs 7, 8, 10, 11, release; about 8 evenings. Run 10 waits for Oliver's call 8 (AI-app rows), still open; decide it before run 10.
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

### 2026-11-25 — v1.6 run 2+3: external review round 1 (`feat/rescan-in-place`)

- [x] Inbox empty; Oliver: no ROADMAP block this time. Findings: 2 High, 2 Medium, 4 Low.
- [x] Proven first, on the unfixed logic (view model collaborators made constructor parameters, no logic change): 24 of 312 tests failed, each with the finding's symptom.
  - H1: 5 in `PrivacyViewModelScanTest` (real view model): a superseded scan failing ended the newer scan's progress; a trusted list that can't be read crashed (`IOException` reached the uncaught handler).
  - H2: 10 in `ScanFeedbackTest`: `ChecksFixed(count=0)`, both checks' points on one name, "No change" over a new issue.
  - M4: a cancelled scan left the history list on its own point. Also found: a superseded scan that notices late published its result; a scan failing with someone else's cancellation left "Checking…" on for good.
  - L5: snackbar 97, screen 100. L6: 3 in `ScanEventTest`. L8: 2 in `AppNavigationStateTest`.
- [x] Fixed: H1, H2, M3, M4, L5 (fell out of H1: the failure path and the publish use the trusted set the app already holds, so nothing is read from storage there; one widget refresh), L6, L8; snackbar item: the Quick Win page ends above the snackbar and scrolls to Check again when a result arrives.
- [x] L7 rejected: all 13 `QuickWin(…)` constructions in `QuickWinsDetector` (the only ones in the app) set a non-null `relatedCheck`.
- [x] M3: kept result pinned by a test; the loss of a background result reproduced on the pre-fix build.
- [x] Reviewer's test gaps: 800 ms minimum (view model and `runScan`), `RunScanTest` asserts the failure was reached. Mutations caught: minimum 0, `runScan` without its delay, each of the three cancellation guards removed.
- [x] Gate: `compileDebugKotlin`, `testDebugUnitTest --rerun` 312 tests, 0 failures (280 before), `assembleDebug` pass (APK 20:12:58, `8940b5fd…`).
- [x] Own diff review against `master`: no blocking problems.
- [x] Oliver (25 Nov): Pixel 8 free; keep "1 better, 1 worse"; ROADMAP block applied (Later: app start with an unreadable trusted list).
- [x] Pixel 8 (device-verify, 20:34–21:17; pre-fix build `5eee7dfe…`, then `8940b5fd…`, then `92b4065a…`; screens c0 = pre-fix, c). Main session looked at the key screens.
  - [x] Pre-fix: M3 (stopped 240 ms after the tap, no snackbar on return) and the covered Check again (54 px) reproduced.
  - [x] Fixed: No change (c02), loss and Details → Breakdown (c04, c05), Not fixed yet by return, page button and snackbar button (c07–c10), fixed and page closed (c14), page opened during a rescan closes (c15, c16), background loss and no change shown once on return (stop at most 272 / 267 ms after the tap; c17b–c19b), Mark as done, not due (c20).
  - [x] Snackbar item: Check again 104 px above the snackbar in four dumps, also on Replace Chrome (c13); a hand scroll stays (c11); no strip afterwards (c12).
  - [x] L8: first fix FAILED (swipe during the move: Checks in Breakdown, c24); second round (saved only once settled): 2 of 2 in LIST (c33, c34), completed moves still save Breakdown (c30–c32).
  - [x] Final state: Wi-Fi scanning 0, animator scale null, review dates restored (equal hash), Checks list view, score 53, crash buffer empty, final build installed (`92b4065a…`, versionCode 11). Today's scans stay in its history.
  - [ ] Not verified on a phone: a failed scan, a trust change during a scan (Trusted is empty), a tab tap interrupting the snackbar's move (the tap had no visible effect, not explained; covered by unit tests only), TalkBack, "+5 pts" for a due review (seen 24 Nov).
- [x] Gate after the second L8 round: `compileDebugKotlin`, `testDebugUnitTest --rerun` 312, 0 failures, `assembleDebug` pass.
- [ ] External review round 2: files written, waits for Oliver.
- [x] Found: one device call held an escaped `$(…)`, ran in the sandbox and started its own adb server there; the agent stopped, a plain `adb devices -l` showed the Pixel 8 as `device`, the run went on. After an install over an existing task the first start can stack a second activity: force-stop before the install.
- [x] Found, not changed (Next): a tab tap while the pager still moves leaves it stuck between two pages (c36–c38, animations 10 times slower; same lines on `master`, not checked on a `master` build). App start with an unreadable trusted list crashes (code reading, on `master` too; ROADMAP Later).

The 2026-11-24 v1.6 run 2+3 build and first Pixel 8 gate, the 2026-11-23 v1.6 run 1 (stock-safe advice), v1.6 spec decisions run, v1.6 spec design run and v1.5.3 release run, the 2026-11-22 overnight readout run, the 2026-11-22 BootReceiver re-arm run, the 2026-11-22 guard serial assignments run, the 2026-11-21 snapshot fix build, review and evening Pixel 8 runs, the 2026-11-21 snapshot alarm time design run, the 2026-11-21 48 h change cap run, the 2026-11-20 HQ section v1.3 run, the 2026-11-18 runs (wrap-up, review branch stays checked out, widget hardening review), the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
