# Privamatic — Development Conventions

## Project Identity
- **Package**: `com.techtrest.privamatic` (NOT `com.techtrest.privacywidget` — grep before prompting)
- **Source root**: `app/src/main/java/com/techtrest/privamatic/`
- **Project path**: `~/Local App Projects/Privamatic` — always quote in shell commands (spaces)
- **Build command (MiniPC)**: `JAVA_HOME=/opt/android-studio/jbr ./gradlew compileDebugKotlin`
- **Build command (Framework laptop)**: `JAVA_HOME=/home/techtrest/.local/share/android-studio/jbr ./gradlew compileDebugKotlin`
  — system JDK 25 breaks Kotlin parser on both machines; never omit JAVA_HOME prefix
- **Default branch**: `master` (not `main`)

## File Structure
app/src/main/java/com/techtrest/privamatic/

├── data/

│   ├── model/              # Data classes (PrivacyScore, PrivacyIssue, PrivacyCheck)

│   ├── scanner/

│   │   └── checks/         # Individual checker classes

│   ├── util/               # PackageManagerUtil and shared helpers

│   └── QuickWinsDetector.kt

├── ui/

│   ├── components/

│   ├── screens/

│   ├── navigation/         # AppNavigationState

│   └── theme/

└── MainActivity.kt

## Architecture Rules

**No DI framework** — no Hilt. Shared components are object singletons passed explicitly:
- `TrustedAppsAdjuster` — singleton shared between `PrivacyViewModel` and `PrivacyWidgetProvider`
- `QuickWinsDetector` — singleton
- Never instantiate these per-caller — widget and ViewModel must produce identical scores

**Widget runs independently** — `PrivacyWidgetProvider` runs its own `PrivacyScanner` scan.
It has no access to ViewModel StateFlow. The scan → TrustedAppsRepository → TrustedAppsAdjuster
sequence must mirror ViewModel logic exactly.

**`getTotalPoints()` rule** — must read directly from DataStore, never delegate to a flow
that applies UI visibility filters. Visibility filters silently drop completed-but-not-overdue
items, losing those points.

**Shared utilities** — package helpers (`getAppName`, `isSystemApp`) belong in
`data/util/PackageManagerUtil.kt`. Never duplicate across Checker files.
`isSystemApp` must default to `true` on errors (conservative for security).

**`PrivacyCheck` enum is single source of truth** — all scoring logic lives in the enum.
ViewModel and widget both derive from the same enum values. No dead code drift.

**Score Breakdown tab** — outer Details-level tab (peer to Checks and Apps), not a sub-tab.
Filter: `pointDeduction > 0`. Sorted descending by deduction.

**`effectivelySecure`** — `issue.isSecure || allPackagesTrusted` — pure UI derivation,
no data model changes. Category header counts use the same logic.

## Strings & Localisation

All user-facing strings in `strings.xml`. Never hardcode English text in Kotlin or XML.

**Enum strings** — use `@StringRes Int` fields, never raw string literals.
In Composables: `stringResource(rating.displayNameRes)`
In non-Composable (widget, checker): `context.getString(rating.displayNameRes)`

**Widget strings** — `RemoteViews` cannot use `stringResource()`. Always use
`context.getString()` in `PrivacyWidgetProvider`.

**Naming convention:**
- UI labels: `label_<screen>_<element>`
- UI copy: `copy_<screen>_<element>`
- Enum display names: `<enum>_<entry>_name`
- Plurals: `plural_<element>` using `<plurals>` tag
- Format strings: `fmt_<element>`

**Do NOT extract to strings.xml:** package names, log tags, DataStore keys,
OS brand detection strings (GrapheneOS, CalyxOS), format placeholders.

## Security & Privacy Rules

**Zero network permissions** — no library that makes network calls may be added.

**No third-party SDKs** — no Firebase, Crashlytics, analytics. Ever.

**No GMS dependency** — removed pre-F-Droid. Never re-add `com.google.android.gms`.

**Debug logging** — all `Log.*` calls wrapped in `if (BuildConfig.DEBUG)`.
Sensitive data (package names, device fingerprints) never in logs, even debug.

**Android Auto Backup exclusions** — exclude from cloud backup:
- `ad_id_prefs` (SharedPreferences)
- `maintenance_prefs` (DataStore)
Declared in both `data_extraction_rules.xml` and `backup_rules.xml`.

**GMS detection pattern** — use `MATCH_SYSTEM_ONLY` to distinguish stock GMS
from sandboxed Play on GrapheneOS.
microG detection requires all three companion packages:
- `org.microg.gms.self` (most reliable)
- `org.microg.gms.droidguard` (optional SafetyNet)
- `org.microg.nlp` (older builds)
Checking a single package is insufficient.

## Build & Release Rules

**F-Droid requirements:**
- `signingConfig = null` in release build type — remove entire `signingConfigs` block
- Never use `jvmToolchain` — use `kotlinOptions { jvmTarget = "11" }`
- Never add `foojay-resolver-convention` or `gradle-daemon-jvm.properties`
- `isMinifyEnabled = true` and `isShrinkResources = true` on release builds
- All response/model DTOs explicitly listed in `proguard-rules.pro`
  (R8 silently breaks Gson/Retrofit in release; symptoms never appear in debug)
- Test `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleRelease` before every tag

**Versioning:**
- Always bump `versionCode` before tagging — F-Droid uses versionCode to detect releases
- A tag without a versionCode bump is silently skipped by F-Droid
- Never tag before bumping versionCode

**Keystore:**
- Release keystore at `~/privamatic-release.jks`
- Passwords via env vars `PRIVAMATIC_STORE_PASSWORD` / `PRIVAMATIC_KEY_PASSWORD`

**Git discipline:**
- Feature branches always — never commit directly to `master`
- `git diff → git status → git add → git commit` — always in this order
- Prefixes: `feat:` / `fix:` / `chore:` / `release:`
- Never commit `.env`, secrets, or the release keystore
- Default branch is `master` — push to `git push origin master`

**F-Droid fastlane metadata:**
fastlane/metadata/android/en-US/

├── changelogs/<versionCode>.txt   # e.g. 3.txt for versionCode 3

└── images/phoneScreenshots/       # exact name — wrong = silent failure
Locale folder: `en-US` (hyphen not underscore).

## Design Constraints
- `MaterialTheme.typography` only — no arbitrary `fontSize`
- `MaterialTheme.colorScheme` only — no hardcoded hex colors
- Widget ARGB colors as named constants, never inline hex
- Material Design spacing: 4dp, 8dp, 12dp, 16dp, 24dp, 32dp — no arbitrary values
- Corner radius: 8dp small, 12dp standard
- `Icons.Default.*` only — no emoji in production UI

*Last updated: 2026-06-18*
