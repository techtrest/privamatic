# **PRIVA**matic

> **Device Privacy Auditor for Android**

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**PRIVA**matic audits your Android device's privacy and security settings, giving you a clear 0–100 score with actionable steps to improve it. Built for GrapheneOS users and anyone who wants to understand what's exposing their data.

**Status:** In active development · Submitting to F-Droid · Contributions welcome

---

## Features

**Privacy Score (0–100)** — Comprehensive assessment across 45+ checks covering system security, network privacy, Google/Meta/Microsoft app detection, default apps, and AI assistants. Each check is weighted by real-world privacy impact.

**Three-Tab Interface** — Dashboard shows your score at a glance with tracking/security/actions summaries. Actions tab surfaces Quick Wins (fast fixes with step-by-step instructions and deep links to Settings) and Manual Checks (periodic reviews with progress tracking). Details tab breaks down every check by category with expand/collapse cards.

**Home Screen Widget** — Material You themed widget showing your privacy score, device info, and score change indicators. Supports opacity configuration and adapts to your wallpaper colors in both light and dark modes.

**Score Change Tracking** — Daily cumulative tracking with 48-hour expiry so you can see how your privacy posture improves over time.

**Manual Checks System** — Time-based reminders (60–120 day cycles) for privacy reviews that can't be automated: location permissions, camera/microphone access, and unused apps.

---

## Privacy Commitment

- **Minimal permissions** — `QUERY_ALL_PACKAGES` (app scanning), `ACCESS_NETWORK_STATE` + `ACCESS_WIFI_STATE` (network checks), `RECEIVE_BOOT_COMPLETED` (widget refresh after reboot)
- **Fully offline** — no network requests, no analytics, no crash reporting
- **No accounts** — no sign-in, no cloud sync
- **Local only** — all data stays on your device
- **Open source** — GPLv3, fully auditable

---

## Technical Stack

- **Kotlin 2.1** + **Jetpack Compose** (100% Compose UI)
- **Material Design 3** with British Racing Green theming
- **MVVM** with ViewModel + StateFlow
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
app/src/main/java/com/techtrest/privacywidget/
├── data/
│   ├── model/              # Data classes, enums, score history
│   ├── scanner/checks/     # Individual privacy check implementations
│   ├── maintenance/        # Manual checks with time-based tracking
│   ├── QuickWinsDetector.kt
│   └── ScoreHistoryRepository.kt
├── ui/
│   ├── components/         # ScoreCard, SummaryCard, IssueItem, etc.
│   ├── screens/            # Dashboard, Actions, Details screens
│   ├── navigation/         # Tab + swipe navigation state
│   └── theme/              # Material 3 theming
├── PrivacyWidgetProvider.kt    # Home screen widget
├── WidgetConfigurationActivity.kt  # Widget opacity settings
└── MainActivity.kt
```

---

## Roadmap

**v1.0 (2026)**
- Submit to F-Droid
- Score history trending

**Future**
- Export audit reports
- GrapheneOS-specific checks
- Localization support

**Distribution:** F-Droid and GitHub Releases. No Google Play Store — intentionally.

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

*Last Updated: 2026-03-01*
