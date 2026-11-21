# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 9
- Branch: fix/snapshot-every-scan
- Commit: 78c2981
- Time: 2026-11-21T18:27:32+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
On fix/snapshot-every-scan: external review round 1 is worked through (no High findings, so no round 2). Two fixes, each proved by a failing test first: a reboot or force-stop after 22:00 on a day with no history point no longer skips that night's safety-net scan (the alarm now fires at once instead of the next evening), and when two scans overlap, today's point keeps the newer result. Low 1 moved to the Pixel 8 check; Low 3 rejected as a wrong check expectation, now corrected in STATE.md. Build passes, 216 unit tests pass. Nothing is verified on a phone: the Pixel 8 is busy with another project, no adb command was sent. The branch stays checked out.
Found: a safety-net scan that fails leaves no point, so a reboot later that night lets the alarm try once more; a clock set back during the day keeps today's point until the clock passes it again.

## Blocked on you
- Run the Pixel 8 check tonight and tomorrow morning (steps in STATE.md, run 2026-11-21), including two cold starts that leave the snapshot alarm's time unchanged.

## Red flags
None

## Pending decisions
None

## Next step for chat
After the Pixel 8 check, merge fix/snapshot-every-scan into master if it passes.

## Unapplied ROADMAP UPDATE blocks
None
