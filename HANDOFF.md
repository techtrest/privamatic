# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 20
- Branch: chore/device-verify-rules
- Commit: 7fe742d
- Time: 2026-11-27T18:27:56+01:00
- Deviations: none

## Where we are
Tooling run, no phone. The device-verify rules are tightened: phone commands start with the device tool itself with nothing around them, never wait for the device, stop on a phone that isn't ready, no server-wide commands, screenshots only through the screenshot path (app data: stop and report). CLAUDE.md has the same rules and is merged into local master. The agent file itself is only proposed (scripts/device-verify.md.proposed), because the sandbox keeps .claude/agents read-only. The two ROADMAP device-verify items tick once it is installed.
Not verified on a device: the agent following the new rules; that shows on the next device-verify run.

## Blocked on you
Copy scripts/device-verify.md.proposed over .claude/agents/device-verify.md and commit it on a branch (the sandbox can't write there). Then push master.

## Red flags
None

## Pending decisions
None

## Next step for chat
Start v1.6 run 4 (fix list), phone-free build first.

## Unapplied ROADMAP UPDATE blocks
None
