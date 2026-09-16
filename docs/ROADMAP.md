# Camera Wall Roadmap

## Primary constraint

The ASUS K012 currently running Camera Wall must remain operational throughout
development. The `main` branch represents the known-good state. New work must
be developed on feature branches, built by CI, and tested on the real tablet
before merge.

The project remains in alpha. Beta and stable releases are intentionally
deferred until the interface, persistence, recovery, and device tests mature.

## Status reconciliation — 2026-09-16

- GitHub `v0.2.0-alpha` is closed. The issue sets attached to `v0.3.0-alpha`
  through `v0.6.0-alpha` are closed because their corresponding PRs were
  merged. This only reports completion of those original tracked scopes; newer
  user-requested work on the local feature branch is not represented by them.
- `v0.7.0-alpha` is in progress: launcher icon issue #27 is complete; network
  hardening #28, the extended ASUS K012 test #29, thermal investigation #57,
  main-branch protection #58, accessibility #59, RTSP credential-field
  separation #60, scanner cancellation #61, authenticated go2rtc testing #62,
  and privacy cleanup #63 are open. The milestone is therefore 1/10 complete,
  not complete.
- The current `main` history includes merged PRs #55 and #56. GitHub reports
  branch protection disabled; do not change branch settings until required
  checks and recovery expectations are agreed.
- The current local feature branch contains additional interface, camera
  management, rotation, playback, settings backup, and documentation changes.
  Draft PR #64 is open with host checks passing locally; GitHub CI and real
  device acceptance remain pending. Do not merge until the queue below passes.
- The feature branch contains no camera configuration, generated APK, or
  private IPv4 literal. The local code-8 APK is ignored by Git.

## Alpha checkpoint — 2026-09-14

Completed and merged into `main`:

- Reusable libVLC playback, persistent camera repository, migration fallback,
  2x2 wall, paging, fullscreen view, Back behavior, and bottom navigation.
- Camera list with add/edit/delete/reorder and RTSP connection testing.
- Persisted application settings and go2rtc address/credential settings with a
  visible unauthenticated-LAN warning.
- go2rtc stream import, opt-in ONVIF discovery, bounded local RTSP candidate
  scanning, launcher icon, and RTSP playback health watchdog.
- x86, armeabi-v7a, and arm64-v8a packaging; ASUS K012 smoke verification;
  public GitHub Project/Issue/PR tracking and alpha changelog.

The current tablet is operational and displays the configured streams. A
watchdog retries a tile when `Playing` does not become healthy. The tablet
screen currently presents the camera images rotated relative to the physical
mount; this is recorded as a follow-up rather than changing the known-good
playback path during this pause.

## Remaining work — execution queue, reconciled 2026-09-16

The local feature branch and `origin/feature/modern-tablet-navigation` were at
the same commit during this review. PR #64 is open as a draft; its latest GitHub
build check passes. No tracked local changes were pending synchronization.
Ignored camera configuration, APKs, device notes, and diagnostic dumps remain
local by design.

1. **Finish PR #64 review gates.** Emulator UI tests pass in portrait and
   landscape, including fixture-only reorder and synthetic video-fit checks.
   On the K012, Settings/Camera List and search/reorder UI-only smoke tests
   passed without starting playback. The app preference before/after integrity
   check was not successfully established. Record that gap; do not claim device
   persistence acceptance. Keep the PR draft until device gates below pass.
2. **Establish a safe K012 test baseline (#57).** Keep the ADB cable for data,
   remove external charging power, and verify the tablet reports no AC or USB
   power input. Let the device cool and obtain applicable ASUS thermal guidance
   before playback. Current telemetry showed AC input active, so the unpowered
   condition is not yet verified. Do not use software battery-status overrides
   as proof that charging stopped.
3. **Run non-playback device acceptance.** After the power/cool-state gate,
   check physical rotation, swipe paging, camera zoom, TalkBack, settings,
   backup/restore, and upgrade preference preservation using disposable test
   data. Keep screenshots local and redact all device and camera information.
4. **Run playback and extended stability only after #57's safety gate (#29).**
   Start with an observed short playback check and stop on abnormal heat or
   unstable behavior. Proceed to the 30-minute reconnect, paging, fullscreen,
   memory/CPU, and recovery checks only when device-specific limits and a safe
   baseline are established. Reboot needs a maintenance window.
5. **Validate the local camera/security fixes (#60, #61, #62).** Exercise URL
   credential separation, scan cancellation/no-address handling, and
   authenticated go2rtc testing against a disposable authenticated endpoint.
   Never use or publish live camera credentials for test fixtures. Close issues
   only with matching acceptance evidence.
6. **Accessibility acceptance (#59).** Verify swipe and zoom navigation with
   TalkBack on the K012; emulator and lint checks do not satisfy this device
   criterion.
7. **Privacy history cleanup (#63).** Current tracked files are clean, but a
   prior private address remains in shared Git history. Plan any history rewrite
   with affected collaborators first; do not rewrite shared history as part of
   routine synchronization.
8. **Server-side hardening (#28).** In an authorized maintenance plan, restrict
   Frigate/go2rtc APIs and RTSP, configure dedicated credentials, rotate
   upstream credentials, and verify firewall boundaries. This is separate
   deployment work and must not be attempted against the live server without
   explicit access and a rollback plan.
9. **Main-branch governance (#58) and lint triage (#65).** Review the required
   CI check names before enabling branch rules; address remaining lint warnings
   separately from functional changes.
10. **ARM physical-device validation.** Validate the universal APK on an ARM
    device; the available K012 is x86.
11. **Release preparation.** After review and device gates pass, configure
    signing, create a signed release, and attach checksums. Keep camera settings
    and credentials out of source, artifacts, and release notes.
12. **Optional polish.** Complete settings propagation to every player option
    and improve discovery-result import UX.

Do not schedule beta or stable work until items 1–6 pass. Item 2 currently
blocks physical playback; items 7–9 also require separate coordination or
repository/deployment access.

## Target navigation

```text
Camera wall / Home
├── Tap a camera → Single-camera fullscreen view
├── Cameras → Camera management
└── Settings → Application and server settings

Camera management
├── Add an RTSP stream manually
├── Import streams from go2rtc
├── Discover network candidates
└── Edit, delete, test, and reorder cameras
```

The bottom navigation bar will contain Home, Cameras, and Settings. Android's
system Back action and a visible in-app Back button will both return from the
single-camera view to the previous wall page.

## Planned interface

### Home and camera wall

- Preserve the existing 2x2 wall and automatic paging.
- Show a compact bottom navigation bar.
- Open a tapped tile in a single-camera fullscreen view.
- Preserve aspect ratio and never crop or stretch video.
- Decode only visible streams to protect old-device performance.

### Single-camera view

- Display only the selected stream.
- Show the camera name and a visible Back button.
- Support the Android system Back action.
- Return to the same camera-wall page.
- Release the single player cleanly when leaving the screen.

### Camera management

- List saved cameras and connection state.
- Add, edit, delete with confirmation, and reorder cameras.
- Store name, RTSP URL, optional username, and masked password separately.
- Test a connection before saving it.
- Ensure an invalid or offline camera cannot prevent other streams from playing.

### Settings

- Page interval and automatic paging.
- Network cache and RTSP TCP/UDP selection.
- Hardware acceleration and camera label visibility.
- Keep-screen-on, boot behavior, and optional menu auto-hide.
- Default opening view.
- go2rtc address and optional credentials.
- Application version and current security warning.

### Application icon

- Simple four-tile camera-wall motif with a camera lens.
- Legible at small launcher sizes.
- Classic mipmap resources for Android 5.
- Adaptive icon resources for newer Android versions.

## Stream discovery strategy

Discovery will be opt-in and ordered from most reliable to least reliable:

1. Manual RTSP entry, which always remains available.
2. Import the configured stream list from go2rtc.
3. Discover ONVIF devices with WS-Discovery.
4. Perform a bounded local-network scan of common RTSP ports such as 554 and 8554.

RTSP does not provide a universal stream-discovery protocol. A port scan can
identify only a candidate server; it cannot reliably determine the stream path
or credentials. Discovery must never brute-force passwords or paths. Scanning
must be user initiated, time bounded, run in the background, and avoid
interrupting active playback.

## Compatibility and data migration

- Continue supporting Android 5.0 / API 21 and the x86 ABI.
- Prefer the existing Java and Android View approach over heavy new frameworks.
- Introduce a `CameraRepository` as the single camera-data interface.
- On first use, migrate the current build-time cameras into private app storage.
- If migration or saved data is invalid, fall back to the known build-time list.
- Do not remove the fallback until migration has been proven on the real tablet.
- Never commit private addresses, RTSP credentials, or signing material.

## Planned architecture

```text
MainActivity / Camera wall
├── CameraPlayerView
├── SingleCameraActivity
├── CameraListActivity
├── CameraEditActivity
├── SettingsActivity
├── CameraRepository
├── SettingsRepository
├── RtspConnectionTester
├── Go2RtcClient
└── NetworkDiscoveryService
```

`CameraPlayerView` will own the proven libVLC surface-size, best-fit, retry, and
cleanup behavior so the wall and single-camera screens use the same playback
implementation.

## Delivery phases

### Phase 1: Safe foundation

- Preserve a known-good APK.
- Create `feature/camera-management-ui` from the accepted baseline.
- Extract the reusable player without changing behavior.
- Add backward-compatible camera persistence and migration.

### Phase 2: Navigation

- Add Home, Cameras, and Settings destinations.
- Add the compact bottom menu and Back behavior.

### Phase 3: Single-camera view

- Add tap-to-fullscreen playback.
- Verify that only the selected stream is decoded.
- Restore the correct wall page on return.

### Phase 4: Camera management

- Add list, create, edit, delete, reorder, and connection-test flows.
- Keep optional RTSP credentials compatible with the current unauthenticated server.

### Phase 5: Settings

- Add playback, paging, screen, boot, menu, and go2rtc preferences.
- Surface the temporarily accepted unauthenticated-LAN warning.

### Phase 6: Discovery

- Add go2rtc import first.
- Add ONVIF discovery and bounded RTSP candidate scanning afterward.

### Phase 7: Visual identity

- Add the launcher icon and final interface polish.
- Recheck Android 5 rendering, memory use, and touch targets.

## Commit and review policy

Use small, buildable commits such as:

```text
Extract reusable camera player view
Add backward-compatible camera repository
Add bottom navigation and home screen
Add single-camera fullscreen view
Add camera management screen
Add manual RTSP camera form
Add RTSP connection testing
Add go2rtc stream discovery
Add application settings
Add launcher icon
Document camera management and discovery
```

Every feature branch must pass CI and real-device verification before merge.
Do not merge a partially working UI into `main`.

## Real-device acceptance checklist

- Existing three streams still open in the 2x2 wall.
- Paging, aspect ratio, fullscreen, and reconnect behavior remain correct.
- Tapping a tile opens only that camera and Back restores the wall state.
- Camera changes survive process restart and device reboot.
- Invalid RTSP input does not break other cameras.
- Boot launch and keep-screen-on behavior remain correct.
- Memory and CPU use remain acceptable on the ASUS K012.
- The known-good APK can be restored if any checkpoint fails.

## Security follow-up

The current unauthenticated LAN deployment is tracked separately in
[`SECURITY_RISK_REGISTER.md`](SECURITY_RISK_REGISTER.md). The application will
remain compatible with unauthenticated streams during development while being
ready for dedicated RTSP credentials when server hardening is scheduled.
