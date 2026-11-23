No High or Medium findings. The button gate and the three strings match the rule: a preinstalled row gets the came-with-your-phone text and no settings button, a user-installed row keeps its uninstall text and button, and nothing in this diff changes a point value.

## Low

**1. `PrivacyIssueTest` does not lock the "1 pt" sentence** — `app/src/test/java/com/techtrest/privamatic/data/model/PrivacyIssueTest.kt:103`

`every scored app check costs 1 pt as the preinstalled advice says` asserts `check.pointDeduction == 1` for scored app checks. It never reads `fmt_recommendation_system_app`. The comment says the string contains "It costs 1 pt"; the string at `app/src/main/res/values/strings.xml:789` says "so it costs 1 pt while it's here."

Failure: change that sentence to "costs 2 pt" (or drop it). This test and `StockSafeAdviceTest` stay green, the chip still shows −1, and the row text is wrong. The S8 check would not be re-run. Confidence: high.

**2. The camera "keep it" wording is not what the camera test checks** — `app/src/test/java/com/techtrest/privamatic/data/model/PrivacyIssueTest.kt:39`

`preinstalled camera gets its own keep-it advice` asserts the resource id only. `StockSafeAdviceTest` then applies the same checks to that string as to the generic one ("came with your phone", no "disabl", no "uninstall it" / "remove it"). Nothing asserts "Keep", "Open Camera", or "built-in camera".

Failure: replace `privacy_check_google_camera_system_recommendation` (`strings.xml:234`) with the generic "pick an alternative" sentence. Tests stay green. Google Camera is not on the S8, and the Pixel 8 check is deferred to run 2+3, so the row would tell the user to switch away from the built-in camera without a failing test. Confidence: high.

**3. Spec section 4.1 still quotes the earlier generic sentence** — `docs/decisions/v1.6-fix-to-result.md:81`

The spec says: "It costs 1 pt for as long as it's here. You don't have to use it: set another app as your default and keep your data out of it." The shipped string (`strings.xml:789`) is the later wording: "so it costs 1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and data out of it."

Failure: a later run that implements section 4.1 literally overwrites the agreed sentence. Confidence: high.

**4. The uninstall ban is narrower than the test name** — `app/src/test/java/com/techtrest/privamatic/StockSafeAdviceTest.kt:45`

`system-app advice never says uninstall or remove it` rejects only the substrings "uninstall it" and "remove it", and only on two string names. The class comment (`StockSafeAdviceTest.kt:11`) says nothing tells the user to uninstall an app that came with the phone. "Consider uninstalling" or "remove this app" would pass. "Can't be removed" is allowed on purpose. Current copy does not say uninstall. Confidence: high.

## Checked, not findings

- Both `IssueItem` branches use `rowActionType` (`IssueItem.kt:150` and `:199`). It is null when `isPreinstalledApp` (`PrivacyIssue.kt:47`). The scored branch shows `systemAppRecommendation` (`IssueItem.kt:188`).
- The informational branch shows the check description, not the "costs 1 pt" sentence. For a preinstalled Gemini that description is the shared AI line (`strings.xml:299`), which does not say disable or uninstall, and the button is gone. Showing the 1 pt sentence there would be false: those rows are informational and the 1 pt test excludes them.
- User-installed rows still take `issue.recommendation` and `check.actionType`. Play Services is unchanged (`packageName` null, so not a preinstalled row, and `actionType` null).
- Apostrophes in the three strings are escaped (`can\'t`, `it\'s`, `don\'t`, `you\'d`, `phone\'s`). No exclamation mark in the two system-app strings. The Quick Win still ends with "rescan to see your improved score!" — the trailing sentence kept on purpose. `%1$s` is reused twice in the camera string, and the row passes one name argument.
- No path in this diff adds a network call, a backup or Room change, a `PrivacyCheck` point change, or a signing config.
- `isSystemApp` tests match their names: `FLAG_SYSTEM` and `FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP` count as came-with-the-phone, flag 0 does not, and `NameNotFoundException` counts as system so the row gives no removal advice. That last one is the safe direction and is what the new test locks.

Ask mode, so this was not written to `docs/reviews/fix-stock-safe-advice-findings-1.md`.

**Blocked on me**
- None from this review. Saving the findings file needs a mode that can write.

**Changed**
- None. Read only.

**Found**
- Four Low findings, above. No High or Medium.
- Roadmap: unchanged.

---

## Resolution (Claude Code, 2026-11-23)

Oliver: fix all four Lows, no round 2. Each new assertion was proven first: the string was mutated as the finding describes, the new test failed, the string was restored and the suite passed (`testDebugUnitTest --rerun`, 234 tests, 0 failures). Fixed in `559995e`.

1. **Fixed.** `PrivacyIssueTest.every scored app check costs 1 pt as the preinstalled advice says` now also reads `fmt_recommendation_system_app` from strings.xml and asserts it contains "costs 1 pt"; the comment quotes the shipped wording. Proof: "costs 2 pt" → the test FAILED; restored → passed.
2. **Fixed.** New `StockSafeAdviceTest.camera advice says keep it and points to Open Camera` asserts "Keep", "Open Camera" and "built-in camera". Proof: the camera string replaced by the generic "pick an alternative" sentence → FAILED; restored → passed.
3. **Fixed.** Spec 4.1 now quotes the shipped sentence ("…can't be removed, so it costs 1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and data out of it."). Docs only, no test.
4. **Fixed.** The test is now `system-app advice never mentions uninstalling or removing, except that it can't be removed`: for both system-app strings it rejects "uninstall" and "remov" in any form, case-insensitive, after taking out the exact phrase "can't be removed". The class comment says the same. Proof: "… Consider uninstalling it." → FAILED; "…, or remove this app." → FAILED; restored → passed.
