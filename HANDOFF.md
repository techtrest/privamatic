# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 16
- Branch: fix/stock-safe-advice
- Commit: 559995e
- Time: 2026-11-23T20:25:53+01:00
- Deviations: 4 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 1 (`fix/stock-safe-advice`) is done and merged into master. It is not pushed.
On stock Android, rows for apps that came with the phone now say "<App> came with your phone and can't be removed, so it costs 1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and data out of it." They have no App settings button. Google Camera's text says to keep it. User-installed apps keep their advice and button. The uninstall Quick Win only says uninstall. Scores are unchanged.
The S8 check passed (Chrome, Gmail and YouTube: new text, −1, no button; a user-installed Google Photos keeps its button). Screens are in `docs/screens/stock-safe-advice/`.
External review round 1 found no High or Medium and four Lows; all four are fixed, each proven first by a test that failed on the reviewer's mutation. 234 unit tests pass. Resolutions are in `docs/reviews/fix-stock-safe-advice-findings-1.md`.
The Google Camera row is checked only by a unit test so far; its phone check is in run 2+3's Pixel 8 gate.
Found: preinstalled AI-app rows (for example Gemini on a Pixel) lose their App settings button too, because the same rule covers every app that came with the phone.

## Blocked on you
None

## Red flags
None

## Pending decisions
None

## Next step for chat
Start v1.6 run 2+3 (rescan in place).

## Unapplied ROADMAP UPDATE blocks
None
