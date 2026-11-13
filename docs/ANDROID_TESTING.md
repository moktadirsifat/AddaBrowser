# Android testing strategy

This repository uses a layered Android testing approach.

## Test layers

- **Local JVM tests (JUnit + Robolectric):** fast tests for Android-backed repository and business-logic behavior. These run on every pull request and main-branch build with `testDebugUnitTest`.
- **Instrumented UI tests (AndroidX Test + Espresso):** tests that require an Android runtime and validate app screens and interactions. The initial browser-launch smoke test lives under `app/src/androidTest`.
- **System-level flows (UI Automator / device lab):** reserve for flows that cross the app boundary, such as Android's package installer and unknown-app install permission screens.
- **Release verification:** verify APK package name, version, signing identity, and update metadata independently from UI tests.

## CI policy

Pull requests run JVM tests before assembling APKs. Do not make a flaky hosted emulator boot a prerequisite for compiling the app or running local JVM tests.

Instrumented tests must run on a stable Android device provider before being treated as release-gating. The In-App Update E2E flow must install the baseline APK, fetch the signed update, pass through Android's package installer, and verify the resulting installed version. A successful build or a UI smoke test alone is not proof that the updater works.

## Change discipline

- Keep tests close to the module/behavior they cover.
- Add or update tests for changed behavior; avoid rerunning unrelated suites.
- Reuse immutable build artifacts when retrying only a test stage.
- Never use production signing credentials in test logs or artifacts.
- Do not merge a CI migration until the pull-request checks pass.
