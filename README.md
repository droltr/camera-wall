# Camera Wall

[![Android CI](https://github.com/droltr/camera-wall/actions/workflows/android.yml/badge.svg)](https://github.com/droltr/camera-wall/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://developer.android.com/about/versions/lollipop)

Camera Wall turns an Android phone or tablet into an RTSP camera monitor. The
screen follows device rotation and adapts the camera grid to portrait or
landscape. The ASUS MeMO Pad 7 K012 (x86, Android 5.0) is the legacy-device
target; the same universal APK also packages ARM 32-bit and ARM 64-bit native
libraries.

The current published version is **v0.3.0-alpha.2**. It is an alpha release.

## Features

### Live camera wall

- Choose a **4-camera 2x2** or **8-camera 4x2** landscape layout. The selection
  is saved on the device. In portrait, the 8-camera layout changes to 2 columns
  and 4 rows.
- Swipe left or right across the camera wall to move between camera pages.
  Automatic paging remains available for additional pages.
- When more cameras are configured than fit in the selected layout, automatic
  paging can cycle through the remaining camera groups. It can be enabled or
  disabled and its interval can be adjusted.
- Tap a tile to open that camera in a single-camera full-screen view.
- Pinch to zoom in or out in the single-camera view. On-screen plus/minus
  controls, a reset button, and double-tap zoom reset are also available.
- Preserve the video aspect ratio, show optional camera-name labels, and display
  connection status while a stream connects or recovers.
- Play RTSP without audio using TCP transport. The player retries a failed
  connection automatically.
- Keep the screen awake while the wall is open and launch the app automatically
  after device boot.

### Camera management

- Add RTSP cameras manually. Search the saved list, hold the **Sürükle** handle
  and drop a camera in its new position to reorder it, or edit and delete it.
- Test a camera connection while editing its settings.
- Camera names, stream addresses, and optional per-camera RTSP usernames and
  passwords are stored in the app's private local preferences. A discovered
  candidate can be saved with both fields blank; enter credentials later by
  editing that camera if the stream requires them.
- Import configured stream names from a go2rtc server.
- Discover ONVIF device endpoint candidates on the local network. Discovery
  lets you select candidates to save; it does not automatically add cameras.
  ONVIF device endpoints are not RTSP video paths, so review or edit the saved
  RTSP address before playback.
- Optionally scan the connected local `/24` network for open RTSP ports
  `554`, `8554`, and `10554`. This finds host/port candidates only; it does not
  guess stream paths or credentials. Select any candidates to save them, then
  edit the RTSP path if needed. Credentials are optional during save and can be
  entered later. The scan is user-started and can be cancelled.
- Recognize Hikvision-style `/Streaming/Channels/` URLs. When a go2rtc host is
  configured, supported Hikvision channels can use the corresponding go2rtc
  restream. The ASUS K012 playback path falls back from the main stream to the
  camera's lower-bandwidth substream when applicable.

### Settings and appearance

- Modern dark interface with a dedicated home, Cameras, and Settings section.
- Rotate the device between portrait and landscape. The eight-camera grid uses
  four rows by two columns in portrait and two rows by four columns in landscape.
- Configure paging interval, automatic paging, RTSP-over-TCP, network buffer,
  camera labels, and keep-screen-on behavior. Paging and layout controls use
  labeled selectors; the buffer value is shown in milliseconds and seconds.
- Enter and test a go2rtc server address with optional credentials.
- Export and restore camera and app settings in a password-encrypted backup.
  The backup includes saved stream addresses and credentials. Choose device
  storage in the file picker to keep the backup on the tablet; use a strong
  passphrase of at least 12 characters and keep it safe, because it cannot be
  recovered if forgotten.
- View the app version, Android version, and license in **About**.
- Reset app preferences to their defaults.

## Compatibility

The release is **one universal APK**, not separate Android 5 and Android 16
packages:

| Item | Support |
|---|---|
| Minimum Android version | Android 5.0 (API 21) |
| Target SDK | Android 16 (API 36) |
| Native ABIs | `x86`, `armeabi-v7a`, `arm64-v8a` |
| ASUS K012 | x86 ABI is included |

ABI and Android-version support describe package compatibility. Actual stream
playback also depends on the device, camera, and local network.

## Camera setup

You can add cameras in the app after installation. For a local development APK,
the build can also use `camera.properties` as its initial camera list:

```bash
cp camera.properties.example camera.properties
```

Edit the copied file with your own values. For example:

```properties
camera.1.name=Front Door
camera.1.url=rtsp://go2rtc.example:8554/frontdoor
camera.1.username=
camera.1.password=
camera.2.name=Office
camera.2.url=rtsp://go2rtc.example:8554/office
camera.2.username=
camera.2.password=
```

Camera entries must be numbered consecutively from `1`. Keep real addresses and
credentials out of source files and Git. `camera.properties` is ignored by Git.
Local debug APKs built with this file contain the configured values; handle
those APKs as private files. The public release build deliberately starts with
an empty camera list.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36

Build a local debug APK:

```bash
JAVA_HOME=/path/to/jdk-17 \
ANDROID_HOME=/path/to/android-sdk \
./gradlew clean assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. If
`camera.properties` exists, its camera entries are included in this local debug
build.

The Android CI workflow builds the debug APK for pushes and pull requests to
`main`. Tagged releases are built as signed, public APKs by GitHub Actions; the
workflow uses repository signing secrets and does not include local camera
configuration.

## Releases

Download the current APK and its SHA-256 checksum from
[GitHub Releases](https://github.com/droltr/camera-wall/releases). The release
workflow creates an alpha pre-release for tags such as `v0.3.0-alpha.2`.

The public release build uses `-PpublicRelease=true`, which excludes
`camera.properties` even if a local copy exists. Maintainers must configure
these GitHub Actions repository secrets for signing:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never commit camera addresses, usernames, passwords, keystores, or signing
passwords.

## Security and privacy

Camera configuration and go2rtc preferences are stored in the app's private
data on the device. A local debug APK may also contain values from
`camera.properties`; the public release build excludes them. Review
[`.gitignore`](.gitignore) before adding local configuration or generated APKs
to Git. Never publish a personalized APK that contains private camera data.

The app connects to the go2rtc HTTP API when importing streams or testing the
server. HTTP does not encrypt credentials or responses. Use this feature only
on a trusted, isolated local network, prefer HTTPS where available, and do not
expose go2rtc or camera service ports to the public internet. Read the
[security risk register](docs/SECURITY_RISK_REGISTER.md) before deployment.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). User-visible changes belong in
[CHANGELOG.md](CHANGELOG.md).

## License

Camera Wall is licensed under the [MIT License](LICENSE).
