# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.2 (versionCode 10), tagged on `2a95fa3`; live on F-Droid (Oliver, 2026-11-07).
v1.5.3 (versionCode 11) is pushed and tagged `v1.5.3` on `00e9d4d` (Oliver, 2026-11-23); not yet confirmed live on F-Droid.
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

- [ ] [L] v1.6 "the loop" (spec `docs/decisions/v1.6-fix-to-result.md`, sections 6–7): runs 1, 2+3 as one branch, 4, 5, 6, 9, release; about 12.25 evenings. Run 1 `fix/stock-safe-advice` done: reviewed, merged into `master` 23 Nov, not pushed; next: run 2+3 `feat/rescan-in-place`.
- [ ] [L] v1.6.1 "explaining": runs 7, 8, 10, 11, release; about 8 evenings. Run 10 waits for Oliver's call 8 (AI-app rows).
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

### 2026-11-23 — v1.6 run 1: stock-safe advice (`fix/stock-safe-advice`, S8 via device-verify)

- [x] Inbox empty; Oliver: no ROADMAP block from chat.
- [x] Strings (spec 4.1 hard rule): `fmt_recommendation_system_app` no longer says disable, it says the app came with the phone, costs 1 pt and needn't be used; `privacy_check_google_camera_system_recommendation` says keep it, use Open Camera instead if wanted; `quick_win_uninstall_app_instructions` lost its pre-installed sentence. Play Services row untouched.
- [x] Call 10: new `PrivacyIssue.rowActionType` (null when `isPreinstalledApp`); both `IssueItem` button sites read it, so preinstalled rows (also the informational AI rows, e.g. a preinstalled Gemini) have no App settings button. User-installed rows keep theirs. Call 12: no scoring change.
- [x] Tests: `PrivacyIssueTest` (button per system/user app, camera text choice, still 1 pt, every scored app check costs 1 pt as the text says), `PackageManagerUtilTest` (system, updated system, user, unresolvable), new `StockSafeAdviceTest` (strings.xml: no "disabl", no "uninstall it"/"remove it", no "!"). `compileDebugKotlin`, `testDebugUnitTest --rerun` (233 tests after the follow-up, 0 failures; 216 before), `assembleDebug` pass.
- [x] Phone gate (S8, Android 9, device-verify, APK `5456439d…` built 19:56): first try the S8 showed `no permissions`; Oliver re-plugged it. Install Success, start scan score 17, crash buffer empty, app alive. Details → Checks → Google Apps: Chrome, Gmail, YouTube (system apps per `pm list packages -s`) show "<App> came with your phone and can't be removed, so it costs 1 pt while it's here. …", chip −1, no button in the row (uiautomator dump: no "Open App Settings", "disable" or "uninstall"). Google Photos (user-installed on the S8, `-3`) keeps "Consider uninstalling…" and its Open App Settings button, chip −1. Screens: `docs/screens/stock-safe-advice/` (dashboard, checks-google-services, row-chrome, row-gmail, row-youtube, row-user-app). Pixel 8, Pixel 9 and the LG G2 not touched.
- [ ] Google Camera row: not verified on a phone: covered by unit test, phone check in run 2+3's Pixel 8 gate (added to spec 4.2).
- [x] Own diff review against `master`: no blocking problems.
- [x] Follow-up (Oliver agreed the texts): `fmt_recommendation_system_app` = "…came with your phone and can't be removed, so it costs 1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and data out of it."; uninstall Quick Win step 2 = "Tap Uninstall." (the rest as before); `StockSafeAdviceTest` checks no "Disable" in those steps.
- [x] Deviation: before handing over to device-verify, the main session ran adb directly in the sandbox (`adb devices`, `start-server`, `wait-for-device`). That breaks the rule: device commands only through device-verify, only the exact excluded forms, never `wait-for-device`. From then on device work went only through device-verify.
- [x] Follow-up 2 (Oliver): uninstall Quick Win first line = "You can improve your privacy by uninstalling this app."; `StockSafeAdviceTest` checks no "disabl" in it, case-insensitive. `testDebugUnitTest --rerun` (233, 0 failures), `assembleDebug` pass. No phone check: same screen path the S8 check covered (Oliver).
- [x] External review round 1: no High or Medium, four Lows; Oliver: fix all four, no round 2. Each test proven first by mutating the string (then restored): 1. `PrivacyIssueTest` checks the text says "costs 1 pt" (mutation "costs 2 pts" → failed); 2. camera text keeps "Keep", "Open Camera", "built-in camera" (keep sentence dropped → failed); 3. spec 4.1 quotes the shipped sentence; 4. system-app texts reject any "uninstall"/"remov" except "can't be removed" (old "can't be uninstalled" → failed). All pass restored: `testDebugUnitTest --rerun` 234, 0 failures; `assembleDebug` pass. No phone (tests and docs only).
- [x] Proofs repeated with the reviewer's own mutations once Oliver saved the findings file ("costs 2 pt"; camera text = the generic sentence; "Consider uninstalling it."; "remove this app"): each FAILED, restored → 234 pass. Resolutions in `docs/reviews/fix-stock-safe-advice-findings-1.md`. Own diff review against `master`: no blocking problems. Merged into `master` with `--no-ff`, not pushed.
- [x] Found: device-verify's first S8 try put a `cd` in front of the install command, so it ran inside the sandbox (its own adb server, "device not found"); rerun once with the bare command, which passed. On the S8, `am start -W` timed out because the screen was off (Dozing); a wake keyevent fixed it.

### 2026-11-23 — v1.6 spec decisions and crash check (`docs/v1.6-spec`, spec only, no phone)

- [x] Inbox empty; Oliver's pasted ROADMAP UPDATE (1 DONE, 1 CHANGE, 2 ADD) applied to ROADMAP.md.
- [x] Crash check, code reading of `master` `00e9d4d` (v1.5.3), not proven on a phone: every settings button goes through `IntentHelper.launchActionIntent` or `launchAdSettings` (`AdIdVerificationScreen.kt:418`); both catch `ActivityNotFoundException` on every step, neither `SecurityException`. No intent in use is known to throw it; the October audit (Pixel 8, Android 17) opened the four manual-check targets, the Wi-Fi scanning Quick Win and the Ad ID Ads page, and those intents are unchanged since. Plausible risk only: the Ad ID screen's explicit Play services component, if a Play services update stops exporting it. No hotfix; runs 4 and 6 add the catch.
- [x] Call 8 check: AI-app rows are not unknown. Installed → `isSecure = false`, "Installed", 0 pts (`InstalledAppsChecker.kt:272-277`), INFO because `isInformational` (`IssueItem.kt:49`). Spec says no, pending Oliver.
- [x] Spec updated: calls 1–16 decided, VPN copy, F1 +15 test in run 4, release split (v1.6 12.25, v1.6.1 8.0, M4 Later), What's new / change label / unknown-row wording per release, run 9 no longer depends on run 7, 2+3 before 5 and 9.
- [x] Own diff review: no blocking problems. No external review (spec only, no app code). Merged into `master` with `--no-ff`, not pushed.

### 2026-11-23 — v1.6 "fix → result" spec (`docs/v1.6-spec`, design run, no phone)

- [x] Inbox empty; Oliver's pasted ROADMAP UPDATE (3 DONE, 2 CHANGE, 3 ADD Later) applied to ROADMAP.md.
- [x] Spec written: `docs/decisions/v1.6-fix-to-result.md`. Every in-scope audit finding checked against `master` `00e9d4d` by reading code (section 1): H1, H3, H4, M4 partly fixed; M3 fixed in code (needs a phone); M2, M5, M6, M7, M11 half, M12, L10 still hold.
- [x] Found (code reading, not proven): Quick Win points use the enum value, so multi-app checks understate the gain (3 listener apps cost 15, tile says +5); the preinstalled-app advice and the Google Camera text still suggest disabling a system app (fails the v1.6 hard rule); the keyboard Quick Win opens Default apps; on Android 9 `ACTION_PRIVACY_SETTINGS` opens Backup in AOSP; `IntentHelper` doesn't catch `SecurityException`.
- [x] Settings intents researched from AOSP sources (section 5); no phone used, every Samsung target unconfirmed.
- [x] Estimate: about 22 evenings with everything, 20 without M4, against two weeks (10–14). Cut line and 17 Oliver's calls in sections 7–8. No code changed.

### 2026-11-23 — v1.5.3 release (`release/v1.5.3`, no phone)

- [x] Inbox: Oliver's pasted block (1 STATE:) and the inbox block (1 CHANGE, 1 ADD Next, 1 STATE:) applied; inbox cleared.
- [x] Oliver checked the F-Droid description against v1.5.3 on 23 Nov: still current, no refresh needed. Oliver agreed the v1.5.3 changelog text on 23 Nov. (2026-11-23)
- [x] Oliver decided (23 Nov): features and UX come first. Order: v1.5.3 release now, v1.6 "fix → result" (tight), v1.7 Privacy Watch as the differentiator. Tooling and HQ work max about one evening a week; HQ topics go to the HQ chat, not into Privamatic runs; reliability bugs only take priority when users would notice them. (2026-11-23)
- [x] Changelog's last line checked against `c5d1631`: before it the provider was `exported="true"` with no permission and an `ACTION_UPDATE_WIDGET` filter, and `onReceive` runs a full scan and refresh on that action. So another app could make the widget refresh; the wording holds.
- [x] `versionCode` 10 → 11, `versionName` "1.5.3"; `changelogs/11.txt` = the agreed text, 374 bytes, one trailing newline. Descriptions untouched.
- [x] Gate: `compileDebugKotlin`, `testDebugUnitTest --rerun` (216 tests, 0 failures), `assembleRelease` all pass. Release build: `isMinifyEnabled = true`, `isShrinkResources = true` (R8 ran); unsigned APK `cc848828…` says versionCode 11 / 1.5.3.
- [x] Own diff review against `master`: version lines and the new changelog only, no blocking problems. No external review (version/copy only). Merged with `--no-ff`, not pushed, not tagged.


The 2026-11-22 overnight readout run, the 2026-11-22 BootReceiver re-arm run, the 2026-11-22 guard serial assignments run, the 2026-11-21 snapshot fix build, review and evening Pixel 8 runs, the 2026-11-21 snapshot alarm time design run, the 2026-11-21 48 h change cap run, the 2026-11-20 HQ section v1.3 run, the 2026-11-18 runs (wrap-up, review branch stays checked out, widget hardening review), the 2026-11-17 runs (device-verify under the sandbox, HQ section v1.2), the 2026-11-10 sandbox run (with its 11-11 follow-up and tiny pass), the 2026-11-07 run (legacy test device) and the 2026-11-05 runs (private device details, second layer and branch cleanup, inbox `STATE:` lines): `docs/state-archive/2026-11.md`.
Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run) and the 2026-10-25 release, keyboard and tracker-contract runs: `docs/state-archive/2026-10.md`.
