# Review findings — `feat/rescan-in-place` (round 1)

Reviewed `docs/reviews/feat-rescan-in-place.diff` (`master..feat/rescan-in-place`) against `docs/decisions/v1.6-fix-to-result.md` §4.2, `CONVENTIONS.md`, the touched source/test files, and `PrivacyWidgetProvider.onUpdate`, `PrivacySnapshotRepository.recordScan`, and `ScoreHistoryRepository.recordScore`.

---

## High

### 1. Cancelled scan failing after `runScan` corrupts the newer scan, and unguarded `applyMissedTrustChange()` in `catch` can crash the app
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/viewmodel/PrivacyViewModel.kt:218-237` (specifically `:222`, `:226-232`)
- **Invariants violated:** Invariant 9 ("A cancelled or failed scan never overwrites a newer result, never ends the newer scan's progress state, never consumes its pending `before`, and never refreshes the widget").
- **Concrete failure scenarios:**
  1. **Cancelled scan failing in `onScanSuccess` / `applyMissedTrustChange`:** `runScan` (`PrivacyViewModel.kt:84`) guards exceptions inside `privacyScanner.performCompleteScan()` with `currentCoroutineContext().ensureActive()`, so a cancelled scan failing inside `runScan` rethrows `CancellationException`. However, `performScan`'s own `catch (e: Exception)` at line 226 has **no** `currentCoroutineContext().ensureActive()` (and no check that this coroutine is still `currentScanJob`). If Scan 1 finishes `runScan`, enters `onScanSuccess` (e.g., `trustedPackages.first()` at line 296 or `reloadHistory()` Room queries at lines 285–286) or `applyMissedTrustChange()` at line 222, gets cancelled by a newer Scan 2 (`currentScanJob?.cancel()`), and the in-flight call throws a non-`CancellationException` (`SQLiteException`, `IOException`, `IllegalStateException`) on its way out, Scan 1 enters `catch (e: Exception)` at line 226 and executes:
     - `_isRefreshing.value = false` (ends Scan 2's progress bar and re-enables "Check again" while Scan 2 is still running),
     - `_scanEvents.trySend(ScanEvent.Failed(it.trigger))` (shows "Couldn't check" over Scan 2),
     - `pendingUserScan = null` (destroys Scan 2's pending `before` and `trigger`, so Scan 2 finishes with no snackbar and does not close the Quick Win page),
     - `applyMissedTrustChange()` (can refresh the widget).
  2. **Uncaught exception in `catch`:** `applyMissedTrustChange()` calls `trustedAppsRepository.trustedPackages.first()` (line 244), which reads DataStore and can throw `IOException`. If `applyMissedTrustChange()` throws at line 222 (after `onScanSuccess` has already succeeded), execution enters `catch (e: Exception)` at line 226 and calls `applyMissedTrustChange()` a second time at line 232 **outside any `try/catch`**, crashing `viewModelScope` (and the app). The same crash occurs if `runScan` fails and `applyMissedTrustChange()` at line 232 throws while reading DataStore.
- **Confidence:** High.

### 2. `ScanFeedback.between` drops checks present on only one side and hits vacuous `emptyList().all { it.isFixed } == true`, producing `"0 checks fixed · +N pts · Score S"`
- **Location:** `app/src/main/java/com/techtrest/privamatic/data/ScanFeedback.kt:61-65`, `app/src/main/java/com/techtrest/privamatic/data/ScanFeedback.kt:112`
- **Invariants violated:** Invariant 11 ("Every snackbar is true for what it compares").
- **Concrete failure scenario:**
  `ScanFeedback.between` maps `before.issues` by check, then iterates only `after.issues` and drops any check missing from `beforeIssues` (`val old = beforeIssues[new.check] ?: return@mapNotNull null`, line 63). Checks present in `before.issues` and absent from `after.issues` are never iterated at all.
  - If a failing check (deduction > 0) was in `before.issues` and is omitted from `after.issues` (for example, any checker that omits non-applicable/uninstalled checks — note line 63 already anticipates mismatched check sets):
    1. `points = after.score - before.score > 0`, `manual == 0`, `worseCount == 0`.
    2. `better` is **empty**, so `single` is `null`.
    3. Line 112 evaluates `better.all { it.isFixed }`, which in Kotlin is **vacuously `true` for an empty list**, returning `ScanOutcome.ChecksFixed(count = 0, points = points, score = after.score)`.
    4. `ScanFeedbackText.text` formats `R.plurals.plural_snackbar_checks_fixed` with `0` → **`"0 checks fixed · +N pts · Score S"`** (directly reproducible in a unit test via `ScanFeedback.between(score(fail(dns, 6)), score(), UserScan.Rescan)`).
  - Related asymmetric cases from the same root cause:
    - If a failing check disappears *and* one other check `A` is fixed in the same scan, `single == A` and `gain()` returns `CheckFixed(A, combinedPoints, score)`, attributing both checks' points solely to `A`.
    - If a new failing check appears only in `after.issues` while another check improves by the same number of points (`points == 0`), `worseCount == 0` and `betterCount == 1`, so line 86 takes the clamp-at-0 branch and returns `NoChange(after.score)` (`"No change · Score S"`) instead of `Mixed`.
  - *(Note: checkers outside the diff were not read per review scope, so which exact production checks can be omitted between scans was not verified here; however, the bug is unconditional in `ScanFeedback.between`/`gain` and line 63 explicitly expects non-identical check sets.)*
- **Confidence:** High (for the `ScanFeedback` bug and unit-level reproduction); Medium on how frequently production checkers omit a check between two scans without reading the checkers.

---

## Medium

### 3. `scanEvents` is collected in `LaunchedEffect(viewModel)` without lifecycle gating, so snackbars for scans finishing while backgrounded are silently lost
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:179-232`
- **Concrete failure scenario:**
  `PrivacyViewModel.scanEvents` uses `Channel(Channel.BUFFERED)` (`PrivacyViewModel.kt:104-105`), which buffers events while nobody is collecting. However, `MainScreen` collects `viewModel.scanEvents` directly inside `LaunchedEffect(viewModel)` without `lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)`. In Compose, `LaunchedEffect` remains active when the Activity enters `ON_STOP`.
  1. User taps **Check again** on `QuickWinDetailScreen` (starting an ≥ 800 ms scan) and immediately taps **Open settings** (still enabled while `isChecking == true`), or taps top-bar Rescan / pull-to-refresh / Mark as Done and switches apps or locks the phone within 800 ms.
  2. `MainActivity` moves to `ON_STOP` while `LaunchedEffect(viewModel)` stays active.
  3. The scan finishes in the background and sends `ScanEvent.Finished` (or `ScanEvent.Failed`).
  4. `MainScreen` immediately consumes the buffered event while backgrounded and runs `snackbarHostState.showSnackbar(..., duration = SnackbarDuration.Short)` (4 s). The snackbar expires while Privamatic is not visible.
  5. When the user returns > 4 s later, the result snackbar is gone and never shown (and if `showQuickWinDetail` closed in the background, the user returns to Actions with no feedback).
- **Confidence:** High.

### 4. `onScanSuccess` mutates `_historySnapshots` (and persisted stores) before its final suspension point in `reloadHistory()`
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/viewmodel/PrivacyViewModel.kt:284-287`, `app/src/main/java/com/techtrest/privamatic/ui/viewmodel/PrivacyViewModel.kt:290-307`
- **Concrete failure scenario:**
  The comment on `onScanSuccess` states: *"Records the scan, then publishes everything at once: no suspension after the first assignment, so a cancelled scan never leaves half of its result on screen."*
  However, line 300 calls `reloadHistory()`, which writes `_historySnapshots.value = snapshotRepository.getSnapshots(_selectedFilter.value)` (line 285) **before** suspending on `snapshotRepository.hasAnySnapshot()` (line 286).
  1. Scan 1 finishes `runScan`, records `score_history` (line 298) and Room snapshot (line 299), enters `reloadHistory()`, assigns `_historySnapshots.value` (line 285), and suspends at `hasAnySnapshot()` (line 286).
  2. Scan 2 cancels Scan 1 while suspended at line 286.
  3. Scan 1 exits via `CancellationException`, leaving `_historySnapshots` updated to Scan 1's snapshot while `_hasHistory`, `_scanState`, `_scoreHistory`, and `_rawPrivacyScore` still hold the previous scan's state. If Scan 2 subsequently fails, persisted `score_history` and Room also contain Scan 1's score while the UI shows the older score and reports "Your last result is still shown."
- **Confidence:** High.

---

## Low

### 5. A trust change during `onScanSuccess`'s Room I/O causes the snackbar to show the pre-trust score while the screen immediately jumps to the post-trust score (and refreshes the widget twice)
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/viewmodel/PrivacyViewModel.kt:220-222`, `app/src/main/java/com/techtrest/privamatic/ui/viewmodel/PrivacyViewModel.kt:296-318`
- **Concrete failure scenario:**
  1. Screen shows score 50 (`trusted = {}`). User triggers a rescan (`_isRefreshing = true`).
  2. `onScanSuccess` reads `trusted = {}` at line 296, computes `adjustedScore = 50`, and suspends in `snapshotRepository.recordScan` / `reloadHistory` (lines 299–300).
  3. During that suspension, the user trusts a 1-pt app (`trusted = {pkg}`). The `trustedPackages` collector at line 165 skips because `_isRefreshing.value` is still `true`.
  4. `onScanSuccess` resumes, publishes `_scanState = Success(50)`, and emits `ScanEvent.Finished(NoChange(50))`. Line 221 requests a widget update, and line 222 (`applyMissedTrustChange`) immediately detects `{pkg} != {}`, updates `_scanState = Success(51)`, and requests a **second** widget update.
  5. The user sees **Score 51** on screen at the same instant the snackbar says **`"No change · Score 50"`**, and two back-to-back widget scans run.
- **Confidence:** High (very narrow timing window during Room I/O).

### 6. `showQuickWinDetail` only updates/closes on `FixReturn` and `CheckAgain`, leaving a stale Quick Win page open after other scans
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:183-190`
- **Concrete failure scenario:**
  1. User fixes a setting outside the app, returns, taps top-bar **Rescan** (or taps **Mark as Done** on a manual review, e.g., Location review after revoking background location access), and within the ≥ 800 ms scan duration taps a Quick Win tile to open `QuickWinDetailScreen`.
  2. The scan finishes with `event.trigger = UserScan.Rescan` (or `UserScan.ManualDone`) and removes that Quick Win from `event.quickWins` (or reduces its gain), showing a snackbar such as `"Background Wi-Fi Scanning fixed · +2 pts · Score N"`.
  3. Because line 184 restricts the update to `FixReturn || CheckAgain`, `showQuickWinDetail` stays open with stale points for an Action that is already gone.
- **Confidence:** High.

### 7. "Check again" button on `QuickWinDetailScreen` is silently a no-op if `quickWin.relatedCheck` is `null`
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:571-573`, `app/src/main/java/com/techtrest/privamatic/ui/screens/QuickWinDetailScreen.kt:229-241`
- **Concrete failure scenario:**
  `QuickWinDetailScreen` always renders the "Check again" `OutlinedButton` (even outside `if (actionType != null)`), while `MainScreen` implements `onCheckAgain` as `quickWin.relatedCheck?.let { viewModel.performScan(UserScan.CheckAgain(it)) }`. If any `QuickWin` has `relatedCheck == null` (a nullable type explicitly handled at `QuickWinDetailScreen.kt:210` and `PrivacyViewModel.kt:329`), tapping "Check again" silently does nothing (no scan, no "Checking…", no snackbar).
- **Confidence:** Medium (did not inspect `QuickWinsDetector.kt` outside the diff to check whether any current `QuickWin` sets `relatedCheck = null`).

### 8. `isBreakdownRequested` can stay latched `true` if pager navigation from Dashboard is interrupted before Details composes
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/navigation/AppNavigationState.kt:36-47`, `app/src/main/java/com/techtrest/privamatic/ui/screens/DetailsScreen.kt:207-213`, `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:368`
- **Concrete failure scenario:**
  `HorizontalPager` uses `beyondViewportPageCount = 1`, so when on Dashboard (page 0), Details (page 2) is not composed. If the user taps **Details** on a loss snackbar (setting `isBreakdownRequested = true`) and interrupts `animateScrollToPage(2)` by tapping Dashboard or Actions before page 2 composes, `ChecksTab`'s `LaunchedEffect(isBreakdownRequested)` never runs to clear the flag. Later, when the user manually navigates to Details → Checks expecting their Checks list, `ChecksTab` immediately flips to `ChecksView.BREAKDOWN` and persists it in `DetailsViewPreferences`.
- **Confidence:** High.

---

## Test Audit

- **`StockSafeAdviceTest.kt`:** Tests genuinely match their names — verifies `quick_win_uninstall_app_instructions` contains no `"disabl"`, checks the exact three steps without trailing full stops, and checks all strings in `strings.xml` contain no `"rescan to see"`.
- **`ScanFeedbackTest.kt`:** All 33 tests genuinely test what their names claim using real `PrivacyScoreCalculator.calculateScore` outputs and exact `ScanOutcome`/`PendingUserScan` assertions. **Gap:** No test covers asymmetric `before.issues` vs `after.issues` (a check present on only one side), which missed Finding 2.
- **`ScanFeedbackTextTest.kt`:** Genuinely tests exact rendered strings for every `ScanOutcome` subtype, singular/plural forms, NBSP (`Score\u00A0N`), and U+2212 (`−`) directly against `strings.xml` and `R.string`/`R.plurals`.
- **`RunScanTest.kt`:**
  - `a failing scan can be caught and never reaches the crash handler` genuinely tests `runScan` in a `SupervisorJob + CoroutineExceptionHandler` scope.
  - `a cancelled scan that fails on its way out is cancelled, not failed` genuinely triggers `error("scan failed")` after cancellation (because `withContext(NonCancellable)` on the same dispatcher resumes via `UndispatchedCoroutine.afterResume` without re-checking cancellation on the outer job), proving `runScan`'s `ensureActive()` turns the failure back into `CancellationException`. Minor test fragility: it does not assert that `error("scan failed")` was actually reached (e.g., via a flag before `error(...)`), so a change in `withContext` behavior could make it pass vacuously.
  - **Gaps:** `a scan's result comes back` passes `minimumMs = 0`, so no test verifies the minimum-duration delay; and no test covers `PrivacyViewModel.performScan` / `onScanSuccess` / `applyMissedTrustChange` cancellation or failure paths (Finding 1).

---

## Invariants & Unverified-on-Phone Checklist

- **1 (Offline):** Holds. No permissions, network calls, or dependencies added.
- **2 (Backup / D2D exclusions):** Holds. No new persisted files, prefs, or DB tables; all new state is in memory.
- **3 (Room schema):** Holds. No schema changes.
- **4 (Scoring source of truth):** Holds. `ScanFeedback` reads `PrivacyScore` / `PrivacyIssue.pointDeduction` / `ManualCheckType.pointValue`; trust math uses `TrustedAppsAdjuster.computeAdjustedScore`.
- **5 (Disabled apps detection):** Holds. No checker touched.
- **6 (Strings in `strings.xml`):** Holds for all new user-facing copy (including U+2212 minus sign and no exclamation marks).
- **7 (Unsigned release builds):** Holds. Build config untouched.
- **8 (History & `score_history` once before display; widget/cap unchanged):** Holds on normal completion (`recordScore` and `recordScan` run once with the trust-adjusted score before `_scanState` is published; widget and repositories untouched). Note Finding 4 for the cancellation-during-`reloadHistory` edge case.
- **9 (Cancelled/failed scan isolation):** **Fails** on post-`runScan` cancellation/failure in `PrivacyViewModel.performScan` (Finding 1) and partially on cancellation inside `reloadHistory()` (Finding 4). The intended trust-change-during-scan exception works on normal `runScan` failure, subject to Finding 1 (if DataStore throws) and Finding 5 (narrow Room I/O window).
- **10 (No scoring change; snackbar compares in-memory scores only):** Holds. `ScanFeedback.between` compares only in-memory scores and never reads `score_history`.
- **11 (Every snackbar true; no `+0 pts`; Quick Win closes only when Action is gone):** **Fails** for asymmetric issue sets in `ScanFeedback.between`/`gain` (`"0 checks fixed · +N pts · Score S"`, Finding 2), plus narrow edge cases in Findings 5 and 6. App-start scans (`trigger = null`), clamp-at-0 (`NoChange(0)`), and double-tap `before` preservation all hold.
- **TalkBack (unverified on phone):** Material 3 `SnackbarHost` uses `LiveRegionMode.Polite` and extends action snackbar timeouts via `LocalAccessibilityManager`; `QuickWinDetailScreen`'s disabled `"Checking…"` button and `LinearProgressIndicator` expose standard semantics; the overlay `SnackbarHost` `Box` does not intercept touches. One caveat to check on-device: when `showQuickWinDetail` closes in the same frame a snackbar is shown (`MainScreen.kt:187, 223`), verify TalkBack does not swallow the polite snackbar announcement behind the screen-change announcement.

---

## Resolution (Claude Code, 2026-11-25)

Proof first: the view model's collaborators became constructor parameters (no logic change), then 24 of 312 tests failed on the unfixed logic. After the fixes: 312 tests, 0 failures. Phone = Pixel 8; "pre-fix build" = `21d6135`. Screens: `docs/screens/rescan-in-place/`, c0 = pre-fix, c = fixed.

1. **High 1: proven, fixed.** 5 tests in `PrivacyViewModelScanTest` failed ("the newer scan's progress must go on"; `IOException: DataStore` reached the uncaught handler). `performScan` has one catch: `ensureActive()` first, then `onScanFailed`. The failure path reads nothing from storage: the trusted set comes from `latestTrusted`, kept by the collector. Also found and fixed, both with tests: a superseded scan that noticed late published its result (`ensureActive()` before the first assignment); a scan failing with a cancellation that wasn't its own left `isRefreshing` on for good. No failed scan was forced on a phone.
2. **High 2: proven at unit level, fixed.** 10 tests in `ScanFeedbackTest` failed, the reviewer's three cases among them. Both sides' checks are compared; a check that disappeared counts in the points only; a failing check that appears is worse. A test over all combinations of two checks never sees `ChecksFixed` below 2. Not reachable today: `PrivacyScanner` always adds the same checks. Choice, accepted by Oliver: a check that disappears while another gets worse for the same points says "1 better, 1 worse".
3. **Medium 3: proven on the phone, fixed.** Pre-fix build: the app stopped 240 ms after the rescan tap and 15 s later no snackbar showed, score 53 → 51 (c0-m3-loss-2; also c0-m3-nochange). Fixed build: stopped at most 272 ms (loss) and 267 ms (no change) after the tap, both results showed on return (c17b, c18b), same Activity, and only once (c19b).
4. **Medium 4: proven, fixed.** `a scan cancelled while it reads the history publishes none of it` failed. `readHistory` reads, `publishHistory` assigns with no suspension.
5. **Low 5: proven, fixed; fell out of 1.** Snackbar 97, screen 100. The publish uses the latest trusted set, so both agree and the widget refreshes once. Today's point keeps the set it was recorded with until the next scan (decided 21 Nov). Not checked on a phone (Trusted is empty on the Pixel 8).
6. **Low 6: proven, fixed.** 3 tests in `ScanEventTest` failed for `Rescan` and the other triggers. Phone: page opened during a plain rescan (c15) closed with "… fixed · +2 pts" (c16).
7. **Low 7: rejected.** All 13 `QuickWin(…)` constructions in `QuickWinsDetector`, the only ones in the app, set a non-null `relatedCheck`.
8. **Low 8: proven, fixed in a second round.** Two `AppNavigationStateTest` tests failed (a tab change kept the request). Round 1 of the fix failed on the phone: Details composes as soon as the move starts, so Checks switched and saved Breakdown before the swipe cut the move off (c24). Round 2: the request only shows Breakdown; it is saved and cleared once the pager has settled on Details. Phone, animations 10 times slower: 2 of 2 swipes → Dashboard page and tab, then Details in LIST (c33, c34); completed moves still save Breakdown (c30), also from Details itself (c32). The tab-tap interruption is covered by the unit tests only: on the phone no tap cut a snackbar's move off (a Dashboard tap during it had no visible effect in 4 tries with slow animations and 2 at normal speed on the pre-fix build; not explained).

Snackbar over Check again (Oliver, 24 Nov): reproduced on the pre-fix build, overlap 54 px (c0-covered). Fixed build: 104 px apart in four checks (c07, c09, c10, c13); a hand scroll stays (c11); nothing left over afterwards (c12).

Test gaps: 800 ms minimum (`a rescan lasts at least 800 ms`, `a fast scan's result waits for the minimum`); `RunScanTest` asserts that the failure was reached. Mutations caught: minimum 0, `runScan` without its delay, each of the three cancellation guards removed.

Found, not changed: tapping a tab while the pager is still moving to another leaves it stuck between two pages (c36, c37, c38, animations 10 times slower, no snackbar involved). The lines that do it are the same on `master`; not checked on a `master` build.
