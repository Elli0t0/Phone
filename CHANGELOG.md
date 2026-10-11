# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.2] - 2026-10-11

### Added
- Real device contacts management via `ContactsContract` and `ContentResolver` on `Dispatchers.IO`.
- `ContactsRepository` with reactive `ContentObserver` callbacks via `callbackFlow` for auto-refreshing contact list and details.
- Clean MVVM architecture for Contacts feature (`ContactsViewModel`, `ContactDetailViewModel`, `ContactEditViewModel`).
- Full Contacts UI inspired by Samsung One UI:
  - Collapsing header with dynamic contact count display.
  - Search bar supporting case-insensitive name matching and digits-insensitive phone number matching (e.g. matching local `0912` against international numbers).
  - Alphabetically sorted contact list with section headers, grouping non-letters and symbols under `#` and safely supporting Unicode, Persian, and Arabic names.
  - Contact Detail screen displaying all phone numbers, email addresses, and organization details with label types.
  - Create and Edit Contact screen supporting dynamic phone and email row additions, label type selection, and validation.
  - Delete contact confirmation dialog.
- In-place differential updates for contact editing: updates existing `Data` rows by `_ID`, inserts new entries, and deletes only removed items without recreating contact records or losing unedited data kinds (photos, custom ringtones).
- Account-aware raw contact selection (`findWritableRawContactId`) for aggregated contacts.
- Reusable Compose permissions state helper (`rememberContactsPermissionState`) managing `READ_CONTACTS` and `WRITE_CONTACTS` states (NotAsked, Denied, PermanentlyDenied with Settings button, Granted) with lifecycle resume re-checks.
- Local unit test coverage for `ContactSearchMatcher` digits-insensitive logic and `ContactGroupingUtil` section headers and sorting.

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
