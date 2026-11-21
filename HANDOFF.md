# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 10
- Branch: fix/snapshot-every-scan
- Commit: 88c311f
- Time: 2026-11-21T19:44:53+01:00
- Deviations: 4 (the words are in status.json, not in this public file)

## Where we are
The snapshot fix (`fix/snapshot-every-scan`) passed this evening's Pixel 8 check. The update kept history intact. One snapshot alarm is set for 22:00 tonight, with a one-hour window and no repeat, and further app starts leave it alone. A scan replaces today's history point, and the crash buffer is empty. The phone is on its home screen with the widget placed and this debug build installed. What's left is tomorrow morning's readout, then the merge. STATE.md (run 2026-11-21, evening) lists exactly what to read.
Found: on this phone an app update keeps the app's alarms, so the comment in `PackageReplacedReceiver` is wrong. It is not changed in this run.
Found: the alarm's flag reads 0x20, not the 0x4 in the written check. That is how the system stores an inexact allow-while-idle alarm. Oliver accepted it.

## Blocked on you
Tomorrow morning, before opening the app, run the readout in STATE.md (run 2026-11-21, evening). That can be a Claude Code run.
Optionally, unplug the Pixel 8 before 22:00 so tonight also tests deep sleep. It is on USB power now.
Check that the Pixel 8's Download folder no longer holds PV_shot.png, a copy of the app's history database left by the check.

## Red flags
None

## Pending decisions
None

## Next step for chat
Tomorrow morning: read the alarm and the history point as STATE.md describes, then merge if it passes.

## Unapplied ROADMAP UPDATE blocks
None
