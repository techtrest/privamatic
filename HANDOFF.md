# Handoff

- Format: 1
- Project: Privamatic
- Run: 4
- Branch: chore/sandbox-tinypass
- Commit: 626bc76 (base; this run's commits are on chore/sandbox-tinypass, not merged)
- Time: 2026-11-11T10:38:20+01:00

## Where we are

v1.5.2 is live on F-Droid. The sandbox follow-up is merged and pushed. A sandbox tiny pass is installed and verified (11 Nov): further sandbox blocks added, every probe denied. The offline build and unit test run inside it follow this commit. A widget fix is verified on two test phones and waits for external review.

## Blocked on you

- Merge chore/sandbox-tinypass with --no-ff, then push master.
- External review of the widget fix (round 1 prompt ready).
- Framework laptop setup, sandbox included (see local notes).

## Red flags

None

## Pending decisions

8 open in ROADMAP.md (kept local: this repo is public).

## Next step for chat

Guard gaps for HQ's shared guard engine: jq is found through PATH, and the guard doesn't expand variables. Then the device-verify agent under the sandbox, and the widget review findings.

## Unapplied ROADMAP UPDATE blocks

None
