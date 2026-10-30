# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.10.32] - 2026-10-16

- Liquid glass per-section settings (storage): each area with glass surfaces — bottom bar, dialpad, headers, contact detail, call screens — now has its own on/off setting (all default to on). The dialpad honours its switch in 0.10.33, the Settings switches arrive in 0.10.34, and the other sections follow as they are wired.

## [0.10.33] - 2026-10-16

- The dialpad honours its liquid-glass switch: with dialpad glass off, keys are bare with press-scale and a circle-clipped ripple, the Call button is the flat green FAB, the no-match actions are tonal buttons, and the T9 chips are plain assist chips.

## [0.10.34] - 2026-10-16

- Settings: a Liquid glass section with a switch for every section — bottom bar, dialpad, headers, contact detail, call screens. Each switch falls back to the flat rendering for its section; the bottom-bar, detail, headers, and call-screen switches become live as their wiring lands in 0.10.35–0.10.37.

## [0.10.35] - 2026-10-16

- The bottom bar honours its liquid-glass switch: with bottom-bar glass off, the bar is flat and the selected-tab indicator is a flat oval instead of the glass droplet.

## [0.10.36] - 2026-10-16

- Contact detail and headers honour their liquid-glass switches: with detail glass off, the floating action bar is flat; with headers glass off, the floating header pill is flat.

## [0.10.37] - 2026-10-16

- Call screens honour their liquid-glass switch: with call-screen glass off, the in-call controls and dock render flat instead of glass.

## [0.10.38] - 2026-10-16

- Liquid glass across the app (4): the Recents filter menu is now a glass popup (22dp corners, specular rim, translucent fill) with staggered fade/slide-in items that respect the Animations setting, and the Dual-SIM picker and calling-SIM dialogs are glass.

## [0.10.39] - 2026-10-16

- Liquid glass across the app (5): the Contacts top bar is split into clean glass pills (Groups on the left, actions on the right), and an interactive vertical glass A–Z fast-scroll scrubber with a floating glass letter preview bubble runs along the right edge.

## [0.10.40] - 2026-10-16

- Liquid glass across the app (6): the contact detail screen is redesigned in glass — hero card, Call/Message action buttons, info section cards, and the floating action bar — and the History card shows a trailing chevron plus a summary of the last call with that contact.

## [0.10.41] - 2026-10-16

- Liquid glass across the app (7): expanding a grouped call in Recents shows a breakdown list of the individual calls (type, exact time, duration, SIM badge) in a recessed container, with the four actions as staggered glass icon buttons. Fixed: the Contacts floating pill no longer fades in over the visible top-bar pills — it appears only after the top bar scrolls off.

## [0.10.42] - 2026-10-16

- Removed the Contacts A–Z fast scroller: no more letter rail along the right edge, preview bubble, or jump-to-section gestures. The grouped list and its section headers are unchanged.

## [0.10.43] - 2026-10-16

- Liquid glass across the app (8): the call background is an animated liquid aurora, the caller avatar gets pulsing glass rings, call controls are glass icon buttons with a bright active state, the swipe track shimmers and tints green/red with the drag, quick replies are glass chips, and a live equalizer wave sits next to the call timer.

## [0.10.44] - 2026-10-16

- Liquid glass across the app (9): the Call Studio preview scales its layout proportionally and always shows the caller header and call buttons, the reorder list uses 40dp glass icon buttons with chevrons, and the option selector chips are glass with animated selection.

## [0.10.45] - 2026-10-16

- App-wide motion (1): a shared bouncyClick modifier on dialpad keys, Recents cards, and dialpad action buttons (Create/Message pills, call button); the dialed number animates per character with haptic feedback on key presses and backspace; screen transitions use spring scale + fade and tab switches cross-fade with a subtle slide, all respecting the Animations setting.

## [0.10.46] - 2026-10-16

- App-wide motion (2): bouncyClick on contact rows, favorite cards, the profile row, and the favorites hint row; bottom-bar tabs; and contact-detail action buttons (Call, Message, Video).

## [0.11.0] - 2026-10-16

- Final polish: design-token audit (no hardcoded hex colors or stray font sizes outside core/design; the dialed-number animation now uses Motion duration/scale tokens), test imports verified against current APIs, and the dialed number is one accessible element again after the per-character split (the hidden measuring Text is excluded from accessibility, the digit row merges into a single readout).

## [0.11.1] - 2026-10-16

- Contacts snackbar (e.g. "Contacts merged") now sits above the floating bottom bar instead of being covered by it: the snackbar is offset by the navigation-bar inset + bar bottom margin + the current bar height + a small gap.
- Contacts overflow menu is liquid glass (background, rim, and shadow from the liquid-glass modifier) instead of the plain Material menu.
- Bottom-bar clearance is now sized for the largest bar (68dp Large + 12dp gap + 16dp), so list content clears the bar with margin at every bar size; the fade scrim behind the floating bar grows with it.

## [0.11.2] - 2026-10-17

- Fixed the glass rendering bug: liquid glass no longer draws a hardware shadow under a translucent fill (that showed through as dark rectangles in cards and dark squares under round-button icons). Shadows are now drawn only outside the shape (outerShadow) and only for floating surfaces — bottom bar, header pill, detail bar, popups; cards, keys, chips, icon buttons, and gem buttons get sheen + rim only.
- Glass brushes (fill, sheen, rim, shadow) are built once per theme/tint/selection (GlassSpec) instead of on every frame.
- Contacts overflow menu, SIM picker, and Calling SIM dialogs now use the outer shadow (floating popups).

## [0.11.3] - 2026-10-17

- Haze blur skipped: its API could not be verified against this project's Compose version (BOM 2024.09.00), so the translucent fallback stands instead.
- Bottom scrim: page color fades 0% to 85% over ~96dp above the floating bar, then stays at 85% under the bar, so list text fades out before reaching the bar.
- Glass fill is near-opaque (0.88 alpha) on the floating bar and header pill for legibility without blur.
- One app-level snackbar host above the floating bar (nav inset + bar margin + bar height + gap); Contacts no longer has a per-screen host under the bar.

## [0.11.4] - 2026-10-17

- Contact detail now uses the app-level snackbar host (no per-screen host under the detail bar) and its floating action bar gets the outer shadow.

## [0.11.5] - 2026-10-17

- Recents now uses the app-level snackbar host (no per-screen host under the bar) and its filter popup gets the outer shadow. Keypad keys no longer pass a dead elevation to liquid glass.

## [0.11.6] - 2026-10-17

- Call Background studio is one scrolling page: the phone-shaped preview card (9:19.5, 60% of the screen tall, capped at 520dp, glass frame) is the first item and scrolls away with the settings; the Incoming|Ongoing switch is a segmented control under it; Full screen is a glass icon button in the preview corner.
- When the preview scrolls off-screen, a floating glass Preview pill (small live thumbnail + label) appears at the bottom center; tap scrolls back to the preview.
- Studio groups in this screen are solid cards; the switch matches the main Settings switch (white thumb on accent track).

## [0.11.7] - 2026-10-17

- Call Studio sections finish the group-card treatment: every settings group is a solid card, all switches match the main Settings switch (white thumb on accent track), and selected chips are filled accent with onPrimary text instead of accent text on glass.

## [0.11.8] - 2026-10-17

- Keypad backspace hides when the number is empty (fade + scale) instead of sitting dimmed; the number stays centered because the backspace overlays it.
- Dark theme: accent text reads in a lighter tone (the accent lightened toward white), while buttons stay filled accent.

## [0.11.9] - 2026-10-17

- Contacts: 12dp gap between the duplicates banner and the favorites card.
- Recents times now honor the system 12/24-hour setting and locale (DateFormat.getTimeFormat) instead of a hardcoded 12-hour format, rebuilt when the setting changes.

## [0.11.10] - 2026-10-17

- New glassPress press effect (scale spring + white sheen) on glass keys, chips, icon buttons, gem buttons, and bottom-bar items, replacing the bounce/ripple there. Only active when Animations is ON.

## [0.11.11] - 2026-10-17

- Popups animate in: the SIM picker and Calling SIM dialogs now use LiquidGlassPopupSurface with a scale 0.92 to 1 plus fade-in (Medium duration, Emphasized easing) and a fading scrim. Enter-only; dismiss is instant. Inactive when Animations is OFF.

## [0.11.12] - 2026-10-17

- Bottom bar: a one-shot specular sweep (a white band crossing the bar, fading in and out) runs on every tab change. The spring selection pill is unchanged. Inactive when Animations is OFF.
- Search fields are glass now: the shared search bar (Contacts, Recents, Dialpad) sits in a liquid-glass container with a transparent field; the focus border stays accent.
- The duplicates banner is a liquid-glass card now (was a solid surface card). List cards stay solid.

## [0.11.13] - 2026-10-17

- Glass menus are legible now: the Contacts overflow menu and the Recents filter menu draw an opaque surface under the glass, so list content no longer bleeds through the menu items.

## [0.11.14] - 2026-10-17

- Contact detail: the scrolling content now clears the floating bottom bar by an extra margin (the detail bar carries a top margin the main bar lacks), so the History card no longer slides behind the bar.

## [0.11.15] - 2026-10-17

- Keypad call button is visible in light theme again: it draws a solid green disc under the glass (the glass fill alone washed out to near-white over a light background).

## [0.11.16] - 2026-10-17

- Glass menus animate in like the dialogs: the Contacts overflow menu and the Recents filter menu now scale 0.92 to 1 plus fade-in (Medium, Emphasized). The plain menus keep the default dropdown animation.

## [0.11.17] - 2026-10-17

- The edit/new-contact action bar is glass now: the top bar (Cancel + Save) draws liquid glass over a transparent container instead of a solid background.

## [0.11.18] - 2026-10-17

- Hardening: the in-call service now cancels its coroutine scope in onDestroy (its Job was never cancelled), and the app declares no cleartext traffic (INTERNET stays, used only for the opt-in update check).

## [0.11.19] - 2026-10-18

- Settings > Appearance: new Header size (Compact 22% / Standard 30% / Large 38%). Standard is the default.
- Update check hardened: the connection is always disconnected, rate limiting (GitHub 403/429) and offline get their own states with Retry, and About notes the check contacts api.github.com only when you tap it.
- The system call-settings row no longer risks a crash when its intent cannot be resolved.

## [0.11.20] - 2026-10-18

- Keypad page now uses the same dark-mode background as the other pages (it was a lighter gray).

## [0.17.9]

- Recents groups with several recordings now show a count badge on the collapsed row's play button, and expanding a group shows a play button on every call that has its own recording. Playing one recording stops any other that is playing.
- New Recordings screen, opened from the Recents 3-dot menu: all recordings grouped by day with search and All / Today / This week / This month filters, in-place playback with a seek bar, share, call back, and delete with confirmation. The list in Call recording settings is unchanged.
- App background is now choosable from Liquid glass settings: Aurora blobs (the current look, still the default), eight gradients, Plain, or a photo of your own with a Darkness slider. Intensity (Off / Subtle / Vivid) is still set in Backdrop. The photo file is not part of settings JSON backup.

## [0.17.8]

- The floating actions pill no longer overlaps the search bar: it hides while search is open on Recents and Contacts, and returns when search closes.
- Short silent recordings are no longer kept: a non-test capture under 2 seconds that is silent (or unmeasured) has its file deleted and no row saved, and rows like that kept by older versions are cleaned up once on start.
- Recents rows now show a play button when a recording matches the call (matched by number and start time, each recording used at most once). Tapping it plays the recording in place; tapping again (or when it ends) stops, and a missing file shows a "Recording file not found" message.
- Settings can now be backed up and restored as JSON from the About page: "Back up settings" saves all portable settings to a file, "Restore settings" loads one back, with a toast reporting how many settings were restored (device-specific keys are never exported).
- After a reboot, if auto-record is on but the recorder is not ready a minute later, a notification warns about it (and says to connect to Wi-Fi once when there is no Wi-Fi, instead of the generic failure text). The manual record button shows the same Wi-Fi hint when that is the reason it cannot start.
- Dead code removed: the unused LiquidGlassGemButton composable is gone.

## [0.17.7]

- Recents and Contacts no longer show a collapsed header bar: no title, hairline, or blur band after scrolling. Only the floating actions pill remains once the big header scrolls off, and a subtle gradient behind just the status-bar area keeps the status icons readable over scrolling rows.
- Settings simplified (nothing removed from storage, old installs keep working): one "Button size" row sets the end-call, answer, control, and DTMF sizes together; one liquid-glass master switch toggles headers, contact details, call screens, and the bottom bar together; one "Bottom bar style" picker (Lens glass / Glass / Solid) replaces the three overlapping bottom-bar controls; one accent row (Dynamic / Presets / Custom) shows the swatches or the color picker only for the matching choice; and a preset app style now shows a single "Customize" entry that switches to Custom keeping the preset's values, with the individual options appearing only for Custom. All the individual controls moved under "Show advanced settings", and Settings search still finds every moved row.
- Call recording can now record all calls automatically: a switch at the top of the Call recording page (needs Wi-Fi once after every reboot). When a call connects and nothing is recording, recording starts on its own; stopping it manually never restarts it in the same call. If auto-recording fails to start, a notification opens the Call recording page. The main Settings row shows "Auto" while it is on.
- While recording, the call screen now shows a pulsing red dot, "REC", and the recording timer next to the caller info (manual and automatic recordings alike). The level meter and the "armed" text are unchanged.
- Quick reply (decline with message) is now reliable: the message is sent directly with SMS for the SIM the call came in on, instead of leaving delivery to the system reply path that silently drops it on some devices. You get a "Message sent to <name>" or "Message failed to send" toast, and the result is logged. The SMS permission is asked once, on the first quick-reply tap; if denied, it falls back to the old system path. Quick replies are hidden for blank/private numbers, the chips keep wrapping instead of overlapping the answer controls, and Settings now lets you edit up to 5 messages (was 3), with existing overrides kept.

## [0.17.6]

- Collapsed header bar (Recents and Contacts): the bar now draws a solid background behind the blur (82% opaque with blur on, 96% with blur off) so list rows never show through it and collide with the status icons; the title is start-aligned at 20sp SemiBold, capped at 45% width, in a shared 60dp row, so it can never sit under the floating pill. The floating pill now centers vertically in that same 60dp row instead of hanging below the hairline (its 48dp buttons are already under the 52dp cap, so no padding change was needed). Layout holds for dark theme, glass headers on and off, and all header sizes, since the collapsed title and row height are fixed.
- Settings search is now behind a top-bar magnifier like Recents/Contacts: tapping it reveals the search field (and focuses it), tapping again or Back closes it and clears the query; tapping a result also closes it. When closed, the field takes no space, so the first card starts right under the top bar. All search entries, the highlight pulse, and the advanced-settings force-show are unchanged.

## [0.17.5]

- Recents header actions now sit in a pill like the Contacts actions toolbar: the filter, search, and menu buttons were bare icons; they now render inside the same pill container (glass when the glass-headers switch is on, flat surface + soft shadow when off), shared with Contacts instead of duplicated. Tapping the search button inside the pill still reveals the search bar exactly as before. The floating actions pill shown after scrolling keeps the bare actions, so nothing double-pills.

## [0.17.4]

- Recents search now sits below the status bar like the Contacts search: the search pill had no top inset, so with search open (and the self-insetting header hidden) it drew under the status bar, clipped by the clock and icons. It now uses the same status-bar padding plus small vertical padding as the Contacts search bar. The bottom overlap in the same screenshot is the list scrolling under the floating bar, which is normal and also happens on Contacts — the list's bottom padding already clears the last item at full scroll.

## [0.17.3]

- Settings search: a 48dp pill search field at the top of the main Settings page. While the query is non-empty, results replace the list: title, section breadcrumb, and a small section icon. Tapping a result opens its page and briefly highlights the row with an accent pulse (1.2s fade, static when animations are off, cleared after 1400ms). One registry covers every settings row (main page, all 7 section pages, App style Customize, Glass Studio, Call Studio groups, recording page), with English + Persian synonym keywords for the hard-to-find rows. Matching normalizes the query and the text (lowercase, Persian/Arabic yeh/kaf unification, tashkeel and joiners stripped, Arabic-Indic and Persian digits mapped to ASCII), requires all query tokens, and puts title-prefix matches first; results run on a 150ms debounce. Advanced rows are force-shown for the visit when opened from a search result, even with the advanced toggle off, and the force-show clears once every settings page is closed. Pages are single cards, not list items, so there is no row to scroll to — the pulse is the locator. Unit tests cover normalization, matching, registry id/anchor uniqueness, and that every page opens its flag.

## [0.17.2]

- Settings sub-pages open on top again: the whole sub-page group (BackHandlers + Answer style, App style, Glass Studio, Call background, Block numbers, Call recording) moved to the very end of the Settings composable, after the section pages. Back now closes the top-most page first. Logic unchanged, only the order.
- Recording helper version handshake: the helper STATUS reply now carries " v2" (HELPER_VERSION = 2, bumped on protocol changes). A stored helper that answers without the current version is told to quit and respawned instead of reused, so an old helper left running after an update can no longer answer "ERR unknown" to START. The in-call dialog never shows a blank or "unknown" message anymore — it shows the setup error text instead. If starting the foreground service throws, the session rolls back fully (descriptor closed, entry deleted, backend capture stopped, state Idle).
- Test record feedback: while the test runs, the button shows "Testing…" with a small spinner (new testing state in the view model, so other busy work cannot trigger it). The result now shows in a Card (error container on failure, secondary container on success) with bodyMedium text, using the same result strings and logic.

## [0.17.1]

- Shizuku recording backend removed: built-in ADB is now the only backend. The Backend radio group is gone from the recording page, the RecordingBackendKind setting and its repository key/setter are gone, and the controller, recording view model, recording page, main Settings recording subtitle, and in-call recording dialog no longer branch on a backend kind — they use the ADB backend directly. The settings test always records from the microphone.
- Deleted: the three shizuku/ classes (backend, IPC, user service), the Shizuku manifest permission and binder provider, the libs.shizuku.api and libs.shizuku.provider dependencies, the Shizuku ProGuard keeps, the Shizuku strings (en + fa, incl. the backend title and the grant/get/open actions), and the Shizuku branches of the recording dialogs. The recording note and pairing notification text no longer mention a second app.
- Migration: a stored backend=SHIZUKU preference is simply ignored now (DataStore drops unknown keys) — every existing user lands on the built-in ADB backend, with no action needed. Note: the push tool cannot delete files, so the three shizuku/ files on GitHub are empty stubs in this commit; delete them by hand on GitHub (nothing references them, CI stays green either way).

## [0.17.0]

- Bottom bar lens artifacts: chromatic aberration is off in both lens surfaces (droplet and whole bar) — it fringed the selected droplet's rim rainbow and separated a green channel blob beside it. The lens bend itself is unchanged.
- The moving droplet container is now clipped to its pill shape in lens mode, so any lens-effect overshoot past the surface box cannot paint past the pill or read as a ghost behind nearby tabs. The Haze branch is untouched (its glow is meant to extend past the pill and fade). Verified in code that lens mode draws exactly one droplet: the Haze accent glow lives only in the non-lens branch.

## [0.16.10]

- Recording test fixed: the settings test now records 3s from the microphone (helper `START MIC`) instead of VOICE_CALL, which is silent outside an active call. The button reads "Test recording (3s, microphone)" and shows bytes, duration and peak level; below 2% peak it says the microphone returned silence and names the RECORD_AUDIO / mic-in-use check; on success it says the recorder works and a call test needs a real call. Shizuku keeps its old call-source test.
- Silence detection for real recordings: the ADB reader tracks peak amplitude while writing PCM. On stop, a peak below 1% keeps the file but badges it "Silent" in the recordings list (with the full message) and shows the silent message in the call UI. Test recordings get the same badge.
- Tapping Record before the call connects no longer records dialing/ringing: it arms the recording, which starts only once the call is ACTIVE (the in-call UI shows "Recording starts when the call connects"); tapping again disarms.
- Live level meter on the in-call Record button while recording, driven by the same per-chunk peak.
- VOICE_CALL fallback (helper, own function): if the first 2s of an ACTIVE call are pure silence, it retries once with VOICE_DOWNLINK + VOICE_UPLINK mixed (summed with clipping) into one mono stream. If mixing is not possible on this device, the exact exception is logged and shown under Recorder status.
- Recorder status / Copy details now include the last capture's line: source tried, sample rate, first-second peak, overall peak, fallback failure text. Room database is now version 4 (recordings gain an isSilent column via a real migration).

## [0.16.9]

- Settings restructured into 8 section pages (Appearance, Liquid glass, Layout & navigation, Calls, Contacts, Call recording, Privacy, About), each with an icon row and a live summary subtitle on the main page; "Reset to defaults" stays at the bottom.
- New "Show advanced settings" switch (default OFF) at the bottom of the main page: when OFF it hides the Glass Studio entry, Call Studio's detailed sub-options (keeps control button size, background, show number), Call recording > Advanced, App style > Customize and the debug-log row; when ON everything is visible.
- Dead-setting audit: only one field was stored but never applied anywhere — answerGesture (Call Studio preset matching only, never read by a call screen). Removed: the enum, the field, the repo setter, the ViewModel setter, the Call Studio "Answer gesture" group and the SWIPE preset (now identical to CLASSIC); the stored value still migrates into answerStyle on first load, so no data is lost.
- Contacts clutter: the duplicates banner shows only while duplicates exist and it has never been dismissed; after dismissal its action moves into the Contacts overflow menu as "Merge duplicates". The "Add your favorite contacts" row shows only when favorites are empty and it has never been dismissed (new dismiss x); both dismissals persist across restarts.

## [0.16.8]

- Smaller APK, no feature removed: native libraries now ship arm64-v8a only (the APK carried four ABIs of the same two libraries); packaging drops dead metadata (META-INF license/version files, DebugProbesKt.bin, kotlin-tooling-metadata.json, *.kotlin_builtins).
- R8 keeps narrowed: the whole Coil Compose package keep is now just the two image composables the avatar UI calls (AsyncImage, SubcomposeAsyncImage) plus ImageRequest, and the whole settings package keep is now just the two DataStore key holders. Room, Shizuku, Kadb and the recording helper (RecordingHelperMainKt) keeps are unchanged.
- Measured on the 0.16.7 release APK (7,072,550 bytes): classes.dex 4,639,352 (stored, 66%), BouncyCastle resources 1,240,355 (kept: its JSSE provider is live in the DEX, used by the ADB TLS stack), fonts 800,370 compressed. The CI font-subsetting step (Latin-only Inter, committed files untouched) is prepared for hand-add to the workflow, since the push tool cannot write workflow files; it saves a further ~700 KB on the first build that runs with it.

## [0.16.7]

- Keypad: long-press paste on the number display is back (the Clipboard chip stays gone).
- Fixed: long-pressing backspace pasted the clipboard before deleting. The old paste detector sat on the whole display area, above the backspace, and the backspace's event consumption could not reliably cancel a long-press wait that had already started. The paste target is now a separate area covering the number field except the backspace slot, so a backspace press can never reach it; backspace long-press only repeats deletion until release.

## [0.16.6]

- Keypad: the "Clipboard / Paste" chip and its title are gone completely, together with every clipboard read or check on that screen — the chip composable, the clip-description check on open/resume, the tap-to-read path, and the number display's long-press paste. The Recent chips (quick redial) and the favorites strip are untouched.

## [0.16.5]

- Pairing UX: submitting the code now immediately replaces the notification with an ongoing "Pairing…" one (indeterminate progress, no reply action), so the system "sending" spinner stops; the pairing itself runs in the application scope instead of goAsync (whose ~10 s budget cut off the 30 s discovery), and the pending result finishes right after launching. If the pairing service is not found within 30 s, the result notification says to open "Pair device with pairing code" first, then type the code; on success it posts "Paired" and refreshes the status.
- Settings > Call recording: once the probe connected (READY, or ERROR with a working connection — i.e. the key is accepted), the status card shows a checked "Paired" line and the big Pair button becomes a small "Pair again" text button; the how-to-pair instructions only appear while status is "Not paired yet".

## [0.16.4]

- Built-in ADB recorder helper now talks to the app over loopback TCP instead of an abstract local socket (which the shell SELinux domain denied to the app): the app picks a random port (20000-60000) and a random 24-byte hex token per launch, stores both so a running helper is reused after an app restart, and passes them to the helper; every connection must send the token first. START streams raw PCM that the app writes into its own MediaStore file (placeholder WAV header, patched on stop); STATUS reports IDLE/RECORDING, QUIT stops the helper.
- When the helper does not answer within the 8 s poll, Recorder status and Copy details now show the helper's own log file, its process line, and the failed socket error (truncated to 1500 chars) instead of a bare failure; the helper logs its startup and every stack trace to that file, and a stale helper is killed and relaunched with a fresh port/token.
- On Android 12+ the helper creates AudioRecord with AudioRecord.Builder; if creation or start throws, the exact exception text travels back as the error instead of a generic one, and the 3-second test button shows that exact text.

## [0.16.3]

- New "App style" setting at the top of Appearance & Display: Classic (system font, rounded cards, glass keys, default switches, fade transitions, dialogs, no swipe actions) and Clean (Inter, inset cards, flat keys, green switches, push transitions, bottom sheets, swipe actions). Picking a preset applies it live across the app.
- The App style page shows both presets as preview cards with a mini mock of a list row, a keypad key and a switch in each look, a "Try it" preview of the current look, and a Customize section; changing any individual option there flips the style to Custom (with a badge on the title). Reset to defaults restores Classic.
- Bottom bar, glass sliders and Glass Studio are untouched and apply in both styles.

## [0.16.2]

- Recents swipe: start->end calls back (green), end->start deletes (red) with an Undo snackbar; light haptic when the swipe crosses the threshold; directions follow the layout direction (RTL swaps them).
- Navigation: tab switches are a short 180ms crossfade only; pushed screens (Settings, contact detail/edit, call screens) slide in on a spring with the screen underneath parallaxing out and dimming; pop reverses. Reduced animations fade only.
- Bottom sheets: contact create/edit, Call waiting, the SIM pickers, and the Incoming call display choice now open as bottom sheets with a 36x5dp grabber, 20dp top corners, dim scrim, and drag to dismiss.
- Scrolled headers: once the large title scrolls off, a translucent blur bar (tint 0.35) with a 0.5dp hairline shows the screen title centered at 17sp SemiBold.

## [0.16.1]

- Typeface: Inter (open source, OFL) is now the app font family; Persian text keeps the system font via automatic fallback.

## [0.16.0]

- Type scale: large title 34sp Bold with -0.4sp tracking, row title 17sp Regular, row subtitle 15sp at 60% alpha, section header 13sp Medium at 55% alpha, on the system font for now (Inter wiring follows once the font files land in res/font).
- New Card style "Inset": solid 14dp cards with 0.5dp hairline dividers at text start (72dp for avatar rows, 16dp in Settings) and an instant white 10% row highlight fading out over 300ms; applied to Recents, Contacts and Settings list containers.
- New Key style "Flat": 78dp solid keys (dark #333336, light #E5E5EA), 36sp Light digits, 10sp Bold letters with 2sp tracking at 60% alpha, instant press fill fading back over 350ms, and a 78dp green call button. Glass keys stay the default; the previous bare-key look is now "Simple".
- New Switch style "Green": green checked track, white thumb, dark unchecked track, no borders; applied to the app switch and the recording switch.
- Settings: Key style picker replaces the old glass-dialpad switch in the glass section; Switch style picker added to Glass Studio; Inset joins the existing Card style picker.

## [0.15.10]

- Lens bar: the droplet area in lens mode draws only the lens droplet (the accent glow moved to the Haze path), and the lens droplet is clipped to the fully rounded pill shape; size (item minus the 4dp inset) and rest centering on the selected tab are unchanged.
- Settings > Calls > Call recording subtitle now shows the selected backend plus its status word ("Built-in ADB · Ready", "Not paired", "Needs Wi-Fi"), refreshed when the row appears.
- Quick-redial chip labels are wrapped in bidi isolation (FSI/PDI) so parentheses stay on the correct side in RTL names.

## [0.15.9]

- Record without Wi-Fi (experimental) in Call recording > Advanced: when the switch is on, a healthy Wi-Fi probe asks adbd to listen on local port 5555, waits for it, and re-starts the helper through it; later probes use 127.0.0.1:5555 first with no Wi-Fi needed until the next reboot.
- Status shows "Offline port active (until reboot)" on the local port, and "Connect to Wi-Fi once to re-enable offline recording" when the switch is on, the port is closed, and there is no Wi-Fi. Switching it off just stops preferring 5555; the Wi-Fi path is unchanged.

## [0.15.8]

- Fixed the crash when opening Call background (the same crash would hit a real incoming call): the quick-reply chips used FlowRow, whose overload changed between foundation-layout versions, while the app was compiled against a newer layout than the one shipped in the APK. All three FlowRow call sites now use a small WrappingRow built on the stable Layout API, so the app no longer depends on the version-sensitive overload.

## [0.15.7]

- ADB recorder endpoint selection reworked (stale-port refused connections): ports are now tried in order — manual port (always wins), getprop service.adb.tls.port, NSD services newest-first, last known working port (persisted after every success) — and every candidate is TCP-checked (loopback first, then NSD hosts) before Kadb sees it.
- NSD now collects every _adb-tls-connect service for ~2.5 s instead of stopping at the first (stale) one.
- One verified endpoint end to end: probe stores it, and ensureBound reuses it with no second discovery unless it stops answering.
- Failures list every attempt under Recorder status (also in Copy details), e.g. which ports refused on which hosts; when nothing answers, it says to toggle Wireless debugging off/on and tap Refresh or type the port.
- New "Detect port" button next to the Connection port field runs the same selection and fills the field with the working port. Pairing and the key store are untouched.

## [0.15.6]

- Quick redial chips: labels are now single-line with ellipsis in both the glass and button variants, so all three chips measure the same height (a two-line label was making its chip taller). RTL names keep bidiWrap plus TextDirection.Content, so parentheses stay on the correct side; the Recents row title already had the same wrap and is unchanged.

## [0.15.5]

- Lens-glass bottom bar (Backdrop 2.0.0, pinned): on API 33+ with the new Glass Studio "Lens bar" switch on (default) and Reduce transparency off, the bar becomes a real lens — blurred, tinted, refractive surface with a gradient rim, and the selected droplet is a pure sharp lens with chromatic aberration that grows with finger lift. Otherwise the current Haze bar is completely unchanged, and nothing new loads below API 33 (separate file, SDK-gated).
- Bar sizing for the lens bar: 78% of the screen capped at 380dp, 64dp tall, 4dp droplet inset; drag, haptics, spring settle, RTL, labels, semantics, testTags, Glass Studio blur/tint sliders and animation settings all carry over.
- The library's AAR requires compileSdk 37, so compileSdk moved 36 → 37 (targetSdk stays 35, minSdk stays 29). Other screens' Haze pills are untouched.
- 2.0.0, not 2.0.1: 2.0.1 was compiled with Kotlin 2.4, whose metadata the project's Kotlin 2.2 compiler cannot read; 2.0.0 was built with Kotlin 2.3 and its library source is identical to 2.0.1's.

## [0.15.4]

- ADB recording diagnostics: the status page now shows the specific reason instead of a bare "Not paired yet" — Wi-Fi missing, wireless debugging off, NSD service not found (with the VPN note), or key rejected — as selectable text with a Copy details button. Only an auth failure still maps to NOT_PAIRED; other connect failures show the raw error.
- Connects robustly: tries 127.0.0.1 first, then the NSD host, each with a 10 s timeout, and reuses the working endpoint for the helper bootstrap (with one fresh discovery if it went stale).
- Advanced card: optional "Connection port" (used only when NSD discovery fails) and "Reset pairing" (deletes the persisted key; forget "Phone" in system Paired devices first).
- Pairing result now shows in-app ("Paired" or the error), not only in the notification; pairing discovery timeout raised to 30 s.
- Helper launch detaches with nohup + setsid (nohup-only fallback) and polls up to 8 s, so it survives the ADB session closing; after the first successful connect, adb_allowed_connection_time is set to 0 so the authorization does not expire.
- Status auto-refreshes on screen resume, on Wi-Fi changes, and when wireless debugging is toggled; a note explains the USB-debugging trick for keeping the recorder alive. Test recording stays disabled until READY and already showed the real error.

## [0.15.3]

- Fixes built-in ADB pairing not sticking: Kadb's identity defaulted to an in-memory key, so pairing authorized a key that vanished when the app process died — the next status check then reported Not paired even though pairing had succeeded. The private key is now persisted in app-private storage, so one pairing survives restarts and updates.
- Pairing now registers as "Phone" in the system's Paired devices list instead of Kadb's default OS-build user@host name.
- Note: pairings made with 0.15.2 cannot be reused — their key was never saved. Pair once more after updating; the old cryptic entry can be forgotten in wireless debugging settings.

## [0.15.2]

- Fixes the built-in ADB pairing flow: the Pair button only appeared once the phone was already pairable, so from the "wireless debugging is off" state there was no way to reach the code entry at all. Pair is now available in every non-ready state, and tapping it posts the notification and opens Developer options in one step.
- Pairing also ensures the notification can actually appear: on Android 13+ a plain notification is silently dropped without the notification permission, so tapping Pair now requests it first, and falls back to the app's notification settings if it was denied for good.

## [0.15.1]

- Fixes the 0.15.0 build: Kadb's SPAKE2 dependency (com.github.Flyfish233:spake2-java) is published only on JitPack, not on Maven Central, so dependency resolution failed. The build now includes the JitPack repository, scoped to that one group. Kadb's library also requires compiling against Android API 36+, so compileSdk moves 35 → 36 (targetSdk stays 35, minSdk stays 29 — no runtime behavior change). No app code changes.

## [0.15.0]

- Call recording without Shizuku: Settings → Calls → Call recording now offers a Backend choice — Shizuku (as before) or Built-in ADB. Built-in ADB pairs the phone with itself over wireless debugging (one-time, six-digit code typed into a notification so the system pairing dialog stays open), then starts Phone's own shell-privileged recorder over that ADB connection. No second app needed; the Shizuku path is unchanged and still available.
- How the ADB backend works: pairing and shell access come from the Kadb library (Apache 2.0, Maven Central); the recorder helper is Phone's own code, started as shell via app_process and detached, so it keeps running after wireless debugging is switched off. It records the same WAV (16 kHz mono) to the same Music/Phone files, with the same in-call Record button, test, playback, delete, and share. After a reboot the helper is gone until the phone is on Wi-Fi with wireless debugging on — the backend re-arms it on demand when you record or test, and says plainly when it cannot.
- No other changes.

## [0.14.0]

- Call recording (manual, via Shizuku): a Record button joins the in-call controls (last in the grid; add it from Call Studio if you customized your buttons), with a red recording indicator under the timer. Tapping it while Shizuku is missing, stopped, or ungranted opens a dialog with the one action that moves setup forward instead of dead-ending.
- Settings → Calls → Call recording: backend status, Shizuku setup actions, a 3-second pipeline test that proves bind + capture + file without a call, and the recordings list with playback (play/pause, seek), delete, and share. Files are WAV (16 kHz mono) in Music/Phone; metadata lives in a new recordings table.
- How it works: the app itself never records. A small privileged service running under Shizuku's shell identity captures VOICE_CALL audio and writes it to a file the app created; a foreground service keeps the recording visible (with a Stop action) and stops it automatically when the call ends. No CallVault code is used: the bridge is original, and Shizuku's own API is MIT-licensed.
- Needs the Shizuku app, started and granted. Built-in ADB pairing (no second app) is planned as a later backend behind the same interface. VoLTE/VoWiFi behavior on your phone is unproven until tested — the pipeline test proves the bridge, a real call proves the audio.
- No other changes.

## [0.13.7]

- Floating actions pill: once the header has scrolled off, a small pill with the header's buttons floats at the top-right — Recents: filter, search, menu; Contacts: Groups, add, search, menu. No title, no full-width band. The pill is a sibling of the list (never a list item), so it cannot hit the 0.13.3 haze crash.
- No other changes.

## [0.13.6]

- Sticky top bar removed: Recents and Contacts go back to the header-only look — the large header with title and pills scrolls away with the list, no floating bar. The per-list haze states and the list-item haze providers go away with it (they only existed for the bar), along with the bar-only compact buttons and the header-height measurement.
- No other changes.

## [0.13.5]

- Contacts crash fixed: opening the Contacts tab crashed on the first draw with Haze's "haze nodes can not draw hazeChild nodes" error. Cause: the glass header pills blur with the shared haze state, but since 0.13.3 they sit inside the list's own haze source, so Haze includes the shared area for them and draws it while the shared source is recording it. The list items now read the list's own haze state (same for Recents, which would hit the same crash with glass cards on), so item glass renders tint + fill only and the sticky bar keeps blurring the list.
- No other changes.

## [0.13.4]

- In-app crash log: an uncaught crash now saves its stack trace inside the app, and the next launch shows it in a dialog with Copy and Dismiss, so a crash can be diagnosed without logcat access. The saved report is deleted once copied or dismissed, and the next crash overwrites it.
- No other changes.

## [0.13.3]
- Sticky glass top bar fixed: no more 0.92 background hiding the blur; the fade band under it is now a background gradient (0.6 to transparent over 16dp, no blur on the band) instead of the inverted one; the bar sits at the very top of the screen at full width with its own status-bar inset (status inset + 56dp), so it covers the status area; each list has its own haze state so the bar blurs only the list; the compact bar renders plain icon buttons instead of the large-header pills (Contacts: Groups icon, add, search, menu; Recents: filter, search, menu).
- Page header rebuilt as a content-based Column (title row, centered title, subtitle) scaled by the Header size setting (0.85x/1x/1.2x) instead of the fraction + offset layout, so the rows cannot overlap; the sticky bar fades in from the header's measured height.
- 24dp blur/scrim band above the bottom bar removed (its gradient was inverted and drew a hard rectangle); the bottom scrim stays.
- Avatars: dark initial on light gradients (average luminance over 0.5), white otherwise.
- No other changes.

## [0.13.2]
- Sticky glass top bar fixed: it was double-inset (Scaffold innerPadding plus its own status-bar padding), so it floated mid-list with list rows visible above it and sharp list text colliding with the title. The bar is pinned to the top of its container now, and its body paints the theme background under the blur — if the haze effect draws nothing, the background still hides the list so the title stays legible (when the blur works it draws opaquely over this fill). Each tab list is also its own haze source now, so the bar blurs the list itself as a sibling overlay (the same pattern the bottom bar uses over the MainActivity source).
- No other changes.

## [0.13.1]
- Sticky glass top bar on Recents and Contacts: an overlay above the list (never a list item) covering the status-bar inset plus a 56dp title row, with the collapsed title centered and the header's action icons reachable. Invisible while the large header is fully visible, fading in as it scrolls away; content scrolling under it blurs through the shared glass style plus a 16dp fade band below the bar (uniform blur fading into the background). The top scroll fade is gone on these screens, and the old floating actions pill is replaced by the bar (its content is only composed once scrolling starts, so no menus can open from an invisible bar). Reduce transparency / API < 31 fall back to the solid treatment.
- Header size now defaults to Compact, and Compact is ~16% of screen height (min 160dp) so the first list item starts higher; the other sizes are unchanged. Action icons sit on the title row now, not far below it.
- Bottom bar legibility: dark fill raised to 0.30 (blur stays 36dp, size/droplet/rim/highlights untouched), plus a short 24dp blur/scrim band directly above the bar so content dissolves instead of reading through the tab labels.
- Recents ripple: only the header row of a call group is clickable now, with the bounded ripple clipped to it; the expanded details and action buttons no longer share that clickable/indication.
- "My profile" avatar now always shows the initial letter: the device profile's photo tile was rendering instead of the letter, so the profile row no longer passes a photo to the avatar.
- Duplicates banner is a slim glass row now (one line: count + Merge text button + close icon, max ~56dp high); dismiss behaviour and tags unchanged.

## [0.13.0]
- Call timer no longer drifts: duration is derived from the real connect time on every tick (Details connect time converted once to the monotonic clock, or the elapsed clock captured when the call becomes ACTIVE), so activity recreation, screen-off and backgrounding can neither reset nor lag it. Ticks align to whole-second boundaries, so no second is ever shown twice, and the loop keeps ticking with the keypad open.
- Recents expand and long-press menu are mutually exclusive now: long-press collapses any expanded row first, then opens the menu; tapping a row dismisses any open menu. The menu is anchored to the row's end side (the time slot) with a small offset, so it opens over the row's right half and never over expanded details text.
- Profile avatar initial now routes through the same helper as every other avatar (first character of the trimmed name, uppercased, "?" when blank), with a small unit test for the helper.
- Bottom clearance under the floating bar fixed where it was missing: Groups (both lists), Blocked numbers and the Settings main page now use one shared value from the bar-height state (current bar height + navigation-bar inset + 16dp). Recents, Contacts and the Favorites section already cleared the bar and were left alone.

## [0.12.10]
- Bottom bar droplet is a glass lens now: white vertical-gradient fill, crescent top highlight, 1dp gradient rim (bright top-left, faint bottom-right), plus a soft accent-colored glow behind it (radial gradient, no real blur).
- Bar surface: dark fill thinned to 0.16 so the colored backdrop shows through (light stays 0.20), rim brighter at the top (white 0.45 to 0.06, still scaled by the Studio edge-light setting), and a very light white wash from the top for the thick-glass look.
- Motion: selected icon pops 1 to 1.18 to 1 over 300ms and lifts 1.5dp while selected (animated when motion is ON, instant when Reduced/Off); the droplet slides with spring(0.62, 380) on release and on programmatic tab changes.
- Press feel (glassPress, used by glass keys, chips, icon/gem buttons and bar items): scale to 0.95 with spring(0.55, 500), white overlay at 0.12 while pressed, light haptic on press down (Studio glass haptic setting; LIGHT by default). Still ON-only; Reduced/Off leave surfaces untouched, and Reduce transparency still falls back to the solid bar.

## [0.12.9]
- Root cause of flat glass fixed: the ambient backdrop used to sit outside the haze source (only the tab content had hazeSource), so glass could never blur it. Backdrop and tab content now share one haze source; the bottom bar stays a sibling drawn after it, never a child. The detail bottom bar and the Edit Contact save pill were checked too: both already sit as siblings after their content, so they pick up the backdrop blur as well.
- Invisible text fixed centrally: LiquidGlassPill, LiquidGlassChip, LiquidGlassCard, GlassListCard and LiquidGlassPopupSurface now pin their content color to onSurface, so Text without an explicit color no longer falls back to black on dark glass. The Favorites name and the Paste label on the keypad also carry an explicit onSurface color.
- Quick-redial Recent pills (icon + name) now keep a 44dp minimum height, and an empty or still-loading list shows no pills at all instead of empty ones.
- In-call screen with the keypad open no longer scrolls: the layout always fits. The avatar shrinks to 56dp (animated) and the SIM label hides while name, number and timer stay; the keypad grid takes the remaining height with four equal rows, each key centered in its cell and capped at 76dp so keys shrink on small screens instead of overflowing; the Speaker/Hide/Mute row keeps its fixed height and the end-call button sits at the bottom at full size, never clipped.
- Edit Contact: the form's bottom clearance is now the save pill height + the navigation bar inset + 16dp, so the last card is never covered by the floating pill; the screen background is transparent like the other tabs, so the status bar area and the body match and the backdrop shows through.
- Bottom bar widths: Small 62%, Medium 75% with a 360dp maximum, Large 82%. The droplet inset stays 4dp.

## [0.12.8]
- Ambient backdrop now shows on Contacts and Keypad too: their roots go transparent whenever Backdrop is not Off and Pure black dark mode is off, like Recents. List cards stay opaque.
- Keypad Paste chip: no more clipboard toast on screen open. The chip appears only when the clip description holds plain text (metadata only, no toast), shows a generic "Paste" label, and reads + validates the text only on tap (spaces, dashes and parentheses removed, optional leading +, 5 to 15 digits); a non-number pastes nothing and hides the chip.
- Glass Studio preview rebuilt as a mini bottom bar (the bar's own glass, droplet + tab icons over the backdrop) so it previews what Studio actually tunes.

## [0.12.7]
- Glass Studio: new "Edge refraction" switch (default on) for the floating bottom bar.
- Edge refraction (API 33+): a subtle AGSL warp on the bar's blur layer only, magnifying the backdrop near the bar's ends; icons and the droplet are untouched.
- Any failure, API below 33, or Reduce transparency falls back silently to the plain blur.

## [0.12.6] - 2026-10-28

- New Glass Studio page, opened from the Liquid glass section of Settings: a live preview card (glass pill + glass key over the real backdrop) plus blur amount (0.5–1.5x), glass tint (0–1), edge light (Off/Soft/Bright), dynamic highlights, color richness (1.0–2.2x), droplet stretch (Off/Normal/Extra), glass haptics (Off/Light/Medium), backdrop (Off/Subtle/Vivid, reusing the existing setting) and a reset-to-defaults button. All defaults reproduce the current behavior; Reduce transparency and Animations Off are respected as before.

## [0.12.5] - 2026-10-28

- In-call screen with the dialpad open: the end-call button now stacks directly under the keypad's Speaker/Hide/Mute row with proper spacing and keeps its full circular size (the keypad-open stack scrolls on short screens instead of squeezing the button off the bottom edge).
- Recents rows: RTL names (e.g. Persian) now start-align with content direction in the same name + subtitle column as LTR rows, and the time sits in its own fixed-width slot, so names can no longer collide with the time.
- Recents overflow menu: replaced the plain dropdown with a glass popup anchored under the three-dots button with a small gap, scaling in from the top-right with a spring and drawn above the list.

## [0.12.4] - 2026-10-28

- Fixed drag navigation intermittently refusing one tab: the bar's pointerInput block is created once and keeps the values it captured, so its navigate check compared against a stale route and skipped navigation back to whichever tab was active when the bar first composed. The gesture now reads the current route and animation option through rememberUpdatedState, so it always sees the latest values.

## [0.12.3] - 2026-10-28

- Bottom bar is now draggable: press and drag across the bar and the droplet follows the finger, lifts and magnifies (12%), stretches with drag speed (up to 25%), ticks a light haptic on each tab crossing, and springs to the nearest tab on release. A tap navigates to the tab under the finger. The programmatic pill flight, icon pop, labels, shrink, Glass/Solid style, size, testTags and RTL are all kept; item clicks moved to semantics (Role.Tab) since the gesture now owns touches.

## [0.12.2] - 2026-10-28

- Bottom bar restyled as a modern liquid-glass pill: stadium shape, 85% of screen width for Medium (70% Small, 90% Large), 64dp tall, floating 12dp above the navigation-bar inset, 26dp icons.
- Bar glass: 36dp Haze blur, near-black tint at 22% (dark) / white at 30% (light) via the existing tint tokens, container fill 0.20 so the blur and backdrop show through, 1dp vertical-gradient rim (white 38% top → 6% bottom) plus faint highlights at the left/right ends, no drop shadow under the glass.
- Selected droplet is now neutral (white 16% dark / black 8% light, no primary tint) with a 1dp white border whose alpha rises to ~40% mid-flight and settles at ~22% at rest; the bouncy sine-bulge slide is kept, stretch 14dp → 20dp.
- Icons: selected filled pure white (dark) / near-black (light), unselected outline white 80% (dark) / black 60% (light); outline-to-filled Crossfade and 1 → 1.15 → 1 pop kept.
- Bottom scrim lightened (max alpha 0.55 → 0.35) so list content scrolling behind the bar stays visible through the blur.

## [0.12.1] - 2026-10-28

- Glass now has something to blur: a soft ambient backdrop (three large accent-hue radial blobs drifting on a 28s loop) drawn at the root behind the tab content, with a new Backdrop setting (Off / Subtle / Vivid, default Subtle; forced Off by Pure black dark mode).
- Glass tints thinned so the blur reads: dark tint 0.55 → 0.22, light tint 0.60 → 0.30.
- Contacts/Recents no longer paint an opaque layer over the backdrop in gaps and header areas (Scaffold and root containers transparent, header background transparent); list cards stay opaque.

## [0.12.0] - 2026-10-28

- Premium glass polish: one shared premium glass treatment (diagonal specular border, soft inner top highlight, diffuse shadow, backdrop saturation boost) applied to dialpad keys, in-call controls, popup sheets and the contact detail bar.
- New "Dynamic highlights" setting (default on, off when Animations is Off): the premium specular border shifts with device tilt (rotation-vector sensor with accelerometer fallback, low-pass filtered, registered only while resumed).
- Glass press feel: dialpad keys and in-call controls now press to 0.96 with a brighter fill and light haptic, releasing on a soft bouncy spring.
- Call screens: the Default background is now a slow animated gradient built from the contact avatar's colors (two or three drifting blobs, 20-30s loops), falling back to the current blue for unknown callers; the ringing avatar pulse ring is unchanged.
- Progressive scroll fades: Contacts, Recents and contact detail now fade content in under the top edge over ~24dp of scroll instead of a hard edge; contact detail also fades above its floating bar, while the tab lists keep the existing bottom-bar scrim (a second bottom gradient there would double-dim).
- Dialpad number display: each new digit now pops in on a spring from 0.8 scale; backspace removal stays smooth via the animated row size.
- Slide-to-answer shimmer now stops when Animations is Reduced or Off (it previously shimmered always) and is drawn in the draw phase instead of recomposing per frame.
- Contact detail: the header avatar now parallaxes at 0.5x scroll speed and scales slightly on overscroll; the sticky glass action bar is unchanged.
- Shared-element transition from rows to contact detail skipped: detail navigation lives in AnimatedContent inside ContactsScreen (plus a cross-tab handoff), not a shared NavHost route, so a shared-element scope cannot span it cleanly.

## [0.11.50] - 2026-10-28

- Keypad favorites: each favorite now shows its name under the avatar, resolved the same way as the Contacts tab's favorite cards (blank names fall back to the number, then Unknown) with bidi isolation and content direction.
- RTL names: Recents and Contacts rows now right-align Persian/Arabic names (direction-aware alignment from the name's first strong character); Latin names are unchanged.
- Contacts: the duplicate-contacts banner now has clearance above it, and the floating actions pill waits until the banner has scrolled off instead of floating over it.
- In-call/incoming screens: the top-right X exists only on the Call Studio fullscreen preview; the real call screens never had one, so nothing was removed there.
- Answer style: when Slider with "Slider only when locked" is on, the Settings row now reads "Slider (locked) / Classic (unlocked)" instead of just "Slider".
- Call waiting: Check status keeps the dialog open and shows the carrier's reply (Active/Inactive) inside it; activate/deactivate still use the separate result popup.
- Keypad overflow menu: now the standard anchored glass dropdown used by the Contacts/Recents menus.
- Block numbers: search field, an Unblocked snackbar with Undo on delete, and prefix blocking (numbers starting with a stored prefix are rejected on arrival; exact numbers still go through the platform provider).
- Dialpad keys: light haptic tick now fires on press-down instead of on release (still gated by the system haptic setting).
- Dialpad long-press: 1 dials the carrier voicemail number; long-pressing 2-9 with a number typed assigns it as that digit's speed dial, and long-pressing 2-9 on an empty keypad dials the assigned number.

## [0.11.49] - 2026-10-28

- Architecture: AndroidCallLogRepository (495 lines) is split by responsibility with no behavior change — the repository skeleton, observer, and orchestration stay in AndroidCallLogRepository (149 lines); the streaming query in AndroidCallLogStreaming, the paged history query in AndroidCallLogHistory, the JSON cache in AndroidCallLogCache, and row mapping in AndroidCallLogRowMapping. Cross-file members are internal (same-module visibility) and the paged query keeps a thin delegating override so the class still implements its interface.
- New unit tests: row-type mapping for every provider type code plus unknown fallback, and SIM badge edge cases (first match wins on duplicate account ids, empty account list shows no badge). The existing SIM badge and Android 10 query-contract tests still pin the split files' shared constants.

## [0.11.48] - 2026-10-27

- Visual balance fixes: the floating actions pill now appears only once the header item has scrolled off completely, instead of fading in on any scroll offset over the still-visible header; Contacts' pill no longer wraps its top-bar pills in a second pill container (Recents' plain icon actions keep the outer wrap); the per-screen bottom scrims are gone — MainActivity's fade behind the bar is now the single owner, ending the double-dimming on Recents and Contacts; the empty keypad centers its content with breathing room instead of squishing it above the keys under a tall blank; the Liquid answer control now vibrates at its 0.7 commit threshold like Slider and Swipe; the contact edit form lifts with the keyboard, so the floating Cancel/Save pill no longer covers the last fields.
- Empty keypad extras: a Favorites strip (favorite contacts with numbers, tap to call) and a Clipboard chip (the clipboard's number, refreshed on resume, tap to paste) sit above the recent chips when the keypad is empty.

## [0.11.47] - 2026-10-27

- Data layer and multi-SIM fixes: call-log SIM badges now resolve through the active accounts list (matching phoneAccountId against the account handle id) instead of slicing the id, so 20-digit ICCIDs no longer produce broken labels like "SIM 89" — unmatched accounts show no badge rather than a wrong one; the contacts index no longer marks itself started when observer registration fails, so it retries once READ_CONTACTS is granted instead of staying stale forever; the call-log queries no longer put LIMIT in sortOrder on Android 10 and below (the provider rejects it) — the 500-row cap is enforced in the cursor loop, and the paged query's loop already stops at its window end; pasted or received Persian/Arabic digits are normalized to ASCII before sanitizing, matching, formatting, or dialing, so they no longer break matchers or produce malformed tel: URIs (this also closes the emergency-number check for Persian-digit input).
- New unit tests: Persian/Arabic digit normalization (including sanitize, matcher, and formatting paths), SIM badge mapping, contacts-index retry after permission grant, and the Android 10 query contract (no LIMIT in the legacy sort order, 500-row cap).

## [0.11.46] - 2026-10-27

- In-call and telecom lifecycle fixes: the InCallService scope now cancels only its children on destroy (cancelling the scope itself poisoned its root Job across rebinds); the keyguard is no longer dismissed when a call starts ringing — it is dismissed only after the user explicitly answers, from the answer button or the notification action (the dead pre-O_MR1 branch is gone, minSdk is 29); DTMF player calls are guarded so a platform player throwing mid-tone can never kill the tone worker; emergency numbers bypass the SIM picker, saved-SIM lookup, and this app's InCall routing entirely and go straight to the system dialer; CallActivity now provides a root HazeState like MainActivity, so call-screen glass actually blurs on API 31+ instead of falling back to flat opaque.

## [0.11.45] - 2026-10-27

- File splits, no behavior changes: AndroidContactsRepository's read queries moved to AndroidContactsQueries and its row lookups to AndroidContactsLookup (same package, internal helpers taking the ContentResolver), and DialpadScreen's sections moved to DialpadSuggestionsArea (search rows, suggestion chips, quick redial, no-match actions), DialpadNumberDisplay (measuring text, auto-shrink, middle truncation, backspace), and DialpadKeypadSections (key grid + call button row). Every moved piece kept its code verbatim; only private became internal and section state became parameters.

## [0.11.44] - 2026-10-27

- File splits, no behavior changes: AnswerControls is now AnswerControls + Classic/Slider/Swipe/Liquid answer files, and CallBackgroundScreen is now the screen + CallStudioComponents (shared cards/switches) + CallPreviewComponents (preview pieces). Every moved piece kept its code verbatim; only private became internal across files.
- New unit tests: DTMF queue ordering, stop-after-every-tone, tone duration, gap timing, drain-and-restart, and inactive-call drops (virtual time); number formatting for Iranian national/international/partial grouping, USSD passthrough, and sanitize. Bidi wrap itself is a one-line Android framework delegate, so it has no JVM unit test.

## [0.11.43] - 2026-10-27

- Keypad number display: the number now centers in the area left of a reserved 56 dp backspace slot instead of sliding under the backspace, shrinks down to 24 sp, and falls back to a middle-ellipsized line when it still overflows at minimum size.
- Unknown callers now show a person icon instead of "+" (incoming avatar and the ongoing/dialing avatar); the incoming number title caps at 40 sp and shrinks instead of overflowing.
- Slide-to-answer: the Decline circle and label are now centered above the track instead of sitting at the left.
- Edit pill: 6 dp inner padding, and Cancel is now onSurface at 80% instead of accent. History filter chips now use the accent fill with onPrimary text when selected.
- Recents search now hides the "Phone" title header while search is open, so results get the screen. Quick-dial chips now sit under a "Recent" label, wrap names/numbers to two lines, and use the accent phone icon.

## [0.11.42] - 2026-10-27

- Headers capped at their height fraction: Contacts' split top bar (Groups pill + actions) now lives inside the header bottom instead of stacking a second row below it, and the header title centers at 40% of the header height (was 42%). The 22/30/38% size ladder is unchanged, and the floating pill now fades in as the header scrolls off.
- Edit contact now has the same fixed inner top bar as every other inner screen (back cancels, title fixed); the scrolling heading is gone. Call Background no longer double-counts the status inset — its content now pads only the bottom inset like History does. The other converted screens were already single-inset.
- Keypad call button stays vivid green in both themes: the glass layer no longer puts a white blur tint over the solid green. Light keypad keys get a slightly darker fill and a 1 dp 12% black rim so they read as keys on a light background.

## [0.11.41] - 2026-10-27

- Removed the system contact-insert intents: other apps can no longer open the app with INSERT / INSERT_OR_EDIT — the in-app New Contact and Add-to-contact flows already cover both cases with the number prefilled, so the handler and its routing are gone too.
- Names and numbers now wrap correctly in right-to-left layouts everywhere: Recents, Contacts, details (including organization), history and inner-screen titles, the SIM picker number, and keypad search results and quick-dial labels — bidi wrap plus content text direction on every displayed name and number.
- Added manifest queries for the intents the app resolves (SMS, ringtone picker, web links, tel:); the call-settings row already guards its resolveActivity call against failure.
- Debug-log timestamps now use java.time instead of SimpleDateFormat.
- Glass surfaces thin their fill when real blur is active, so the bottom bar finally reads as blurred glass instead of flat gray; the near-opaque fill stays as the fallback below API 31 and when transparency is reduced.

## [0.11.40] - 2026-10-27

- In-call keypad (IVR menus) fixed: digits now go through one serialized DTMF queue — each tone plays ~150 ms, stops, then an ~80 ms gap — so fast taps all land instead of only the first digit. Tones only play while the call is ACTIVE; the queue drains and the tone stops when the call ends, goes on hold, the keypad hides, or the call screen stops.
- The digits typed in a call now show above the in-call keys (auto-shrinking, with a Clear button), and the keypad block sits 12 dp below the timer.
- New Call Studio setting: Dialpad tones (default off) plays quiet local key tones during a call.
- Post-dial: numbers with ';' now ask with a glass dialog ("Send these tones? <remaining>", Send/Cancel); ',' pauses still continue automatically.

## [0.11.39] - 2026-10-26

- Keypad no-match actions are now three equal-width vertical chips (icon on top, label below, up to two lines, 64 dp high): New contact, Add to contact, Message. Nothing truncates at font scale 1.3 on 360 dp or in Persian.
- Edit/new contact pill: Save is now a filled accent pill with onPrimary text, Cancel stays a plain text button; both 48 dp high and a third of the pill wide. Save still shows its inline progress while saving, and the pill still sits above the keyboard.

## [0.11.38] - 2026-10-25

- One shared inner top row (status inset + 8dp, 48dp high, back arrow + title) replaces the detail overlay bar and the 64dp app bars on: contact details (back row above the card, name appears in the row once the header card scrolls away), History, Answer style, Call background, Blocked numbers, Duplicates, and Setup (title only, no back). The History gap is gone: Scaffold was adding the full status inset on top of its own row.
- History: calls grouped by day (Today/Yesterday/date headers) in rounded cards, filter chips (All, Missed, Incoming, Outgoing), and each row's title is now the call type in its color (Incoming green, Outgoing accent, Missed red) with date/time + duration + SIM below. Paging is end-based so it keeps working under a filter and with grouped rows.
- Detail History preview rows use the same new row style.

## [0.11.37] - 2026-10-25

- Real glass blur: the bottom bar, header pill, details action bar and edit pill now blur what is behind them (Haze 1.5.3, verified hazeSource/hazeEffect API — the 2.x line's new API was deliberately not used). One blur state at the root, the navigation content as the source. Blur radius 20/28/36 dp by Glass intensity, tint dark #1C1C1E at 55% / light white at 60%, 1dp vertical rim (white 35% top to 5% bottom). API < 31 and Reduce transparency fall back to a 92% opaque fill, never just translucent. Note: the spec's outer-only soft shadow is omitted on purpose — a hardware shadow under a translucent fill shows through as a dark rectangle, and blur + tint now carry the separation.
- Glass intensity still drives the tint/blur strength; Reduce transparency still forces the solid fallback.

## [0.11.36] - 2026-10-25

- Answer style page rebuilt: tiles now render the real controls in a fixed 360x220 design frame scaled to the tile (no more empty or cropped previews), with the style name and animated selection ring. Try-it shows a sample caller (avatar, name, number) and a green/red Answered/Declined chip for 1200 ms, then resets the controls to idle so you can try again.
- Liquid: knob stretch clamped to 1.1x, knob clipped to the track, commit lands at the track end minus padding, and the opposite hint fades out as its side tints (no Decline label over a green track).
- Swipe buttons: chevron hints hide while dragging, and a new drag cancels the previous spring-back so the button returns or flies away cleanly.

## [0.11.35] - 2026-10-25

- Empty-keypad quick dial: when the input is empty, the keypad shows chips for the last 3 distinct recent numbers (name when known, else the number). One tap redials through the normal SIM-aware call path. Nothing shows when call-log permission is missing.

## [0.11.34] - 2026-10-25

- Open from other apps: tapping a phone number and choosing this app now fills the keypad with the number (cold start and already-running, via onCreate + onNewIntent reading the tel: intent, %2B/%23 decoded, +, * and # kept). No auto-call — you press the call button. A bare dial action just opens an empty keypad.

## [0.11.33] - 2026-10-23

- Bottom bar shrink rewritten: it now reacts to post-scroll consumed deltas only, with a directional accumulator — shrink after ~48 dp of downward scroll, expand after ~12 dp upward, expand at the top of the list and after ~700 ms without scroll, and reset on every destination change. It never shrinks on the keypad, and labels follow the labels setting whenever the bar is expanded.
- New bottom scrim behind the Recents and Contacts lists: a 56 dp gradient fading to the page background at 55% alpha, drawn behind the list so it never tints the cards.
- Page headers: the shared header height minimum is now 220 dp (Compact 22% / Standard 30% / Large 38% of the screen), driving both Recents and Contacts.
- Bidi fix: contact names and numbers are unicode-wrapped and rendered with content-based text direction in Recents, Contacts, contact details, both call screens, and call notifications — Persian/Arabic names with parentheses or adjacent numbers no longer reorder.

## [0.11.32] - 2026-10-23

- Edit / New contact: the glass top bar is gone — the title is a large heading that scrolls with the form, and Cancel / Save live in a floating rounded glass pill at the bottom (~75% width, 60 dp, above the keyboard and navigation bar, press animation; Save shows inline progress). The form's bottom padding clears the pill.
- Contact details: the camera badge is removed (photo editing lives in Edit). The header card is a row — name (28 sp bold, up to 2 lines) with the number under it on the left, a ~72 dp round avatar on the right, and the Call / Message buttons below inside the card.
- Details content now starts 8 dp under the status bar (no more ~110 dp of dead space), and the bottom clearance (bar + navigation inset + 24 dp + scrim height) keeps the History card fully visible above the floating bar.

## [0.11.31] - 2026-10-23

- Call background page: the Incoming/Ongoing switch is now a glass segmented control — a rounded glass track with one sliding accent pill (spring, gated by the Animations setting) and onPrimary selected text, no square edges.
- The floating pill shown when the preview scrolls off is now labeled "Preview" (with its live thumbnail), and the page's bottom padding is tall enough that the pill never covers the last settings row.

## [0.11.30] - 2026-10-23

- Glass surfaces no longer draw any shadow: the outer shadow is removed from the liquid-glass treatment and the glass surface (sheen + rim only). Flat solid surfaces keep their soft shadows.
- Ongoing call screen: the caller identity is now an avatar (contact photo, or gradient initials, ~96 dp, shape/size from the avatar settings) above the name, formatted number, SIM label, and timer. With the centered caller-info position the block sits at ~30% of the screen height, and the controls stay in the lower third.
- The call model now carries the contact photo and SIM label, resolved in the background from the contact index and the call's phone account.
- Removed the decorative equalizer bars next to the call timer (they showed no real data).

## [0.11.29] - 2026-10-23

- Settings > Calls has a new "Answer style" page above Call background: four live mini-previews of the real answer controls (Classic, Slider, Swipe buttons, Liquid) with an animated selection ring — tapping a tile applies the style.
- "Try it" on the same page renders the selected style at full size over the call background and reports Answered / Declined without a real call.
- "Slider only when locked" switch appears when the Slider style is selected.
- The Call background page preview now uses the selected answer style.

## [0.11.28] - 2026-10-23

- New answer-style engine: one AnswerControls component renders the incoming-call answer/decline controls in four styles — Classic, Slider, Swipe buttons, and Liquid — all honoring the Animations setting and exposing Answer/Decline to TalkBack as custom actions.
- Classic: button-only halos (1→1.25 scale, 400 ms offset between the two), labels 12 dp below, and the answer handset wiggles every 3 seconds. Slider: glass track with a growing green fill and shimmer label, commits at 85%, plus a separate decline circle. Swipe buttons: drag either round button in any direction (glow, scale, haptic at the threshold; commit at 96 dp or on a fast fling) with sequenced chevron hints. Liquid: one glass pill with a stretchy center knob, drag right to answer or left to decline, committing at 70%.
- The chosen style is stored as a new setting; the old answer gesture migrates automatically (Tap → Classic, Swipe → Swipe buttons). A "slider only when locked" option (on by default) falls back to Classic while the phone is unlocked. The Settings UI for picking a style arrives in the next version.
- Incoming avatar pulse rings now match the spec exactly (scale 1→1.35, alpha 0.22→0, 2.4 s) and stay behind the avatar; the flat quick-reply chips got the same press animation as the glass ones.

## [0.11.27] - 2026-10-23

- Keypad suggestions are de-duplicated: the same contact, or two entries with the same name and number, show as one row with an "N contacts" hint.
- Keypad no-match actions are now three single-line chips: Create new contact, Add to a contact (the in-app picker from 0.11.26), and Message — labels ellipsize instead of wrapping.
- The call button is a vivid solid green with a white handset in both themes, so it always looks enabled.

## [0.11.26] - 2026-10-23

- Saving a number now happens fully inside the app — the system contact chooser is gone. Recents "Add to contacts" offers Create new contact (the app's editor, number prefilled as a Mobile row, cursor in Name) or Add to existing contact (an in-app picker with the same search and alphabetical list as Contacts; the chosen contact's editor opens with the number appended as a new phone row — existing data is never replaced — and saving returns to the list, which refreshes in place).
- The app is now also a target for other apps: system Insert / Insert-or-edit contact intents open the same in-app screens, prefilled with the intent's name, phone and email, or appending the number to the targeted contact.

## [0.11.25] - 2026-10-23

- The call screen now runs in its own task (singleInstance, excluded from Recents): when a call arrives while you are using the app and you decline it — from the banner or the call screen — you stay on the exact screen you were on, with the typed number, scroll position and open contact untouched. Decline and hang-up paths only reject/disconnect the call and clear its notifications.
- Incoming presentation unchanged in behavior and now guaranteed by the task split: Pop-up shows the heads-up banner only, Full screen opens the call screen.
- New Settings > About > Debug log: PhoneNav lifecycle entries (MainActivity and CallActivity create/new-intent/start/stop/destroy with task id, plus every answer/reject/disconnect) are kept in-app and viewable without a computer.

## [0.11.24] - 2026-10-22

- Keypad contact suggestions: contact names now use the theme's on-surface color explicitly — they were invisible (black on black) in dark mode.

## [0.11.23] - 2026-10-21

- Keypad contact suggestions are now full-width list rows (contact photo or initial avatar, name, and the number with the dialed digits highlighted) instead of horizontal chips: up to three matches show at once, and an "N more..." row expands the rest in place.

## [0.11.22] - 2026-10-19

- Settings: the call-studio and call-background strings, the About update-check strings and the Recents duration/missed strings are now in the language files (English + Persian), instead of hardcoded English in the UI code.
- SettingsScreen split into focused files (main, components, dialog state, dialogs, section groups), ContactDetailScreen split into content, rows and bottom bar, and PhoneInCallService split into state, notifications and notification builders — every file under 400 lines. The Settings switch and the call-background switch are now one shared component.

## [0.11.21] - 2026-10-19

- Settings > Liquid glass: new Glass intensity (Subtle / Standard / Strong) scaling the glass effect, Reduce transparency making glass surfaces more opaque, and Glass cards (off by default) putting the Contacts and Recents list cards in liquid glass.

## [0.10.31] - 2026-10-16

- Liquid glass across the app (3): dialpad keys are now circular liquid-glass discs (no more square gray ripple; the key scales to 0.90 and springs back), the Call button is a glossy liquid-glass emerald button, the no-match Create new contact / Message actions are glass pills that animate in/out, and the T9 suggestion chips are glass chips showing the contact name and the matched number with the dialed digits highlighted in accent. Key press feedback is now the glass surface scale (the digit-dip pulse is gone).

## [0.10.30] - 2026-10-16

- Liquid glass across the app (2): the bottom bar's selected-tab indicator is now a sliding liquid-glass droplet (accent-tinted glass, specular rim) instead of a flat oval, and a smooth vertical fade scrim behind the floating bottom bar fades list content out before it can bleed through the bar's tab labels/icons. Glass fill/rim/shadow alphas now match the design-system reference values in both themes.

## [0.10.29] - 2026-10-16

- True liquid glass design system in core/design: a `liquidGlass` modifier (translucent multi-stop specular gradient fill, 1.dp diagonal refractive rim border, tinted elevation/glow shadow) plus `LiquidGlassCard` (Column content, so multi-child content stacks instead of overlapping), `LiquidGlassPill`, `LiquidGlassIconButton`, `LiquidGlassChip`, `LiquidGlassGemButton` (saturated Answer/Decline/End Call buttons), and `LiquidGlassPopupSurface`. Works in Light and Dark themes and over the call background.

## [0.10.28] - 2026-10-16

- Reverted liquid glass from the Recents and Contacts rows/cards: the glass surface lays content out in a Box, so multi-row cards stacked their rows on top of each other. Rows and cards are flat cards again. Liquid glass stays only on the bottom bar.

## [0.10.27] - 2026-10-16

- Liquid glass (1): the Recents day cards and the Contacts list cards — contact rows, group cards, favorites hint, profile row, duplicates card, groups row, permission card, empty state — now use the shared liquid-glass surface (translucent fill, gradient border, soft shadow) instead of flat cards. Dense rows inside a glass card stay flat.

## [0.10.26] - 2026-10-16

- Fixed the call background studio preview showing nothing: the scrollable options list measured unbounded and took all the height, squeezing the preview to zero. The preview and the options now split the screen evenly, so the live preview (and its Full screen button) is visible.

## [0.10.25] - 2026-10-16

- Full-screen preview in the call background studio: a Full screen button on the live preview shows the real incoming or ongoing call screen edge to edge with a sample caller, exactly as it looks on a call; Back or the close button returns to the studio.

## [0.10.24] - 2026-10-16

- Call screen customisation (13): quick replies position — a switch in the call background studio puts the quick-reply chips below the answer buttons on the incoming call screen (above by default).

## [0.10.23] - 2026-10-16

- Call screen customisation (12): avatar shape — Circle (default) or Rounded for the incoming-call avatar, in the call background studio.

## [0.10.22] - 2026-10-16

- Call screen customisation (11): avatar size — Small, Medium (default), or Large for the caller avatar and its initial on the incoming call screen, in the call background studio.

## [0.10.21] - 2026-10-16

- Call screen customisation (10): control button size — Small, Medium (default), or Large for the ongoing-call buttons; and keypad key size — Small, Medium (default), or Large for the in-call keypad keys and their digits/letters. Both in the call background studio.

## [0.10.20] - 2026-10-16

- Call screen customisation (9): answer button size — Small, Medium (default), or Large for the answer/decline buttons and the swipe bar thumb/icons on the incoming call screen; and a Quick replies switch to show or hide the quick-reply chips. Both in the call background studio.

## [0.10.19] - 2026-10-16

- Call screen customisation (8): control grid alignment — Spread (default), Center, Start, or End for the ongoing-call buttons in each row; and end call size — Small, Medium (default), or Large for the red end-call button. Both in the call background studio.

## [0.10.18] - 2026-10-16

- Call screen customisation (7): caller info position now also applies to the incoming call screen — Top (default) or Center for the caller name/avatar, above the quick responses.

## [0.10.17] - 2026-10-16

- Call screen customisation (6): caller info position — Top (default) or Center for the name/number on the ongoing call screen, in the call background studio. The incoming call screen follows in the next update.

## [0.10.16] - 2026-10-16

- Call background studio code split into two files (screen shell + option sections), no behavior change. This keeps each file small enough to ship, so more call-screen options can follow.

## [0.10.15] - 2026-10-16

- Call screen customisation (5): layout presets in the call background studio — Classic, Left-handed, and Swipe set the answer gesture, answer side, and end-call position together in one tap. Any manual change after a preset shows no preset selected (custom).

## [0.10.14] - 2026-10-16

- Call screen customisation (4): end call position — Left, Center (default) or Right for the red end-call button on the ongoing call screen, in the call background studio.

## [0.10.13] - 2026-10-16

- Call screen customisation (3): answer side — Answer on right (default) or Answer on left, in the call background studio. Swaps the answer/decline buttons, and mirrors the swipe bar (swipe left to answer) in swipe mode.

## [0.10.12] - 2026-10-16

- Call screen customisation (2): ongoing-call buttons (Mute, Speaker, Bluetooth, Keypad, Hold) can be shown/hidden and reordered in the call background studio; the ongoing call screen renders them in that order, 3 per row. End call stays always visible.

## [0.10.11] - 2026-10-16

- Call screen customisation (1): answer gesture choice in the call background studio — Tap buttons (default) or Swipe (drag the thumb right to answer, left to decline). Applies to real incoming calls and the studio preview.

## [0.10.10] - 2026-10-16

- Settings > About: "Check for update" row checks the newest GitHub release (pre-releases included) against the installed version and offers to open the release page when a newer one exists.

## [0.10.9] - 2026-10-16

- Incoming call: pulsing glow rings around the answer (green) and decline (red) buttons; off when Animations is Reduced/Off.

## [0.10.8] - 2026-10-16

- Recents: removed the first-load staggered reveal animation (rows appear instantly); list placement animation kept.

## [0.10.7] - 2026-10-16

- Contact details: history section is now just one row with the title; tapping it opens the full contact history screen.
- Contact history rows (details + full history): type icon colors match Recents — incoming green, outgoing blue, missed red, rejected/blocked orange.
- Contact details: bottom action bar is now glass instead of solid.

## [0.10.6] - 2026-10-16

- Call background: 5 new styles (Lavender, Rose, Midnight, Amber, Custom).
- Call background: custom color via hex code (tap the selected Custom swatch to edit it), applied to real incoming and ongoing calls and to the preview.
- Call background: dim range extended from 0-60% to 0-100%.
- Call background: style swatch row scrolls horizontally so all 11 styles fit.

## [0.10.5] - 2026-10-16

### Changed
- Performance: Recents and Contacts list transforms (filter, sort, group) now run only when their inputs change — expanding a row, showing a message, or toggling the duplicates card no longer re-sorts the whole list. Recents date filter uses one epoch-millis range instead of a per-row date conversion, and name matching avoids a per-row lowercase copy. The contacts whitespace regex is compiled once instead of twice per contact per transform.

## [0.10.4] - 2026-10-16

### Changed
- Settings: all hardcoded row, dialog, and accent-picker sizes moved to design tokens. The custom switch thumb slide now respects the Animations setting (On = spring, Reduced = short tween, Off = instant).

## [0.10.3] - 2026-10-16

### Changed
- Ongoing call screen: all hardcoded sizes (caller info, control grid, end-call button, DTMF keypad) moved to design tokens. Dual-SIM picker row sizes moved to design tokens too.

### Fixed
- In-call DTMF keypad keys now play the light haptic like every other call button.

## [0.10.2] - 2026-10-16

### Changed
- Incoming call screen: avatar size, title size, header/action padding, chip spacing, and answer/decline button and icon sizes moved to design tokens.

## [0.10.1] - 2026-10-16

### Changed
- Edit/New contact: the photo badge now uses the shared badge-elevation design token, matching the contact-details badge.

## [0.10.0] - 2026-10-16

### Changed
- History: rows now use the same first-load reveal (first 8 stagger in) and item animation as Recents and Contacts; later paged rows appear immediately.

## [0.9.9] - 2026-10-16

### Changed
- Contact details: phone and email dividers are now subtle (8% alpha) like the history section on the same screen. Top-bar height and photo-badge elevation moved to design tokens.

## [0.9.8] - 2026-10-15

### Changed
- Contacts: group-card dividers are now subtle (8% alpha) like Recents and History. Favorite-card avatar size, padding, and avatar-name gap moved to design tokens.

## [0.9.7] - 2026-10-15

### Changed
- Recents: expanded call details now respect the Animations setting (On = expand + fade, Reduced = fade only at ~60% duration, Off = instant).

### Fixed
- Keypad: long-pressing backspace no longer pastes from the clipboard before deleting (the backspace press is consumed, so the number row's long-press paste never fires).

## [0.9.6] - 2026-10-15

### Changed
- Keypad: T9 matches now show as suggestion chips above the keypad (tap selects the contact). The number row and backspace are always rendered, so typing the first digit never shifts the layout; backspace is dimmed and inert when empty. Each keypress plays one digit dip animation (0.1 scale, 0.3 alpha, spring stiffness 1000 / damping 0.7).

## [0.9.5] - 2026-10-15

### Changed
- Call background page is now a studio: live full-screen preview of the real incoming or ongoing call screen with a sample caller, background swatches, a dim slider that previews live, and a show-number switch. Everything saves immediately.

## [0.9.4] - 2026-10-15

### Added
- Call background plumbing: 3 new gradients (Sunset, Ocean, Forest), background dim (0-60%) and show-number settings, applied to real incoming and ongoing calls. The studio page with live preview arrives in 0.9.5.

## [0.9.3] - 2026-10-15

### Fixed
- Settings: Call background and Block numbers rows did nothing — their pages composed behind the settings list. They now draw on top, and system Back closes the open page instead of leaving Settings.

## [0.9.2] - 2026-10-15

### Added
- Bottom bar settings UI in Settings: style (Glass/Solid), size (S/M/L), and Shrink on scroll rows. Scrolling down compacts the bar to icons only (labels fade and collapse, height shrinks 12dp); scrolling up restores it.

## [0.9.1] - 2026-10-15

### Added
- Bottom bar settings plumbing: style (Glass/Solid), size (S/M/L), and Shrink on scroll are stored and read by the bar (defaults Glass/Medium/off). The Settings rows to change them arrive in 0.9.2.

## [0.9.0] - 2026-10-15

### Changed
- Bottom bar: one shared selection pill now slides between tabs with a Bouncy spring and stretches ~14dp mid-flight. Icons cross-fade outline to filled with a 1 to 1.15 to 1 scale pop; labels cross-fade weight and color. The bar is real glass, and it slides + fades in and out when entering or leaving inner screens, and slides up on app start.

## [0.8.9] - 2026-10-15

### Changed
- Headers: Recents and Contacts now have an expanded header (~38% of the screen, min 260dp) with the title centered at ~42% of its height and the action row right-aligned at the bottom. When the icon row scrolls off, the actions reappear in a floating glass pill at the top-right, fading and scaling in from the scroll offset. The old pinned bar is gone.

## [0.8.8] - 2026-10-15

### Fixed
- Recents reveal lag: press-scale and first-load reveal now read the animated value inside graphicsLayer instead of during composition, so list rows no longer recompose on every animation frame.

## [0.8.7] - 2026-10-15

### Changed
- Shared style: cards use 28dp corners, light page is #F6F6F8, dark keypad page is #161618. GlassSurface now uses the spec fill (light white 72%, dark #2C2C2E 60%), a 1dp vertical-gradient border (white 35% to 5%), and a soft 10% shadow. Real Haze blur stays out: its 2.x API is unverified against this Compose version, so the surface remains honestly translucent.

## [0.8.6] - 2026-10-15

### Changed
- Motion: Recents and Contacts lists now animate item placement, and the first 8 rows reveal with a 25ms stagger on first load (fade + slide on full motion, fade only on reduced, instant on off).

## [0.8.5] - 2026-10-15

### Changed
- Settings: rows, choice-dialog options, accent-color circles, and SIM picker rows now press-scale to 0.92 with ripple and a light haptic. Switches and the slider keep their own slide/drag feedback.
- Performance: recents grouping no longer builds a SimpleDateFormat per group or parses the group number per call. Day headers use two shared thread-safe formatters, day comparison uses LocalDate, and the group number is normalized once per group.

## [0.8.4] - 2026-10-15

### Changed
- Settings: call background is now a full page instead of a dialog. Every option shows a live preview of the real call background with a sample caller, and tapping an option saves it immediately.

## [0.8.3] - 2026-10-15

### Changed
- Call screens (ongoing): control buttons (mute, speaker, bluetooth, keypad, hold), end-call button, and DTMF keys now press-scale to 0.92 with ripple; controls and end-call also give a light haptic. DTMF keys skip the haptic because the tone itself is the feedback.

## [0.8.2] - 2026-10-15

### Changed
- Call screens (incoming): answer and decline buttons now press-scale to 0.92 with ripple and a light haptic on tap (same size, shape, and colors). Dual-SIM picker rows now press-scale with ripple and a light haptic. Quick-reply chips and dialog buttons unchanged.

## [0.8.1] - 2026-10-15

### Changed
- Edit/New contact: avatar and ringtone row now press-scale to 0.92 with ripple and a light haptic on tap. Fields, buttons, and dropdowns unchanged.

## [0.8.0] - 2026-10-15

### Changed
- History: call-history dividers (detail preview and full history screen) now use the same subtle 8% outline alpha as Recents, instead of full-strength outline. Rows are display-only, so no press behavior was added.

## [0.7.9] - 2026-10-15

### Changed
- Contact details: avatar, action buttons, ringtone row, calling-SIM row, and bottom-bar actions now press-scale to 0.92 with ripple and a light haptic on tap. Layout and behavior unchanged.

## [0.7.8] - 2026-10-15

### Changed
- Contacts: rows, favorites cards, profile row, groups row, and favorites hint now press-scale to 0.92 with ripple and a light haptic on tap. Layout and behavior unchanged.

## [0.7.7] - 2026-10-15

### Changed
- Recents: rows now press-scale to 0.92 with ripple (shared press source on the row click) and swipe-to-call / swipe-to-message confirms with a light haptic. Row layout, swipe actions, and expanded details unchanged.

## [0.7.6] - 2026-10-15

### Changed
- Keypad: keys now press-scale to 0.92 with ripple (combinedClickable + shared pressScale, same tap/long-press behavior) and every keypad tap uses the shared light-haptic helper. The two local View-based haptic helpers are gone.

## [0.7.5] - 2026-10-15

### Changed
- Bottom bar tabs now press-scale to 0.92 with ripple and a light haptic on switch, using the shared Motion pressScale and haptic helper (press-scale is off under Reduced/Off, haptic follows the system setting). Bar colors and layout unchanged.

## [0.7.4] - 2026-10-15

### Changed
- Headers (Recents, Contacts): the large header is now a compact fixed 144dp with the title and subtitle start-aligned at the bottom, instead of a centered title in a 30%-of-screen hero area. The pinned bar title is start-aligned with end padding reserved for the action icons, so it can no longer run under them on narrow screens. The bar fade still completes over the header scroll distance.

## [0.7.3] - 2026-10-15

### Fixed
- Recents "open contact" now actually opens that contact's detail: the contact id was dropped and the tap only landed on the Contacts list. The id is now passed to the Contacts tab (same pending-value pattern as the keypad-to-editor flow) and the navigation uses single-top with saved state, so it no longer stacks a duplicate Contacts destination.

## [0.7.2] - 2026-10-15

### Added
- Theme-aware glass: GlassSurface now defaults to the current theme (white tints on dark, dark tints on light) instead of always using the dark set, with explicit colors still available for surfaces over a non-theme background such as the call background. New light-theme glass tokens, a GlassColors set with a `glassColors(dark)` resolver, and `isDarkGlassTheme()` (judged by theme background luminance, so it follows the ThemeMode setting, pure black mode, and dynamic color). Later sections adopt the glass surface.

## [0.7.1] - 2026-10-15

### Added
- Motion tokens in core/design: duration tokens (Micro 100ms, Short 180, Medium 280, Long 420), easing tokens (Standard, Emphasized, Exit), spring tokens (Snappy, Bouncy), plus option-aware helpers (scaled duration, tween spec, first-load stagger). New `pressScale` (press to 0.92, ON only), `PulseRings` (expanding rings, ON only), and a light-haptic helper that follows the system haptics setting. Existing Motion behavior is unchanged; screens adopt the tokens in the following versions.

## [0.7.0] - 2026-10-14

### Added
- Reusable `GlassSurface` in core/design: translucent fill + thin translucent border, configurable shape and colors. Ships as the translucent fallback — Haze was checked first (2.0.1 latest) but its new 2.x API is unverified against this project's Compose version, so no real blur yet. Replaces the unused `FrostedGlassCard`.

### Fixed
- Build: AGP 9.1.1 → 9.3.1 with Gradle 9.5.0 (fixes the config-time "kotlin.sourceSets DSL not allowed with built-in Kotlin" error from KSP registering its generated dirs; Kotlin 2.2.10 and KSP 2.2.10-2.0.2 unchanged).
- Build: fixed the version catalog parse failure in CI — four files (version catalog, Gradle properties, changelog, GlassSurface source) were stored base64-encoded in git and are now plain text again.

## [0.6.21] - 2026-10-14

### Added
- Recents date filter: "Pick a date" in the filter menu opens a date picker; the list shows only calls on that day, with a chip showing the date (tap to pick another, X to clear). Combines with the type filter and text search.

## [0.6.20] - 2026-10-14

### Added
- Default calling SIM picker in Settings > Calls: Ask every time / SIM 1 / SIM 2, wired to the 0.6.17 storage and the 0.6.18 / 0.6.19 call sites.

## [0.6.19] - 2026-10-14

### Changed
- Default calling SIM now applies when placing calls from contact details. Settings UI ships in 0.6.20.

## [0.6.18] - 2026-10-14

### Changed
- Default calling SIM now applies when placing calls from the keypad and Recents. Contact details ships in 0.6.19, Settings UI in 0.6.20.

## [0.6.17] - 2026-10-14

### Added
- Default calling SIM (storage + SIM resolution): Ask every time / SIM 1 / SIM 2, stored in DataStore. `SimCallHelper` honors it after the per-contact saved SIM. Callers and Settings UI ship in 0.6.18 / 0.6.19.

## [0.6.16] - 2026-10-14

### Changed
- Call background setting now applies to the real call screens: Default keeps the current gradient, Dark is solid black, Accent is a gradient of the chosen accent darkened so the white call UI stays legible.

## [0.6.15] - 2026-10-14

### Added
- Keypad no-match actions: when a dialed number matches no contact, the keypad shows "No matching contacts" with Create new contact and Message actions. Create opens the contact editor prefilled with the number; Message opens the messaging app for the number.

## [0.6.14] - 2026-10-14

### Removed
- Keypad voicemail: the voicemail glyph on the 1 key and long-press 1 calling voicemail are gone; long-press 1 now just enters "1" like a tap.

### Fixed
- Keypad keys without a sublabel (1, *, #) now keep the same height as the lettered keys — the sublabel is always emitted, even when empty — so all digits stay aligned.

## [0.6.13] - 2026-10-14

### Added
- Call background picker in Settings > Calls: Default / Dark / Accent, wired to the 0.6.12 storage. Not applied to the call screens yet — that ships in 0.7.2.

## [0.6.12] - 2026-10-14

### Added
- Call background setting (storage): Default / Dark / Accent, stored in DataStore with a `SettingsRepository` setter. Settings UI ships in 0.6.13; applying it to the call screens ships in 0.7.2.

## [0.6.11] - 2026-10-14

### Changed
- Call-screen animations through `Motion`: the incoming screen fades + slides in, answering transitions incoming -> ongoing with the same slide + fade, and ending a call fades + slides the screen out before the activity finishes (the finish now waits out the Motion exit duration instead of a fixed 600ms). Reduced fades only, Off swaps instantly.

## [0.6.10] - 2026-10-14

### Changed
- Inner-screen transitions through `Motion`: pushed screens (Settings, Setup, contact detail/edit/create, groups, history) slide + fade (200ms) on On, fade only (100ms) on Reduced, and swap instantly on Off. The Contacts sub-screens slide by navigation depth (forward in from the right, back in from the left, same level fades).

## [0.6.9] - 2026-10-14

### Changed
- Animations setting now drives bottom-bar and tab motion through a single `Motion` controller in `core/design`: On = tab fade 120ms + pill color 300ms, Reduced = tab fade 60ms + pill color 100ms, Off = no tab transition + instant pill color snap. Previously Reduced behaved like On and the bottom-bar pill ignored the setting.

## [0.6.8] - 2026-10-14

### Added
- Baseline Profile: hand-written `baseline-prof.txt` covering app startup (Application, MainActivity, container, notification channels, contact index, call-log preload), the first-screen ViewModels and data flows, and the call screens. Added `androidx.profileinstaller` 1.4.1, which installs the shipped profile on first launch via its App Startup initializer.

## [0.6.7] - 2026-10-14

### Changed
- Performance: Recents and Contacts list transforms (filter, group, sort, name formatting) now run on Dispatchers.Default instead of the main thread. Avatars cache their gradient brush and Coil request, decoding photos at the displayed pixel size. List Cards render with no shadow. Recents rows share one time formatter instead of one per row.

## [0.6.6] - 2026-10-14

### Fixed
- Crash opening a contact's History: the call-log provider does not honor the paging offset on all devices, so the paged query re-read the same rows and returned duplicate entries, crashing the list on duplicate keys. Paging now uses a growing limit with in-memory windowing instead of an offset, and appended pages are deduplicated by call id.

## [0.6.5] - 2026-10-14

### Changed
- Contact History screen now pages 50 calls at a time (ContentResolver limit/offset, newest first), matching all of the contact's numbers with normalized matching. The details History button is now labeled Show all.

## [0.6.4] - 2026-10-14

### Changed
- Contact details show only the last 5 calls, with a History button opening a separate History screen that renders the full history in a LazyColumn.

## [0.6.3] - 2026-10-14

### Changed
- Recents rows: icon color by call type (incoming green, outgoing accent blue, missed red, rejected/blocked orange); only a missed name is red. Row is a colored arrow, name, subtitle "Mobile · SIM 1" (SIM only on dual-SIM phones), time on the right. Type and duration moved into the expanded card. 64dp rows, lighter dividers, softer date headers.

## [0.6.2] - 2026-10-14

### Fixed
- Scroll jump in Recents and Contacts: the large header is now the first list item with fixed height and scrolls away naturally, and the small pinned top bar fades in via graphicsLayer alpha from the scroll offset. No snap, no scroll-driven size change.

## [0.6.1] - 2026-10-14

### Fixed
- Keypad display: number formats live while typing with the same grouping (0912 358 8236, 021 9120 0150, +98 912 358 8236), regular weight, 40sp, centered, auto-shrinks on overflow, backspace at the right of the number. Raw digits stay the source of truth.

## [0.6.0] - 2026-10-14

### Fixed
- One number formatter everywhere: region is the SIM/network country (fallback IR), never the device locale. Iran mobiles render 0912 358 8236, landlines 021 9120 0150 / 013 3322 3782, +98 forms as +98 912 358 8236 / +98 21 9120 0150. Other countries format with their own country, never US as default. Short codes and USSD are not grouped. Recents, details, history, call screens and notifications all use it.

## [0.5.15] - 2026-10-14

### Changed
- Recents expanded call details no longer repeat the status: the subtitle already shows type, duration and SIM, so the extra status/duration row is gone.
- Expanded call actions are now circular buttons (call, message, contact, block) matching the reference layout.

## [0.5.14] - 2026-10-14

### Added
- Calls settings now has a "Quick decline messages" row that edits the three reply chips on the incoming call screen. Clearing a message restores its built-in default.

## [0.5.13] - 2026-10-14

### Changed
- Incoming-call quick replies now resolve from the stored quick-decline setting (built-in defaults until edited). The Settings editor to change the three messages lands in 0.5.14.
- Declining an incoming call really rejects it through the telecom stack, with or without a reply message, from both the full-screen call screen and the incoming-call notification.

## [0.5.12] - 2026-10-14

### Added
- Calls settings now has an "Incoming call display" row: Full screen (default) or Pop-up, using the call-display setting already stored since 0.5.5. The choice takes effect for the next incoming call.

## [0.5.11] - 2026-10-14

### Changed
- Call waiting dialog redone: title, description, three full-width actions (Activate, Deactivate, Check status), Cancel bottom-right.
- Call waiting codes (*43#, #43#, *#43#) now run on the chosen SIM via TelephonyManager USSD: dual-SIM phones pick the SIM first, and the carrier response or error is shown in a result dialog. When USSD cannot start, the code falls back to dialing it.
- The Supplementary services row is hidden when no system call-settings screen handles it, instead of doing nothing when tapped.

## [0.5.10] - 2026-10-14

### Changed
- Main bottom bar is now 60% of the screen width (was capped at 250dp / 85%). Height stays 60dp, three equal-width items, selected pill with 4dp inset.
- Bottom bar labels stay on by default and only disappear when the "Bottom bar labels" setting is turned off.

## [0.5.9] - 2026-10-14

### Fixed
- Contact details top bar: status-bar inset + 56dp, transparent at the top, solid page-color bar with the name once the header card scrolls away. Content scrolls underneath with no blank gap.
- Contact details bottom action bar is now 70% of the screen width, and the content clears it (bar height + gap + 16dp), so the last History rows are no longer covered.

## [0.5.8] - 2026-10-14

### Fixed
- Same one-format number rendering on the call screen (incoming + ongoing) and contact details (header + phone rows): device-country numbers nationally, all others internationally.

## [0.5.7] - 2026-10-14

### Fixed
- One number format: every number normalizes to E.164 using the device region, Recents groups by that key, and one formatter renders device-country numbers nationally and all others internationally — so the same number never shows in two formats on one screen. Short codes, emergency numbers, USSD codes and partial input are shown as typed. The call screen and contact details get the same rendering in 0.5.8.

## [0.5.6] - 2026-10-14

### Fixed
- Recents names now resolve instantly from a single in-memory contact index (one contacts query, rebuilt when contacts change) instead of a lookup per row. No more raw-number rows while names load: the last resolved list shows instantly on cold start and refreshes silently, or a skeleton on first run. The same index powers incoming-call names. Index build time and first data emit are logged (PhoneBoot).

## [0.5.5] - 2026-10-14

### Added
- Call display setting (Full screen / Pop-up): incoming calls take over the screen (current behavior) or show as a heads-up banner only. Default is Full screen. The Settings row to change it arrives in 0.5.6.

## [0.5.4] - 2026-10-14

### Changed
- Settings and contact details now have the same 8dp gap above the top bar, on top of the status-bar inset, completing the inner-screen top-bar gaps from 0.5.3.

## [0.5.3] - 2026-10-14

### Changed
- Inner screens (duplicates, blocked numbers, edit/new contact) now have an 8dp gap above the top bar, on top of the status-bar inset. Settings and contact details follow in 0.5.4.

## [0.5.2] - 2026-10-14

### Changed
- Edit/New contact: Save and Cancel buttons align to the 20dp content margin.
- Edit/New contact: all text fields are now soft filled tonal fields (tonal surface fill, no outline), including the phone/email type pickers.

## [0.5.1] - 2026-10-14

### Changed
- Contact details redesigned: the avatar, name, organization, and primary number now sit in one top card, with a Call / Message row below it.
- Contact details now show a History section with that contact's calls (type, date and time, duration, SIM), or a short empty message when there are none.
- Contact details now have a floating pill bottom bar: Favorites (star toggle), Edit, Share (as a vCard), More (photo, ringtone, delete). The top-bar edit/overflow buttons moved into the bar.

## [0.5.0] - 2026-10-14

### Changed
- Favorites tiles are calm now: soft muted pastel gradients (same family as the avatar palette) instead of the loud saturated colors. Layout, photos, names, and tap/long-press behavior are unchanged.
- Duplicate Merge button is now a tonal button (theme tonal style) instead of the loud filled accent button. Merging still asks for confirmation first.

## [0.4.9] - 2026-10-14

### Added
- Keypad: long-press 1 calls voicemail using the real carrier voicemail number (TelephonyManager.getVoiceMailNumber), going through the same SIM handling as any other call. Tap 1 still enters "1". If the carrier has no voicemail number configured, long-press 1 does nothing.

### Changed
- Keypad: the 1 key keeps the clean voicemail glyph (two circles and a bar) below the digit, matching the keypad style.

## [0.4.8] - 2026-10-14

### Fixed
- No reload flash on tab switch: Recents, Contacts and favorites lists are cached in the repositories and exposed with stateIn(WhileSubscribed(5000)), so returning to a tab shows the previous list instantly. The loading skeleton and the empty state never appear when cached data exists; skeletons show only on the very first load. Queries do not re-run on every tab switch.
- List ViewModels for Recents and Contacts are activity-scoped, so their state survives tab switches.

## [0.4.7] - 2026-10-14

### Fixed
- Tab-switch flicker: the root layout now paints one opaque theme background, and the window background in the light and dark themes is the same color, so nothing from the window ever shows through during a transition. The keypad no longer uses its own gray: every tab screen uses exactly the theme background.
- Tab transitions: switching between Keypad, Recents and Contacts uses a short 120ms fade-in with no exit animation instead of the default crossfade, so the old screen never turns semi-transparent over a different background. The bottom bar stays outside the navigation host and never animates with the screens. Animation Off means no transition.
- Tab state: switching tabs keeps each tab's saved state (including list scroll position) via saveState/restoreState with launchSingleTop.

## [0.4.6] - 2026-10-14

### Changed
- Bottom bar: fixed 250dp width (capped at 85% of screen width), 60dp height, three equal-width items, selected pill with a 4dp inset, very soft shadow, and a gap of navigation-bar inset + 12dp so it floats above the gesture handle. Nothing overflows or clips.
- Bottom bar icons: keypad is a clean 12-dot grid (4 rows of 3), Recents is a handset with incoming and outgoing arrows. Selected tabs use filled icons, unselected tabs use outlined icons.
- Lists on the Keypad, Recents and Contacts tabs keep clearance of bar height + gap + 16dp above the bar, so the last row is fully reachable.
- Theme: one light background (#F7F8FA) everywhere, including the keypad. Dark: pages #000, cards #1C1C1E, keypad #161618. Bottom bar colors: light #EBEBED with #D9D9DD selected pill, dark #2C2C2E with #48484A selected pill.

## [0.4.5] - 2026-10-14

### Changed
- Duplicates screen now shares the common header bar, card, and divider styling used elsewhere in the app. Duplicate groups are surface cards with dividers, and the merge dialog is a rounded card.
- Permissions setup screen now shares the common card styling. Screen title uses the standard title style, descriptions and warnings are 16sp body text, and checklist spacing and icon sizes use the design tokens.

## [0.4.4] - 2026-10-14

### Changed
- Contact details and edit contact screens now share the common card, avatar, header bar, and divider styling used elsewhere in the app.
- Screen titles on these screens use the standard title style, and body text is 16sp.
- Dialogs on these screens (delete, photo, phone picker) are rounded cards.

## [0.4.3] - 2026-10-14

### Changed
- Settings: cards now use the same color as cards elsewhere (white in light mode), and the header matches the page background.
- Settings: font size slider now has a thin track with tick marks and a vertical-bar thumb.
- Settings: switches are now wide tracks (52x28dp) with a large white round thumb.

## [0.4.2] - 2026-10-14

### Changed
- Contacts: duplicate banner is now a compact centered card with centered text, a Merge pill button, and a circular dismiss button.
- Contacts: "Add your favorite contacts", "My profile", and "Groups" are now pill-shaped cards.
- Contacts: list rows show only the name. Contact avatars use soft pastel gradient circles with a white initial when no photo is set.
- Contacts: dividers inside each letter-section card now start at the text.

## [0.2.5] - 2026-10-13

### Changed
- Settings: flat transparent sections with dividers instead of cards, muted section headers, like the reference list. Same settings and controls.

## [0.2.4] - 2026-10-13

### Changed
- Incoming call screen: warm gradient background, big initial avatar circle, larger caller name, bigger answer/decline circles. Red/green call colors moved to design tokens.

## [0.2.3] - 2026-10-13

### Changed
- Recents: top-right search and menu icons like the reference. Search is collapsible, the menu holds Settings (the gear moved into the menu).
- Contacts: the menu now shows Settings and Find duplicate contacts.

## [0.2.2] - 2026-10-13

### Changed
- Contacts: top-right + / search / menu row like the reference. Search is now collapsible (tap the icon to show/hide), "Find duplicate contacts" moved into the menu. Floating add button removed.
- Bottom navigation: stacked icon-over-label with selected oval highlight, no shadow, like the reference.

## [0.2.1] - 2026-10-13

### Changed
- Recents: each entry now shows call type and duration in the subtitle (e.g. "Incoming • 2m 41s • 5:54 PM • SIM 1"), like the reference.
- Contacts: add-contact moved from the floating button (it overlapped the last row) to a + icon at the top right of the header, like the reference.

## [0.2.0] - 2026-10-13

### Changed
- Contacts: centered "Phone" header with "N contacts with phone numbers" count, favorites as a 3-column grid of gradient cards with avatar and name, pill-shaped contact rows, muted letter headers.
- Recents: centered "Phone" header, flat call rows with dividers instead of cards, muted day headers, empty state vertically centered. Favorites shortcut row removed (favorites live in Contacts).
- Note: profile and groups rows from the reference were not added — the app has no groups feature and no profile plumbing; nonfunctional rows were not added.

## [0.1.27] - 2026-10-13

### Fixed
- Test failure: NavigationRouteTest asserted the old bottom-nav order (Recents, Contacts, Dialpad). Updated to the new order (Keypad, Recents, Contacts) from 0.1.26. No app change.

## [0.1.26] - 2026-10-13

### Changed
- Keypad: bare large digits with letters (no key backgrounds), bigger blue call button with white icon, airier key spacing, and a floating pill bottom bar (selected tab gets a lighter pill). Tab order is now Keypad, Recents, Contacts.
- In-call screen: warm gradient background, large caller name with number and timer, and a control grid with labels: mute, speaker, Bluetooth (only when available), keypad, hold. Speaker and Bluetooth are now direct toggles instead of opening a route picker. End call is a red circle button. The DTMF keypad overlay now shows digits with letters plus speaker, hide, and mute controls.
- Note: no Add call button — multi-call placement is still not implemented, so a nonfunctional button was not added.

## [0.1.25] - 2026-10-13

### Fixed
- Build failure: PhoneInCallService read audioState from the Call object, which has no such property. The audio route now comes from the call model, which is updated from onCallAudioStateChanged. No behavior change.

## [0.1.24] - 2026-10-13

### Fixed
- Screen now turns off when the phone is held to the ear during a call. The in-call service holds a proximity screen-off wake lock while a call is dialing, connecting, or active with audio on the earpiece, and releases it on speaker, Bluetooth, or wired headset, on disconnect, and on service destroy. Added the WAKE_LOCK permission. The lock is only created if the device reports the proximity wake lock level as supported, and release is guarded by isHeld.

## [0.1.23] - 2026-10-13

### Fixed
- Build failure: LiquidGlass.kt imported GenericShape from androidx.compose.ui.graphics, where it does not exist. It lives in androidx.compose.foundation.shape. One import fix; the other 8 errors cascaded from the unresolved reference.

## [0.1.22] - 2026-10-13

### Changed
- In-call screen: liquid-glass redesign. Frosted background with soft dynamic gradients, centered caller info with squircle avatar and translucent border, floating glass dock for mute, keypad, audio route, and hold, and a bottom-anchored pill-shaped red end-call button. All call controls, audio route picker, and DTMF behavior unchanged.
- Removed brand-name references from code, comments, and docs; renamed the accent blue tokens.

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
- Redesigned contact details screen featuring large avatar, action row (Call, Message, Favorite, More), and a clean modern aesthetic.
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
- Interactive Dial Pad with circular digit keys (`1-9`, `*`, `0`, `#`) and letter sublabels.
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
- Full Contacts UI:
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
- Core design tokens: `Color.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`, `Theme.kt`.
- Navigation structure with sealed `Screen` routes and `PhoneBottomBar` featuring Recents, Contacts, and Keypad tabs.
- Initial feature screens with MVVM architecture: `RecentsScreen`, `ContactsScreen`, `DialpadScreen` and their corresponding ViewModels and UiStates.
- Adaptive vector launcher icon with custom telephone handset foreground and custom styling.
- Local unit test coverage verifying navigation destinations and initial ViewModel UiState.
- GitHub Actions CI workflow (`.github/workflows/android-build.yml`) for automated building, signing validation, and release artifact generation.
