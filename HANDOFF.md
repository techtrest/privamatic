# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 15
- Branch: docs/v1.6-spec
- Commit: b70c3a4
- Time: 2026-11-23T19:28:53+01:00
- Deviations: 1 (the words are in status.json, not in this public file)

## Where we are
Oliver's v1.6 spec decisions of 23 Nov are recorded in `docs/decisions/v1.6-fix-to-result.md`, and `docs/v1.6-spec` is merged into master. It is not pushed.
v1.6 "the loop" is runs 1, 2+3 as one branch, 4, 5, 6, 9 and a release with today's single What's new card, including the Private DNS line: about 12.25 evenings.
v1.6.1 "explaining" is runs 7, 8, 10, 11 and a release: about 8 evenings. The Ad ID item (run 12) moved to Later.
Crash check of v1.5.3's settings buttons, from reading code only: no button is known to crash. The Ad ID screen's "Open settings" is the one plausible risk: it would crash only if a Google Play services update stops exporting the page it opens. Runs 4 and 6 add the missing catch. No hotfix needed.
Spec only: no app code changed, no phone used.

## Blocked on you
Push master (the v1.6 spec merge).
Before v1.6.1 run 10: decide call 8, whether AI-app rows get "I've checked this".

## Red flags
None

## Pending decisions
Call 8: AI-app rows aren't unknown. The scan reads them fine: the app is installed, costs 0 pts and is shown for awareness. So the spec says no until you decide.

## Next step for chat
Start v1.6 run 1 (stock-safe advice).

## Unapplied ROADMAP UPDATE blocks
None
