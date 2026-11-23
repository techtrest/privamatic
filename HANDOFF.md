# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 16
- Branch: chore/wrap-up-2026-11-23
- Commit: 6906571
- Time: 2026-11-23T20:27:37+01:00
- Deviations: none

## Where we are
v1.6 run 1 (`fix/stock-safe-advice`) is done and merged into master. Master waits for Oliver's push.
On stock Android, rows for apps that came with the phone have no disable advice and no App settings button. Scores are unchanged. The S8 check passed, the external review's four Lows are fixed, and 234 unit tests pass.
Next is v1.6 run 2+3, `feat/rescan-in-place`. It needs the Pixel 8, which a Calluna session was using on 23 Nov. Its gate also checks the Google Camera row on the Pixel 8.
Open items for run 2+3: the uninstall Quick Win's steps need to be made consistent when its trailing rescan sentence goes (step 2 "Tap 'Uninstall'" quoted like step 1, no full stops on the steps), and the roadmap line on device-verify's `cd` prefix (the agent file must say adb commands start with adb itself) is still open.
Call 8 (AI-app rows and "I've checked this") is still open and has to be decided before v1.6.1 run 10.

## Blocked on you
None

## Red flags
None

## Pending decisions
1. Call 8: "I've checked this" on AI-app rows? The spec says no for now. It has to be decided before v1.6.1 run 10.

## Next step for chat
Start v1.6 run 2+3 (rescan in place) once the Pixel 8 is free.

## Unapplied ROADMAP UPDATE blocks
None
