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

## Device checks after this change

The old `am broadcast -n …/PrivacyWidgetProvider` form no longer reaches the provider. Debug extras now go to:
`adb shell am broadcast -n com.techtrest.privamatic.debug/com.techtrest.privamatic.WidgetDebugReceiver --ei debug_delta -6`

## STOP points

| When | What Oliver does |
|---|---|
| After implementation, unit tests and `compileDebugKotlin` | Switches to effort high and says continue. |
| After the diff review, before merge | Runs the external review (see CLAUDE.md, External review) and saves the findings file. |
