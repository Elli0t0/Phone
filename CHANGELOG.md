# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.21] - 2026-10-13

### Changed
- Settings: the "Permissions & setup" card is hidden once the contacts, call log, make calls, and phone state permissions plus the default dialer role are all granted. It reappears automatically if any of them is missing or revoked (re-checked on resume).

## [0.1.20] - 2026-10-13

### Fixed
- Real build failure: ContactsViewModel combined 10 flows in one combine() call, but kotlinx-coroutines 1.8.1 only provides combine() overloads for up to 5 flows, so the call fell back to the vararg overload and failed type checking. Split into two 5-flow combines grouped into data classes, then one 2-flow combine. No behavior change.
- Note: 0.1.19 did not fix the build; its two changes (runCatching to try/catch, ifBlank to takeIf) were for code that already compiled and are kept as-is.

## [0.1.19] - 2026-10-13

### Fixed
- Build failure: mergeContacts called suspend functions (updateContact, deleteContact) inside runCatching, whose block is not suspend. Replaced with try/catch.
- Build failure: recents contact lookup used ifBlank { null }, which is a type error (ifBlank must return a non-null String). Replaced with takeIf.

## [0.1.18] - 2026-10-13

### Added
- Number matching: one shared PhoneNumberMatcher (device-region normalization + trailing-digit match) used consistently in keypad search, contacts search, recents search and lookup, and the call screen.
- Duplicate contacts: detected by same normalized number or same name, listed from the contacts tab, merged only after confirmation. Merge keeps the first contact, moves the others' numbers and emails into it, then deletes the merged contacts.
- R8 minification + resource shrinking for release builds, with keep rules for manifest components, Room, DataStore, Coil, and reflection-safe members. Release resources kept for en and fa locales only.
- R8 mapping.txt uploaded as a separate workflow artifact (Phone-mapping), not attached to the public Release.

### Changed
- README rewritten: current features, full permissions list, install steps, and limitations.
- Removed unused dependencies: compose ui-tooling-preview, Room testing.

## [0.1.17] - 2026-10-13

### Fixed
- Build failure: SimCallResolution is a top-level type, not nested under SimCallHelper. Fixed the references in contact details, keypad, and recents. Added the missing layout width import in the in-call audio route picker.

## [0.1.16] - 2026-10-13

### Added
- Per-contact calling SIM in contact details: Ask every time (default) or a specific SIM, saved in Room (contact_sims, Migration 1 to 2). Calls to that contact use it without the dialog from keypad, recents, and contacts. Falls back to the dialog if the SIM is gone. Hidden on single-SIM phones.
- In-call audio route picker: Earpiece, Speaker, Wired headset, and each Bluetooth device by name, from the real CallAudioState supported routes. Replaces the earpiece/speaker toggle so Bluetooth can be returned to.

### Fixed
- SIM dialog: rows at least 64dp, clear spacing, whole row clickable.

### Changed
- BLUETOOTH_CONNECT is requested only when the audio route picker needs Bluetooth device names, not at call start.

## [0.1.15] - 2026-10-13

### Fixed
- Incoming calls not popping up: always set full-screen intent on incoming HIGH notification (system shows heads-up when unlocked, full-screen when locked). Gating it to locked removed the heads-up pop. Post the incoming notification once via startForeground; a second notify update suppresses heads-up.
- Outgoing HIGH notification stuck on screen: when CallActivity becomes visible, replace the foreground notification with LOW ongoing via startForeground(ONGOING). Cancel alone does not update the foreground notification, so HIGH stayed over the call screen and home screen.
- Channels: createNotificationChannel does not update importance of an existing channel. Delete and recreate when importance mismatches, so HIGH channels actually trigger heads-up.

## [0.1.14] - 2026-10-13

### Fixed
- Call UI not appearing in background: InCallService now posts a foreground notification synchronously in onCallAdded (channels first, before lookups), with startForeground + notify fallback, and logs failures with tag PhoneCall instead of swallowing them.
- Outgoing calls: both startActivity (FLAG_ACTIVITY_NEW_TASK) and call notification with full-screen intent run in onCallAdded and on first state change.
- Incoming ringing: HIGH heads-up notification with Answer/Decline, full-screen intent only when device is locked.
- After call screen visible: only quiet LOW ongoing notification, no heads-up, no full-screen intent.
- Manifest: FOREGROUND_SERVICE + FOREGROUND_SERVICE_PHONE_CALL, service foregroundServiceType phoneCall, CallActivity launchMode singleTask + excludeFromRecents false.
- Default dialer: ROLE_DIALER state logged in service onCreate/onCallAdded and in CallActivity onCreate; service not bound when not default dialer is now visible in logcat.

## [0.1.13] - 2026-10-13

### Added
- First-run permissions setup: "Set up Phone" screen on first launch and whenever something is missing, with one card per item (contacts, call history, make calls, phone state/SIMs, notifications, Bluetooth, full-screen notifications, default phone app, battery), check mark when granted, app settings button if permanently denied, re-check on resume, and "Skip for now".
- Same permissions checklist in Settings ("Permissions & setup").
- Notification channels created at app start (call start HIGH, ongoing LOW, incoming HIGH).
- Warning card on Keypad/Recents when notifications or full-screen intent are blocked, with a button to the right setting.
- Manifest permissions: READ_PHONE_STATE, BLUETOOTH_CONNECT.

## [0.1.12] - 2026-10-13

### Fixed
- Outgoing call start: Post HIGH importance call start notification with full-screen intent alongside activity start, transitioning to LOW ongoing notification once in-call activity becomes visible.

## [0.1.11] - 2026-10-13

### Fixed
- Ongoing call notification: Switched to low-importance non-alerting channel without full-screen intent, deleted obsolete channel, and cancelled incoming notification upon answer.

## [0.1.10] - 2026-10-13

### Fixed
- Outgoing calls: Immediately start the in-call activity on NEW, SELECT_PHONE_ACCOUNT, CONNECTING, and DIALING states in onCallAdded and onStateChanged.
- Ongoing call notification: Post NotificationCompat.CallStyle.forOngoingCall with Hang up action, CATEGORY_CALL, and fullScreenIntent fallback; cancel when call ends.

## [0.1.9] - 2026-10-13

### Added
- Appearance settings: Theme selector with Light, Dark, and System (default System) persisted in DataStore and applied across the app.

### Changed
- Incoming calls: Post high-importance heads-up call notification via NotificationCompat.CallStyle with Answer and Decline actions and full-screen intent instead of opening call activity directly.

## [0.1.8] - 2026-10-13

### Fixed
- In-call and incoming screens: Resolved contact name asynchronously via PhoneLookup with normalized number and Call.Details fallback, displaying name as title and number as subtitle in UI and notification.
- Handset icons: Replaced call vectors with shared Material Symbols handset path and updated launcher foreground inside centered group.

## [0.1.7] - 2026-10-13

### Fixed
- Recents performance: Preload call log on app launch, stream first 50 rows immediately, batch PhoneLookup, and show loading skeleton instead of empty state.
- Edit/New Contact: Added avatar with camera picker/crop/remove and per-contact ringtone picker, staged until saved.
- Edit Contact layout: Widened phone/email type dropdowns to prevent wrapping at 1.3 font scale and small screens.
- Call handset icons: Replaced distorted vectors with official Material Symbols and centered adaptive launcher icon in 66dp safe zone.

### Added
- Appearance settings stored in DataStore: 8 accent color presets + custom, dynamic color toggle, pure black dark mode, list density, card styles, font scaling, animation toggles, bottom bar labels toggle, start tab, contact sort/format, and reset to defaults.

## [0.1.6] - 2026-10-13

### Added
- System photo picker integration with square cropping and preview dialog for contact avatars.
- Coil image loading integration with safe fallback to initials avatar.
- Redesigned contact details screen featuring large avatar, action row (Call, Message, Favorite, More), and One UI aesthetic.
- Per-contact custom ringtone selection via system RingtonePicker with default and silent options.

## [0.1.5] - 2026-10-13

### Fixed
- Recents: Re-check READ_CALL_LOG permission on resume and after grant, reloading call log flow dynamically.
- Recents: Query CallLog on API 30+ using ContentResolver Bundle query arguments (limit 500, date DESC) instead of SQL LIMIT in sortOrder.
- Recents: Caught and logged query/lookup exceptions, displaying an error state instead of a false empty state.
- Recents: Changed search placeholder to "Search calls".

### Added
- Default phone app integration with RoleManager ROLE_DIALER in Settings screen.
- PhoneInCallService handling telecom calls with BIND_INCALL_SERVICE permission.
- Incoming call screen with full-screen notification over lock screen and answer/decline controls.
- Ongoing call screen with timer, mute, audio routing (Speaker, Earpiece, Bluetooth), call hold, and DTMF keypad.
- Multi-SIM support with dual SIM account picker dialog.

## [0.1.4] - 2026-10-13

### Fixed
- dial pad backspace hold (continuous vibration, delayed deletion)

### Added
- READ_CALL_LOG and WRITE_CALL_LOG permissions management with full state UI.
- CallLogRepository with ContentObserver Flow querying CallLog.Calls on Dispatchers.IO.
- Consecutive call grouping on the same day with count and day headers (Today, Yesterday, date).
- Recents row UI with call type indicators, tap-to-expand details, call-back, and long-press action menu.
- Contact resolution via ContactsContract.PhoneLookup with in-memory caching.
- Search filter chips: All, Missed, Incoming, Outgoing.
- Favorites local persistence powered by Room database with FavoritesDao and toggle in contact details.

## [0.1.3] - 2026-10-12

### Added
- One UI inspired interactive Dial Pad with circular digit keys (`1-9`, `*`, `0`, `#`) and letter sublabels.
- Real-time search-as-you-type above the keypad with 150 ms debounce, matching by number substring and T9-style ITU E.164 name mapping.
- T9 search result cards showing contact name and primary number with tap-to-populate and direct quick-call button.
- Raw dial string state management in `DialpadViewModel` decoupled from presentation formatting.
- `PhoneNumberFormatter` using Android `PhoneNumberUtils` with local formatting fallbacks (including Iranian `0912...`, US, and international `+` numbers) and strict dialpad sanitization on paste.
- Accelerated backspace deletion on long-press with repeating interval reduction, ensuring single-digit synchronous deletion without skipping.
- Long-press on `0` key inserts international prefix `+`.
- Full telephone call dispatching via `CallManager`:
  - Direct call placement with `TelecomManager.placeCall` when `CALL_PHONE` permission is granted (with immediate `ACTION_CALL` fallback).
  - Graceful zero-permission fallback to system dialer via `ACTION_DIAL` when `CALL_PHONE` permission is denied.
  - Safe USSD code support (`*#...`) via `Uri.encode` of `#` to `%23` to prevent URL fragment truncation by Android Uri parser.
  - Call button automatically disabled when input is empty.
- System haptic feedback integration respecting `Settings.System.HAPTIC_FEEDBACK_ENABLED`.
- Keypad state preservation across configuration changes and device rotations via `SavedStateHandle`.
- Unit test suite for phone number formatting, USSD preservation, paste sanitization, T9 matching, and ViewModel state transitions.

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
