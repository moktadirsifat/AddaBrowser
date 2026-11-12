# SIFAT Browser — Master Task Schedule

Last reviewed: 2026-11-12  
Repository: https://github.com/moktadirsifat/AddaBrowser

## Project rules
- Work on one module/submodule at a time; keep unrelated modules isolated.
- Preserve existing working features. Do not move tasks between versions without the user's approval.
- A commit or source file does not mean a feature is fully complete. Track implementation, build verification, integration testing, and Android 11 device testing separately.
- After each focused change: inspect the diff, run the available build/tests, fix errors, then continue.
- Do not start the next version until the current version builds, passes regression checks, and its APK is delivered for Android 11 testing.
- Do not add new v1.16 scope without the user's approval.

## Current baseline — reviewed from repository source/commit history
| Area | Evidence-based status |
|---|---|
| NavigationController | Source extraction/integration commit exists; runtime regression still needs verification |
| TabManager / Multi-Tab | Source extraction/integration commit exists; runtime regression still needs verification |
| HistoryRepository | Source extraction/integration and history formatting fix commits exist; runtime regression still needs verification |
| Pull-to-Refresh | Controller extraction/integration commit exists; runtime regression still needs verification |
| HomePageProvider | Exists and is wired into MainActivity |
| HomeBranding | Separate CSS module exists |
| HomeLayout | Separate CSS module exists; a Java string concatenation syntax fix is in commit history |
| Home shortcuts | SVG logos for YouTube, Facebook, Wikipedia and Gmail are present in HomePageProvider |
| Home background | One scenic background and flying-bird animation exist; multi-theme selector is not present in inspected source |
| Launcher icon | Redesign commit exists; final visual/spec verification is still required |
| Build/release | Inspected app/build.gradle still reports versionCode 14 / versionName 1.14; CI/build success was not confirmed in this review |

**Important:** Status above means source/history evidence was found. It does not certify a successful build or Android device test.

## v1.16 — Current release: Browser stabilization + Home module ONLY

### 0. Baseline and build verification
- [ ] Inspect current default-branch HEAD and working tree/source state.
- [ ] Run a clean Gradle build with the repository's configured modern Android toolchain.
- [ ] Record exact build errors before changing code; fix only confirmed blockers.
- [ ] Confirm Android 11 compatibility and existing app launch before feature work.

### 1. Existing browser feature regression
- [ ] NavigationController: back/forward/page navigation regression test.
- [ ] TabManager/Multi-Tab: open, switch, close, and preserve independent pages.
- [ ] History: readable page titles, correct ordering, no malformed/raw URL display.
- [ ] Pull-to-Refresh: gesture refreshes the active page/tab without breaking scroll or other tabs.
- [ ] Verify existing browser features remain intact after Home changes.

### 2. Home Branding
- [x] Branding CSS extracted into HomeBranding (source exists).
- [ ] Verify logo styling/readability on Android screen sizes.
- [x] Set exact approved slogan: “সিফাতের সাথে, চলো অজানায়।” (source updated; visual check pending)
- [x] Implement small “Powered by Dr. Muktadir Sifat” credit (visual check pending).

### 3. Home Layout
- [x] Layout CSS extracted into HomeLayout (source exists).
- [ ] Verify search bar, spacing, alignment, responsive layout and smaller screens.
- [ ] Check that Home layout changes do not affect browser navigation or WebView.

### 4. Home Shortcuts
- [x] SVG shortcut artwork for YouTube, Facebook, Wikipedia and Gmail exists.
- [ ] Verify all shortcut links open the correct destinations.
- [ ] Verify icons and labels render correctly on Android 11 and different screen sizes.

### 5. Home Background
- [x] One scenic background and flying-bird animation exist.
- [x] Add 4 selectable background themes (Scenic, Aurora, Night, Forest).
- [x] Include subtle lightweight motion across themes (birds, aurora drift, twinkling stars, drifting leaves).
- [x] Add prefers-reduced-motion and static-motion fallback.
- [ ] Verify smooth performance and readability on Android 11.

### 6. Launcher icon and release metadata
- [ ] Verify the latest icon against the approved deep-teal/white globe design.
- [ ] Ring must show only its front-facing arc; rear arc hidden behind the globe.
- [ ] Confirm icon assets/adaptive icon configuration and launcher appearance.
- [ ] Update versionCode/versionName to v1.16 only when preparing the release build.

### 7. v1.16 integration, release and handoff
- [ ] Integrate Home submodules without unrelated refactors.
- [ ] Run clean release build; resolve all build errors.
- [ ] Run full regression checks for navigation, tabs, history, refresh and Home.
- [ ] Install, launch and test final APK on Android 11.
- [ ] Share the final v1.16 APK and release notes.
- [ ] Do not begin v1.17 until v1.16 is verified and handed off.

## v1.17 — Later version; do not start during v1.16
- [ ] Toolbar module organization/refinement (inspect existing toolbar first; do not assume it is absent).
- [ ] Menu module.
- [ ] Bookmark module.
- [ ] Shortcut module refinement/management.
- [ ] Dialog module.
- [ ] Module-level tests, integration tests, build and Android 11 regression.

## v1.18 — Later version
- [ ] Settings module.
- [ ] Download module.
- [ ] WebView module.
- [ ] Theme module.
- [ ] Module-level tests, integration tests, build and Android 11 regression.

## v1.19 — Later version
- [ ] In-app update system.
- [ ] Persistent private release-signing key setup and safe backup procedure.
- [ ] Code cleanup and dependency/toolchain review.
- [ ] Final clean release build and full regression.
- [ ] Android 11 installation/update-path verification.
- [ ] Final release handoff.

## Change log / status discipline

### 2026-11-12 — Home background and branding implementation
- Added `HomeBackground.java` with four selectable themes, per-theme lightweight animation, saved theme preference, and reduced-motion handling.
- Wired the selector into `HomePageProvider`; updated the exact approved Bengali slogan and attribution text; improved slogan contrast in `HomeBranding`.
- Commits: `0485a1af2a030a2a6319a9366048a2d4f3676fc3`, `1e9b0b45d0c4e7e941d2add531e06d9a247165ec`, `844ddb92b1242a977f71fb9af520f7def2107347`.
- Build, emulator, visual, and Android 11 physical-device checks: pending; do not mark these implementation tasks as fully release-verified yet.

## Change log / status discipline
For every task, record: date, module, files changed, commit, build result, test result, and any remaining issue. Never mark a task complete solely because code was committed.
