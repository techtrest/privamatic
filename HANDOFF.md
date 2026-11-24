# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 17
- Branch: feat/rescan-in-place
- Commit: 681d922
- Time: 2026-11-24T20:24:05+01:00
- Deviations: 5 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 2+3 (`feat/rescan-in-place`) is built and passed its Pixel 8 gate. 280 unit tests pass and the debug build assembles. The review files are written; the branch is checked out.
The phone check found two looks problems, both fixed and checked again on the phone: the scan bar vanished against the light Dashboard, and the score's number could wrap alone in the snackbar.
Not verified on a phone: trusting an app during a scan (no app to trust on the Pixel 8) and TalkBack (its speech can't be read from the computer).
Found: on a long Quick Win page, Check again is below the fold, and the snackbar covers part of it for a few seconds. Kept as it is.
Found: the phone agent's step that deletes the old APK first was refused and not worked around; the APK's time and its checksum on the phone show the right build was tested.
The Pixel 8 is as it was before, with the new debug build installed and today's scans in its history.

## Blocked on you
Run the external review of feat/rescan-in-place in Cursor. Files: `docs/reviews/feat-rescan-in-place.diff` and `docs/reviews/feat-rescan-in-place-prompt-1.md`; save the result as `docs/reviews/feat-rescan-in-place-findings-1.md`.

## Red flags
None

## Pending decisions
1. Call 8: "I've checked this" on AI-app rows? The spec says no for now. It has to be decided before v1.6.1 run 10.

## Next step for chat
After the review: prove or reject the findings, then merge. Next run after that is run 4.

## Unapplied ROADMAP UPDATE blocks
None
