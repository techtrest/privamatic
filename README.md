# Privacy Guard

> **Device Privacy Auditor for Android**

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Privacy](https://img.shields.io/badge/Privacy-First-green)](https://github.com/techtrest/PrivacyWidget)

Privacy Guard is an Android app that audits your device's privacy and security settings, providing a comprehensive 0-100 privacy score with actionable recommendations. Understand what's exposing your data and get step-by-step guidance to lock down your device.

**🚧 Status:** In active development • Not yet published • Contributions welcome

---

## 🛡️ Privacy Philosophy

Privacy Guard is built on principles inspired by [GrapheneOS](https://grapheneos.org/) and the privacy-first movement:

- **Transparency:** All checks are clearly explained with severity ratings
- **No Invasive Permissions:** We only request permissions absolutely necessary for auditing
- **Local-First:** All scanning and analysis happens on your device
- **Open Source:** Fully auditable code with no hidden behavior
- **No Tracking:** Zero analytics, telemetry, or data collection
- **Minimal Attack Surface:** Clean architecture with minimal dependencies

The app helps users understand the privacy implications of their device configuration without requiring technical knowledge. Every recommendation includes context about *why* it matters and *how* to fix it.

---

## ✨ Key Features

### Privacy Auditing
- **Comprehensive Privacy Score (0-100)** based on 20+ checks across multiple categories
- **System Security:** Screen lock, encryption, biometric auth, USB debugging, developer options
- **Network & Tracking Privacy:** VPN status, private DNS, advertising ID, WiFi scanning
- **Google Services:** Find My Device, Play Services integration detection
- **Default Apps:** Browser, SMS, phone, assistant privacy analysis
- **AI & Assistant Privacy:** Voice assistant, keyboard, digital wellbeing audits

### Smart Recommendations
- **Quick Wins Detection:** Identifies high-impact, low-effort privacy improvements
- **Severity Ratings:** Each issue weighted by actual privacy impact (not just checkbox counting)
- **Category Breakdown:** Detailed issue lists organized by privacy domain
- **Manual Check Guidance:** Step-by-step instructions for settings that require manual verification

### Modern UX
- **Material Design 3:** Clean, accessible interface with British Racing Green primary color
- **Edge-to-Edge Display:** Modern Android UI with proper insets handling
- **Pull-to-Refresh:** Instant rescanning with smooth animations
- **Dark Mode:** Full Material You theming support (coming soon)

---

## 📸 Screenshots

> **Coming Soon:** Screenshots will be added once the UI is finalized.

Features to be showcased:
- Dashboard with privacy score and category summaries
- Detailed issue breakdown by category
- Quick wins recommendations
- Manual check instructions
- Scoring system transparency

---

## 🛠️ Technical Stack

### Core Technologies
- **Language:** Kotlin 2.1
- **UI Framework:** Jetpack Compose (100% Compose UI)
- **Architecture:** MVVM with ViewModel + StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)

### Key Dependencies
- **Compose BOM:** Latest stable Compose libraries
- **Material 3:** Official Material Design 3 components
- **Coroutines:** Asynchronous scanning and state management
- **ViewModel + Lifecycle:** State preservation and lifecycle awareness
- **Biometric:** Device authentication detection
- **Play Services (ads-identifier):** Advertising ID detection only

### Architecture Highlights
- **Separation of Concerns:** UI layer (Compose) → Business logic (Scanners) → Data models
- **Stateless Composables:** All UI components are stateless with hoisted state
- **Extension Functions:** Type-specific logic encapsulated as Kotlin extensions
- **No Business Logic in UI:** All calculations delegated to calculator/scanner objects
- **Immutable Data Models:** Thread-safe data classes with `val` properties

See [CONVENTIONS.md](CONVENTIONS.md) for detailed coding standards.

---

## 🔨 Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or later
- JDK 11 or later
- Android SDK 35
- Gradle 8.9+ (included via wrapper)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/techtrest/PrivacyWidget.git
cd PrivacyWidget

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Project Structure

```
app/src/main/java/com/techtrest/privacywidget/
├── data/
│   ├── model/          # Data classes (PrivacyScore, PrivacyIssue, etc.)
│   ├── scanner/        # Privacy detection logic
│   └── QuickWinsDetector.kt
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Full screen composables
│   ├── navigation/     # Navigation state management
│   └── theme/          # Material Design 3 theming
└── MainActivity.kt
```

### Configuration

The app requires no configuration files or API keys. All functionality works offline.

---

## 🗺️ Roadmap

### Version 1.0 (Q2 2026)
- [ ] Complete all 20+ privacy checks
- [ ] Finalize Quick Wins detection algorithm
- [ ] Add dark mode support
- [ ] Implement persistent score history
- [ ] Write comprehensive user documentation
- [ ] Submit to F-Droid repository

### Future Features
- [ ] Privacy score trending over time
- [ ] Export audit reports (PDF/JSON)
- [ ] Custom check weighting/priorities
- [ ] GrapheneOS-specific checks (privileged extensions, sandboxed Play Services)
- [ ] Privacy comparison with device averages (anonymized, opt-in)
- [ ] Localization (i18n) support

### Distribution
- **F-Droid:** Primary distribution channel (planned)
- **GitHub Releases:** APKs available for direct download
- **Google Play Store:** Not planned (conflicts with privacy philosophy)

---

## 🔒 Privacy Commitment

### What We Don't Do
- ❌ **No tracking or analytics** (not even privacy-preserving analytics)
- ❌ **No network requests** (100% offline functionality)
- ❌ **No crash reporting** (no Crashlytics, Sentry, etc.)
- ❌ **No ads or monetization**
- ❌ **No user accounts** (no sign-in, no cloud sync)
- ❌ **No hidden permissions** (manifest is fully transparent)

### What We Do
- ✅ **Open source code** (GPLv3 license - all changes must remain open)
- ✅ **Minimal permissions** (only what's necessary for auditing)
- ✅ **Local-only storage** (all data stays on your device)
- ✅ **Transparent methodology** (scoring system fully documented)
- ✅ **Regular security audits** (community review encouraged)

### Permissions Used
The app requests **zero runtime permissions**. All privacy checks use publicly accessible Android APIs:

- `READ_PHONE_STATE` - ❌ Not requested (relies on documented API limitations)
- `INTERNET` - ❌ Not included (app is fully offline)
- `ACCESS_NETWORK_STATE` - ✅ Used to detect VPN connections
- Other permissions - ❌ Not requested

See [AndroidManifest.xml](app/src/main/AndroidManifest.xml) for the complete permission list.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

**Why GPL-3.0?**
We chose GPL-3.0 to ensure that Privacy Guard and all derivative works remain open source and transparent. Privacy-focused tools should never become closed-source.

### Implications
- ✅ Free to use, modify, and distribute
- ✅ Commercial use permitted
- ⚠️ Modified versions must also be open source (copyleft)
- ⚠️ Changes must be documented

---

## 🤝 Contributing

We welcome contributions! Privacy Guard is built by privacy advocates, for privacy advocates.

### Ways to Contribute

**🐛 Report Issues**
- Found a bug? [Open an issue](https://github.com/techtrest/PrivacyWidget/issues)
- Privacy check giving incorrect results? Let us know with device details

**💡 Suggest Features**
- Have ideas for new privacy checks? Share them!
- UX improvements? We're all ears

**🔧 Submit Code**
- Fork the repository
- Create a feature branch (`git checkout -b feature/amazing-privacy-check`)
- Follow [CONVENTIONS.md](CONVENTIONS.md) for code style
- Write clear commit messages (see existing commits for examples)
- Submit a Pull Request with a description of your changes

**📚 Improve Documentation**
- Clarify privacy check descriptions
- Add translations (future)
- Write guides for privacy hardening

### Development Guidelines
- **Read [CONVENTIONS.md](CONVENTIONS.md)** - Our coding standards are strictly enforced
- **No business logic in Composables** - Keep UI and logic separate
- **Test your changes** - Run `./gradlew test` before submitting
- **Material Design 3** - Follow Material guidelines for UI components
- **Privacy-first** - Never add tracking, analytics, or unnecessary permissions

### Code Review Process
1. All PRs reviewed by maintainers
2. Automated checks: build, lint, tests
3. Manual review for privacy implications
4. Merge once approved (squash merge preferred)

---

## 🙏 Acknowledgments

- **GrapheneOS** - Inspiration for privacy-first mobile computing
- **Material Design 3** - Design system and component library
- **Jetpack Compose** - Modern Android UI toolkit
- **F-Droid** - Champion of open source Android apps

---

## 📞 Contact

- **Issues:** [GitHub Issues](https://github.com/techtrest/PrivacyWidget/issues)
- **Discussions:** [GitHub Discussions](https://github.com/techtrest/PrivacyWidget/discussions)
- **Security:** Report vulnerabilities via GitHub Security Advisories (private disclosure)

---

**Built with 🛡️ by privacy advocates, for everyone who values their digital privacy.**

*Last Updated: 2026-02-01*
