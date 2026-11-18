No High or Medium findings. Two Low findings.

Low
1. An app open that does not refresh the widget still postpones the 6-hour alarm. Confidence: high.

MainActivity.onCreate always rearms before it decides whether to scan:


MainActivity.kt
Ln 26–41

SnapshotReceiver.scheduleMidnightSnapshot(this)
PrivacyWidgetProvider.rearmPeriodicUpdatesIfPlaced(this)
setContent {
    PrivacyWidgetTheme {
        val onboardingPrefs = remember { OnboardingPreferences(applicationContext) }
        var onboardingComplete by remember { mutableStateOf(onboardingPrefs.isComplete()) }
        // ...
            if (onboardingComplete) {
                MainScreen()
            } else {
                OnboardingScreen(
schedulePeriodicUpdates sets the next fire to now + 6 h (PrivacyWidgetProvider.kt:299-303). The scan that would refresh the widget lives in PrivacyViewModel.init (PrivacyViewModel.kt:113-123 and 174), and MainScreen is what creates that ViewModel (MainScreen.kt:71, viewModel()).

Failure: a widget is placed at noon, so the alarm is 18:00 and the score is from noon. At 17:00 the user opens the app, onboarding is still incomplete, and they leave. No scan runs. The alarm moves to 23:00, so the home-screen score is 11 hours old when it next updates. The same slide happens on a configuration change after onboarding is done: the activity is recreated, viewModel() keeps the existing ViewModel, init does not run again, and the alarm moves 6 hours out from the rotation with no new scan.

2. The debug receiver crashes if AppWidgetManager.getInstance returns null. Confidence: medium.


WidgetDebugReceiver.kt
Ln 16–21

override fun onReceive(context: Context, intent: Intent) {
    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
        ComponentName(context, PrivacyWidgetProvider::class.java)
    )
    WidgetDebugOverride.applyExtras(context, intent, ids)
    PrivacyWidgetProvider.requestImmediateUpdate(context)
The app-start path treats a null manager as "no widgets" and returns (PrivacyWidgetProvider.kt:314). This new receiver dereferences it. Failure: a debug build on a device with no widget service, then am broadcast to WidgetDebugReceiver, throws a NullPointerException in the app process. The API 35 stub leaves getInstance unannotated, so a null return does crash this call. AOSP itself usually returns a manager whose service is null, and getAppWidgetIds then returns an empty array, which is why this stays Low.

Invariants
1–7. This diff adds no permission, network call, dependency, backup rule, Room change, score math, package detection, user-facing string, or signing config.

Release has no silent scan path from another app. PrivacyWidgetProvider is exported="false" (AndroidManifest.xml:66-68). WidgetDebugReceiver is only in src/debug. The other exported components are MainActivity (visible), WidgetConfigurationActivity (finishes immediately without a widget id; a scan runs only from the Save button at WidgetConfigurationActivity.kt:80-84), BootReceiver (returns unless the action is BOOT_COMPLETED, BootReceiver.kt:21), and PackageReplacedReceiver (returns unless the action is MY_PACKAGE_REPLACED, PackageReplacedReceiver.kt:19). Both of those actions are protected broadcasts.

Every schedule goes through schedulePeriodicUpdates → buildAlarmPendingIntent (PrivacyWidgetProvider.kt:339-348): component PrivacyWidgetProvider, action ACTION_UPDATE_WIDGET, request code 1001, ELAPSED_REALTIME_WAKEUP, 6 h. setInexactRepeating replaces an alarm with that same identity. rearmPeriodicUpdatesIfPlaced calls it only when getAppWidgetIds is non-empty (PrivacyWidgetProvider.kt:315-318). BootReceiver uses the same function and the same empty-id check (BootReceiver.kt:30-35).

The new onCreate call returns when getInstance is null, and otherwise does one widget-id lookup plus, only if a widget is placed, one alarm set. That does not crash or add a delay a person would notice on onCreate.

Pixel Launcher and Samsung's launcher update widgets through AppWidgetService, which sends the provider broadcast as the system uid. That reaches an unexported receiver. I don't know a current Pixel, Launcher3, or Samsung path that broadcasts to the provider as the launcher's own uid. A non-system sender of APPWIDGET_UPDATE was already refused with SecurityException while the provider was still exported. The alarm, a tap, an app start, and Save all refresh through an explicit same-uid ACTION_UPDATE_WIDGET (requestImmediateUpdate, PrivacyWidgetProvider.kt:332-336), which an unexported receiver still receives.

---

## Dispositions (Claude Code, 2026-11-18)

**L2 (debug receiver, null AppWidgetManager): rejected, no change.** `WidgetDebugReceiver` lives only in `src/debug` and is not in the release
APK. On AOSP, `AppWidgetManager.getInstance` returns a manager whose service may be null, not null itself, and `getAppWidgetIds` then
returns an empty array (the finding says so too). No device we have, or can get, returns null, so the crash can't be shown on a device.

**L1 (app start postpones the 6-hour alarm): accepted, proven, fixed.**
- Proof on the unfixed build (Pixel 8, Android 17, run `l1-proof`): with widget 24 placed, the widget alarm's absolute fire time moved
  190943370 → 191005245 → 191059891 elapsed ms over two cold starts (`am kill`, no force-stop): +61.9 s and +54.6 s, the time between the starts.
- Fix: `rearmPeriodicUpdatesIfPlaced` arms only when a widget is placed and the alarm's PendingIntent is missing (`FLAG_NO_CREATE`, same
  component, action and request code 1001 as `buildAlarmPendingIntent`). `cancelPeriodicUpdates` now also cancels that PendingIntent, so
  it exists only while the alarm is set. The same function serves `PackageReplacedReceiver`, which therefore no longer postpones it either.
  Unit test `WidgetAlarmRearmTest` (decision only; the PendingIntent lookup needs a device).
- Gate (Pixel 8, run `l1-gate`): install and two cold starts leave the fire time at 191059891, 1 alarm, no arm logged; force-stop → 0 widget
  alarms and no widget PendingIntent record, next start → 1 alarm (armed by the start, then again by `BootReceiver` 335 ms later); rotation
  to landscape and back → fire time unchanged, same pid. Crash buffer empty.
- Cancel path (Pixel 8, run `cancel-gate`, widget removed and re-added by Oliver): before, 1 widget alarm and its PendingIntent (request code 1001);
  after removal, 0 alarms and no request code 1001 record (log: `APPWIDGET_DISABLED`, "Periodic widget updates cancelled"), snapshot alarm
  unchanged; after re-adding with Save, 1 new alarm about 6 h out (fire time = arm time + 6 h), a new request code 1001 record, widget id 26.
  Crash buffer empty.

No High or Medium findings: last round.
