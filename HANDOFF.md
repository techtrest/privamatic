# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 6
- Branch: chore/handoff-2026-11-21
- Commit: a092445
- Time: 2026-11-21T12:53:20+01:00
- Deviations: 2 (the words are in status.json, not in this public file)

## Where we are

External review round 1 of the score history fix found nothing (no High, Medium or Low); no round 2. The fix is merged into local `master` with `--no-ff` (`a092445`) and its branch is deleted.
`master` is 6 commits ahead of `origin/master` (decision paper merge, fix merge and their commits); nothing is pushed.
This handoff sits on `chore/handoff-2026-11-21` because HANDOFF.md can't be committed on `master`; that branch holds only this file.
Pixel 8: this branch's debug build, home screen, history intact (unchanged since the gate). S8 and Pixel 9 not touched.
The findings file was saved as `ix-score-history-...` (missing "f"); renamed before committing.

## Blocked on you

1. Push `master`.
2. Merge `chore/handoff-2026-11-21` into `master` (handoff only), or drop it.
3. Choose snapshot option A, B or C in `docs/decisions/snapshot-alarm-time.md`.

## Red flags

None

## Pending decisions

- Daily snapshot fix: Option A, B (recommended) or C — see the paper.

## Next step for chat

Decide the snapshot option; then a build run for it (B starts with a one-night Doze check on the Pixel 8).

## Unapplied ROADMAP UPDATE blocks

None
