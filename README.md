# Dayline — A Live Timer for Your Selected Apps

Dayline is a free, open-source Android app that makes time spent in distracting apps
visible. Choose the apps you want to track, and a small floating timer shows their
combined daily usage while you use them. Navigation, workout apps, and everything
else stay out of the total unless you select them.

Optional limits warn without interrupting you. Dayline never blocks an app or
asks you to extend a timer before continuing.

## Highlights

- One shared daily counter across selected apps, resetting at local midnight.
- A draggable floating timer with adjustable text size and opacity.
- An optional daily budget with amber and red timer colors.
- Searchable app selection with icons and a weekly History graph with combined totals.
- A dark navy and cyan design with an adaptive, themed launcher icon.
- Local settings and history, with no account or network access.

## Requirements and setup

Android 11 or newer. Install the APK, open Dayline, enable Usage access and Display
over other apps, select apps, and press Start tracking. Set a shared daily limit
if you want a warning; zero disables it.

The timer is always visible while Dayline is open, so appearance changes update live.
Outside Dayline it appears only in selected apps and hides when you leave them or lock the screen.
Start tracking is shown only while tracking is off; use the tracking options menu to pause.
Android requires a foreground-service notification while monitoring. It is static
and silent; Android 13+ lets you hide drawer notifications in system settings while
tracking continues. Android still lists the service under active apps.

History shows Monday–Sunday weeks with seven daily bars and a combined weekly total.
Use the arrows to browse previous weeks; existing daily records stay saved.

## Visual reminders

Milestone animations are enabled by default and can be switched off in settings.
One cyan halo marks each tenth of the shared daily budget. From 70%, the timer and
halo turn amber. At the limit, two soft-red halos play and the timer stays red.
Two red halos repeat after every additional five minutes of selected-app usage.
Ordinary reminders are at least two minutes apart; crowded milestones are skipped.
The exact limit takes priority. Without a budget, one cyan halo marks ten-minute
usage intervals.

The capsule fades in and out without moving or changing size. A brief hide delay
bridges app switches without adding tracked time. Rapid returns cancel hiding or
reverse an in-progress fade; screen locking and permission loss hide it immediately.
Turning milestone halos off keeps these fades; Android's system animation setting
still applies.

The timer never grows, pulses, or adds a warning label. Only the surrounding halo
animates; it passes taps through to the app beneath. No sound, vibration, or
blocking is involved. Android's animation settings are respected.

Reminders fire only on usage crossings, with no replay after app switches or
tracker restarts. State resets at local midnight. The Milestone halos switch lives with the timer
appearance controls; turning it on previews the current halo without changing
usage totals or reminder history. Real reminders also play while Dayline is visible
beside a selected app in split-screen.

## Build from source

Install JDK 17, Android SDK Platform 36, Build Tools 36, and platform-tools.
Configure the SDK with `ANDROID_HOME` or `sdk.dir` in untracked `local.properties`.
The wrapper script also recognizes Homebrew Android and JDK installations.

```sh
./scripts/gradle.sh testDebugUnitTest lintDebug assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. First-time builds
need dependency downloads; cached builds can use `--offline`.

Settings shows the version, build number, Git commit, and build type. Modified
source adds `-dirty`; builds without Git metadata show `source archive`.

Source, tests, and the Android application ID use `io.github.iamtoolino.dayline`.
See the [release checklist](docs/releasing.md) for preparing a version for distribution.

## Privacy

Dayline has no INTERNET permission, analytics, ads, account, or backend. It uses
Android usage events to detect the active app and stores settings and usage totals
in app-private storage. It does not read screen text, messages, or browsing content.
Cloud backup and device transfer of app data are disabled. History can be deleted
inside the app. No Accessibility Service is required.

## Current limits

- Force-stop or reboot requires reopening Dayline and starting monitoring again.
- Service downtime, clock jumps, and polling gaps over five seconds are not counted.
- Split-screen tracks the most recently resumed activity that remains visible.
  Picture-in-picture, system panels, and manufacturer battery restrictions need
  broader device testing.
- Sensitive screens can suppress floating overlays.
- The interface is currently English only; builds are local debug APKs.

The implementation uses native Android views, a foreground tracking service,
local preferences, and a small usage ledger. Unit tests cover midnight, daylight-saving
transitions, duration formatting, reminder thresholds, calendar weeks, and restoration
after cancelled navigation gestures.
Instrumentation tests cover accounting, persistence, history migration/deletion,
overlay fades, visible halos, and reminder delivery while Dayline is open.

## License

Dayline's source is licensed under the [MIT License](LICENSE). Bundled dependencies
and development tools retain their respective licenses.
