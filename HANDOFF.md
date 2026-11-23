# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 14
- Branch: docs/v1.6-spec
- Commit: 746ec2f
- Time: 2026-11-23T18:46:24+01:00
- Deviations: none

## Where we are
The v1.6 "fix → result" spec is written and committed on `docs/v1.6-spec` (`docs/decisions/v1.6-fix-to-result.md`). It is not merged. It checks each in-scope finding from the October UX audit against master: H1, H3, H4 and M4 are partly fixed; M3 is fixed in code but not yet seen on a phone; M2, M5, M6, M7, half of M11, M12 and L10 still hold.
For each open item it gives what the user sees, the data and scoring changes, edge cases, tests, a table of settings pages by phone, a build order of 12 implementation runs and a cut line.
Read-only design run: no app code changed, no phone used.
Found: Quick Win point values understate fixes that cover several apps. The advice for apps that came with the phone still suggests disabling them. The keyboard Quick Win opens the wrong page. These are from reading code only.

## Blocked on you
Review the v1.6 spec in docs/decisions/v1.6-fix-to-result.md, decide the 17 open questions in section 8 and the cut line in section 7.

## Red flags
None

## Pending decisions
The 17 Oliver's calls in section 8 of the spec, each with a recommended default.
The cut line: everything is about 22 evenings, 20 without the Ad ID merge, against two weeks (10–14 evenings).

## Next step for chat
Once the spec is agreed, the first implementation run is `fix/stock-safe-advice` (run 1 in section 6).

## Unapplied ROADMAP UPDATE blocks
None
