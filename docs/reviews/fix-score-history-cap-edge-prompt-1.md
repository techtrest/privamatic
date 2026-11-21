Review docs/reviews/fix-score-history-cap-edge.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..fix/score-history-cap-edge: on the first score
recording of a new day, a non-zero change now gets its own "first seen" time instead of keeping
an older change's, so the widget's 48 h change indicator no longer hides a fresh change;
ScoreHistoryRepository also gets an internal constructor (prefs + clock) for unit tests). You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata and
screenshots. The new test ScoreHistoryRepositoryTest is part of the point: check
that it really tests the cases its names claim. If you need more than
~10 files, stop and say which and why.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review. The widget reads the timestamp in PrivacyWidgetProvider
(showPrivacyChange, `now - deltaFirstAppearedTimestamp <= 48 h`); the in-app card
uses lastUpdateTimestamp and is not changed. The test was committed failing first
(2 of 5 failed on the old logic), then the one-line fix made all 200 unit tests pass.

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
8. No Room schema or migration change (PrivacyDatabase, entities, DAOs untouched).
9. No change to stored keys or file names: the SharedPreferences file stays
   "score_history" with the same five keys and types; nothing new is written.
10. Existing history entries read back unchanged: a value stored by the released
    version (including a non-zero delta_first_appeared_timestamp) is read the same
    way; only a new-day recording with a non-zero delta writes a different value.
11. Score math unchanged: currentScore, dailyBaselineScore, the baseline
    promotion at a new day and scoreDelta are computed exactly as before.
