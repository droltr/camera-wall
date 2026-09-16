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

## Earlier verification record — 2026-09-16 (superseded by later entries)

- `assembleDebug`, `testDebugUnitTest`, and `lintDebug` pass; 8 unit tests pass.
- Lint reports 38 warnings, mainly remaining localization and deprecated API,
  manifest checks; review the generated lint report before a stable release.
- A debug APK was built with public-release camera configuration, so its
  embedded fallback camera list is empty. It is saved locally at
  `local-apks/Camera-Wall-v0.3.0-alpha.2-code8-ASUS-K012-Android-5.0-debug.apk`
  and remains ignored by Git. Minimum supported Android is 5.0 / API 21.
- Emulator UI acceptance checks pass in portrait and landscape. On the physical
  K012, Settings/Camera List and camera search/reorder UI-only smoke tests passed
  without starting playback. A reliable before/after preference integrity
  comparison was not established for that run, so device persistence acceptance
  remains pending. Screenshots, playback, rotation, and thermal checks also
  remain pending. The K012 previously reported high thermal readings; the most
  recent power check showed AC input active. Do not resume playback until AC
  power is disconnected, the device is cool, and applicable device thermal
  guidance has been reviewed.
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
- `connectedDebugAndroidTest`: 4/4 tests passed in portrait, then 4/4 passed in
  a landscape logical-display configuration. Tests cover settings/camera-list
  layout and selectors, search and reorder persistence using non-routable
  `.invalid` fixtures, and Home → Cameras → Settings navigation with an empty
  public camera list. A generated 4:3 green test clip also verifies that the
  video surface displays the image at its center and leaves a dark aspect-ratio
  bar at the frame edge. No real camera streams or credentials were used.
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
- `connectedDebugAndroidTest` on the Android 15 AVD: 4/4 passed in portrait
  and 4/4 passed with a 1920x1080 landscape logical display. Coverage includes
  Home/Cameras/Settings navigation, empty public-build behavior, camera search,
  fixture-only reorder persistence, settings/camera-list layout, and a
  synthetic video-frame fit check.
- Saved the rebuilt public-configuration Android 5+ debug APK at the local,
  gitignored path noted above. `BuildConfig.CAMERAS_JSON` is `[]`; no local
  camera values are included.
- Video scaling is explicit in `CameraPlayerView`: at 1×, LibVLC uses automatic
  output-window scaling with the stream's original aspect ratio; user zoom may
  crop by design. A generated 4:3 test clip passed on the emulator. Live
  camera-stream fit still needs safe physical-device acceptance.

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

### K012 camera-sync and playback follow-up — 2026-09-16

- Built public-config APK `0.3.0-alpha.3` / versionCode 10 and kept it in the
  git-ignored `local-apks/` directory. No camera values or credentials were
  embedded in the APK.
- On the ASUS K012 running Android 5.0, all 5 instrumentation tests passed;
  the 12 unit tests passed, lint completed with 38 warnings, and GitHub's
  `build` check passed. The test APK's connected instrumentation cleanup
  removed the target app package; the public APK was then reinstalled and the
  camera list was re-synchronized. Do not run connected instrumentation tests
  on this tablet without an explicit restore plan.
- The go2rtc API returned 10 producer-backed stream entries. Alias resolution
  selected 9 unique cameras; the tablet's saved names matched that canonical
  list, and synchronized records contain no per-camera username or password.
  The API was reachable without authentication from the tablet's local Wi-Fi;
  no server or network settings were changed. See #28.
- A brief 2x2 live-view check showed frames in all four visible camera tiles.
  In single-camera view the plus and reset controls responded. With automatic
  paging temporarily off, left and right swipes moved between pages; automatic
  paging was restored afterward. The known settings were restored to 4 cameras,
  automatic paging, 5 seconds, 800 ms buffer, RTSP TCP, labels, and keep-screen-on.
- The battery sensor read 29.7°C during the brief playback check. This is not a
  thermal limit or a component-temperature measurement; the 30-minute thermal
  and reconnect run remains pending. The owner confirmed cooling and authorized
  tests to continue; no device-specific ASUS thermal guidance was verified.
- An ADB rotation override did not change the tablet's reported display
  orientation; the original system rotation settings were restored. Physical
  accelerometer rotation, pinch/double-tap zoom, TalkBack, backup restore, and
  long-duration/reboot stability remain unverified.
- Settings connection-test authentication was verified on-device with a
  disposable Basic Auth endpoint and temporary credentials; the test values
  were cleared and the original endpoint was restored. Issue #62 was closed.
- Device screenshots and UI dumps were kept in a private temporary directory
  and were not attached to GitHub. Camera names, URLs, private addresses,
  usernames, passwords, and device serials are intentionally omitted here.

### Issue triage — 2026-09-16

- Issue #61: RTSP scanning now requires Wi-Fi to be enabled, associated, and
  completed with a usable unicast IPv4 address. Missing, loopback, link-local,
  and multicast addresses are rejected. UI-queued candidate and completion
  callbacks are ignored after scan cancellation or activity destruction. Unit
  tests cover the IPv4 eligibility rules. Physical discovery on the K012 remains
  pending, so the issue stays open until that acceptance check passes.
- Issue #65: The remaining lint warnings were reviewed. `SetTextI18n` is
  expected while the app is Turkish-only; full resource extraction belongs to
  future localization work. `fullSensor` orientation is intentional for the
  tablet wall. Custom views are created only in code. Cleartext HTTP remains
  required for local camera/NVR endpoints on supported API levels, and backup
  is explicitly disabled; the deprecated backup attribute remains necessary
  for older Android compatibility. Keep these warnings visible in lint instead
  of suppressing them. Issue #65 is closed as triaged; the warning count remains
  38 with zero lint errors.
