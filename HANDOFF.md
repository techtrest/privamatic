# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 12
- Branch: fix/guard-serial-assignments
- Commit: f2ab120
- Time: 2026-11-22T20:11:01+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
The guard's device-serial check has a gap, and a fix is ready for review. It is not installed.
- 16 test cases from Cull, adapted, plus 6 for the export path: when two serials are given, the guard already judges the last one, as Cull does.
- 7 cases fail on the installed guard: when `env -i` or `env -u ANDROID_SERIAL` drops the serial, the guard still lets a Gradle install through, which could then reach any attached phone.
- The proposed guard (scripts/guard.sh.proposed) makes env forget the dropped serial and changes nothing else. All 563 guard tests pass against it.
- The branch stays checked out; it merges after the guard is installed. Master also has the widget refresh fix, still not pushed.
- Roadmap block from chat applied (1 change, 3 additions, the HQ rule on proposed files noted in STATE.md).
Found: the sandbox won't write into .claude/hooks, so the proposed file sits in scripts/ beside the test script.
Found: no phone was used.

## Blocked on you
Take scripts/guard.sh.proposed to the HQ chat for review.
After the review, install it as .claude/hooks/guard.sh and run bash scripts/test-hook.sh.
Push master.

## Red flags
None.

## Pending decisions
Stopgap in today's guard for hanging device commands: decided at HQ's pilot review on 25 Nov.

## Next step for chat
Review scripts/guard.sh.proposed in the HQ chat. Once Oliver has installed it and the tests pass, a short run merges fix/guard-serial-assignments.

## Unapplied ROADMAP UPDATE blocks
None.
