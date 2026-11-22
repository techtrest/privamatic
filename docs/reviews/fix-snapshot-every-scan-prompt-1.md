Review docs/reviews/fix-snapshot-every-scan.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..fix/snapshot-every-scan: every finished scan now sets
today's score-history point (a later scan the same day replaces it, via one new
PrivacySnapshotRepository.recordScan), and the nightly snapshot alarm becomes a single inexact
one-shot (setAndAllowWhileIdle, next 22:00 local, re-armed on delivery) that app start, boot and
update never cancel or postpone; also the History empty-state text changes). You can't run git,
Gradle or tests in this workspace. Read only the files this diff touches, and only the functions
around the changed lines — don't explore the rest of the repo. Skip strings.xml, fastlane metadata
and screenshots. The new tests PrivacySnapshotRepositoryTest and SnapshotAlarmTest are part of the
point: check that they really test the cases their names claim (local-midnight day bounds, the 25 h
and 23 h DST days, "arm only when missing"). If you need more than ~10 files, stop and say which
and why.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review. The Pixel 8 check has not run yet; it runs tonight, so nothing in this diff
is verified on a device (unit tests: 211 pass). Oliver chose the design (21 Nov) after
docs/decisions/snapshot-alarm-time.md. The old repeating 23:00 alarm (same PendingIntent) is
replaced at its first delivery after an update. Trusting or untrusting an app does not record a
point (accepted by Oliver: today's point keeps the old score until the next scan).

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
8. No Room schema, entity, version or migration change; the new DAO methods run in one transaction
   (check that the find-then-insert/update really cannot interleave between the app scan and the
   widget scan).
9. History points before today are never rewritten or moved: only the row of the current local
   day can be updated, and the day bounds are local midnight to next local midnight (also on DST days).
10. No new scans are triggered; every existing scan path records through
    PrivacySnapshotRepository.recordScan (app scan, widget scan, snapshot alarm). Find any scan
    path that doesn't, or any path that records without a finished scan.
11. The alarm is inexact (setAndAllowWhileIdle): no exact-alarm permission and no manifest
    permission change; app start, boot and update never cancel or postpone a pending snapshot alarm
    (only arm when the PendingIntent is missing), and delivery re-arms before doing any work.
