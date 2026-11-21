# Snapshot fix, B plus C — spec

Run type: implementation run. Branch: `fix/snapshot-every-scan` (from `master`).
Decision: Oliver, 2026-11-21, after `docs/decisions/snapshot-alarm-time.md` (Option B plus C).

## Goal

Score history gets one point for every day the phone scanned, filed on the right day: every finished scan sets
today's point, and a later scan the same day replaces it. One nightly alarm stays as a safety net for days with no
other scan.

## Scope

**In:**
- Every finished scan (app scan, widget update, the safety-net alarm) records through one call; a later scan the same
  local calendar day replaces that day's point. Day boundaries are local midnights (DST days have 23 or 25 hours).
- One daily safety-net alarm, roughly 21:00–23:30, allowed while idle, inexact; no exact-alarm permission, no manifest
  change, no extra alarms. Its scan records through the same call.
- App start, boot and update never cancel or postpone a pending safety-net alarm (FLAG_NO_CREATE test, as for the
  widget alarm).

**Out:**
- Room schema change, migration, new table or column (hard condition: stop and report if needed).
- Rewriting or moving points of earlier days.
- The History chart's spacing by index (own roadmap item).
- New scans; the trust-change recompute (no scan) records nothing.

## Oliver's calls

| Question | Default |
|---|---|
| Empty-history text "Your first data point will appear tonight." | Left as is; the first scan now records a point at once. |
| Today's point on the update day (may hold last night's late alarm) | Replaced by today's later scans like any of today's points. |

## Invariants

1. No change to `PrivacySnapshot`, `PrivacyDatabase` (version 1) or the manifest.
2. At most one row per local calendar day is written; rows of earlier days are never touched.
3. At most one snapshot alarm exists; app start, boot and update leave a pending one alone.
4. A failed history write never stops the widget or the app showing the score.

## Milestones

| # | Milestone | Gate (evidence that proves it) |
|---|---|---|
| 1 | Every scan sets today's point | Unit tests `PrivacySnapshotRepositoryTest` (replace, midnight, alarm-only day, old points, both DST days) |
| 2 | Safety-net alarm at 22:00 local | Unit tests `SnapshotAlarmTest`; Pixel 8 `dumpsys alarm`: one snapshot alarm, `origWhen` 22:00, `window=+1h`, no repeat, allow-while-idle flag |
| 3 | On a device | Pixel 8: history intact after `install -r`; a scan replaces today's point; next morning the alarm fired 22:00–23:00 and re-armed; crash buffer empty (STATE.md, run 2026-11-21) |

## STOP points

| When | What Oliver does |
|---|---|
| Before the phone test | The Pixel 8 is in use elsewhere: Oliver runs or frees it for the check in STATE.md. |
| After the phone test and diff review, before merge | Runs the external review (risky code: score history) and saves the findings file. |
