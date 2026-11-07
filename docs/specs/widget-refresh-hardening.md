# Widget refresh hardening — spec

Run type: implementation run. Branch: `fix/widget-refresh-hardening` (from `master`, 2026-11-07).

## Goal

The 6-hour widget refresh comes back after a force-stop without waiting for a reboot.
Other apps can no longer make Privamatic run a full scan, and the user's own refresh paths keep working.

## Scope

**In:**
- Fix A: re-arm the widget alarm on app start and `MY_PACKAGE_REPLACED`, only if at least one widget is placed.
- Fix B: `PrivacyWidgetProvider` becomes `android:exported="false"`.
- The debug-only widget extras (`debug_delta`, `debug_clear`, `debug_opacity`, `debug_device_name`) move to a debug-only receiver,
  because the shell can no longer reach the provider.
- One debug log line at the start of `onUpdate`, as a witness for the device gates.

**Out:**
- The opacity slider on Android 8–11: one read-only S8 observation, no code.
- The 48-hour change cap and the snapshot alarm's time.
- `WidgetConfigurationActivity` stays exported, because launchers on older Android start it directly. Another app can open it, but that is visible,
  and only the user's Save triggers a scan.
- `BootReceiver` and `PackageReplacedReceiver` stay exported. Their actions are protected, and the shell can't send `BOOT_COMPLETED`.

## Findings before the change

Unchanged 1.5.2 debug build, 2026-11-07. Witness: `last_update_timestamp` in `score_history`, written by every widget scan.

| Check, from `adb shell` (a uid other than the app's) | Pixel 8, Android 17 | S8, Android 9 |
|---|---|---|
| Custom `ACTION_UPDATE_WIDGET` to the provider | Delivered, timestamp moved: a full scan ran | Not run: the debug app isn't installed yet |
| Standard `APPWIDGET_UPDATE` to a component | `SecurityException` ("from unknown caller"), timestamp unchanged | Same `SecurityException` |
| Control: a made-up action to the same component | Allowed | Allowed |

- The custom action is the only way in that this run found. The provider's other actions are refused to the shell, or are the standard one above.
  The shell has no caller package, so it takes the "unknown caller" branch; a real app has one. That the framework limits an app to sending this action to its own package is recalled from AOSP, not measured here.
- All internal senders use explicit, same-uid intents: the alarm's `PendingIntent` and `requestImmediateUpdate` (`PrivacyWidgetProvider.kt`).
- The premise "the provider must be exported for system widget broadcasts" is doubtful. The system sends as the system uid, which is not subject to the exported check,
  and developer.android.com shows the widget receiver both ways. The Glance guide's "Declare a widget in the manifest" uses `android:exported="false"` with this filter;
  the same page's first snippet uses `true`. **This is a hypothesis until the device gates pass**, on Samsung's launcher too.

## Fix B options

| Option | Size | Closes the hole? | Risk |
|---|---|---|---|
| **1. Provider `exported="false"` (chosen)** | One manifest attribute | Yes, for every action from every other uid | Hosts or OEM launchers that need it exported: device gates on Pixel and Samsung. Debug extras need a new door. |
| 2. Custom action on a new non-exported receiver | New receiver; `onUpdate` needs a refactor, since `goAsync()` belongs to the provider's own `onReceive` | Yes | The alarm's `PendingIntent` changes component, so an update leaves the old alarm running beside the new one unless it is cancelled explicitly. |
| 3. Signature-level custom permission on the provider | Manifest permission plus `uses-permission` | Yes | Same effect as 1, with a new entry in the app's permission list. |
| 4. Secret token in the intent extras | Stored token, checks on every path | Yes | Most code, for a battery-only issue. |
| 5. Check the sender in `onReceive` | n/a | No | `getSentFromUid` is API 34+ and only works if the sender opts in. |

Choice: **option 1**, the smallest that closes the hole, and it leaves the alarm's `PendingIntent` identical, so Fix A can't end up with two alarms.
The custom action stays in the intent filter: the filter is not what protects it, and removing it would risk the explicit intents failing strict matching on future Android targets.
If a device gate fails on a launcher, option 2 is the fallback.

## Oliver's calls

| Question | Default |
|---|---|
| Re-arming restarts the 6-hour timer | Accepted: the existing call replaces the alarm, and its next run is 6 hours later. Opening the app refreshes the widget anyway, so the widget is never older than about 6 hours since the last refresh, and there are fewer wake-ups, not more. |
| Debug extras | Kept, through a new exported receiver that exists only in `src/debug`. Debug builds therefore still let another app trigger a scan; release builds don't contain it. |

## Invariants

1. A release build has no exported path into a widget scan: the merged manifest's provider is `exported="false"`, and no `WidgetDebugReceiver` is in the manifest or the dex.
2. Exactly one widget alarm after any number of app starts and package replacements; same type (`ELAPSED_REALTIME_WAKEUP`) and interval (6 h).
3. With no widget placed, app start and package replacement create no widget alarm. The snapshot alarm is still armed in both cases.
4. `AppWidgetManager.getInstance` can return null on a device without the widget feature, so the new app-start path must not crash there.
5. The system's widget broadcasts, the alarm, a tap and an app start still refresh an already placed widget after an update from an exported build.

## Milestones

| # | Milestone | Gate (evidence that proves it) |
|---|---|---|
| 1 | Implementation | `compileDebugKotlin`, `testDebugUnitTest --rerun` green, `WidgetManifestTest` included. |
| 2 | Fix A on both phones | `dumpsys alarm`: widget placed: force-stop 0, open 1, 5 more opens still 1, `install -r` still there. No widget: none after app start. |
| 3 | Fix B on both phones | From `adb shell`, the old action: no `onUpdate` log line, `last_update_timestamp` unchanged. A tap and an app start still refresh. |
| 4 | Update path | `install -r` over the exported build keeps the placed widget, and it still updates. A widget can be placed fresh on Samsung's launcher. |
| 5 | Release | `assembleRelease`: merged manifest and dex hold no debug receiver. |
| 6 | S8 observation | Card style at 76 % and 100 %: one screenshot each, say whether they differ. No code. |
| 7 | Restore | Both phones as found, both alarms where they were, crash buffers empty. |

## Device results (2026-11-07)

Pixel 8: Android 17, Pixel Launcher. S8: Android 9, Samsung launcher. Screenshots: `docs/screens/2026-11-07-widget-hardening/`.
Witnesses: the provider's debug log lines, `last_update_timestamp` (TS), `dumpsys alarm`. Alarm counts read widget / snapshot.

| Check | Pixel 8 | S8 |
|---|---|---|
| Update over the exported build, widget placed | Stays placed, draws 53 Fair, `Package replaced`, 1 / 1 | Stays placed, draws 17 Critical, `Package replaced`, 1 / 1 |
| No widget, then app start | 0 / 1 | 0 / 1 |
| Place while unexported | `ENABLED`, `UPDATE`, scan, real score, 1 widget alarm | Same |
| Resize, both ways | `APPWIDGET_UPDATE_OPTIONS` each way, redraws, no scan | Same |
| Font scale 2.0, then back | `UPDATE` x2, `UPDATE_OPTIONS` x3, scan at each change | 2.0: `UPDATE_OPTIONS` only. Back: nothing received. Redraws both times, no scan |
| Reboot | Process started for a broadcast to the provider, TS moved, 1 / 1, draws. Its `Received` lines rotated out of the log | `ENABLED`, `UPDATE`, scan 7 s after boot. Widget alarm at once; snapshot alarm only when the app's `BootReceiver` ran, 1 min 53 s after boot |
| Fix A: force-stop, then open | 0 / 0, then 1 / 1 | 0 / 0, then 1 / 1 |
| Fix A: 5 more cold starts | 1 / 1 after every one | 1 / 1 after every one |
| Update, not force-stopped | `Package replaced`, 1 / 1 | `Package replaced`, 1 / 1 |
| Fix B: old action from the shell, process dead | No `Received`, TS unchanged, no process. Three forms sent | Same, with three `Permission Denial … not exported` log lines |
| Fix B: process alive | No `Received`, TS unchanged | Same, one `Permission Denial` line |
| Control: `WidgetDebugReceiver` | `Received`, scan, TS moved | Same |
| Widget tap, app start | Scan, TS moved | Scan, TS moved, once past the first-run screen (see below) |
| Restore | One widget in the original cell, opacity 88.33 restored under the new id, 1 / 1, font 1.0 | Debug app and widget gone. Release 1.3.1 and its `lastUpdateTime` unchanged, snapshot alarm present |

Release build, no device: `WidgetDebugReceiver`, `applyExtras` and the `debug_*` strings have 0 hits in the dex and manifest (control string `PrivacyWidgetProvider`: hit). Provider `exported=false`.

**The hypothesis held:** on both launchers, the unexported provider is listed, places, receives the system's broadcasts, and survives an update from the exported build.

**Update of a force-stopped app, app not opened** (observation; not patched, Oliver decides):

| | Pixel 8 | S8 |
|---|---|---|
| Before | 0 / 0, `stopped=true` | 0 / 0, `stopped=true` |
| `Package replaced` | **Did not arrive** | **Did not arrive** |
| Provider | `APPWIDGET_UPDATE`, scan | `APPWIDGET_UPDATE`, scan |
| `BootReceiver` | Ran | Did not run |
| After 30 s | **1 / 1**, `stopped=false` | **0 / 0**, `stopped=false` |

So `MY_PACKAGE_REPLACED` doesn't reach a force-stopped app, on either version. The re-arm added to `PackageReplacedReceiver` only helps an app that wasn't force-stopped, and there the alarms had survived anyway (STATE, three `install -r`).
On Android 9, the alarms stay at 0 until the next app open or reboot. The widget still got that one update.

**Android 15+** (developer.android.com, "Changes to package stopped state"): a force-stop cancels all `PendingIntent`s, and `ACTION_BOOT_COMPLETED` is delivered when the app leaves the stopped state, so the app can re-register them.
That explains the Pixel 8's `BootReceiver` lines with no reboot, and it means the existing `BootReceiver` could already re-arm the alarm there. Fix A's app-start path is what the S8 needs: 6 schedule lines in 6 opens, and no `BootReceiver`.

**Other results:**
- Pixel 8, cold starts right after a force-stop: `BootReceiver` ran on all 6, so each armed the alarms twice (12 schedule lines) and ran two scans. Still exactly 1 alarm. Later cold start (9e): no `BootReceiver`, one scan. Why it repeats: not investigated. `am kill` is not a normal user path.
- S8 font-scale control on the old exported build: redraws at both changes, TS doesn't move. So the missing scan doesn't come from unexporting. Whether the old provider received `UPDATE_OPTIONS` couldn't be shown: it has no log line for it.
- S8, first-run screen: a tap or app start opens onboarding, and neither scans. Past the first-run screen, both do. This path isn't changed here.
- S8 reboot: the app's `BootReceiver` ran twice, in two processes; only the second armed the widget alarm. Cause not investigated.

**Opacity, S8, read-only** (`11-opacity-76.png`, `11-opacity-100.png`): 76 and 100 are stored and draw identically: 0 of 258,077 pixels differ inside the widget (rechecked in the main session). Card `(0,66,37)` in both, over a non-plain wallpaper around `(154,117,125)`, so the wallpaper isn't hiding a difference. Confirms the Android 8–11 item.

**Not verified:** a real second app sending the broadcast (the shell uid stood in for it); invariant 4 (no device without the widget feature); the last commit before review, a comment-only change, was not reinstalled.
**Crash coverage:** Pixel 8: exit reasons for the whole run show no crash, dropbox empty. S8: crash buffer empty from the Phase 5 reboot onwards; phases 0–4 aren't covered, because the buffer didn't survive the reboot.

## Device checks after this change

The old `am broadcast -n …/PrivacyWidgetProvider` form no longer reaches the provider. Debug extras now go to:
`adb shell am broadcast -n com.techtrest.privamatic.debug/com.techtrest.privamatic.WidgetDebugReceiver --ei debug_delta -6`

## STOP points

| When | What Oliver does |
|---|---|
| After implementation, unit tests and `compileDebugKotlin` | Switches to effort high and says continue. |
| After the diff review, before merge | Runs the external review (see CLAUDE.md, External review) and saves the findings file. |
