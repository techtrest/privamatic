Review docs/reviews/fix-widget-refresh-hardening.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..fix/widget-refresh-hardening: the 6-hour widget alarm is
re-armed on app start and MY_PACKAGE_REPLACED if a widget is placed; PrivacyWidgetProvider becomes
android:exported="false" so other apps can't trigger a full scan; debug-only widget extras move to an
exported WidgetDebugReceiver in src/debug). You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata, tests
and screenshots unless they are the point of the run. If you need more than
~10 files, stop and say which and why.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review. Device results are in docs/specs/widget-refresh-hardening.md, "Device results":
Pixel 8 (Android 17) and Samsung S8 (Android 9) passed placement, update from an exported build, resize,
reboot, tap and app start; the old action sent from the shell ran no scan. Known and left alone on purpose: MY_PACKAGE_REPLACED
does not reach a force-stopped app, so on the S8 the alarms stay off after such an update until the next
open or reboot. Oliver decides that one. Don't report it unless you find a case the spec's table misses.
Accepted: each re-arm restarts the 6-hour timer. Accepted: debug builds stay open through WidgetDebugReceiver.

Standing invariants — check that each holds on every path this diff touches:
1. Offline: no INTERNET permission, no network calls, no new dependency that
   can reach the network.
2. User data never leaves the device: score history (Room), the trusted list
   and settings are excluded from cloud backup and device transfer, in both
   data_extraction_rules.xml (Android 12+) and the legacy backup rules.
3. Room: every schema change bumps the version and ships a migration; no
   destructive fallback; existing score history survives an upgrade.
4. Scoring: the PrivacyCheck enum is the single source of truth for
   deductions; trust math goes through PrivacyCheck.cappedDeductionFor();
   no score logic elsewhere.
5. Detection: a disabled app still returns ApplicationInfo — check
   appInfo.enabled before treating a package as active.
6. All user-facing text comes from strings.xml.
7. Release builds stay unsigned (signingConfig = null) for F-Droid.

Run-specific invariants:
8. A release build has no path from another app into a widget scan: the provider is not exported, and
   nothing in src/debug can reach a release manifest or dex. Look for any other exported component
   that a foreign app could use to trigger a scan silently.
9. Scheduling stays idempotent: the same PendingIntent identity (component, action, request code) on every path, so any
   number of app starts and package replacements leaves one widget alarm, with the same type
   (ELAPSED_REALTIME_WAKEUP) and interval (6 h). No widget placed: no widget alarm is created.
10. The new app-start call can't crash or noticeably delay MainActivity.onCreate, including on a device
    where AppWidgetManager.getInstance returns null.
11. The system's widget broadcasts, the alarm, a widget tap, an app start and the configuration screen's Save all
    still refresh a placed widget now that the provider is unexported, on API 26–37. Name any host or
    launcher path you know that sends directly to the provider rather than through the system.
