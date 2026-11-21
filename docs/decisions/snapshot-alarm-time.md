# Daily history snapshot: when it runs and what a "day" means — design run (2026-11-21)

Read-only design run. No code changed. Branch `docs/snapshot-alarm-time`.
Code read on `master` at `364915b`; device evidence from the Pixel 8 (debug build 1.5.2, versionCode 10) in
`docs/screens/snapshot-alarm-time/`.

## 1. Why the comments say 23:50 and the code sets 23:00

- `0405dea` (2026-06-27, "replace scan-triggered snapshots with daily 23:50 AlarmManager job") added `SnapshotReceiver` with
  `HOUR_OF_DAY 23, MINUTE 50`, and three comments saying 23:50.
- The next day `0603e0a` (2026-06-28) changed only `MINUTE 50` → `MINUTE 0` (`SnapshotReceiver.kt:69-70`). Its message lists four
  changes ("… reschedule snapshot to 23:00") and gives no reason. The comments were not updated:
  - `SnapshotReceiver.kt:22` (class doc), `SnapshotReceiver.kt:62` (`scheduleMidnightSnapshot` doc);
  - `PrivacyWidgetProvider.kt:276` (`ACTION_MIDNIGHT_SNAPSHOT` doc).
- So 23:00 is the deliberate value and the comments are stale. Why 23:00 was chosen is not recorded. A likely reason is
  margin before midnight, but with an inexact alarm the start time barely changes where it lands (section 2).
- The user-facing empty text promises the same thing: "Your first data point will appear tonight. Privamatic records your
  score once a day." (`strings.xml:1045`).

## 2. What the inexact daily alarm does

The alarm (`SnapshotReceiver.kt:65-86`): `setInexactRepeating(RTC_WAKEUP, next 23:00 local, INTERVAL_DAY)`.

- **Window.** For an inexact repeating alarm Android allows delivery up to 75 % of the interval after the nominal time
  (AlarmManagerService `maxTriggerTime`: `trigger + 0.75 × interval`). For one day that is **18 h**: a 23:00 alarm may fire
  any time up to **17:00 the next day**. The 6 h widget alarm shows the same rule on the Pixel 8 (+4 h 30 m = 75 % of 6 h,
  2026-10-24 design run). Today's Pixel 8 evidence: section 4.
- **Where it lands in practice.** Somewhere in the window, typically when the phone wakes for something else. In deep
  Doze a wakeup alarm without allow-while-idle also waits for the next maintenance window. On the Pixel 8, left idle at
  night, it landed with the 10:02 widget wakeup the next morning on three of the last four days (section 4).
- **Nominal time stays at 23:00.** A repeating alarm's next nominal time is the previous nominal time + 24 h, not the
  delivery time + 24 h, so lateness does not add up from day to day.
- **Each app start replaces the pending alarm.** `scheduleMidnightSnapshot` runs in `MainActivity.onCreate`
  (`MainActivity.kt:26`), on boot (`BootReceiver.kt:26`) and after an app update (`PackageReplacedReceiver.kt:23`). It
  computes the *next* 23:00 and calls `setInexactRepeating` with the same PendingIntent, which cancels the alarm already
  set (AlarmManager docs: "If there is already an alarm scheduled for the same IntentSender, that previous alarm will
  first be canceled"). So:
  - opening the app before 23:00 moves nothing (same nominal time), only restarts the alarm;
  - opening the app **after 23:00 but before the alarm has fired** — late evening, or the next morning before 17:00 —
    re-arms it for the next 23:00, and **that day's snapshot is never taken**.
- **DST.** `INTERVAL_DAY` is 24 h of elapsed time. On the night the clocks change, the nominal time moves to 22:00 or
  00:00 local until the next app start, boot or update re-arms it at 23:00. A 00:00 nominal time lands on the next calendar
  day.

## 3. What a "day" in score history means today

- `PrivacySnapshotRepository.recordSnapshot` (`:14-27`) stores `timestamp = now` (the **delivery** time) and skips the insert
  if any snapshot already exists for the calendar day of `now` (`countSnapshotsForDay`, local midnight to midnight).
- `HistoryScreen` plots each snapshot at its own timestamp and shows it as "d MMM yyyy, HH:mm" when tapped
  (`HistoryScreen.kt:155, 251`). It never claims a point is "the score at the end of day X".
- So a history "day" is **the calendar day on which the alarm happened to fire**, and the score is the score **at that
  moment**, not the day's closing score. Consequences, with the 23:00 alarm for Monday as the example:

  | Monday's alarm fires | Stored as | Score is | Tuesday's own 23:00 alarm |
  |---|---|---|---|
  | Mon 23:00–23:59 | Monday | Monday late evening | fires, stored as Tuesday |
  | Tue 00:00–17:00 (late, idle phone) | **Tuesday** | Tuesday morning | if it also fires on Tuesday: **skipped** (Tuesday already has one). Monday has **no point**. |
  | never (app opened Mon 23:30 or Tue 08:00 first) | — | — | Monday has **no point** |

- So a late delivery shifts a point onto the next day and leaves a gap. The cases above can't create two points on
  one day or a wrong score: the stored score is always a real scan at the stored time.
- The chart spaces points evenly by index (`HistoryScreen.kt:391-392`), so a gap does not show as a gap.
- The widget is not affected: its change indicator comes from `ScoreHistoryRepository` (SharedPreferences), which the
  snapshot never touches. Only the History screen reads the snapshots.

## 4. Device evidence (Pixel 8)

Read-only `device-verify` run, 2026-11-21 12:15 CET, Europe/Warsaw (CET), installed debug build, nothing installed or cleared.

- **The alarm** (`dumpsys alarm`): `RTC_WAKEUP … ACTION_MIDNIGHT_SNAPSHOT origWhen=2026-11-21 23:00:00.000 window=+18h0m0s0ms
  repeatInterval=86400000`. So nominal 23:00, latest delivery 17:00 the next day, as section 2 says. The widget alarm next
  to it: `window=+4h30m0s0ms repeatInterval=21600000`.
- **Last delivery:** the snapshot alarm last fired 8 h 23 m before the check, about **21 Nov 03:52**: the 20 Nov alarm,
  4 h 52 m late. The newest history point is "21 Nov 2026, 03:52".
- **What the History screen shows** (1W, every point tapped):

  | Point shown as | Score | Is really the alarm for |
  |---|---|---|
  | 21 Nov 2026, 03:52 | 53 | 20 Nov |
  | 20 Nov 2026, 10:03 | 53 | 19 Nov |
  | 18 Nov 2026, 10:02 | 53 | 17 Nov |
  | 17 Nov 2026, 10:03 | 53 | 16 Nov |

  Three of four points landed at 10:02–10:03 the next morning: the minute the 6 h widget alarm woke the phone
  (`last_update_timestamp` today is 10:02:19). The phone sits idle overnight, so the snapshot waits for the next wakeup.
  **On this phone the daily point is, in practice, the next morning's score, filed under the next day.**
- **A lost day:** there is no 18 Nov alarm point (it would be dated 19 Nov). The cause is not shown by this run. Section 2
  gives the likely one: an app start on 19 Nov before delivery re-arms for 19 Nov 23:00. Earlier runs started the app on
  the Pixel 8 on 18–19 Nov.
- **Gaps are invisible:** the chart spaces points by index, not by date (`HistoryScreen.kt:391-392`). The 1W screenshot
  shows four evenly spaced points; the missing day can't be seen. Screenshots: `history-week.png`, `history-point.png`,
  `history-all.png` (4 Oct – 21 Nov, about 34 points in 49 days).
- **App start re-arms without moving it:** after a cold start at 12:15 the snapshot alarm is a new alarm object, same
  `origWhen` 23:00 and same `whenElapsed`. The debug log shows "Midnight snapshot alarm scheduled for Sat Nov 21 23:00:00"
  twice, 350 ms apart, in one process. The second is most likely the `BOOT_COMPLETED` that Android 15+ sends on the first
  start after a force-stop (STATE Next, `BootReceiver` item, 2026-11-18): not verified here.
- Not seen in this run: a delivery after 17:00 the next day (none expected), Doze state at the time of delivery, the S8.

## 5. Options

### Option A — Keep the daily inexact alarm; fix the comments and stop app starts from dropping a day

- Correct the three 23:50 comments to 23:00.
- Re-arm only when no snapshot alarm is pending, so an app start after 23:00 can't cancel tonight's snapshot. How to tell
  "pending" needs a device check first: `PendingIntent.FLAG_NO_CREATE` is the usual test, but a force-stop removes the alarm,
  and whether the PendingIntent survives it must be seen on the Pixel 8 and the S8.
- Points still land up to 17:00 the next day and can still move to the next day.
- **Cost:** small, no new permission, no data change. **Leaves:** the day shift and gaps of section 3.

### Option B — A one-shot alarm with a short window, re-armed each night

- `setWindow(RTC_WAKEUP, 23:00, 55 min)` instead of `setInexactRepeating`, and the receiver arms the next night's alarm
  after it runs. Same re-arm points as today, with Option A's "only if none is pending" rule.
- No exact-alarm permission is needed: `setWindow` is allowed for every app (since Android 12 the window is at least
  10 min, which 55 min is). When the phone is awake the point lands between 23:00 and 23:55, on the right day.
- In deep Doze `setWindow` still waits for the next maintenance window, which can be after midnight. The receiver can
  then record the point (honest timestamp, next day) or skip it if the day already has one, as today.
- **Cost:** medium: alarm code in risky code, both test phones, a Doze check (`dumpsys deviceidle force-idle`).
  No data or schema change.
  **Leaves:** late points on phones that sit idle and unplugged at 23:00.

### Option C — Take the day's point from scans the app already runs; keep the alarm as a fallback

- Every widget update (6 h alarm, boot, settings) and every app scan already computes the adjusted score. Each of them
  would also write that day's snapshot: insert if the day has none, otherwise update today's row with the newer score
  and timestamp. The 23:00 alarm stays, for days with no other scan.
- A day's point then becomes **the last score seen that day**, which is what "a day in history" suggests, and the alarm's
  lateness no longer matters for anyone with the widget placed or who opens the app.
- This partly reverses `0405dea`, which removed scan-triggered snapshots. Its reason is not recorded; the old rule was
  "first scan of the day wins", which this is not.
- **Cost:** medium to large: a new DAO update query (no schema change, no new column), Room in risky code, careful
  testing of the "update today's row" path. **Changes:** what an existing point means from the upgrade on; old points
  stay as they are.

## 6. Recommendation

**Option B, with Option A's comment fix and "don't cancel a pending alarm" rule.** It fixes the real problem — points on the
wrong day and lost days — inside the receiver that already exists, without changing what is stored or how. It needs no
new permission, keeps one point per day, and keeps the History screen's honest timestamps. Option C gives the best data
but changes the meaning of a stored point and touches Room; worth it only if Oliver wants "closing score of the day"
rather than "a score each night". Option A alone is the floor if neither is wanted now: it costs almost nothing and stops
the dropped days.

Risk in B, from section 4: if the Pixel 8 is in deep Doze at 23:00, a `setWindow` alarm also waits for the next wakeup,
and B gains little. So B's first step is a device check, not code: one night on the Pixel 8 with `setWindow` in a throwaway
debug build, then read the delivery time. If it still lands the next morning, switch to C.

Separate small item, any option: the chart spaces points by index, so missing days are hidden. Spacing by date (or a
marked gap) is a UI change of its own.

Oliver's call: which option, and whether the empty-history text ("will appear tonight") stays. Nothing is built in this run.
