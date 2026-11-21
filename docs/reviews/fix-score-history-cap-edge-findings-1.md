No High, Medium, or Low findings. The new-day branch matches the change described in the prompt, and each of the five tests asserts the case its name describes.

What the change does
On the first recordScore of a new calendar day, a non-zero delta now stores now instead of keeping an older delta_first_appeared_timestamp. A zero delta still stores 0. The same-day path still goes through computeDeltaFirstAppeared, so a later scan that day keeps the time that day's change was first recorded.


ScoreHistoryRepository.kt
Ln 75–75

val deltaFirstAppeared = if (delta == 0) 0L else now
That value is what the widget uses. PrivacyWidgetProvider records the score, then shows the change only when the delta is non-zero, the timestamp is non-zero, and now - deltaFirstAppearedTimestamp <= 172_800_000 (48 h). The in-app delta is currentScore - dailyBaselineScore and does not use this timestamp.

Only one stored result changes: a new-day recording whose score differs from the previous current_score. Same keys and types (score_history, the same five prefs), same baseline promotion (dailyBaselineScore becomes the previous current score, baseline timestamp becomes today's midnight), same scoreDelta.

Tests
InMemorySharedPreferences applies edits immediately, and each example shares one store across recordScore calls, so later calls really read what earlier calls wrote. Europe/Berlin in mid-November 2026 has no daylight-saving change; 16 Nov 2026 is a Monday, so the weekday comments match the dates.

Test	What it locks
Fresh change after a few days away
Thu 09:00 after Mon 11:00 (70 h) stores Thu 09:00, baseline 70, delta −10. The old helper would have kept Mon 11:00.
Change again each morning, still shown in the evening
Wed 23:00 keeps Wed 07:00 (16 h), not Mon 22:00 (49 h, past the cap). The evening read is the same-day path, so this also checks the morning write was stored.
Later scans the same day
15:00 keeps 09:00 while the delta grows from −8 to −10 against the same baseline.
No change since yesterday
The first scan after midnight with an unchanged score stores delta 0 and timestamp 0.
First scan ever
Baseline equals the score, delta 0, timestamp 0, baseline time is that day's local midnight.
The <= cap asserts are implied by the timestamp equalities (age 0 h and 16 h). The equalities are the checks that fail on the old behavior.

Invariants
1–7. This diff does not touch the manifest, dependencies, backup rules, Room, PrivacyCheck, app detection, user-facing strings, or signing.

No database, entity, or DAO change.
Prefs file name, keys, and value types are unchanged. Nothing new is written.
The same-day path still reads and keeps a stored timestamp, including one written by the released build. The new write is only the new-day, non-zero-delta case.
currentScore, dailyBaselineScore, baseline promotion, and scoreDelta are computed as before.
