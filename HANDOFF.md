# Handoff

- Format: 1
- Project: Privamatic
- Run: 3
- Branch: chore/sandbox-followup
- Commit: 2c23297 (base; this run's commits are on chore/sandbox-followup, not merged)
- Time: 2026-11-11T09:26:27+01:00

## Where we are

v1.5.2 is live on F-Droid. The Claude Code sandbox follow-up is installed and verified (11 Nov): cross-project secret blocks added, PATH program folders write-protected, the guard hook called through /usr/bin/bash. Offline build, 189 unit tests and 541 hook tests pass inside it. A widget fix is verified on two test phones and waits for external review.

## Blocked on you

- Merge chore/sandbox-followup with --no-ff, then push master.
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
