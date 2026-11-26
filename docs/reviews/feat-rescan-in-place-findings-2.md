# Review findings — `feat/rescan-in-place` (round 2)

Reviewed `docs/reviews/feat-rescan-in-place.diff` (`master..feat/rescan-in-place`) against `docs/decisions/v1.6-fix-to-result.md` §4.2, `CONVENTIONS.md`, round 1 resolutions (`docs/reviews/feat-rescan-in-place-findings-1.md`), the touched source/test files, `QuickWinsDetector.kt`, `PrivacyWidgetProvider.onUpdate`, `PrivacySnapshotRepository.recordScan`, and `ScoreHistoryRepository.recordScore`.

---

## Round 1 Fixes & Rejected Finding Judgement

- **H1 (cancelled scan failing after `runScan`; storage read in failure path): Verified fixed.** `performScan` (`PrivacyViewModel.kt:236-245`) has a single `catch (e: Exception)` calling `ensureActive()` before `onScanFailed(e)`. `onScanFailed` (`PrivacyViewModel.kt:248-261`) performs no storage read and applies any missed trust change via in-memory `latestTrusted`. Both extra fixes also hold: `currentCoroutineContext().ensureActive()` runs after `readHistory()` (`PrivacyViewModel.kt:328`) with zero suspension points before publish completes, and a non-self `CancellationException` thrown by `scan()` passes `ensureActive()` in both `runScan` and `performScan` and ends cleanly via `onScanFailed`.
- **H2 (`ScanFeedback.between` dropping one-sided checks / `0 checks fixed`): Verified fixed.** `ScanFeedback.between` (`ScanFeedback.kt:64-66`) compares the union `(beforeIssues.keys + afterIssues.keys)`. `gain()` checks `better.any { it.isGone }` first (returning points-only `Gain`) and guards `ChecksFixed` with `better.isNotEmpty() && better.all { it.isFixed }` (`ScanFeedback.kt:103, 115`).
- **M3 (`scanEvents` lost when backgrounded): Verified fixed.** `MainScreen.kt:248-253` collects `viewModel.scanEvents` inside `lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)` with `withContext(Dispatchers.Main.immediate)`, while `snackbarScope` outlives the collector so showing a snackbar is not cancelled on `ON_STOP`.
- **M4 (`onScanSuccess` half-publishing `_historySnapshots`): Verified fixed.** `readHistory()` reads `hasAnySnapshot()` and `getSnapshots(...)` into a `History` object, `ensureActive()` runs at `PrivacyViewModel.kt:328`, and `publishHistory(history)` plus all state assignments run synchronously with no further suspension points.
- **L5 (trust change during Room I/O causing snackbar/screen mismatch and double widget refresh): Verified fixed.** `onScanSuccess` uses `val trusted = latestTrusted ?: recordedTrust` (`PrivacyViewModel.kt:332`) for `_scanState`, `shownTrusted`, and `ScanEvent.Finished`, issuing one `requestWidgetUpdate()`.
- **L6 (`showQuickWinDetail` only updating on `FixReturn` / `CheckAgain`): Verified fixed.** `MainScreen.kt:206` applies `showQuickWinDetail = showQuickWinDetail?.let(event::updated)` for every `ScanEvent.Finished` regardless of trigger.
- **L7 (rejected: "Check again is a no-op if `quickWin.relatedCheck` is null"): Rejection confirmed.** All 13 `QuickWin(...)` constructions in `QuickWinsDetector.kt` (lines 61, 71, 81, 93, 103, 113, 133, 146, 157, 168, 179, 190, 204) pass a non-null `relatedCheck`. `QuickWinsDetector.detectQuickWins` is the sole source of `QuickWin` instances entering `PrivacyViewModel._rawQuickWins` / `filteredQuickWins` / `ScanEvent.Finished.quickWins`, so no `QuickWin` with `relatedCheck == null` can reach `QuickWinDetailScreen` in production.
- **L8 (`isBreakdownRequested` latched when interrupted): Mostly fixed, with two residual pager-settle edge cases** (Findings 1 and 2 below). `AppNavigationState` drops the flag on non-Details / non-Checks selection, and `ChecksTab` previews `BREAKDOWN` without persisting until `isSettled`.
- **New `QuickWinDetailScreen(snackbarHeight)`:** Clean. `.onSizeChanged` is placed after `.windowInsetsPadding(WindowInsets.navigationBars)` on `SnackbarHost` (`MainScreen.kt:631-640`), so `snackbarHeight` measures only the snackbar itself; `QuickWinDetailScreen` pads below `paddingValues` by `snackbarHeight` and scrolls to `maxValue` after `withFrameNanos { }` only when `snackbarHeight` increases while the page is open (`QuickWinDetailScreen.kt:85-93, 135`).

---

## High

None.

---

## Medium

### 1. Swipe-interrupt recovery waits only for `!pagerState.isScrollInProgress`, which can fire in the drag→fling gap and leave the tab and `isBreakdownRequested` out of sync
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:187-195`
- **Invariants violated:** Invariant 13 ("A Breakdown request never changes Details > Checks unless the user arrives there through it").
- **Concrete failure scenario:**
  In Compose Foundation `ScrollableNode` / `DefaultScrollableState`, `isScrollInProgress` is `true` only while a `scroll { ... }` block is active. When a user interrupts `pagerState.animateScrollToPage(2)` with a swipe and lifts their finger with fling velocity, the drag's `scroll(MutatePriority.UserInput)` exits first and sets `isScrollingState.value = false` (scheduling `Snapshot.sendApplyNotifications()` immediately), and only afterwards does `onDragStopped` launch the fling/snap coroutine (`scroll(MutatePriority.Default)`).
  1. User is on **Actions** (page 1, `settledPage == 1`), with Details > Checks saved in `LIST` view.
  2. User taps **Details** on a loss snackbar (`openBreakdown()` sets `selectedTab = DETAILS`, `isBreakdownRequested = true`; `animateScrollToPage(2)` starts).
  3. After the pager crosses halfway (`pagerState.currentPage == 2`, `currentPageOffsetFraction < 0f`), the user flicks back toward Actions and lifts their finger while `currentPage` is still `2` (the fling carries the pager back to page 1).
  4. In the drag→fling gap, `isScrollInProgress` momentarily becomes `false` before the fling starts. `snapshotFlow { pagerState.isScrollInProgress }.first { !it }` (`MainScreen.kt:193`) completes while `pagerState.currentPage == 2` and runs `navigationState.selectTab(NavigationTab.DETAILS)` (`MainScreen.kt:194`), keeping `selectedTab == DETAILS` and `isBreakdownRequested == true`.
  5. The fling then finishes on **Actions** (page 1). Because `pagerState.settledPage` remained `1` throughout, `LaunchedEffect(pagerState)` (`MainScreen.kt:175-179`) sees no change and does not emit.
  6. The pager is now on Actions while the bottom bar highlights Details and `isBreakdownRequested` stays `true`. On a later manual swipe to Details, `ChecksTab` sees `isBreakdownRequested && isSettled` and switches to and saves `ChecksView.BREAKDOWN`. (Starting on Dashboard and flicking back from `currentPage == 2` likewise leaves Dashboard shown with Details highlighted and `isBreakdownRequested == true`; flicking back from `currentPage == 1` selects Actions while settling on Dashboard.)
  *(Note: waiting for `!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction == 0f` avoids completing mid-page during the drag→fling gap.)*
- **Confidence:** Medium (depends on Compose Foundation's drag→fling dispatch ordering in this project's Compose version; the phone test swiped back near page 0 where `currentPage` was already `0` at finger-up, so this case was not exercised).

---

## Low

### 2. `isSettled` ignores `pagerState.isScrollInProgress`, so a Breakdown request while scrolling away from Details saves Breakdown immediately
- **Location:** `app/src/main/java/com/techtrest/privamatic/ui/screens/MainScreen.kt:457`, `app/src/main/java/com/techtrest/privamatic/ui/screens/DetailsScreen.kt:213-218`
- **Invariants violated:** Invariant 13 ("A Breakdown request never changes Details > Checks unless the user arrives there through it").
- **Concrete failure scenario:**
  In Compose Foundation, `PagerState.settledPage` keeps the *previous* settled page for the entire duration of a scroll until `isScrollInProgress` becomes `false`.
  1. User is on **Details → Checks** in `LIST` view (`settledPage == 2`), with a loss snackbar visible.
  2. User taps **Dashboard** (or begins swiping toward Actions/Dashboard); while the pager is mid-scroll away from Details (e.g. over page 1), `pagerState.settledPage` is still `2`, so `isSettled` (`MainScreen.kt:457`) remains `true`.
  3. Mid-scroll, the user taps **Details** on the snackbar (`openBreakdown()` sets `isBreakdownRequested = true`), then immediately taps or swipes back to Dashboard/Actions so the pager never returns to Details.
  4. On the frame `openBreakdown()` runs, `ChecksTab`'s `LaunchedEffect(isBreakdownRequested, isSettled)` (`DetailsScreen.kt:213-218`) sees both `true` and immediately persists `ChecksView.BREAKDOWN` to `DetailsViewPreferences`, even though the pager was scrolling away and never settled on Details.
  *(Checking `!pagerState.isScrollInProgress && pagerState.settledPage == NavigationTab.DETAILS.ordinal` prevents firing mid-scroll.)*
- **Confidence:** High.

### 3. `ScanFeedback.gain` does not require `single.isFixed` for `UserScan.AdIdConfirmed`
- **Location:** `app/src/main/java/com/techtrest/privamatic/data/ScanFeedback.kt:111-112`
- **Invariants violated:** Invariant 11 ("Every snackbar is true for what it compares: … nothing called fixed that still costs points or can't be read").
- **Concrete failure scenario:**
  `ScanFeedback.gain` checks `trigger == UserScan.AdIdConfirmed && single?.check == PrivacyCheck.ADVERTISING_ID` before checking `single.isFixed`. If `ADVERTISING_ID` moves from failing (`-5`) to `isUnknown = true` (0 pts, unreadable) or to a partial non-zero deduction on an `AdIdConfirmed` trigger, `ScanFeedback.between` returns `ScanOutcome.AdIdConfirmed(points, score)` (`"Advertising ID confirmed · +N pts · Score S"`) instead of `ScanOutcome.CheckImproved`. (Unlikely with the current checker because confirming sets the preference flag that causes `ADVERTISING_ID` to pass, so this is mainly a `ScanFeedback` contract gap if `ADVERTISING_ID` ever returns unknown/partial.)
- **Confidence:** High (at the `ScanFeedback` unit level; Low likelihood with the current checker).

---

## Unexplained Phone Observation (Dashboard tap 0.1–1.5 s after snackbar's Details)

The diff **does not explain** why a Dashboard tap 0.1–1.5 s after tapping the snackbar's **Details** action had no visible effect while the move completed to Details and saved Breakdown:
- When **Details** is tapped on the snackbar, `SnackbarData.performAction()` resumes `showSnackbar` immediately, and `MainScreen.kt:223-232, 245` synchronously calls `navigationState.openBreakdown()`, setting `selectedTab = DETAILS` and `isBreakdownRequested = true`.
- If a Dashboard tap 0.1–1.5 s later invoked `BottomNavigationBar`'s `onTabSelected(NavigationTab.DASHBOARD)` (`MainScreen.kt:331-333`), `AppNavigationState.selectTab(DASHBOARD)` (`AppNavigationState.kt:53-56`) would immediately set `selectedTab = DASHBOARD` and `isBreakdownRequested = false`, cancelling `LaunchedEffect(DETAILS)` and stopping `pagerState.animateScrollToPage(2)`.
  - If `pagerState.currentPage == 0` at that moment, the new `LaunchedEffect(DASHBOARD)` would return early at `MainScreen.kt:184` (`if (pagerState.currentPage == page) return@LaunchedEffect`), leaving the pager stuck partway between Dashboard and Actions (matching the known pre-existing "stuck between two pages" behaviour).
  - If `pagerState.currentPage >= 1`, `LaunchedEffect(DASHBOARD)` would animate back to page 0.
  - In **neither** case could the move finish onto Details or save Breakdown.
- Nothing in the diff intercepts touches over the bottom bar (`SnackbarHost` at `MainScreen.kt:628-641` is padded above `bottomBarHeightPx` when no overlay is shown, and `drawerState.close()` on a closed drawer keeps `Scrim` disabled).
- Therefore, in those phone trials (including the 2 at normal speed on the pre-fix build), `onTabSelected(NavigationTab.DASHBOARD)` was almost certainly never triggered by the tap itself (e.g. tap coordinates/timing or touch-handling outside this diff).

---

## Test Audit

- **`StockSafeAdviceTest.kt`:** Genuinely tests both claims (no `"disabl"`, exact 3 uninstall steps without full stops, and no `"rescan to see"` anywhere in `strings.xml`).
- **`ScanFeedbackTest.kt`:** All 44 tests genuinely test their names against real `PrivacyScoreCalculator.calculateScore` outputs. The new exhaustive test (`no combination is ever called 0 checks fixed`, lines 282–304) checks 7,500 two-check combinations and genuinely verifies `ChecksFixed.count >= 2`, `CheckFixed` passes in `after`, and equal-score cost changes produce `Mixed`. Minor gap: none of the combinations in that loop includes `PrivacyCheck.ADVERTISING_ID`, so `AdIdConfirmed` with `fail → unknown` / partial is not exercised (Finding 3).
- **`AppNavigationStateTest.kt`:** All 3 tests genuinely verify `AppNavigationState`'s `openBreakdown` / `selectTab` / `selectDetailsTab` / `onBreakdownShown` transitions. (Cannot test `MainScreen`'s pager effects or `ChecksTab`'s `isSettled` logic.)
- **`ScanFeedbackTextTest.kt`:** Genuinely tests exact strings against `strings.xml` for every `ScanOutcome` subtype, plurals, U+2212, NBSP, and no `!`.
- **`PrivacyViewModelScanTest.kt`:**
  - All H1 (`supersededScanFails`, failed scan with unreadable DataStore, non-self `CancellationException`), M3, M4 (`supersededScanPublishesNothing` for both cancellable and late-noticing non-cancellable gates), L5, and 800 ms minimum tests genuinely exercise the real `PrivacyViewModel` paths their names claim.
  - **Minor test-name mismatch (post-fix):** In `a superseded scan failing after its result is shown leaves the newer scan alone` (`PrivacyViewModelScanTest.kt:213-241`), `trustReadGate` is armed after `hasAny.entered` to catch the removed `applyMissedTrustChange()` second read. Because no second read exists now, `gate` is never entered: Scan 1 finishes normally before `vm.performScan(UserScan.Rescan)` starts, so no scan is actually superseded or failing. Similarly, `a trusted list that can't be read after a finished scan doesn't crash or report a failure` (`PrivacyViewModelScanTest.kt:255-266`) passes because no post-publish read occurs. Both still serve as valid regression guards against re-introducing a second read after `onScanSuccess`, though the first test's name no longer reflects what executes on the fixed code.
- **`RunScanTest.kt`:** All 4 tests genuinely verify their claims; round 1's gaps are closed (`isFailureReached` asserted, and `a fast scan's result waits for the minimum` measures wall time `>= 300 ms`).
- **`ScanEventTest.kt`:** All 3 tests genuinely verify `ScanEvent.Finished.updated` across all 7 `UserScan` triggers and matching on `(type, relatedCheck)`.

---

## Invariants & Unverified-on-Phone Checklist

- **1 (Offline):** Holds. No permissions, network calls, or dependencies added.
- **2 (Backup / D2D exclusions):** Holds. No new persisted files, preferences, or tables added.
- **3 (Room schema):** Holds. No schema changes.
- **4 (Scoring source of truth):** Holds. All deductions come from `PrivacyScore` / `PrivacyIssue` / `ManualCheckType`; trust math uses `TrustedAppsAdjuster.computeAdjustedScore`.
- **5 (Disabled apps detection):** Holds. No checker touched.
- **6 (Strings in `strings.xml`):** Holds for all new user-facing copy (U+2212 minus sign, NBSP before score, no exclamation marks).
- **7 (Unsigned release builds):** Holds. Build config untouched.
- **8 (History & `score_history` once before display; widget/cap unchanged):** Holds. `PrivacyViewModel.onScanSuccess` records `score_history` once and today's Room snapshot once with the trust-adjusted score before publishing `_scanState`. `PrivacyWidgetProvider.onUpdate`, `PrivacySnapshotRepository.recordScan`, and `ScoreHistoryRepository.recordScore` are unchanged.
- **9 (Cancelled/failed scan isolation):** Holds (including the intended trust-change-during-scan exception on failed scans and non-self `CancellationException` handling).
- **10 (No scoring change; snackbar compares in-memory scores only):** Holds. `ScanFeedback.between` never reads `score_history`.
- **11 (Every snackbar true; no `+0 pts`; Quick Win closes only when Action is gone):** Holds across production paths (minor contract gap on `AdIdConfirmed` without `isFixed`, Finding 3).
- **12 (Result shown once; survives background; updates/closes matching Quick Win page):** Holds. `Channel.BUFFERED` + `repeatOnLifecycle(STARTED)` + `Dispatchers.Main.immediate` delivers once without dropping background completions; `ScanEvent.Finished.updated` matches `(type, relatedCheck)`.
- **13 (Breakdown request only changes Details > Checks on arrival; header toggle & rows unchanged):** Mostly holds; two residual pager-settle edge cases (Findings 1 and 2).
- **Unverified on a phone:**
  - *Failed scan / trust change during a scan / scan cancelled mid-way:* Code paths and `PrivacyViewModelScanTest` coverage are solid; no remaining bug found in code review.
  - *Tab tap / swipe interrupting snackbar's move to Details:* See Findings 1 and 2, and the unexplained Dashboard-tap note above.
  - *TalkBack:* Material 3 `SnackbarHost` uses `LiveRegionMode.Polite` and extends action timeouts via `LocalAccessibilityManager`; `QuickWinDetailScreen` buttons expose standard semantics. As noted in round 1, still worth one on-device TalkBack pass when `QuickWinDetailScreen` closes in the same frame a snackbar appears (`MainScreen.kt:206, 240`) to confirm the polite snackbar announcement is not pre-empted by the screen transition.

---

## Resolutions (Claude Code, 2026-11-26; decisions by Oliver)

- **M1 (swipe recovery completes in the drag→fling gap): Parked, not fixed in this branch.** It sits in the L8 pager code, which already had two fix rounds; CLAUDE.md says stop after the second round and propose a different approach. That approach is the parked "stuck pager" item (same settle logic, also on `master`). ROADMAP joins M1 to it: one pager-navigation run fixes them together.
- **L2 (`isSettled` ignores `isScrollInProgress`): Parked with M1**, same reason and same pager-navigation run.
- **L3 (`AdIdConfirmed` without `isFixed`): Fixed.** Proven first: two new `ScanFeedbackTest` cases, Advertising ID fail → unknown and fail → partial on an `AdIdConfirmed` trigger, failed on the unfixed code (`was: AdIdConfirmed(points=5, score=100)` and `AdIdConfirmed(points=3, score=98)`). `ScanFeedback.gain` now reports `AdIdConfirmed` only when the check passes; otherwise it falls through to `CheckImproved`. Both pass; 314 tests, 0 failures.
- **Test-name mismatch:** renamed to `a finished scan reads the trusted list no second time, so the next scan runs undisturbed`; the test body is unchanged and stays as the regression guard.
- **TalkBack:** still not verified on a phone; open in STATE.md for a later release check.
- **Unexplained Dashboard-tap observation:** no change; it stays with the parked pager item.
- No High findings: review rounds end here, no round 3.
