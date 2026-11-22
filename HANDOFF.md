# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 11
- Branch: fix/snapshot-every-scan
- Commit: 837fefd
- Time: 2026-11-22T09:32:33+01:00
- Deviations: none

## Where we are
The history snapshot fix passed its overnight check on the Pixel 8 and is merged into master locally; master is not pushed yet.
- Overnight: the 22:00 safety-net alarm fired while the phone was idle and unplugged (22:00:26), and its scan set the 21 Nov history point (22:00, score 53).
- Widget refresh pushed back on each app start: not caused by this fix. Master does the same once the app has been force-stopped: from then on every app start is treated like a boot, which re-arms the 6-hour widget refresh. An app update ends it. Left in Next with a fix idea.
- Pixel 8: fix build installed, tonight's 22:00 alarm armed, widget shows 53 Fair, crash log empty, on the home screen.
Found: users updating from 1.5.2 keep the old 23:00 nightly alarm until it fires once; it then switches itself to 22:00. By design, not tested on a device.

## Blocked on you
None.

## Red flags
None.

## Pending decisions
None.

## Next step for chat
Push master. Then pick the next item: the widget refresh push-back fix (small), or v1.6 planning.

## Unapplied ROADMAP UPDATE blocks
None.
