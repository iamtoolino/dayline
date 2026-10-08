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
- An optional daily limit, warning color, and once-per-day vibration.
- Optional gentle vibration at each ten minutes of combined daily usage, with test buttons.
- Searchable app selection with icons and a separate daily History screen.
- A dark navy and cyan design with an adaptive, themed launcher icon.
- Local settings and history, with no account or network access.

## Requirements and setup

Android 11 or newer. Install the APK, open Dayline, enable Usage access and Display
over other apps, select apps, and press Start tracking. Set a shared daily limit
if you want a warning; zero disables it.

The timer appears in selected apps and hides when you leave them or lock the screen.
Android requires a foreground-service notification while monitoring. It is static
and silent; Android 13+ lets you hide drawer notifications in system settings while
tracking continues. Android still lists the service under active apps.

Reminders use the phone’s Notification vibration category and respect Silent mode
and Do Not Disturb.
The ten-minute option is off by default; enabling it starts with the next milestone.
App switches and service restarts do not replay old ticks. A limit warning takes
priority when it coincides with a ten-minute tick. Test buttons do not alter usage
or consume the daily warning.

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
Version 0.3.4 installs as a separate app from earlier prototypes. Existing prototype
settings and history remain in the earlier installation and are not migrated automatically.

## Privacy

Dayline has no INTERNET permission, analytics, ads, account, or backend. It uses
Android usage events to detect the active app and stores settings and usage totals
in app-private storage. It does not read screen text, messages, or browsing content.
Cloud backup and device transfer of app data are disabled. History can be deleted
inside the app. No Accessibility Service is required.

## Current limits

- Force-stop or reboot requires reopening Dayline and starting monitoring again.
- Service downtime, clock jumps, and polling gaps over five seconds are not counted.
- Split-screen tracks the latest resumed app. Picture-in-picture, system panels,
  and manufacturer battery restrictions need broader device testing.
- Sensitive screens can suppress floating overlays.
- The interface is currently English only; builds are local debug APKs.

The implementation uses native Android views, a foreground tracking service,
local preferences, and a small usage ledger. Unit tests cover midnight, daylight-saving transitions, invalid
intervals, and duration formatting; instrumentation tests cover combined accounting,
warning persistence, history migration, and deletion using isolated storage.

## License

Dayline's source is licensed under the [MIT License](LICENSE). Bundled dependencies
and development tools retain their respective licenses.
