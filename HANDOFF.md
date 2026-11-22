# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 12
- Branch: fix/boot-receiver-rearm
- Commit: ac9d105
- Time: 2026-11-22T19:45:57+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
App starts no longer push the widget's 6-hour refresh back. The fix is merged into master locally; master is not pushed yet.
- Pixel 8: after a force-stop and two more app starts, the widget refresh stayed at the same time, and there was still only one. After a real reboot: one widget refresh about 6 hours out, one refresh scan, tonight's 22:00 history alarm still armed. No app crashes.
- Pixel 8 left with the fix build, on the home screen, widget 53 Fair, 22:00 alarm armed.
Found: after a reboot the widget itself set the refresh before the boot step ran, so the boot step's own "set it again" path wasn't used on the phone; only the unit tests cover it.
Found: no external review, because this is a small change that reuses the already-reviewed check from app start.
Found: device commands that wait for the phone, or that filter the output on the phone side, hang inside the sandbox; the device checks avoid them now.

## Blocked on you
Push master.

## Red flags
None.

## Pending decisions
None.

## Next step for chat
Push master. The next small items in STATE.md's Next are the PackageReplacedReceiver comment fix and the first-run screen scan question.

## Unapplied ROADMAP UPDATE blocks
None.
