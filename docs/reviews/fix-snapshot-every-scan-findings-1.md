No High findings. One Medium: a reboot in the alarm's delivery hour drops the only scan of the day for a phone with no widget.

Medium
Reboot after 22:00, before the alarm has fired, skips that night's safety-net scan. SnapshotReceiver.kt:87, with BootReceiver.kt:33. Confidence: high.

nextTriggerAt arms the next 22:00 strictly after now. The alarm is inexact and may not fire until 23:00. A reboot clears it. BootReceiver then arms tomorrow, and it returns without a scan when no widget is placed.

A phone with no widget and no other scan that day never gets a point. This is a regression from the old 23:00 alarm: a reboot at 22:15 used to schedule 23:00 the same night. Opening the app still records today, and a placed widget still scans on boot. Invariant 11 holds: boot does not postpone an alarm that is still pending.

Low
The "arm only when missing" test does not arm anything. SnapshotAlarmTest.kt:71. Confidence: high.

It asserts that needsArm negates its boolean. It never calls ensureSnapshotAlarm or isAlarmPending, and it never checks that a live alarm is not passed to setAndAllowWhileIdle. If isAlarmPending stopped matching the alarm's PendingIntent, every cold start after 22:00 would replace tonight's alarm with tomorrow's, and this test would stay green.

Two overlapping writes can save the earlier scan. PrivacySnapshotRepository.kt:39, PrivacySnapshotDao.kt:37. Confidence: medium.

The find-then-insert/update cannot both insert. Room 2.6.1 runs that @Transaction body in one withTransaction, and the second scan blocks until the first commits, then updates that row. The timestamp is taken before the transaction, so the scan that commits last wins even when its timestamp is earlier. That only matters when two scans reach recordScan together, such as a widget update and a refresh finishing at the same moment.

Tonight's History check can disagree with a placed widget. STATE.md:144, PrivacyViewModel.kt:174. Confidence: high.

Every successful app scan requests a widget update, and that update scans and calls recordScan again. With a widget on the home screen, the 21 Nov point ends a few seconds after the cold start or the pull-to-refresh, not at that time. One point for the day is still right.

Checked, holding
The 25-hour day test locks the fix. Scans at 10:00, 23:15 and 23:45 on 25 Oct 2026 become one row at 23:45, and 26 Oct 00:10 is a new row. With the old midnight-plus-24h end, the 23:45 scan would not see the row it had just moved past 23:00, and a third row would be inserted.
The 23-hour day test checks a real boundary: 29 Mar 2026 23:30 stays that day and 30 Mar 00:30 starts the next. Those two times also stay apart with a midnight-plus-24h end, so this test alone would not catch that regression. The 25-hour test would.
Both alarm DST tests check real local 22:00. The night of 24–25 Oct 2026 is 25 hours, and 28–29 Mar 2026 is 23 hours.
Day bounds are local midnight to the next local midnight. Updates copy the existing row id, so earlier days are not moved. No schema, entity, or migration change.
All three performCompleteScan call sites record through recordScan after the scan returns: the app scan, the widget scan, and the safety-net alarm. Trust changes still do not record on their own.
App start, boot, and update only call ensureSnapshotAlarm, which schedules only when the PendingIntent is missing. Delivery calls scheduleNextSnapshot before the scan. setAndAllowWhileIdle needs no exact-alarm permission, and the manifest does not gain one. On Android 12 and later the one-shot window is capped at one hour, so a fire at 22:00 stays on that local day unless battery restrictions defer it further.
No network permission, no network call, and no new dependency. User-facing copy stays in strings.xml. Release signing is untouched.

---

## Disposition (Claude Code, 2026-11-21, decided with Oliver)

No High findings: no round 2.

**Medium — reboot after 22:00 skips the night's scan: accepted, fixed.**
Proved first: `SnapshotAlarmTest` "armed after 22_00 on a day with no point it fires now" failed on the old rule
(armed 22 Nov 22:00 for a re-arm at 21 Nov 22:15, 23 h 45 m later). Fix: when app start, boot or update arms a
missing alarm after 22:00 and today has no history point, the alarm fires now (still `setAndAllowWhileIdle`,
inexact); otherwise unchanged. Its delivery arms tomorrow 22:00, so still at most one safety-net alarm a night;
no exact-alarm permission. Tomorrow's alarm is armed at once as a fallback, and replaced only after today's point
is read off the main thread (receivers keep themselves alive with `goAsync`). Device check: not done (Pixel 8 busy).

**Low 1 — the "arm only when missing" test arms nothing: accepted, covered on the device instead.**
The test checks only the boolean rule; whether `isAlarmPending` matches the armed PendingIntent needs a real
AlarmManager. Added to the Pixel 8 check in STATE.md: two cold starts leave the snapshot alarm's trigger time
unchanged in `dumpsys alarm`. Not verified yet (Pixel 8 busy).

**Low 2 — overlapping scans can save the earlier one: accepted, fixed.**
Proved first: `PrivacySnapshotRepositoryTest` "a scan that started earlier but saves last never replaces a newer
point" failed (expected 61, was 58). Fix inside the existing `@Transaction` `setPointForDay`: a point stamped
earlier than the day's newest is dropped. No schema change.

**Low 3 — History check versus the widget's extra scan: rejected as a code finding.**
One point per day still holds; only the check's expected time was wrong. Reason: every app scan asks the widget to
update, and that update scans and records, which is intended. The Pixel 8 check in STATE.md now says that with a
widget placed, today's point lands a few seconds after the cold start or refresh.
