# Privamatic — workflow inventory (2026-10-20)

Read-only inventory of the checkout at `~/Local App Projects/Privamatic` and the MiniPC it lives on,
taken to ground the planned Claude Code workflow (CLAUDE.md, STATE.md, guard hook, verify subagent).
Every answer quotes the command that produced it, trimmed. Secrets were not read: only file paths,
property names and variable names appear here.

Branch for this file: `chore/workflow-inventory`, cut from `master` at `08b42fc`.

---

## 1. Repo

```
$ git branch --show-current          → master (before this run; now chore/workflow-inventory)
$ git symbolic-ref refs/remotes/origin/HEAD
refs/remotes/origin/master
$ git remote -v
origin  git@github.com:techtrest/privamatic.git (fetch)
origin  git@github.com:techtrest/privamatic.git (push)
$ git status --short
?? docs/audits/
?? docs/store-graphics/
?? docs/store/
$ git log --oneline -5
08b42fc Merge branch 'fix/redacted-dev-settings'
f927453 copy: explain why USB debugging and Developer options are 'to review' on Android 17
791929d docs: USB_STATE fallback behaviour when unplugged
44565a7 store: v1.5.1 changelog adds the 1.5 highlights
90a7fd8 fix: show the 1.5 What's new notes only to users who haven't seen them
$ git tag --sort=-creatordate
v1.5.1  v1.5.0  v1.4.1  v1.4.0  v1.3.1  v1.3.0  v1.2.1  v1.1  v1.0.0
```

- **Default branch: `master`** (matches CONVENTIONS.md).
- 19 local branches besides master (many `fix/*` that look merged, plus `release/1.4.0`); only
  `master` and `fix/privacy-tip-selection` exist on the remote.
- The three untracked `docs/` folders hold about 28 MB of screenshots and graphics
  (`du -sh`: audits 25M, store 2.3M, store-graphics 704K). They were not committed.

## 2. Existing Claude setup and project docs

```
$ ls -la CLAUDE.md STATE.md
ls: cannot access 'CLAUDE.md': No such file or directory
ls: cannot access 'STATE.md': No such file or directory
$ find .claude -type f
.claude/settings.local.json
$ git ls-files .claude CLAUDE.md
.claude/settings.local.json                     ← TRACKED
$ grep -n claude .gitignore
51:.claude/
$ git log --oneline --follow -- .claude/settings.local.json | head -3
2cb04e4 release: v1.3.1 — trust math fix, backup exclusions, ...   (last change)
...
5aeeb78 Update .gitignore: exclude IDE, Claude, and decompiled files   (first commit with it)
$ jq '{keys: keys, allow: (.permissions.allow|length)}' .claude/settings.local.json
{ "keys": ["permissions"], "allow": 51 }
```

- **CLAUDE.md:** missing. **STATE.md:** missing. **`.claude/settings.json`:** missing.
  **`.claude/agents/`, `.claude/hooks/`:** missing. **`~/.claude/agents`, `~/.claude/hooks`:** missing.
  `~/.claude/settings.json` exists with keys `agentPushNotifEnabled, model, modelSettings, tui` and no `hooks`.
- **`.claude/settings.local.json` is committed even though `.gitignore` ignores `.claude/`.**
  The file was committed in the same commit that added the ignore rule, and an ignore rule has no
  effect on a file git already tracks. It holds 51 accumulated allow rules, among them broad ones:
  `Bash(git:*)`, `Bash(adb:*)`, `Bash(git commit:*)`, `Bash(./gradlew assembleRelease:*)`,
  `Bash(sudo dnf install -y imagemagick)`, plus one-off rules that still use the old `privacywidget`
  package and a stale JBR path. A grep for password, secret, token and api key names in it found nothing.
- **CONVENTIONS.md** (316 lines, tracked): the main rulebook. It covers project identity and build
  commands for each machine (Temurin 21 at `~/.local/share/jdk/temurin-21`; JBR 25 breaks Gradle 8.10),
  the file structure, architecture, detection and scoring rules, strings and localisation, security
  (no network, no SDKs, no GMS, backup exclusions), F-Droid build and release rules, and on-device test rules.
- **.cursorrules** (270 lines, tracked): an older copy of CONVENTIONS.md with the same headings.
  `diff` shows it is behind, for example it still has the old MiniPC JBR build command. Two copies
  of the rules that can drift apart.
- **docs/QUICK_REFERENCE.md** and **docs/POST_IMPLEMENTATION_CHECKLIST.md** (51 lines each, tracked):
  `diff -q` says they are **identical**. They hold a prompt preamble for AI code generation ("Read
  CONVENTIONS.md and .cursorrules"), recipes for common tasks, and a 30-second quality checklist.
- **docs/audits/ux-audit-2026-10.md** (untracked, 261 lines plus 147 PNGs): an on-device UX audit
  of v1.4.1 debug on the Pixel 8, done 2026-10-03, with findings ranked High, Medium and Low and the top 5 recommendations.
- **docs/store/2026-10/raw, docs/store-graphics/round1** (untracked): raw store screenshots,
  feature-graphic drafts and a `store.py` generator script.

## 3. Build

```
$ for j in /opt/android-studio/jbr ~/.gradle/jdks/* ~/.local/share/jdk/temurin-21; do "$j/bin/java" -version; done
/opt/android-studio/jbr                                  openjdk 25.0.2
~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2         openjdk 21.0.7
~/.local/share/jdk/temurin-21                            openjdk 21.0.7
(system java: openjdk 25.0.4.1; JAVA_HOME unset)

$ JAVA_HOME=<jdk> ./gradlew :app:compileDebugKotlin
/opt/android-studio/jbr                         → BUILD FAILED  ("What went wrong: 25.0.2")
~/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 → exit 0
~/.local/share/jdk/temurin-21                    → exit 0
```

- **Working JDK:** `~/.local/share/jdk/temurin-21`, the one CONVENTIONS.md pins. The `~/.gradle/jdks` copy
  also works, but Gradle manages that cache and can delete it. Gradle wrapper: 8.10.
- Every successful build prints a harmless warning: `SDK processing. This version only understands SDK XML versions up to 3 but ... version 4`.
- **Debug applicationIdSuffix: yes.** `app/build.gradle.kts`:
  ```kotlin
  debug {
      // Lets debug builds install alongside the F-Droid release on the same device
      applicationIdSuffix = ".debug"
  }
  ```
- **Debug applicationId and APK:**
  ```
  $ JAVA_HOME=~/.local/share/jdk/temurin-21 ./gradlew :app:packageDebug   → exit 0 (UP-TO-DATE)
  $ jq . app/build/outputs/apk/debug/output-metadata.json
  applicationId: com.techtrest.privamatic.debug, versionCode 9, versionName 1.5.1, outputFile app-debug.apk
  $ aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
  package: name='com.techtrest.privamatic.debug' versionCode='9' versionName='1.5.1'
  ```
  APK path: **`app/build/outputs/apk/debug/app-debug.apk`**. The task was up to date, so the APK on disk
  dates from 2026-10-05 19:40. The Pixel 8 install has the same `lastUpdateTime`.

## 4. Signing

```
$ grep -n -E 'signingConfig' app/build.gradle.kts
            signingConfig = null          (inside buildTypes.release)
```
- **There is no `signingConfigs` block. The release build type uses `signingConfig = null`**, which is
  what F-Droid expects (CONVENTIONS: "remove entire `signingConfigs` block").
- Property names only:
  - `gradle.properties`: `org.gradle.jvmargs, android.useAndroidX, kotlin.code.style, android.nonTransitiveRClass`
  - `local.properties`: `sdk.dir`
  - `~/.gradle/gradle.properties`: does not exist
- Environment variables whose names match PASSWORD, KEY, SECRET or TOKEN: only `CLAUDE_CODE_MESSAGING_TOKEN`,
  which belongs to the Claude Code session. `PRIVAMATIC_STORE_PASSWORD` and `PRIVAMATIC_KEY_PASSWORD`
  are **not set** in this shell.
- Keystore search:
  ```
  $ find . -path ./.git -prune -o \( -name '*.jks' -o -name '*.keystore' \) -print     → (none in repo)
  $ find ~ -maxdepth 4 \( -name '*.jks' -o -name '*.keystore' \)
  ~/shoppingapp-release.jks
  ~/.android/debug.keystore
  ~/Documents/ShoppingApp/.android/debug.keystore
  ~/.local/share/keyrings/user.keystore
  $ ls ~/privamatic-release.jks
  ls: cannot access '/home/techtresthome/privamatic-release.jks': No such file or directory
  $ find ~ -maxdepth 6 -iname '*privamatic*' \( -name '*.jks' -o -name '*.keystore' \)   → (none)
  ```
  **CONVENTIONS.md says the release keystore is at `~/privamatic-release.jks`. No such file exists on
  this machine.** It may be on the Framework laptop or offline. **Unconfirmed.** I did not look in
  other mounts or on other machines.
- Tracked by git:
  ```
  $ git ls-files | grep -E '\.jks$|\.keystore$|local\.properties$'   → none tracked
  $ git log --all --oneline -- '*.jks' '*.keystore' local.properties  → (no history)
  ```
  `local.properties` is ignored by `.gitignore` (lines 17–18).

## 5. Release metadata

```
$ find fastlane -maxdepth 4
fastlane/metadata/android/en-US/{title.txt, short_description.txt, full_description.txt}
fastlane/metadata/android/en-US/changelogs/{1..9}.txt
fastlane/metadata/android/en-US/images/{icon.png, featureGraphic.png, phoneScreenshots/1..6.png}
$ git log -1 --format='%h %s' -- fastlane/metadata/android/en-US/changelogs/9.txt
44565a7 store: v1.5.1 changelog adds the 1.5 highlights
$ grep -E 'versionCode|versionName' app/build.gradle.kts
        versionCode = 9
        versionName = "1.5.1"
```
- Latest changelog: **`fastlane/metadata/android/en-US/changelogs/9.txt`**, which matches versionCode 9.
  The latest tag is `v1.5.1`. Only the en-US locale exists.

## 6. User data in the app

All paths are relative to `app/src/main/java/com/techtrest/privamatic/`.

**DataStore (Preferences)**: files in `files/datastore/<name>.preferences_pb`

| Name | Defined in | Keys | Purpose |
|---|---|---|---|
| `trusted_apps_prefs` | `data/TrustedAppsRepository.kt:14` | `trusted_packages` (string set), `apps_banner_dismissed` (bool) | trusted list |
| `sdk_scan_prefs` | `data/SdkScanRepository.kt:18` | `sdk_scan_result` (string, serialised) | SDK scan cache |
| `quick_win_dismissals` | `data/maintenance/QuickWinDismissalManager.kt:15` | `dismissed_<checkName>` (bool) | dismissed Quick Wins |
| `maintenance_prefs` | `data/maintenance/MaintenanceManager.kt:28` | `manual_check_<ManualCheckType>_timestamp` (long) | manual-check completion times |

**SharedPreferences**: files in `shared_prefs/<name>.xml`

| Name | Defined in | Keys | Purpose |
|---|---|---|---|
| `score_history` | `data/ScoreHistoryRepository.kt:112` | `current_score, daily_baseline_score, daily_baseline_timestamp, last_update_timestamp, delta_first_appeared_timestamp` | score delta / daily baseline |
| `ad_id_prefs` | `data/scanner/checks/NetworkSecurityChecker.kt:200` (also `AdIdVerificationScreen.kt:412`, `MaintenanceManager.kt:58`) | `ad_id_verified, ad_id_verified_timestamp` | Ad ID verification |
| `onboarding_prefs` | `data/OnboardingPreferences.kt:10` | `onboarding_complete, force_show_ad_id_check` | onboarding state |
| `whats_new_prefs` | `data/WhatsNew.kt:8` | `last_seen_version_code` (int) | What's new card |
| `details_view_prefs` | `data/DetailsViewPreferences.kt:7` | `checks_view` | Details/Checks view setting |
| `widget_preferences` | `WidgetPreferences.kt:7` | `widget_opacity_<widgetId>` | widget settings |
| `privacy_tip_history` | `data/maintenance/PrivacyTipHistory.kt:6` | `tip_shown_<…>` | tip rotation history |

**Room**: `data/PrivacyDatabase.kt`, database `privacy_history.db`, table `privacy_snapshots`
(entity `PrivacySnapshot`), `version = 1`, `exportSchema = false`, `JournalMode.TRUNCATE`. This is the score-history chart data.

**Migrations:**
```
$ grep -rn -E 'Migration\(|addMigrations|fallbackToDestructive|AutoMigration|SharedPreferencesMigration|produceMigrations|DataMigration' app/src/main/java
(no matches)
```
- There are no Room migrations and no DataStore migrations. The Room schema is still at version 1 and is not exported,
  so the first schema change will need a hand-written migration, and there are no exported schemas to test it against.
- The only upgrade-aware logic: `WhatsNewPreferences.shouldShow()` treats "no stored version and not an update" as a
  fresh install, and uses install and update times to detect upgrades from v1.4.1 and older.
  `MaintenanceManager.resetAdIdCheck()` clears the Ad ID state in both stores. That is a reset, not a migration.

**Backup / data extraction:**
```
$ grep -n -E 'allowBackup|dataExtractionRules|fullBackupContent' app/src/main/AndroidManifest.xml
13: android:allowBackup="true"
14: android:dataExtractionRules="@xml/data_extraction_rules"
15: android:fullBackupContent="@xml/backup_rules"
```
- `res/xml/backup_rules.xml` (API ≤ 30) excludes: sharedpref `ad_id_prefs`, `score_history`;
  file `datastore/{maintenance_prefs, sdk_scan_prefs, trusted_apps_prefs, quick_win_dismissals}.preferences_pb`;
  database `privacy_history.db`.
- `res/xml/data_extraction_rules.xml` (API 31+) excludes the same set from **both** `<cloud-backup>` and
  `<device-transfer>`, and adds `privacy_history.db-shm` and `privacy_history.db-wal`.
- Nothing is listed under `<include>`, so everything not excluded is backed up: `onboarding_prefs`, `whats_new_prefs`,
  `details_view_prefs`, `widget_preferences`, `privacy_tip_history`.
- **Possible bug (not verified on a device):** the `sharedpref` excludes use `path="ad_id_prefs"` and
  `path="score_history"` with no `.xml`. Android's examples name the shared-prefs file with its extension
  (for example `path="device.xml"`). If the extension is required, these two excludes match nothing and both files are
  still backed up. This could be checked with `adb shell bmgr backupnow` and a look at the transport. I did not run that,
  because it changes device state.
- Smaller gap: `backup_rules.xml` does not exclude `-shm` and `-wal`. That is harmless with `TRUNCATE`
  journal mode, but it does not match `data_extraction_rules.xml`.

## 7. Devices

```
$ adb devices -l                     (first run: only the Pixel 8; the Pixel 9 was connected mid-run, second run:)
<test serial>   device usb:7-1 product:shiba model:Pixel_8 device:shiba transport_id:1
<daily phone serial>   device usb:5-1 product:tokay model:Pixel_9 device:tokay transport_id:2
```

**Pixel 8** (stock, dedicated test device)
```
$ adb -s <test serial> shell getprop ro.product.model          → Pixel 8
$ adb -s <test serial> shell getprop ro.build.version.release  → 17   (SDK 37)
$ adb -s <test serial> shell getprop ro.build.fingerprint
google/shiba/shiba:17/CP3A.260905.009/16091614:user/release-keys
$ adb -s <test serial> shell pm list packages | grep -i privamatic
package:com.techtrest.privamatic.debug
$ adb -s <test serial> shell dumpsys package com.techtrest.privamatic.debug
    versionCode=9 minSdk=26 targetSdk=35   versionName=1.5.1
    installerPackageName=null   initiatingPackageName=com.android.shell
    firstInstallTime=2026-10-03 17:21:37   lastUpdateTime=2026-10-05 19:40:24
$ adb -s <test serial> shell dumpsys package com.techtrest.privamatic   → (empty; not installed)
$ adb -s <test serial> shell pm list users  → only user 0 (Owner)
```
- The F-Droid release `com.techtrest.privamatic` is **not installed** on the Pixel 8. Only the debug build is,
  installed with `adb install` (`installerPackageName=null`, initiated by the shell).
- The UX audit recorded build CP2A.260805.005. The device is now on CP3A.260905.009, so it has taken an OTA since 2026-10-03.

**Pixel 9** (GrapheneOS, daily phone; only read-only commands were run)
```
$ adb -s <daily phone serial> shell getprop ro.product.model          → Pixel 9
$ adb -s <daily phone serial> shell getprop ro.build.version.release  → 17   (SDK 37)
$ adb -s <daily phone serial> shell getprop ro.build.fingerprint
(value removed: personal device detail)
$ adb -s <daily phone serial> shell pm list packages | grep -i grapheneos
package:app.grapheneos.info  package:app.grapheneos.gmscompat.config  package:app.grapheneos.setupwizard
$ adb -s <daily phone serial> shell pm list packages | grep -i privamatic
package:com.techtrest.privamatic
$ adb -s <daily phone serial> shell dumpsys package com.techtrest.privamatic
    versionCode=9 minSdk=26 targetSdk=35   versionName=1.5.1
    installerPackageName=org.fdroid.fdroid   initiatingPackageName=org.fdroid.fdroid
    User 0: installed=true    (other users removed: personal device detail)
$ adb -s <daily phone serial> shell dumpsys package com.techtrest.privamatic.debug   → (empty; not installed)
$ adb -s <daily phone serial> shell pm list users
  (output removed: personal device detail; more than one user exists)
```
- The GrapheneOS fingerprint uses the stock `google/tokay` format. It is identified by its `app.grapheneos.*` packages
  and its numeric build id. `ro.grapheneos.release_version` is empty.
- The F-Droid release v1.5.1 (versionCode 9) is installed for user 0 only. The debug build is not installed.
- The phone has **more than one user**. Any install or adb command
  that leaves out `--user` can land in the wrong profile. CONVENTIONS already says to use `adb install --user 0`.

No emulator was running.

## 8. Tooling for a hook

```
$ jq --version          → jq-1.8.1           (/usr/bin/jq)
$ bash --version        → GNU bash 5.3.9(1)-release
$ gh --version          → gh 2.97.0 (2026-07-31)   (/usr/bin/gh)
$ gh auth status        (token line removed)
  ✓ Logged in to github.com account techtrest (keyring)
  - Git operations protocol: ssh
  - Token scopes: 'admin:public_key', 'gist', 'read:org', 'repo'
$ which adb             → /usr/bin/adb   (the system adb, not ~/Android/Sdk/platform-tools)
```

## 9. Tests

```
$ find app/src/test -name '*.kt' | wc -l          → 17 files (16 produce test suites)
$ grep -rh '@Test' app/src/test | wc -l            → 146
$ find app/src/androidTest -name '*.kt' | wc -l    → 3 (instrumented; not run)
$ JAVA_HOME=~/.local/share/jdk/temurin-21 ./gradlew :app:testDebugUnitTest --rerun
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 2s
$ (parse app/build/test-results/testDebugUnitTest/*.xml)
suites=16 tests=146 failures=0 errors=0 skipped=0 latest_timestamp=2026-10-20T18:26:37
```
- Without `--rerun`, the first run reported `testDebugUnitTest UP-TO-DATE` and ran nothing. A verify step
  needs `--rerun` (or `--rerun-tasks`) to be sure the tests actually ran.

---

## Blocked on me

- **Release keystore location.** CONVENTIONS.md says `~/privamatic-release.jks`, but it is not on this machine
  (searched `~` to depth 6). Where does it live, and is that rule in CONVENTIONS still current?
- **Backup `sharedpref` path suffix (§6).** Confirming it needs a device-side backup test, which goes beyond read-only.
  Decide whether to test it or simply add `.xml`.

## Changed

- Created branch `chore/workflow-inventory` from `master` (`08b42fc`).
- Added `docs/workflow/inventory.md` (this file) in one commit. Nothing pushed, nothing tagged.
- Side effects of running Gradle: `app/build/` outputs were refreshed (test results re-run; compile and package were
  up to date), and Gradle daemons were started for JDK 21 and JDK 25. The adb server was started
  (`daemon not running; starting now`). The untracked `docs/audits`, `docs/store` and `docs/store-graphics` were left untouched and uncommitted.

## Found (risks for the planned workflow)

1. **`.claude/settings.local.json` is tracked despite `.claude/` in `.gitignore`.** Any permission the workflow
   adds there gets committed and pushed. It already ships broad allows (`Bash(git:*)`, `Bash(adb:*)`,
   `sudo dnf install …`) that would let a guard hook be bypassed through permissions. Fix it with
   `git rm --cached .claude/settings.local.json`. Also decide whether the shared `.claude/settings.json`
   and `.claude/agents/` should be tracked: the current `.claude/` ignore rule would hide them too, so it needs
   `!.claude/settings.json` and `!.claude/agents/` style exceptions.
2. **The guard hook must not trust `Bash(git:*)` and `Bash(adb:*)`.** Those allows cover `git push`, `git tag`,
   `adb uninstall` and `adb shell pm clear`, which are exactly what the guard is meant to block.
3. **Duplicate rulebooks.** CONVENTIONS.md, .cursorrules (stale) and two identical docs files, QUICK_REFERENCE and
   POST_IMPLEMENTATION_CHECKLIST, all hold the rules. A new CLAUDE.md would make a fourth copy. Better to have
   CLAUDE.md point to CONVENTIONS.md than copy it.
4. **The system JDK is 25 and `JAVA_HOME` is unset.** A bare `./gradlew` fails (`What went wrong: 25.0.2`).
   The verify subagent and any hook must set `JAVA_HOME=~/.local/share/jdk/temurin-21`, not the `~/.gradle/jdks`
   cache. Also allow for the harmless "SDK XML version 4" warning.
5. **Gradle caching hides test runs.** `testDebugUnitTest` reports UP-TO-DATE and runs nothing unless `--rerun` is given.
6. **The release keystore is missing from this machine** while CONVENTIONS points at it. This does not affect F-Droid
   builds (`signingConfig = null`, no `signingConfigs` block), but any local `assembleRelease` signing step will fail here.
7. **Possible backup leak:** the `sharedpref` excludes for `ad_id_prefs` and `score_history` lack `.xml` (§6), so the
   files CONVENTIONS says must be excluded may still be backed up. Unverified.
8. **No Room migrations, and `exportSchema = false`.** The first schema change to `privacy_snapshots` will crash
   upgrades or wipe score history unless a migration is written. Worth a STATE.md or CLAUDE.md note.
9. **The Pixel 9 is a multi-user daily phone** with the F-Droid build from `org.fdroid.fdroid`. The guard hook
   should block every `adb -s <daily phone serial>` command except read-only ones (`getprop`, `pm list`, `dumpsys`),
   and should reject any adb call that doesn't name a device with `-s`. With two phones attached, a bare `adb` errors out
   at best, and at worst hits the wrong phone when only one is connected.
10. **The Pixel 8 has no F-Droid install.** The "compare against the F-Droid release" workflow in CONVENTIONS
   cannot run there until the release is installed. The debug package is present (v1.5.1, versionCode 9).
11. **The `gh` token has `repo` and `admin:public_key` scopes.** An unrestricted agent could push or open
    PRs. The guard should block `gh pr create`, `gh release` and `git push` unless I confirm.
12. Safe already: debug `applicationIdSuffix = ".debug"` is set, no keystore or `local.properties` is tracked or in history,
    and release `signingConfig = null` is F-Droid-compatible.
