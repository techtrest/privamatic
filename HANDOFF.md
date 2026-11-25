# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 18
- Branch: feat/rescan-in-place
- Commit: fe57328
- Time: 2026-11-25T20:13:48+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 2+3 (`feat/rescan-in-place`): external review round 1 is fixed in code. Each finding was shown by a failing test first, where a test can show it (24 failing tests on the old logic); one Low is rejected with evidence. 312 unit tests pass (280 before) and the debug build assembles.
Also done: after a result, the snackbar no longer covers Check again on long Quick Win pages (not seen on a phone yet).
Not verified on a phone yet: everything in this round. No phone was touched.
Found: if the trusted list can't be read when the app starts, the app crashes; this is on `master` too and was not changed.

## Blocked on you
Say whether the Pixel 8 is free. The re-check covers the fix loop, the top-bar rescan, a loss with Details, Mark as done, the snackbar on a long Quick Win page, and leaving the app during a scan.

## Red flags
None

## Pending decisions
1. Call 8: "I've checked this" on AI-app rows? The spec says no for now. It has to be decided before v1.6.1 run 10.

## Next step for chat
Pixel 8 re-check, then external review round 2 of run 2+3, then merge. After that comes run 4 (`feat/fix-list`), which needs the S8 and the Pixel 8.

## Unapplied ROADMAP UPDATE blocks
None
