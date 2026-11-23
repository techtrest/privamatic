Review docs/reviews/fix-stock-safe-advice.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..fix/stock-safe-advice: on stock Android, rows for
flagged apps that came with the phone no longer tell the user to disable them and no longer show
an Open App Settings button; the Google Camera text says to keep it; scores are unchanged). You
can't run git, Gradle or tests in this workspace. Read only the files this diff touches, and only
the functions around the changed lines — don't explore the rest of the repo. strings.xml is part
of the point here (three strings), and so are the tests: check that they really test what their
names claim. Skip screenshots. If you need more than ~10 files, stop and say which and why.

This is a light review: a small copy-and-button change (v1.6 run 1, spec
docs/decisions/v1.6-fix-to-result.md, section 4.1 "Hard rule changes" and calls 10 and 12).

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review. "Came with the phone" is the existing `PrivacyIssue.isPreinstalledApp`
(`isSystemApp && check.packageName != null`), where `isSystemApp` comes from
`PackageManagerUtil.isSystemApp` (`FLAG_SYSTEM`, which Android keeps on updated system apps).
No second rule was added: the new `PrivacyIssue.rowActionType` returns null for those rows and
both button sites in `IssueItem` read it. It also applies to the informational AI-app rows
(e.g. a preinstalled Gemini), on purpose: same App info page, same Disable button. The Play
Services row was already compliant (no actionType, `isPreinstalledApp` false) and is untouched.
The uninstall Quick Win only exists for user-installed apps (`QuickWinsDetector.checkInstalledApps`,
`!issue.isSystemApp`); its text loses the pre-installed sentence, and its step 2 now says
"Tap Uninstall." (a user app's App info page has no Disable button), and its first line is now
"You can improve your privacy by uninstalling this app." Its trailing "rescan" sentence stays
until run 2+3, on purpose (Oliver).
The texts, as Oliver agreed them:
- `fmt_recommendation_system_app`: "%1$s came with your phone and can't be removed, so it costs
  1 pt while it's here. You don't have to use it: pick an alternative, and keep your account and
  data out of it."
- `privacy_check_google_camera_system_recommendation`: "%1$s came with your phone and can't be
  removed. If you'd rather use an open-source camera, install Open Camera and use that instead.
  Keep %1$s as it is: it is your phone's built-in camera."
Unit tests pass (233, 0 failures).
Phone check passed on the S8 (Android 9): Chrome, Gmail and YouTube (system apps there) show
the new text with their own name, cost −1 and have no button; Google Photos (user-installed there)
keeps its uninstall text and Open App Settings button. Google Camera isn't on the S8: its text is
covered only by PrivacyIssueTest, and the Pixel 8 check is in run 2+3's gate.

Standing invariants — check that each holds on every path this diff touches:
1. Offline: no INTERNET permission, no network calls, no new dependency that
   can reach the network.
2. User data never leaves the device: score history (Room), the trusted list
   and settings are excluded from cloud backup and device transfer, in both
   data_extraction_rules.xml (Android 12+) and the legacy backup rules.
3. Room: every schema change bumps the version and ships a migration; no
   destructive fallback; existing score history survives an upgrade.
4. Scoring: the PrivacyCheck enum is the single source of truth for
   deductions; trust math goes through PrivacyCheck.cappedDeductionFor();
   no score logic elsewhere.
5. Detection: a disabled app still returns ApplicationInfo — check
   appInfo.enabled before treating a package as active.
6. All user-facing text comes from strings.xml.
7. Release builds stay unsigned (signingConfig = null) for F-Droid.

Run-specific invariants:
8. No string or button tells the user to disable or uninstall an app that came with the phone:
   every row where `isPreinstalledApp` is true shows the system-app text and no settings button,
   on both the scored and the informational branch of `IssueItem`; user-installed flagged apps
   (including Google apps on GrapheneOS, where they are user apps) keep their uninstall text and button.
9. No score changes: no `PrivacyCheck` point value, deduction path or `isSecure` result changes;
   preinstalled flagged apps still cost their 1 pt. The new text's "It costs 1 pt" must stay true
   for every row that can show it (`PrivacyIssueTest` guards that).
10. Copy follows the strings.xml voice: direct, short, no exclamation marks; apostrophes escaped
    so aapt doesn't drop them; `%1$s` used consistently.
