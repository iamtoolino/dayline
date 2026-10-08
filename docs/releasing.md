# Preparing a Dayline release

Dayline currently builds local debug APKs. There is no production signing workflow
configured. A normal request for an APK only needs `./scripts/gradle.sh assembleDebug`;
the checklist below is for an explicitly requested release.

## Candidate

- Confirm the intended source revision and changes. Preserve unrelated or explicitly
  uncommitted work; do not sweep it into a release.
- Set `versionName` and an increasing `versionCode` in `app/build.gradle.kts`.
- Build the final release candidate from the agreed clean commit. Settings must show
  the correct version, source revision, and build type.

## Verification

Run the debug checks and build the app and instrumentation APKs:

```sh
./scripts/gradle.sh testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Run instrumentation on a disposable emulator or an explicitly authorized device.
Preserve the user's installed app and data; follow `AGENTS.md` for phone testing.

Check the behavior affected by the release, including:

- Selected-app accounting, the combined budget, app switches, and local-day rollover.
- Permission setup/denial/revocation, screen locking, and tracking start/pause.
- One shared capsule across Dayline and selected apps, including split-screen.
- Dragging, size/opacity, fades, animation settings, and halo previews/reminders.
- Weekly history totals, previous/next navigation, and history deletion.
- Activity/service recreation, font scaling, touch interaction, and TalkBack.

Use deterministic tests for time boundaries where practical. Validate material
service/overlay changes on a physical device. Report checks not run and known
limitations, including manual tracking start after reboot or force-stop.

Inspect the built APK's package ID (`io.github.iamtoolino.dayline`), version, build
type, permissions, exported components, backup restrictions, and packaged files.
Confirm no network/vibration permission, test package, private data, or temporary
experiments were included. Check bundled dependencies and license obligations.

Verify the APK signature with `apksigner verify --verbose --print-certs` and record
the APK's SHA-256 digest. Updates require the same signing certificate as the
installed build. Test a clean installation on a disposable emulator and an update
without clearing data. Selected apps, budget, history, capsule appearance/position,
and animation preference must survive. Never uninstall the user's app to bypass
a signing mismatch. Do not describe a debug APK as a production build.

## Distribution

Present the candidate commit, version/code, build type, APK path/digest, test results,
and unresolved issues. Keep generated reports and APKs in ignored build directories
or `/tmp`.

Only with explicit user authorization, tag the verified commit as `vX.Y.Z`, push,
and publish a GitHub release with the verified APK. Include user-facing changes,
build type, installation requirements, known limitations, and the APK digest.
Confirm the uploaded file matches the verified artifact.
