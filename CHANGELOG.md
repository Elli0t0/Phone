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
