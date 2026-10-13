# Phone

An open-source Android dialer and contacts application with a clean, modern visual style. Built purely in Kotlin using modern Jetpack Compose.

## Project Status

**Version**: `0.1.18`

See [CHANGELOG.md](CHANGELOG.md) for version history.

## Features

- **Keypad**: Dialer with circular keys (`1-9`, `*`, `0`, `#`) and letter sublabels, T9 search-as-you-type with 150 ms debounce, tap-to-dial and direct call buttons, non-mutating display formatting, long-press `0` for `+`, accelerated repeating backspace, call placement via `TelecomManager.placeCall` with `ACTION_DIAL` fallback, safe USSD (`*#`) encoding, and haptics.
- **Recents**: Call log with consecutive-call grouping, day sections (Today, Yesterday, date), filters (All, Missed, Incoming, Outgoing), search, favorites, tap-to-expand details, call-back, and delete. Contact names are resolved via `PhoneLookup` with an in-memory cache.
- **Contacts**: Real device contacts management via `ContactsContract` (create, edit, delete, differential in-place updates), contact details with photos, custom ringtones, and favorites. Duplicate contacts are detected by same normalized number or same name, listed, and merged only after confirmation — nothing is deleted without confirmation.
- **Calling**: Default dialer integration (`ROLE_DIALER`) with `InCallService`, incoming call screen with full-screen notification over the lock screen, ongoing call screen with timer, mute, hold, DTMF keypad, and an audio route picker (Earpiece, Speaker, Wired headset, each Bluetooth device by name).
- **Dual SIM**: SIM picker dialog, per-contact calling SIM (Ask every time or a fixed SIM, saved in Room), and a calling-SIM setting. Saved SIMs are used from the keypad, recents, and contact details, with a dialog fallback if the SIM is gone.
- **Number matching**: One shared matcher everywhere (keypad search, contacts search, recents search and lookup, call screen). Numbers are normalized with the device region (trunk `0` becomes the country calling code, `00` becomes `+`), and numbers sharing the last 10 digits match.
- **Appearance**: Theme (Light, Dark, System), accent colors (presets + custom), dynamic color, pure-black dark mode, font scaling, list density, card styles, animation toggles, bottom-bar labels, start tab, contact sort order and name format, and reset to defaults.
- **Setup**: First-run "Set up Phone" checklist (contacts, call history, calls, phone state, notifications, Bluetooth, full-screen intent, default phone app, battery), also available in Settings.

## Minimum Android Version

- **Minimum SDK**: Android 10 (API level 29)
- **Target SDK**: Android 15 (API level 35)
- **Compile SDK**: Android 15 (API level 35)

## Architecture & Technology Stack

- **UI**: Jetpack Compose, Material 3 with custom theming
- **Language**: Kotlin 2.2.x (100% Kotlin)
- **Architecture**: MVVM + Repository pattern with Unidirectional Data Flow (`StateFlow`)
- **Dependency Injection**: Manual dependency injection via `AppContainer` in `PhoneApplication`
- **Navigation**: `androidx.navigation.compose` with sealed `Screen` classes
- **Contacts Data Layer**: `ContactsContract` ContentProvider with batch `ContentProviderOperation`
- **Local Storage**: Room (favorites, per-contact SIM) and DataStore Preferences (settings, setup state)
- **Images**: Coil (contact photos)
- **Release builds**: R8 minification (`minifyEnabled`) + resource shrinking (`shrinkResources`), resources kept for `en` and `fa` locales only

## Install

1. Download the latest release from [GitHub Releases](https://github.com/Elli0t0/Phone/releases) (marked as pre-release for `0.x.x`): `Phone-<version>.apk` and its checksum `Phone-<version>.apk.sha256`.
2. Verify the checksum:
   ```bash
   sha256sum -c Phone-<version>.apk.sha256
   ```
3. Install the APK on your device (allow "install from this source" when asked). Every release installs over the previous version: same application ID and signing certificate, increasing `versionCode`.
4. Open the app and complete **Set up Phone**: grant the permissions and set Phone as the **default phone app** so incoming and ongoing calls open in Phone's call UI.
5. Workflow artifacts (from the **Android Build** run) also contain the release APK (`Phone-0.1.0`) and the R8 mapping file (`Phone-mapping`, `mapping.txt`, for debugging only — it is not part of the public Release).

## Build Instructions

Ensure you have JDK 17 installed and configured.

> **Note on Gradle Wrapper**: The Gradle wrapper executable scripts (`gradlew`, `gradlew.bat`) and wrapper jar are not committed to the repository. For local builds, install **Gradle 9.3.1** on your system and run `gradle`, or generate the wrapper locally with `gradle wrapper --gradle-version 9.3.1 --distribution-type bin`.

### Run Unit Tests
```bash
gradle testDebugUnitTest
```

### Build Debug APK
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/Phone-0.1.18-debug.apk`

### Build Release APK
To build a release-signed APK locally, set the 4 signing environment variables:
```bash
export PHONE_KEYSTORE_PATH="/path/to/release.jks"
export PHONE_KEYSTORE_PASSWORD="your-keystore-password"
export PHONE_KEY_ALIAS="phone-release"
export PHONE_KEY_PASSWORD="your-key-password"

gradle assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/Phone-0.1.18-release.apk`

*Note*: If the signing environment variables are omitted, Gradle produces an unsigned release build. Release builds are minified with R8; the mapping file is at `app/build/outputs/mapping/release/mapping.txt`.

## GitHub CI & Required Secrets

The automated CI workflow (`.github/workflows/android-build.yml`) compiles, tests, verifies signatures, and publishes release APKs to GitHub Releases. The R8 `mapping.txt` is uploaded as a separate workflow artifact (`Phone-mapping`), not attached to the public Release.

To enable release signing in GitHub Actions, configure exactly **4 repository secrets** in **Settings > Secrets and variables > Actions**:
1. `PHONE_KEYSTORE_BASE64`: The release keystore file encoded in Base64 (`base64 -w0 phone-release.jks`).
2. `PHONE_KEYSTORE_PASSWORD`: Keystore store password.
3. `PHONE_KEY_ALIAS`: Keystore entry alias (e.g. `phone-release`).
4. `PHONE_KEY_PASSWORD`: Key password.

## Permissions

- `android.permission.READ_CONTACTS`: Display, search, and view device contacts.
- `android.permission.WRITE_CONTACTS`: Create, edit, merge, and delete contacts on device.
- `android.permission.CALL_PHONE`: Place calls directly from the keypad, recents, and contacts.
- `android.permission.READ_CALL_LOG`: Show call history in Recents.
- `android.permission.WRITE_CALL_LOG`: Remove calls from the call log.
- `android.permission.MANAGE_OWN_CALLS`: Manage the app's own calls.
- `android.permission.POST_NOTIFICATIONS`: Incoming and ongoing call notifications.
- `android.permission.USE_FULL_SCREEN_INTENT`: Show the incoming call screen over the lock screen.
- `android.permission.READ_PHONE_STATE`: Read SIM/phone-account state for dual-SIM calling.
- `android.permission.BLUETOOTH_CONNECT`: List Bluetooth audio devices in the in-call route picker (requested only when the picker needs device names).
- `android.permission.FOREGROUND_SERVICE` / `android.permission.FOREGROUND_SERVICE_PHONE_CALL`: Keep the in-call service alive with a call notification.

Permissions are handled gracefully at runtime with comprehensive state representation (Not Asked, Denied, Permanently Denied with App Settings button, Granted). The first-run setup screen walks through all of them.

## Known Limitations

- **Default dialer required**: The in-call UI only appears when Phone is set as the default phone app (a system requirement for `InCallService`).
- **English UI**: App strings are English only. `fa` resources are kept for libraries; there is no Persian translation of the app UI yet.
- **Duplicate merge**: Merging keeps the first contact in each group and moves its phone numbers and emails. The kept contact's name, photo, ringtone, and organization stay; the removed contacts' photos are not carried over.
- **Number matching**: Two numbers sharing the last 10 digits are treated as the same number (trailing-digit match). This is what makes local, national, and international formats match, but it can theoretically collide for distinct numbers with identical trailing digits.
- **No sync or backup**: Contacts live in the device Contacts provider; there is no cloud sync. App settings (DataStore) and favorites (Room) are local and survive app updates, but are not backed up by the app itself.
- **R8 mapping**: Release builds are minified. `mapping.txt` is published as a workflow artifact (`Phone-mapping`), not in the public Release — keep it if you need to deobfuscate crash stack traces.
