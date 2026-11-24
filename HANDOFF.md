# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 17
- Branch: feat/rescan-in-place
- Commit: b69f076
- Time: 2026-11-24T20:01:36+01:00
- Deviations: 2 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 2+3 (`feat/rescan-in-place`) is built, self-reviewed and committed on its branch. Nothing has been checked on a phone; the Pixel 8 was not confirmed free.
A rescan keeps the current screen, and every scan the user asks for shows one snackbar with the points and the score. 280 unit tests pass (234 before), and the debug build assembles.
Found: before this branch, a scan that failed would have crashed the app (shown by a unit test on the same code pattern). Fixed here.
The self-review fixed four things on the rescan path. STATE.md lists them, the choices made where the spec is silent, and everything the Pixel 8 gate has to check, including the Google Camera row from run 1 and the Breakdown jump.
After the phone gate come the external review files, then the review.

## Blocked on you
Tell the session when the Pixel 8 is free for the run 2+3 phone check. The gate needs it, and other projects share it.

## Red flags
None

## Pending decisions
1. Call 8: "I've checked this" on AI-app rows? The spec says no for now. It has to be decided before v1.6.1 run 10.

## Next step for chat
Confirm the Pixel 8 is free, then start the device gate on `feat/rescan-in-place`.

## Unapplied ROADMAP UPDATE blocks
None
