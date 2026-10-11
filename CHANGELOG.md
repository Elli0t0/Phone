# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-10-11

### Added
- Project foundation structure with `io.github.elli0t0.phone` package namespace.
- Gradle build system with Version Catalog (`gradle/libs.versions.toml`) and single-source versioning (`VERSION_MAJOR`, `VERSION_MINOR`, `VERSION_PATCH`).
- Application entry point with `PhoneApplication` and manual DI `AppContainer`.
- Edge-to-edge `MainActivity` with Jetpack Compose Material 3 theme.
- Core design tokens inspired by Samsung One UI: `Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`, `Theme.kt`.
- Navigation structure with sealed `Screen` routes and `PhoneBottomBar` featuring Recents, Contacts, and Keypad tabs.
- Initial feature screens with MVVM architecture: `RecentsScreen`, `ContactsScreen`, `DialpadScreen` and their corresponding ViewModels and UiStates.
- Adaptive vector launcher icon with custom telephone handset foreground and One UI styling.
- Local unit test coverage verifying navigation destinations and initial ViewModel UiState.
- GitHub Actions CI workflow (`.github/workflows/android-build.yml`) for automated building, signing validation, and release artifact generation.
