# Privamatic handoff

- Format: 3
- Project: Privamatic
- Run: 16
- Branch: fix/stock-safe-advice
- Commit: a46fc33
- Time: 2026-11-23T19:48:43+01:00
- Deviations: 2 (the words are in status.json, not in this public file)

## Where we are
v1.6 run 1 (`fix/stock-safe-advice`) is built and committed. On stock Android, rows for apps that came with the phone (Chrome, Gmail, YouTube and the like) no longer say they can be disabled and no longer show an App settings button. Google Camera's text now says to keep it. User-installed apps keep their advice and button. Scores are unchanged.
The build, the unit tests (232, 0 failures) and the debug APK all pass. The phone check has not run: the S8 shows as "no permissions" in `adb devices`, so nothing could be installed on it. The Pixel 8 was busy and the Pixel 9 wasn't used.
The review files are written (`docs/reviews/fix-stock-safe-advice.diff`, `-prompt-1.md`), and the branch stays checked out for Cursor.
The Google Camera row can't be checked on the S8 (it has no Google Camera); that check is now part of run 2+3's Pixel 8 gate in the spec.
Found: the uninstall Quick Win, shown for user-installed apps only, still says "Tap 'Uninstall' or 'Disable'", and user apps have no Disable button. Left for run 2+3, which rewrites that text.
Found: preinstalled AI-app rows (for example Gemini on a Pixel) lose their App settings button too, because the same rule covers every app that came with the phone.

## Blocked on you
1. Make the S8 reachable over USB, then tell this session so it can run the phone check. Right now `adb devices` lists it as "no permissions", which usually means USB mode or computer access, not the debugging prompt. A second, unknown device on another USB port shows the same and was left alone.
2. Run the external review of fix/stock-safe-advice in Cursor and save `docs/reviews/fix-stock-safe-advice-findings-1.md`.

## Red flags
None

## Pending decisions
None

## Next step for chat
Once the S8 is reachable: install the debug build, scan, and check that the Chrome, Gmail and YouTube rows show the new text and no button. Then apply the review findings, merge, and start run 2+3 `feat/rescan-in-place`.

## Unapplied ROADMAP UPDATE blocks
None
