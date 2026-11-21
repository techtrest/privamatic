# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 7
- Branch: fix/snapshot-every-scan
- Commit: 8b778cf
- Time: 2026-11-21T15:06:38+01:00
- Deviations: 2 (the words are in status.json, not in this public file)

## Where we are

The snapshot fix (B plus C) is built on `fix/snapshot-every-scan` (`8b778cf`) and passes its unit tests (211/211), the build and the debug APK. Every finished scan now sets today's history point, and a later scan the same day replaces it. That covers the app scan, the widget update and the nightly alarm. The one nightly alarm is now a safety net at 22:00, allowed while the phone is idle, with no new permission. Opening or updating the app no longer cancels a pending snapshot. The database layout is unchanged.
Nothing has run on a phone yet: the Pixel 8 was in use by another project, so no device command was sent.
Found: the old code ended a day 24 h after midnight, so on the day the clocks go back a late-evening scan counted as the next day. A unit test showed it, and the fix uses the real local midnight.

## Blocked on you

Run the Pixel 8 check tonight, ideally before 21:30, and read the alarm tomorrow morning. The exact steps are in STATE.md under the run "2026-11-21 — snapshot fix, B plus C". The branch stays checked out for it. External review comes after the phone check (score history is risky code).

## Red flags

None

## Pending decisions

- The empty History text still says "Your first data point will appear tonight". The first scan now records a point straight away. Keep the text or change it?
- On the day the update lands, today's existing point is replaced by today's later scans, like any of today's points. On the Pixel 8 that point is last night's late alarm. Earlier days are never touched.
- Trusting or untrusting an app changes the score without a scan, so today's point keeps the old score until the next scan. Leave as is, or record on trust changes too?

## Next step for chat

After the Pixel 8 check passes, review the diff externally (round 1), then merge. The chart spacing by date is its own roadmap item.

## Unapplied ROADMAP UPDATE blocks

None
