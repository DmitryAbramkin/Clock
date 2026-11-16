# Changelog — Clock

**English** · [Русский](changelog.ru.md)

A short history of changes to the app itself. Technical details are in
[PROJECT_NOTES.md](PROJECT_NOTES.md).

## 3.13.0 — 2026-11-16

- Time wheel feedback now also works on phones with a regular vibration motor:
  short low-power pulses imitate haptic ticks, and the pulse rate is capped so
  fast scrolling does not turn into continuous vibration. The settings screen
  explains when haptic hardware is unavailable.
- On phones without a haptic motor, onboarding continues after permissions
  with a live time-wheel demo: it warns that the vibration imitation may feel
  unpleasant and offers a switch to turn it off right away. The screen also
  points to keyboard input in Settings. Phones with a haptic motor go straight
  to the app.
- Secondary text in cards and settings uses tighter line spacing, so long
  captions take less vertical space.

## 3.12.0 — 2026-11-15

- Sound and vibration are grouped in a dedicated section. New alarms and timers
  inherit the global settings until the user changes them manually; changed
  values are stored separately.
- Timers now have their own sound and vibration, like alarms. Reset returns the
  item to the global setting.
- In "Alarm and timer settings", section names and hints were updated;
  "Reliability" became part of "Permissions and background".
- Fixed keyboard time input: invalid values no longer block the fields, focus
  moves on, and the first field gets focus immediately.
- Added a new test app icon: two dials, 10:10 hands and a soft glow. Themed
  icons have a separate monochrome layer.
- Removed icon selection: the app uses a single icon, and the extra screens and
  resources were deleted.
- Updated links and author information in "About". Added a "Licenses" screen
  with the full MIT and Apache 2.0 texts.
- Split the README into English and Russian versions, added screenshots, a
  header icon and a shorter feature overview.
- APKs are now published on GitHub Releases: a versioned build, a stable
  `Clock.apk` link and `SHA256SUMS.txt` checksums.

## 3.11.0 — 2026-11-15

- The stopwatch moved to its own bottom navigation tab. Timer swipes no longer
  conflict with switching to the stopwatch.
- Timers now share the alarm style: a card, a name, progress, reset and pause.
  Swipe to delete, with the shared confirmation setting.
- Paused timers keep their progress line; new and reset timers do not.
- Timer notifications are silent: starting and pausing make no sound and do not
  vibrate. Tapping opens the corresponding timer.
- Added a "Keep screen on" button to the timer page and the stopwatch.
- Bottom navigation works correctly with large system fonts: all four buttons
  stay visible.

## 3.10.0 — 2026-11-14

- Multiple timers can run at the same time: each has its own countdown, name,
  card and notification.
- Added a timer list with progress and a separate `HH:MM:SS` creation screen
  with a name and duplicate checking.
- Tapping a card opens the timer page with a ring, pause, reset and a `+1`
  button.
- Each timer gets its own notification with progress, pause, `+1` and cancel.
- Timers are saved and restored after an app restart and a device reboot.

## 3.9.x — 2026-11-14

- Added a custom wheel time picker: infinite scrolling, a center highlight and
  separate columns for hours, minutes and seconds.
- Keyboard time input is more convenient: autofocus, field-to-field navigation
  and invalid value clamping.
- Added haptic feedback when scrolling the wheels.
- The timer got a separate sound, independent of the alarm sound.

## 3.8.x — 2026-11-12 — 2026-11-14

- The timer now rings when it finishes: with sound, vibration and a "Time's up"
  screen, even if the app was killed by the system.
- Alarms have an optional name: on the card, on the ringing screen and in
  settings.
- The back gesture became smooth: content follows the finger, and screen
  transitions are animated.
- Improved the bottom navigation, the name field and shared UI animations.

## 3.7.x — 2026-11-12

- Added a unified "Permissions and reliability" screen: notifications, exact
  alarms, full-screen notifications, battery optimization, background activity
  and autostart.
- Added a link to dontkillmyapp.com with tips for different ROMs.
- The interface works better with large fonts, tablets and landscape.
- Settings were regrouped, and subpages got a shared pinned header with a
  "Back" button.
- The timer and stopwatch share button sizes and a cleaner look.

## 3.6.0 — 2026-11-11

- Fixed alarms ringing without a notification and ringing screen: the
  notification is now posted before the foreground service starts.
- If full-screen notifications are disabled, the ringing screen opens directly.
- Tapping an alarm notification opens the ringing screen.
- Added diagnostics for system background restrictions and instructions for
  Motorola.

## 3.0–3.5 — 2026-10-27

- Package renamed to `com.olivka.clock`.
- Added skipping the next occurrence of a repeating alarm.
- Improved the widget, bottom navigation, tab and list position restoration.
- Release builds now use R8, resource shrinking and local signing.
- README rewritten for the Clock fork.

## 2.0–2.9 — 2026-10-26

- Removed wake-up challenges, AI/ML, Zen/Invincible and extra permissions.
- Switched to 24-hour time everywhere.
- Added shift schedules, reliable rescheduling and snooze.
- Added English and Russian languages, theme selection and AMOLED.
- Added a 4×2 widget and a test app icon.
- Added duplicate protection, Snooze hiding and a schedule setup screen.

---

History before 2.0 belongs to the upstream project
[Mascot](https://github.com/Hotaro26/Mascot).
