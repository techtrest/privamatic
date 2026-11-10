# Handoff

- Format: 1
- Project: Privamatic
- Run: 2
- Branch: chore/sandbox-rollout
- Commit: 58145bc (base; this run's commit is on chore/sandbox-rollout, not merged)
- Time: 2026-11-10T18:02:04+01:00

## Where we are

v1.5.2 is live on F-Droid. Claude Code sandbox installed and verified (10 Nov). Unit tests now run inside it (Mockito loaded as a test agent, merged into local master, not pushed). A widget fix is verified on two test phones and waits for external review.

## Blocked on you

- Merge chore/sandbox-rollout, then push master.
- External review of the widget fix (round 1 prompt ready).
- Framework laptop setup, sandbox included (see local notes).

## Red flags

None

## Pending decisions

8 open in ROADMAP.md (kept local: this repo is public).

## Next step for chat

Three sandbox follow-ups: the device-verify agent (one adb command per call, no redirects); a third test phone waits for HQ's shared guard engine; the Framework laptop setup. Then the widget review findings.

## Unapplied ROADMAP UPDATE blocks

None
