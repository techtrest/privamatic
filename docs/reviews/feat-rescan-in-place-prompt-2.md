Review docs/reviews/feat-rescan-in-place.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..feat/rescan-in-place: a rescan keeps the current
screen, and every scan the user asked for shows one snackbar with the points and the score). You
can't run git, Gradle or tests in this workspace. Read only the files this diff touches, and only
the functions around the changed lines — don't explore the rest of the repo. The tests are part of
the point: check that they really test what their names claim. Skip fastlane metadata, docs/screens,
STATE.md, HANDOFF.md and the state archive; in strings.xml look only at the snackbar strings and the
Quick Win instructions. If you need more than ~10 files, stop and say which and why. Besides the
touched files you may read PrivacyWidgetProvider.onUpdate, PrivacySnapshotRepository.recordScan and
ScoreHistoryRepository.recordScore: the widget runs its own scan and calls the same two.

Risky code (the rescan path), v1.6 run 2+3: spec docs/decisions/v1.6-fix-to-result.md, section 4.2.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: second review. Round 1 had 2 High, 2 Medium, 4 Low; docs/reviews/feat-rescan-in-place-findings-1.md
ends with a resolution per finding. Judge the fixes first, then look for what they broke.
- Fixed: H1 (one catch in `performScan`: `ensureActive()` first, then `onScanFailed`; no storage read in
  the failure path, the trusted set comes from `latestTrusted`), H2 (`ScanFeedback.between` compares the
  union of both sides' checks; a check that disappeared counts in the points only), M3 (`scanEvents`
  collected in `repeatOnLifecycle(STARTED)` on Main.immediate), M4 (`readHistory` / `publishHistory`),
  L5 (the publish uses `latestTrusted`, so `applyMissedTrustChange` is gone), L6 (`ScanEvent.updated` for
  every trigger), L8 (below). Two extra fixes: `ensureActive()` before `onScanSuccess` publishes; a scan
  failing with a cancellation that isn't its own now ends as failed.
- Rejected: L7. All 13 `QuickWin(...)` constructions in `QuickWinsDetector`, the only ones in the app, set
  a non-null `relatedCheck`.
- L8 took two rounds: the request isn't latched, Details composes as soon as the move starts and Checks
  saved Breakdown at once. Now a request only shows Breakdown; `ChecksTab` saves it and clears the request
  once the pager has settled on Details; selecting another tab or Details tab drops it. A swipe that
  interrupts a programmatic move re-syncs the tab in the pager effect's catch.
- New: `PrivacyViewModel` has an internal constructor for tests; the app uses the public
  `(Application)` one. `QuickWinDetailScreen(snackbarHeight)`: the page ends above the snackbar and scrolls
  to its end when a snackbar appears or grows while it is open.
- Choice Oliver accepted: a check that disappears while another gets worse for the same points says
  "1 better, 1 worse".
- Known, not for this run: tapping a tab while the pager still moves to another leaves it between two
  pages (the same lines are on master); the app would crash at start if the trusted list can't be read
  (also on master); today's point keeps the trusted set it was recorded with until the next scan (21 Nov).
- From round 1, still valid: choices Oliver accepted where 4.2 is silent or its literal text would be
  false (partial improvement and fail → unknown are not called "fixed"; mixed gains show only the points;
  "1 better, 1 worse" when not exactly fixed/new; a review that wasn't due shows no points; "Not fixed yet"
  only when the check still costs points and a fix was attempted). Don't report these; do report a text
  that is false in some state. `before` is kept raw and trust-adjusted with the new result's trusted set on
  purpose. One intended exception to invariant 9: a trust change made while a scan ran is shown when that
  scan ends, also a failed one, and then refreshes the widget.
- Unexplained: on the phone (animations 10 times slower) a Dashboard tap 0.1 to 1.5 s after the snackbar's
  Details had no visible effect; the move completed and Breakdown was saved. Say why if the diff shows it.
Unit tests pass (312, 0 failures; 280 in round 1). Pixel 8 checks passed (STATE.md, run 2026-11-25). Not
verified on a phone, so look harder there: a failed scan, a trust change during a scan, a scan cancelled
mid-way, a tab tap interrupting the snackbar's move to Details, TalkBack.

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
8. Every finished scan records today's history point exactly once, and `score_history` once, with
   the trust-adjusted score, before its result is shown; the widget's own scan, the point's replace
   rule and the 48 h change cap behave as on master.
9. A cancelled or failed scan never overwrites a newer result, never ends the newer scan's progress
   state, never consumes its pending `before`, and never refreshes the widget (exception above).
10. No score or scoring change: no deduction, cap, rating or stored key differs from master; the
   snackbar only compares two in-memory scores and never reads `score_history`.
11. Every snackbar is true for what it compares: no "+0 pts", nothing called fixed that still costs
   points or can't be read, nothing shown for an app-start scan, and no user scan whose gain is lost
   to a double tap. The Quick Win page closes only when its Action is gone.
12. A result is shown exactly once: never lost because the app was in the background, never shown twice
   after a return, and the Quick Win page it closes or updates is the one it belongs to.
13. A Breakdown request never changes Details > Checks unless the user arrives there through it; a
   completed one still saves Breakdown, and the header toggle and Breakdown rows behave as on master.

Also judge this rejected finding: "Check again is a no-op if quickWin.relatedCheck is null". Rejection
reasoning: no QuickWin is ever built without a relatedCheck (QuickWinsDetector.kt, 13 constructions; you
may read that file). Find a path where it fails, or confirm it.
