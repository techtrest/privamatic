# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 16
- Branch: fix/stock-safe-advice
- Commit: d1e56f0
- Time: 2026-11-23T20:03:00+01:00
- Deviations: 3 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 1 (`fix/stock-safe-advice`) is built, checked on the S8 and waiting for the external review. The branch stays checked out for Cursor.
On stock Android, rows for apps that came with the phone now say "<App> came with your phone and can't be removed, so it costs 1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and data out of it." They have no App settings button. Google Camera's text says to keep it. User-installed apps keep their advice and button. Scores are unchanged. The uninstall Quick Win's step now says "Tap Uninstall."
The build and the unit tests pass: 233 tests, 0 failures.
On the S8 (Android 9), Chrome, Gmail and YouTube showed the new text, −1 and no button. Google Photos, which is user-installed on that phone, kept its Open App Settings button. The crash buffer was empty. Screens are in `docs/screens/stock-safe-advice/`.
The Google Camera row can't be checked on the S8 (no Google Camera); its check is in run 2+3's Pixel 8 gate.
Found: preinstalled AI-app rows (for example Gemini on a Pixel) lose their App settings button too, because the same rule covers every app that came with the phone.
Found: the uninstall Quick Win's first line still says "by uninstalling or disabling this app"; it was left as it is on Oliver's instruction.

## Blocked on you
1. Run the external review of fix/stock-safe-advice in Cursor and save `docs/reviews/fix-stock-safe-advice-findings-1.md`.

## Red flags
None

## Pending decisions
None

## Next step for chat
After the review: prove or reject each finding, fix what holds, re-run the gate, merge into master with --no-ff, then start run 2+3 `feat/rescan-in-place`.

## Unapplied ROADMAP UPDATE blocks
None
