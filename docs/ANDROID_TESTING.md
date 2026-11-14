# Android testing strategy

This repository uses a layered Android testing approach.

## Test layers

- **Local JVM tests (JUnit + Robolectric):** fast tests for Android-backed repository and business-logic behavior. These run on every pull request and main-branch build with `testDebugUnitTest`.
- **Instrumented UI tests (AndroidX Test + Espresso):** tests that require an Android runtime and validate app screens and interactions. The initial browser-launch smoke test lives under `app/src/androidTest`.
- **System-level flows (UI Automator / device lab):** reserve for flows that cross the app boundary, such as Android's package installer and unknown-app install permission screens.
- **Release verification:** verify APK package name, version, signing identity, and update metadata independently from UI tests.

## CI policy

Pull requests run JVM tests before assembling APKs. A separate GitHub-hosted Android 11 emulator job downloads the already-built APKs and runs the Espresso smoke test in an isolated job, so emulator memory pressure cannot interfere with Gradle compilation. Standard GitHub-hosted runners are free for this public repository. The emulator job is still subject to hosted-runner availability and can fail due to emulator/ADB instability; it must not be confused with Firebase Test Lab.

The Firebase Test Lab job is deliberately opt-in: start the workflow manually and set `run_test_lab=true`. Ordinary pull requests and pushes do not consume device-test minutes. The job uses GitHub OIDC Workload Identity Federation with the repository variables `GCP_WIF_PROVIDER` and `GCP_SERVICE_ACCOUNT`; do not add a long-lived service-account JSON key.

Firebase Test Lab currently requires the Firebase project to be linked to a Cloud Billing account (Blaze plan), even though its pricing page lists daily no-cost testing allowances. Do not enable billing automatically. Before opting in, the project owner must consciously link a billing account and review the current quotas and pricing.

The instrumented smoke test targets an Android 11 virtual device. A successful run proves only that this UI smoke test passed on that test device. It does not prove compatibility with every physical Android 11 phone.

The In-App Update E2E flow must be tested separately: install the baseline APK, fetch a newer correctly signed APK, pass through Android's package installer, and verify the resulting installed version and signing identity. A successful build or a UI smoke test alone is not proof that the updater works.

## Change discipline

- Keep tests close to the module/behavior they cover.
- Add or update tests for changed behavior; avoid rerunning unrelated suites.
- Reuse immutable build artifacts when retrying only a test stage.
- Never use production signing credentials in test logs or artifacts.
- Do not merge a CI migration until the pull-request checks pass.
