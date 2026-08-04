# **PRIVA**matic

> **Device Privacy Auditor for Android**

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**PRIVA**matic audits your Android device's privacy and security settings, giving you a clear 0–100 score with actionable steps to improve it. Built for GrapheneOS users and anyone who wants to understand what's exposing their data.

**Status:** Available on F-Droid · Active development · Contributions welcome

---

## Download

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.techtrest.privamatic/)

---

## Features

**Privacy Score (0–100)** — Comprehensive assessment across 50+ checks covering system security, network privacy, Google/Meta/Microsoft app detection, default apps, and AI assistants. Each check is weighted by real-world privacy impact.

**Score Breakdown** — See exactly which checks are costing you points, sorted by impact, so you always know why your score is what it is.

**Score History** — Track your privacy score over time with a line chart and filter strip (1W/1M/3M/6M/1Y/All), so you can see whether you're actually improving.

**SDK Fingerprinting** — Scan your installed apps for known tracker SDKs, powered by the Exodus Privacy database. See exactly which apps are embedding ad networks, analytics, and profiling tools — purely informational, no score impact.

**Trusted Apps** — Whitelist FOSS apps that legitimately use accessibility, notification, or location permissions. Trusted apps are removed from your score penalty in real time. You decide what you trust — no distinction between F-Droid, Aurora, or Play Store installs.

**Four-Tab Details Interface** — Dashboard shows your score at a glance with tracking/security/actions summaries. Actions tab surfaces Quick Wins (fast fixes with step-by-step instructions and deep links to Settings) and Manual Checks (periodic reviews with progress tracking). Details tab splits into Checks, SDK, Trusted, and Breakdown — each check by category with expand/collapse cards.

**Home Screen Widget** — Material You themed widget showing your privacy score, device info, and score change indicators. Supports opacity configuration and adapts to your wallpaper colors in both light and dark modes.

**Score Change Tracking** — Daily cumulative tracking with 48-hour expiry so you can see how your privacy posture improves over time.

**Manual Checks System** — Time-based reminders (60–120 day cycles) for privacy reviews that can't be automated: location permissions, camera/microphone access, and unused apps.

**Smart VPN + DNS scoring** — When a VPN is active, the Private DNS check is treated as neutral since DNS is likely handled by the VPN tunnel. No unfair penalties for good setups.

**De-Googled aware** — microG is detected and not penalised, GrapheneOS sandboxed Play Services is recognised, and checks that don't apply to your setup are automatically skipped.

---

## Privacy Commitment

- **Minimal permissions** — `QUERY_ALL_PACKAGES` (app scanning), `ACCESS_NETWORK_STATE` + `ACCESS_WIFI_STATE` (network checks), `RECEIVE_BOOT_COMPLETED` (widget refresh after reboot)
- **Fully offline** — no network requests, no analytics, no crash reporting
- **No accounts** — no sign-in, no cloud sync
- **Local only** — all data stays on your device
- **Open source** — GPLv3, fully auditable

---

## Technical Stack

- **Kotlin 2.0.21** + **Jetpack Compose** (100% Compose UI)
- **Material Design 3** with British Racing Green theming
- **MVVM** with ViewModel + StateFlow
- **DataStore** for local whitelist persistence
- **Target SDK 35** (Android 15), minimum SDK 26 (Android 8.0)
- Widget uses direct system colors (`@android:color/system_*`) for proper Material You integration

See [CONVENTIONS.md](CONVENTIONS.md) for coding standards.

---

## Building

```bash
git clone https://github.com/techtrest/privamatic.git
cd privamatic
./gradlew assembleDebug
./gradlew installDebug
```

Requires Android Studio Ladybug+, JDK 11+, and Android SDK 35. No API keys or configuration needed.

---

## Project Structure

```
app/src/main/java/com/techtrest/privamatic/
├── data/
│   ├── model/              # Data classes, enums, score history
│   ├── scanner/
│   │   └── checks/         # Individual privacy check implementations
│   ├── maintenance/        # Manual checks with time-based tracking
│   ├── util/               # PackageManagerUtil and shared helpers
│   ├── TrustedAppsRepository.kt
│   ├── TrustedAppsAdjuster.kt
│   ├── QuickWinsDetector.kt
│   └── ScoreHistoryRepository.kt
├── ui/
│   ├── components/         # ScoreCard, SummaryCard, IssueItem, etc.
│   ├── screens/            # Dashboard, Actions, Details screens
│   ├── navigation/         # Tab + swipe navigation state
│   └── theme/              # Material 3 theming
├── PrivacyWidgetProvider.kt        # Home screen widget
├── WidgetConfigurationActivity.kt  # Widget opacity settings
└── MainActivity.kt
```

---

## Roadmap

**v1.0 (2026)** ✅
- F-Droid release

**v1.1 (2026)** ✅
- Trusted Apps — whitelist FOSS apps to remove unfair penalties
- New app icon and feature graphic
- VPN + Private DNS scoring fix
- Widget score synced with trusted apps whitelist

**v1.2 (2026)** ✅
- Security patch date check — flags outdated and end-of-life devices
- microG correctly detected and not penalised
- Ad ID check auto-passes on GMS-free devices (GrapheneOS, LineageOS)
- Wi-Fi scanning explanation updated to reflect real-world retail tracking
- Privacy-friendly app allowlists updated (Thunderbird, Kvaesitso added; Niagara removed)
- Full i18n groundwork — all strings extracted to resources

**v1.3 (2026)** ✅
- Score History — daily score tracking with line chart and filter strip
- Score Breakdown tab — see exactly which checks cost you points
- SDK Fingerprinting — detect known tracker SDKs, powered by Exodus Privacy
- Outdated App check — flags apps targeting Android 5.1 or older
- Tiered security patch scoring
- Biometric check now notes the PIN advantage for high-risk users
- Trust and score-adjustment fixes, Private DNS false-positive fix, backup exclusions

**v1.4 (2026)** ✅
- Fixed trust incorrectly hiding tracker SDKs from the SDK tab
- SDK tab scroll performance and layout fixes
- microG no longer blocked from being marked as trusted
- Disabled Google Play Services now correctly detected
- WhatsApp Business, Instagram Lite, and Facebook Lite now detected
- "Next tip" and tip variety fixes

**Planned**
- Translations: DE, PL, FR, ES, IT
- "No internet permission" trust badge
- Network permission awareness (GrapheneOS/LineageOS firewall detection)
- Score card sharing
- Home screen widget discoverability prompt
- "Sensitive Access" checker category (notification listener, accessibility, device admin, usage stats)

**Distribution:** F-Droid only. No Google Play Store — intentionally.

---

## Contributing

Contributions welcome. Read [CONVENTIONS.md](CONVENTIONS.md) first — standards are strictly enforced.

- Report bugs or suggest features via [GitHub Issues](https://github.com/techtrest/privamatic/issues)
- Fork → feature branch → follow conventions → PR
- All PRs reviewed for privacy implications

---

## License

[GNU General Public License v3.0](LICENSE) — all derivative works must remain open source.

---

## Acknowledgments

[GrapheneOS](https://grapheneos.org/) · [Material Design 3](https://m3.material.io/) · [Jetpack Compose](https://developer.android.com/jetpack/compose) · [F-Droid](https://f-droid.org/)

---

*Last Updated: 2026-08-04*
