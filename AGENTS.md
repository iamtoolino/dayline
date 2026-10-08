# Dayline agent workflow

## Coordination

Delegate only when the user explicitly requests agents or a workflow below calls
for them. Keep persistent `agentic_reviews` and `release_task` tasks separate.

## Scope, Git, and privacy

Read the current source and README before changing behavior. Work on the current
branch unless a new branch is requested. Preserve existing dirty changes and
explicitly uncommitted prototypes. Keep commits focused; inspect staged paths,
the staged diff, and `git diff --check` before committing. Never push, tag,
publish, rewrite history, or delete branches without explicit user authorization.

Dayline is an offline, private screen-time awareness app. Track only selected
apps against one combined daily budget. Never block access to another app.
Keep usage history and settings local; no analytics, accounts, network access,
backup/transfer, or vibration. Avoid manufacturer-specific behavior.

Do not commit private device identifiers, personal usage history, screenshots,
logs, APKs, generated audits, or temporary experiments. Keep diagnostics in
`/tmp` or ignored build directories.

## Architecture and verification

- `TrackingService` owns usage accounting; avoid duplicate writers, stale events,
  counting screen-off time, service downtime, or gaps across clock changes.
- `UsageLedger` owns time splitting, including local midnight and DST.
- `TrackerStore` preserves selected apps, budget, settings, and historical usage
  through upgrades and migrations.
- `VisualReminderPolicy` owns reminder thresholds and persistence. Previewing an
  animation must not change accounting or consume a real reminder.
- `TimerDisplay` owns one shared overlay; keep lifecycle and permission handling
  centralized. Avoid retaining activities/services or duplicating windows.
- Keep capsule geometry stable. Respect animation settings, touch safety, and
  immediate hiding when the device locks or permission is revoked.

Use `./scripts/gradle.sh` with checks appropriate to the change. The normal debug
verification is `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
Report actual results and meaningful checks not run. Documentation-only changes
need link/path checks and `git diff --check`, not an Android build.

Use an explicit device serial when operating a device. Preserve installed app data.
Do not run Gradle connected tests on the user's phone if the runner may uninstall
the app. Prefer a disposable emulator; for authorized phone tests, install the test
APK manually, run instrumentation, and remove only the test package. Never uninstall
Dayline to bypass a signing mismatch without the user's explicit approval.

## agentic_reviews

Reviews are read-only unless fixes are separately authorized. Initialization
only reads these rules and reports readiness; do not begin a review automatically.
Record the target checkout, commit, branch, and dirty files before every review.
Understand current product intent and recent changes; don't revive discarded ideas.

When explicitly asked for a full or agentic review, coordinate five independent
specialist passes, using waves if concurrency is limited:

1. Architecture fitness, ownership, and shared policy.
2. Correctness, concurrency, security, packaging, and Android behavior.
3. Code quality, semantic duplication, maintainability, and experiment residue.
4. Performance, battery, polling, rendering, and resource lifetime.
5. Documentation, build reproducibility, release readiness, and verification gaps.

All specialists remain read-only: no
edits, commits, additional delegation, or live-device actions. Each finding must
include severity, exact file/line, concrete impact, evidence, and a concise remedy.
Distinguish confirmed bugs from credible risks, tradeoffs, and optional cleanup;
style preferences alone are not defects. Do not invent findings to fill a quota.

Independently verify consequential claims, reconcile disagreements, deduplicate,
and deliver one findings-first report ordered by practical severity. Agreement
between agents is not proof. If no actionable issues remain, say so and identify
meaningful verification gaps. Do not implement or commit findings without separate
authorization. Do not message another task without direct user authorization.

## release_task

Initialization only reads these rules and `docs/releasing.md`, inspects release
support, and reports readiness and missing prerequisites. Do not change versions,
build, sign, install, tag, push, or publish just because this task was created.

For an explicitly requested release, follow `docs/releasing.md`: establish the
candidate, run appropriate verification, inspect the APK, and verify installation
and a data-preserving update. Report the source revision, version, build type,
artifact digest, and actual checks. Keep preparation separate from publication;
tag, push, or publish only with explicit user authorization. A request to build
an APK alone does not require the full release workflow.
