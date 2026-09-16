# Camera Wall Verification Plan

## Safety and privacy

- Do not start or continue playback when the tablet feels unusually hot or its
  thermal readings are elevated. Stop the app and let the device cool before
  any further on-device test.
- Do not change, print, screenshot for publication, or upload camera URLs,
  credentials, usernames, device serials, or private network addresses.
- Device screenshots and UI hierarchy dumps are local-only evidence. Redact
  camera imagery and settings values before sharing any screenshot.
- Do not reboot the deployed tablet during acceptance testing without arranging
  a maintenance window.

## Host checks

Run on the feature branch before copying an APK to the tablet or publishing code:

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
git diff --check
```

Record the test count, failures, lint warning count, and APK version code. Scan
changed files for local camera property values without printing the values.
Confirm `camera.properties` and local APKs remain ignored by Git.

## ASUS K012 device checks

Run only after the tablet has cooled and its owner is available to observe the
device. Keep screenshots in a private temporary directory and do not attach raw
screenshots or UI dumps to GitHub.

1. Record the installed app version and confirm camera/settings preference
   hashes before and after update; do not uninstall the app.
2. Capture the Home screen. Check that no unexpected keyboard appears, the
   4/8-camera selector is legible, and the wall fits portrait and landscape.
3. Exercise automatic paging on and off, then swipe between pages in both
   directions. Check that the page indicator follows the visible cameras.
4. Open a camera, verify Back returns to the same page, and test pinch, plus,
   minus, reset, and double-tap zoom.
5. Capture Cameras. Search saved entries, drag one item to reorder, edit and
   cancel, then delete only a disposable test entry after explicit confirmation.
6. Capture Settings. Confirm the keyboard remains hidden on entry; inspect the
   view-size, automatic paging, and duration selectors; confirm buffer values
   show both milliseconds and seconds. Test About and local backup/restore with
   a disposable configuration and a strong temporary passphrase.
7. Monitor battery, `skin*`, and chip sensors during playback and after stop.
   Stop if the device becomes uncomfortably hot. Do not infer safe limits from
   another device; obtain ASUS guidance for the K012.
8. Perform the 30-minute playback/reconnect observation only after the thermal
   check is acceptable. Rotation, reconnection, and reboot checks require a
   separate observed test; reboot needs a maintenance window.

## GitHub recording and closure

- Create a sanitized issue for each confirmed defect or unresolved device risk.
- Fix code on a feature branch and open a PR that references the issue.
- Run the host checks above and the applicable device checks before approval.
- Close an issue only after its acceptance criteria pass. Keep tests requiring
  unavailable hardware, physical observation, or deployment access open.
- Never include camera credentials, local addresses, usernames, serials, or raw
  private screenshots in issues, PRs, commits, or release artifacts.

## Current verification record — 2026-09-16

- `assembleDebug`, `testDebugUnitTest`, and `lintDebug` pass; 8 unit tests pass.
- Lint reports 38 warnings, mainly remaining localization and deprecated API,
  manifest checks; review the generated lint report before a stable release.
- A debug APK was built with public-release camera configuration, so its
  embedded fallback camera list is empty. It is saved locally at
  `local-apks/Camera-Wall-v0.3.0-alpha.2-code8-ASUS-K012-Android-5.0-debug.apk`
  and remains ignored by Git. Minimum supported Android is 5.0 / API 21.
- Emulator UI acceptance checks pass in portrait and landscape; physical tablet
  screenshot, playback, and thermal checks remain pending. The K012 previously
  felt unusually hot; do not resume playback until its cool/safe state is
  confirmed and it is disconnected from AC power.
- Exact local camera configuration values and private IPv4 literals were
  scanned across the current source tree; no matches remain. A previous private
  address was removed from the tracked stability note; shared Git history still
  contains its old revision and is not rewritten here.

### Test run — 2026-09-16 13:10:20–13:10:32 +03:00

- Command: `./gradlew -PpublicRelease=true clean assembleDebug testDebugUnitTest lintDebug`
  (host checks from this plan), run on branch `feature/modern-tablet-navigation`.
- Toolchain: local JDK 17.0.20.1 and local Android SDK (both outside the repo,
  under the project's ignored `.toolchain/` directory); Gradle daemon.
- Result: `BUILD SUCCESSFUL in 12s`, 50 actionable tasks executed, exit code 0.
- Unit tests: 8/8 passed, 0 failures, 0 errors
  (`BackupCipherTest`: 5/5, `RtspCredentialsTest`: 3/3;
  `app/build/test-results/testDebugUnitTest/`).
- Lint (`lintDebug`): 0 errors, 50 warnings
  (`app/build/reports/lint-results-debug.html`); one more warning than the
  49 recorded in the prior run above — not yet triaged against that baseline.
- `assembleDebug` produced `app/build/outputs/apk/debug/app-debug.apk`
  (public-release camera configuration, so its embedded fallback camera list
  is empty), 68,457,401 bytes.
- A first attempt at 13:00–13:00 (JDK 21 from Homebrew, no `ANDROID_HOME`) failed
  fast with "SDK location not found"; re-run above used the project's local
  toolchain instead and succeeded.

### Android emulator UI run — 2026-09-16

- Provisioned a local-only Android 15 / API 35 x86_64 AVD under the ignored
  `.toolchain` directory. Emulator Vulkan crashed on this host; it runs with
  SwiftShader ANGLE and Vulkan disabled.
- `connectedDebugAndroidTest`: 3/3 tests passed in portrait, then 3/3 passed in
  a landscape logical-display configuration. Tests cover settings/camera-list
  layout and selectors, search and reorder persistence using non-routable
  `.invalid` fixtures, and Home → Cameras → Settings navigation with an empty
  public camera list. No camera streams or real credentials were used.
- Landscape was induced with the emulator's logical display size override
  (`1920x1080`); this verifies responsive layout but does not verify physical
  accelerometer rotation. The ASUS K012's orientation behavior remains a
  separate hardware check.
- A clean test build initially hit the default Gradle heap limit while
  packaging libVLC; increasing the local Gradle heap resolved it. A separate
  test attempt was rejected because the AVD had code 8 while Gradle defaulted
  to code 3; rerunning with the matching alpha.2 version code passed.
- Current AVD UI tests run only on this host. Screenshots and test data remain
  local; do not attach device screenshots or camera data to GitHub.

### Final automated run — 2026-09-16

- `clean assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`:
  `BUILD SUCCESSFUL`; unit tests 8/8 passed; Android test APK built; lint has
  0 errors and 38 warnings.
- `connectedDebugAndroidTest` on the Android 15 AVD: 3/3 passed in portrait
  and 3/3 passed with a 1920x1080 landscape logical display. Coverage includes
  Home/Cameras/Settings navigation, empty public-build behavior, camera search,
  fixture-only reorder persistence, and settings/camera-list layout.
- Saved the rebuilt public-configuration Android 5+ debug APK at the local,
  gitignored path noted above. `BuildConfig.CAMERAS_JSON` is `[]`; no local
  camera values are included.
- Video scaling is explicit in `CameraPlayerView`: at 1×, LibVLC uses automatic
  output-window scaling with the stream's original aspect ratio; user zoom may
  crop by design. No live or synthetic decoded video was used in this run, so
  visual stream fit still needs a safe stream/device acceptance check.

### Lint fix-up and re-verification — 2026-09-16 13:41:27–13:41:37 +03:00

- Fixed the safe, behavior-preserving subset of warnings from the run above:
  `BootReceiver` now checks the received action before launching
  (`UnsafeProtectedBroadcastReceiver`); removed a dead `SDK_INT >= 17` check in
  `CameraListActivity` (`ObsoleteSdkInt`); switched `Gravity.LEFT`/`RIGHT` to
  `START`/`END` in `CameraPlayerView` and `SingleCameraActivity`
  (`RtlHardcoded`); made the backup-export `OutputStream` a proper
  try-with-resources in `SettingsActivity` (`Recycle`); removed 7 unused
  color resources from `colors.xml` (`UnusedResources`).
- Re-ran `./gradlew -PpublicRelease=true clean assembleDebug testDebugUnitTest
  lintDebug`: `BUILD SUCCESSFUL in 9s`, exit code 0.
- Unit tests: 8/8 passed, 0 failures, 0 errors (unchanged).
- Lint: 0 errors, **38 warnings** (down from 50): `SetTextI18n` 29,
  `DiscouragedApi` 4 (locked `screenOrientation`, an intentional kiosk-tablet
  choice), `ViewConstructor` 3 (custom views are only ever created in code, a
  known false positive), `UnusedAttribute` 1 (`usesCleartextTraffic`, API
  23+ only, harmless below minSdk 21), `DataExtractionRules` 1 (deprecated
  `allowBackup` attribute; app already ships `allowBackup="false"`).
- Remaining categories are tracked for later triage rather than fixed here;
  see issue #65.
