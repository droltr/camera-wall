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
- Lint reports 49 warnings, mainly existing localization, deprecated API, and
  manifest checks; review the generated lint report before a stable release.
- A debug APK was built with public-release camera configuration, so its
  embedded fallback camera list is empty. It is saved locally at
  `local-apks/Camera-Wall-v0.3.0-alpha.2-code8-ASUS-K012-Android-5.0-debug.apk`
  and remains ignored by Git. Minimum supported Android is 5.0 / API 21.
- Device screenshots and UI acceptance checks are pending. The K012 previously
  felt unusually hot; playback remains stopped and the device is still connected
  to AC power. Do not resume device testing until the owner confirms it is cool
  and safe to use.
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
  see the linked GitHub issue.
