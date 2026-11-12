# SIFAT Browser — Master Task Schedule

Last reviewed: 2026-11-12  
Repository: https://github.com/moktadirsifat/AddaBrowser

## Project rules
- Work in small, isolated modules; do not rewrite working features unnecessarily.
- Keep the version boundaries below. Do not add new v1.16 features without the user's approval.
- Track implementation, build, emulator interaction, visual review, and physical Android 11 testing as separate evidence.
- A source file or successful APK build alone does not prove that a feature works correctly.
- Do not start v1.17 until v1.16 has passed the agreed checks, its APK is delivered, and the user has had the opportunity to test it on Android 11.

## v1.16 — Browser stabilization + Home module ONLY

### A. Existing browser features — implementation already present; regression verification remains
These are not new implementation tasks. The controllers/repository are already present and connected in `MainActivity`. Preserve them and test their actual behavior.

#### A1. Navigation
- [x] `NavigationController` exists and is used for back/forward and home/reload actions.
- [ ] Test Back and Forward on a page with multiple navigation steps.
- [ ] Confirm Back/Forward do nothing safely when no history exists.
- [ ] Confirm Home returns to the SIFAT home page and normal reload reloads the current website.
- [ ] Confirm navigation still works after switching tabs.

#### A2. Multi-Tab
- [x] `TabManager` exists and is connected to `MainActivity`.
- [ ] Open multiple tabs; confirm the tab counter updates correctly.
- [ ] Open different websites in separate tabs and confirm each keeps its own page/state.
- [ ] Switch between tabs; confirm the correct page and address/title are shown.
- [ ] Close the active tab; confirm a sensible remaining tab becomes active.
- [ ] Close a non-active tab; confirm the active page does not change.
- [ ] Close the last tab; confirm the browser creates or displays a usable Home tab rather than leaving a broken screen.
- [ ] Check repeated open/switch/close operations for crashes or stale WebViews.

#### A3. History
- [x] `HistoryRepository` exists and is connected to `MainActivity`.
- [x] Store page title, URL, and visit time; limit stored entries and avoid duplicate URL entries.
- [x] Display a readable page title, with a clean host-name fallback when a title is missing; do not show a raw URL as the visible title.
- [ ] Visit several pages and confirm newest entries appear first.
- [ ] Confirm title and date/time display are readable and each entry opens the correct URL.
- [ ] Confirm older legacy history entries remain readable.
- [ ] Confirm Clear removes history and the empty-history state is displayed correctly.

#### A4. Pull-to-Refresh
- [x] `PullToRefreshController` exists and is attached to the WebView.
- [ ] On a page scrolled to the top, pull down past the trigger distance and confirm the current page reloads.
- [ ] Confirm a small pull does not reload the page.
- [ ] Confirm a pull while the page is scrolled down does not reload it.
- [ ] Confirm refresh acts on the currently active tab after switching tabs.
- [ ] Check that normal page scrolling and touch interaction remain usable.

### B. Home module

#### B1. HomeBranding
- [x] Branding CSS is separated into `HomeBranding`.
- [x] Current approved slogan: “চলো অজানায়, দিগন্তের পথে..”
- [x] Small footer credit: “Powered by Dr. Muktadir Sifat”.
- [ ] Check logo, slogan, contrast, and footer readability on Android 11.
- [ ] Check that branding does not overlap or clip on smaller screens.

#### B2. HomeLayout
- [x] Layout CSS is separated into `HomeLayout`.
- [ ] Check search field size, spacing, alignment, and responsive behavior.
- [ ] Confirm keyboard submission opens the entered address/search correctly.
- [ ] Confirm the layout remains usable on smaller screens and does not interfere with WebView navigation.

#### B3. HomeShortcuts
- [x] Shortcut artwork is present for YouTube, Facebook, Wikipedia, and Gmail.
- [ ] Confirm each shortcut opens its correct destination.
- [ ] Confirm logos and labels render clearly and are correctly aligned.
- [ ] Check shortcut layout on Android 11 and smaller screens.

#### B4. HomeBackground
- [x] Four selectable themes are implemented: Scenic, Aurora, Night, and Forest.
- [x] Lightweight motion is implemented: flying birds, aurora drift, twinkling stars, and drifting leaves.
- [x] Selected theme preference is saved.
- [x] Reduced-motion/static fallback is implemented.
- [ ] Check theme selection and preference persistence after app restart.
- [ ] Check animation smoothness, battery/performance impact, and text readability on Android 11.
- [ ] Confirm reduced-motion mode disables decorative movement.

### C. Launcher icon and release metadata
- [x] Adaptive icon resources and legacy launcher fallback are present.
- [x] Approved concept: deep-teal/white stylized globe; only the front-facing ring arc is visible over the globe and the rear arc stays behind it.
- [x] Release metadata is set to `versionCode 16` and `versionName '1.16'`.
- [ ] Inspect the actual launcher icon on an Android launcher; verify scaling, safe area, colors, and front/rear ring layering.

### D. Build, integration, regression, and handoff
- [x] Latest source has a successful Debug and Release APK build in GitHub Actions run #88.
- [ ] Complete run #88 Android 11 emulator install/launch step and confirm artifact upload.
- [ ] Run focused interaction regression for Navigation, Multi-Tab, History, and Pull-to-Refresh.
- [ ] Run Home module checks: branding, layout, shortcuts, and all four backgrounds.
- [ ] Inspect the launcher icon on device/emulator.
- [ ] Resolve only confirmed defects; avoid unrelated refactoring.
- [ ] Run a clean final v1.16 Release build after any fixes.
- [ ] Deliver the final v1.16 APK and concise release notes.
- [ ] User tests the delivered APK on their physical Android 11 phone and reports any issue.
- [ ] Only after these checks are complete may v1.17 begin.

- Follow-up code review found that switching back to a loading tab could leave the shared progress bar showing another tab's state, and a tab with an empty page title could show a blank address field. The active-tab switch now restores that WebView's own progress, and title display falls back to its URL when empty. Commit: 6ad0f150c04613ba8d79f05bf39440ea1290a31a. CI and interactive regression are pending.

- CI log review identified the repeated Android 11 emulator failures: the runner executed the multiline host `for` loop as a separate `/usr/bin/sh -c` command, causing a shell syntax error before APK installation. Replaced that host loop with a device-shell boot wait; install/launch validation now proceeds directly after boot. Commit: 1638def0129d03c2f51ba490741db21680ad5b90. New CI verification pending.

- CI follow-up: run #98 passed build + Android 11 emulator install/launch, while the documentation-only commit run #99 built successfully but failed at the hard-coded `am start` component step (`Activity class ... does not exist`). Replaced the brittle explicit component launch with Android's package launcher intent (`monkey -p com.sifat.browser 1`) and retained the process check. Commit: `dfd9661a5484bb35a8987ba0562d38525d78acad`. Verification of this workflow correction is pending; interactive feature regression remains separate.

- History code review found a migration visibility defect: legacy URL-only history was shown only when `history_v2` was empty, so the first new visit could hide all older legacy entries. `getEntries()` now merges non-duplicate legacy URLs after newer v2 entries, respecting the 50-entry cap. Commit: `3a62a9a0ae3d7fd27b1c40061e4243a683c81c7f`. Build and legacy-history regression verification are pending.

- Pull-to-Refresh code review found that a drag beginning mid-page could scroll back to the top and accidentally trigger a reload on release. The controller now records whether the gesture began at scroll position zero and resets that state on cancellation; build and gesture regression verification are pending. Commit: `9f1ed3766f1563d577abfd19e70d3dda01f079d2`.

- Home theme polish: flying birds are now restricted to the Scenic theme instead of appearing over Aurora, Night, and Forest backgrounds. Reduced-motion still hides the bird animation. Build/runtime visual verification pending. Commit: `9b30acb490d30cff1bc21d52e50658e7e21cb712`.

## v1.17 — Later version; do not start during v1.16
- Toolbar module organization/refinement (inspect existing toolbar first; preserve current behavior).
- Menu module.
- Bookmark module.
- Shortcut module refinement/management.
- Dialog module.
- Add focused tests, integrate, build, and perform Android 11 regression.

## v1.18 — Later version
- Settings module.
- Download module.
- WebView module.
- Theme module.
- Add focused tests, integrate, build, and perform Android 11 regression.

## v1.19 — Later version
- In-app update system.
- Persistent private release-signing key setup and safe backup procedure.
- Code cleanup and dependency/toolchain review.
- Final clean release build and full regression.
- Android 11 installation/update-path verification and release handoff.

## Evidence and change log

### 2026-11-12 — Current v1.16 state
- Multi-Tab, History, Pull-to-Refresh, and Navigation implementation files exist and are wired into `MainActivity`; their interactive regression checks remain open.
- Code review found a lifecycle edge case when closing the active tab: the old `WebView` could still be referenced while switching to the replacement. `closeCurrentTab()` now clears the active reference before destroying the old view. Commit: `d7513e9c10a75e73919667128af34e584d8b0880`. Build and tab-close regression for this fix are pending.
- Home modules and four animated background themes are implemented; Android 11 visual/performance checks remain open.
- Current home slogan is “চলো অজানায়, দিগন্তের পথে..” (this supersedes older slogan text in earlier notes).
- CI run #88 is for source commit `0189335d0dcdd249385f3635d23aef8fcf26636c`. Debug and Release APK build steps passed; Android 11 emulator install/launch was still running at last check. Run: https://github.com/moktadirsifat/AddaBrowser/actions/runs/43191803767
- CI emulator launch is not the same as interactive regression or physical-phone testing.

- Code review found a shared-UI race: the original WebView's delayed page-finished/progress callbacks could overwrite the address/progress UI after the user switched to another tab. The callbacks now continue recording history for their own page, but update shared UI only when their WebView is active. Commit: `46f0c12ae506a1bcda8610022fa930b173978829`. Build and multi-tab callback regression are pending.

- CI reliability improvement: the Android 11 emulator check now waits for the boot-complete signal, uses headless/disabled-animation emulator settings, retries APK installation after restarting ADB (up to three attempts), verifies the launched app process, and has a 30-minute job timeout. Commit: `c6177937e2a58e39cce8999deecd451e2f068161`. The new workflow run must still pass before this change is considered verified.

### Earlier implementation records
- History display was changed to readable titles/host fallback and date/time instead of raw URL lines. Commit: `6c93a2263840ac5100583b4f1eeaabdc9b55f26e`.
- Adaptive launcher icon resources and manifest wiring were added. Commits: `94113c4bd48ae2e96a23453ef69729e8aca68a0f`, `db95bf583a6ea9e34108e37575af9f6a9e48136b`, `142ee64505bca376593d1c3f3a774f16c553ede1`, `ecfc89cdd93841b0ab1b2f3167d831e75a57d144`.
- Release metadata set to v1.16. Commit: `8475f73f319ff0c65c58a4a465ab7443ef924a2c`.
- Reduced-motion behavior extended to decorative branding animations. Commit: `e5f80fa5a8973aece07d58831eaca7ceba7aea1b`.
- Home background themes, branding, and Home integration commits: `0485a1af2a030a2a6319a9366048a2d4f3676fc3`, `1e9b0b45d0c4e7e941d2add531e06d9a247165ec`, `844ddb92b1242a977f71fb9af520f7def2107347`.
- Latest slogan-only commit: `0189335d0dcdd249385f3635d23aef8fcf26636c`.

For each future change, record the date, module, files changed, commit, build result, test result, and remaining issue. Never mark a feature fully verified solely because its source code exists or a commit was created.
