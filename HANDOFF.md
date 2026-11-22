# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 12
- Branch: fix/guard-serial-assignments
- Commit: 2ad5a11
- Time: 2026-11-22T20:18:07+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
The guard fix for the device-serial gap has the HQ chat's first review worked in and waits on its re-check. It is not installed.
- M1 fixed: env options written together (like -vu NAME or -vC DIR) slipped past the check; the guard now denies env options it can't read safely and asks for them to be written separately.
- M2 is not a gap: device commands without -s were already always denied, also after env drops the serial. Test cases added to show it.
- Chained case added: dropping the serial for one command doesn't carry over to the next.
- All guard tests: the installed guard passes 567 of 579 (12 known gaps), the proposed one passes 579 of 579.
- The branch stays checked out for the re-check. Master also has the widget refresh fix, still not pushed.
Found: no phone was used.

## Blocked on you
Take the new scripts/guard.sh.proposed diff to the HQ chat for its re-check.
After the re-check, install it as .claude/hooks/guard.sh and run bash scripts/test-hook.sh.
Push master.

## Red flags
None.

## Pending decisions
Stopgap in today's guard for hanging device commands: decided at HQ's pilot review on 25 Nov.

## Next step for chat
HQ chat re-checks the new diff of scripts/guard.sh.proposed. Once Oliver has installed it and the tests pass 579 of 579, a short run merges fix/guard-serial-assignments.

## Unapplied ROADMAP UPDATE blocks
None.
