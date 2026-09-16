# Camera Wall Roadmap

## Primary constraint

The ASUS K012 currently running Camera Wall must remain operational throughout
development. The `main` branch represents the known-good state. New work must
be developed on feature branches, built by CI, and tested on the real tablet
before merge.

The project remains in alpha. Beta and stable releases are intentionally
deferred until the interface, persistence, recovery, and device tests mature.

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

## Remaining backlog

1. **Server-side network hardening (#28, deferred):** authenticate/restrict
   Frigate and go2rtc APIs and RTSP, rotate credentials, and verify firewall
   boundaries. This must be coordinated with the deployment and is not an app-
   only change.
2. **Extended pre-beta test:** run the documented 30-minute+ playback,
   reconnect, paging, fullscreen, reboot, and memory/CPU checks on the K012.
3. **Orientation polish:** determine whether rotation belongs to camera source,
   device mounting, or an explicit per-camera transform setting.
4. **ARM physical-device validation:** install the universal APK on an ARM
   device; x86 APK contents are already verified on the K012.
5. **Release preparation:** configure signing secrets, create a signed
   `0.3.0-alpha.1` GitHub pre-release, and attach checksums.
6. **Optional polish:** apply all settings to every player option, improve
   discovery-result import UX, and add automated UI/regression tests.

No beta or stable work is scheduled until the first two items are complete.

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
