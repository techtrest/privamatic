# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 18
- Branch: feat/rescan-in-place
- Commit: 8e0b7fb
- Time: 2026-11-25T21:18:54+01:00
- Deviations: 6 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 2+3 (`feat/rescan-in-place`): external review round 1 is answered. Seven findings were proven and fixed, one Low is rejected with evidence (resolutions at the end of `docs/reviews/feat-rescan-in-place-findings-1.md`). 312 unit tests pass (280 before), the debug build assembles, and the scan path passed its re-check on the Pixel 8.
The lost result after leaving the app and the covered Check again button were seen on the old build first, then checked on the new one. The fix for the interrupted move to Details failed its first phone check; the second round passed.
Not verified on a phone: a failed scan, a trust change during a scan (no app to trust on the Pixel 8), a tab tap interrupting the snackbar's move to Details, and TalkBack.
Found: tapping a tab while the pages still move to another tab can leave the app stuck between two pages (seen with slow animations; the same lines are on `master`). Not changed.
Found: if the trusted list can't be read when the app starts, the app crashes (code reading, on `master` too). Not changed.
Found: a Dashboard tap right after the snackbar's Details had no visible effect; not explained. The review is asked about it.
The Pixel 8 is as it was before, with the new debug build installed and today's scans in its history.

## Blocked on you
Run external review round 2 of feat/rescan-in-place in Cursor. Files: `docs/reviews/feat-rescan-in-place.diff` and `docs/reviews/feat-rescan-in-place-prompt-2.md`; save the result as `docs/reviews/feat-rescan-in-place-findings-2.md`.

## Red flags
None

## Pending decisions
1. Call 8: "I've checked this" on AI-app rows? The spec says no for now. It has to be decided before v1.6.1 run 10.

## Next step for chat
Findings of review round 2; if no High is left, merge. After that comes run 4 (`feat/fix-list`), which needs the S8 and the Pixel 8.

## Unapplied ROADMAP UPDATE blocks
None
