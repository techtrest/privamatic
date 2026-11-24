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

Context: first review.
- Core: `PrivacyViewModel.performScan`, `runScan`, `onScanSuccess`, `applyMissedTrustChange`;
  `ScanFeedback.between` and `merge`; the event collector and snackbar host in `MainScreen`;
  `IntentHelper.consumeRescanOnReturn`. A new scan cancels a running one and keeps the earliest
  pending `before`. All view model state is touched on the main thread.
- On master a failing scan's `async` failed the whole job and would crash the app; `runScan` gives
  it its own scope (RunScanTest). A trust change made during a scan was skipped and never applied;
  `applyMissedTrustChange` applies it when the scan ends.
- The own review fixed: a cancelled scan that fails on its way out reported a failure over the newer
  scan; a failed scan asked the widget to refresh; a quick second scan could skip the widget refresh
  or the 800 ms minimum.
- Choices Oliver accepted where 4.2 is silent or its literal text would be false: partial improvement
  and fail → unknown are not called "fixed"; mixed gains show only the points; "1 better, 1 worse"
  when not exactly fixed/new; a review that wasn't due shows no points; "Not fixed yet" only when the
  check still costs points and a fix was attempted, never after a manual review. Don't report these
  as spec deviations; do report a text that is false in some state.
- `before` is kept as the raw score and trust-adjusted with the new result's trusted set, on purpose:
  trusting an app while a scan runs must not be reported as a fix.
- Decided earlier (21 Nov), not a finding: trusting an app doesn't rewrite today's history point
  until the next scan.
- One intended exception to invariant 9: if an app was trusted or untrusted while a scan ran, the end
  of that scan (also a failed one) shows the recomputed score and refreshes the widget, exactly as
  the trust collector does outside a scan.
Unit tests pass (280, 0 failures; 234 before). Pixel 8 gate passed (STATE.md, run 2026-11-24). Not
verified on a phone, so look harder there: a trust change during a scan, a failed scan, a scan
cancelled mid-way, TalkBack.

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
