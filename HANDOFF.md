# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 6
- Branch: fix/score-history-cap-edge
- Commit: 2338f09
- Time: 2026-11-21T12:25:20+01:00
- Deviations: 4 (the words are in status.json, not in this public file)

## Where we are

Unattended run, two parts.
Part 1 (design, no code): `docs/decisions/snapshot-alarm-time.md` is merged into local `master` (`f4cf3f7`); its branch is deleted. The daily History point is set for 23:00 (the 23:50 comments are out of date) and may arrive until 17:00 the next day. On the Pixel 8 it lands the next morning, so each point is dated a day late, one day in the last week has no point, and the chart's even spacing hides the gap. Three fix options; the paper recommends B after a one-night phone check.
Part 2 (fix): `fix/score-history-cap-edge` is checked out and stopped at external review round 1. A score change on the first scan after midnight now starts its own 48-hour widget indicator instead of inheriting an older change's start. Unit test written failing first, then fixed; 200/200 unit tests pass; Pixel 8 check passed with the existing history unchanged. The midnight case itself is shown by the unit test, not on the phone.
`master` is 2 commits ahead of `origin/master` (the Part 1 merge).
Phones: Pixel 8 has this branch's debug build, on the home screen, history intact. S8 and Pixel 9 not touched.

## Blocked on you

1. Run external review round 1 in Cursor on `docs/reviews/fix-score-history-cap-edge-prompt-1.md` and save `docs/reviews/fix-score-history-cap-edge-findings-1.md`.
2. Pick the snapshot option (A, B or C) in `docs/decisions/snapshot-alarm-time.md`, and say whether "Your first data point will appear tonight" stays.
3. Push `master` when convenient (2 commits, Part 1).
4. The start-of-session question was skipped because you were away: any decisions from chats since 2026-11-20? Paste a ROADMAP UPDATE block into the inbox.

## Red flags

None

## Pending decisions

- Daily snapshot fix: Option A, B (recommended) or C — see the paper.

## Next step for chat

Review findings for `fix/score-history-cap-edge` round 1; if no High, merge with `--no-ff`. Then the snapshot option once chosen.

## Unapplied ROADMAP UPDATE blocks

None
