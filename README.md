# Camera Wall

[![Android CI](https://github.com/droltr/camera-wall/actions/workflows/android.yml/badge.svg)](https://github.com/droltr/camera-wall/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://developer.android.com/about/versions/lollipop)

The app targets Android 16 (API 36) and remains installable on Android 5.0 (API 21) and newer devices.

A minimal Android app that turns an old x86 tablet into a dedicated RTSP
camera monitor. It was built for an ASUS MeMO Pad 7 K012 running Android 5.0.

## Features

- Fixed 2x2 landscape grid in immersive full-screen mode
- Full camera frames without cropping or stretching
- Automatic paging for more than four cameras, every three seconds
- Decoding only for streams on the visible page
- RTSP over TCP with an 800 ms network cache
- Hardware-accelerated H.264 decoding and disabled audio
- Automatic reconnect attempts every 15 seconds
- Start on boot and keep the screen awake while visible

## Development status

Camera Wall is in active alpha development. The ASUS K012 and the existing
camera wall must remain operational at every accepted checkpoint.

Follow live status in the public
[Camera Wall Alpha Roadmap](https://github.com/users/droltr/projects/2), or
review the ordered parent tracker in
[#31](https://github.com/droltr/camera-wall/issues/31).

| Workstream | Status | Tracking |
|---|---|---|
| Proven playback foundation | Complete | [#18](https://github.com/droltr/camera-wall/issues/18), [#16](https://github.com/droltr/camera-wall/issues/16) |
| Home and bottom navigation | Complete | [#10](https://github.com/droltr/camera-wall/issues/10) |
| Tap-to-fullscreen camera | Complete | [#13](https://github.com/droltr/camera-wall/issues/13) |
| Camera management | Complete | [#19](https://github.com/droltr/camera-wall/issues/19), [#11](https://github.com/droltr/camera-wall/issues/11), [#20](https://github.com/droltr/camera-wall/issues/20), [#17](https://github.com/droltr/camera-wall/issues/17), [#21](https://github.com/droltr/camera-wall/issues/21) |
| Application and go2rtc settings | Complete | [#22](https://github.com/droltr/camera-wall/issues/22), [#23](https://github.com/droltr/camera-wall/issues/23) |
| go2rtc stream import | Complete | [#24](https://github.com/droltr/camera-wall/issues/24) |
| ONVIF and bounded RTSP discovery | Complete | [#25](https://github.com/droltr/camera-wall/issues/25), [#26](https://github.com/droltr/camera-wall/issues/26) |
| Launcher icon | Complete | [#27](https://github.com/droltr/camera-wall/issues/27) |
| Network hardening | Risk accepted temporarily | [#28](https://github.com/droltr/camera-wall/issues/28) |
| Extended ASUS K012 stability test | Alpha checkpoint recorded | [#29](https://github.com/droltr/camera-wall/issues/29) |

Alpha milestones: [v0.2 foundation](https://github.com/droltr/camera-wall/milestone/1),
[v0.3 camera management](https://github.com/droltr/camera-wall/milestone/2),
[v0.4 settings](https://github.com/droltr/camera-wall/milestone/3),
[v0.5 go2rtc import](https://github.com/droltr/camera-wall/milestone/4),
[v0.6 discovery](https://github.com/droltr/camera-wall/milestone/5), and
[v0.7 hardening](https://github.com/droltr/camera-wall/milestone/6).

See [docs/DEVELOPMENT_PROCESS.md](docs/DEVELOPMENT_PROCESS.md) for how an item
moves from plan to code, real-device validation, merge, changelog, and alpha
release. Completed history remains visible through closed Issues, merged pull
requests, the changelog, and GitHub Releases.

## Compatibility

The application requires Android 5.0 (API 21) or newer. The APK packages
**x86**, **armeabi-v7a**, and **arm64-v8a** so the original ASUS K012 and ARM
phones/tablets can use the same alpha build.

## Camera configuration

Camera URLs are read at build time from an untracked `camera.properties` file.
Copy the supplied template, then edit it:

```bash
cp camera.properties.example camera.properties
```

```properties
camera.1.name=FRONT DOOR
camera.1.url=rtsp://go2rtc.example:8554/frontdoor
camera.1.username=camera_wall
camera.1.password=replace-with-a-strong-password
camera.2.name=OFFICE
camera.2.url=rtsp://go2rtc.example:8554/office
```

Entries must be numbered consecutively from 1. Every four cameras form a page.
The `username` and `password` fields are optional, so existing unauthenticated
go2rtc streams continue to work. When RTSP authentication is enabled later,
credentials can be added without changing application source code.
`camera.properties` is ignored by Git because RTSP URLs may contain credentials.
The compiled APK still contains the configured URLs, so distribute private
builds accordingly.

For cameras with low concurrent-session limits, prefer a restream server such
as Frigate's go2rtc or MediaMTX instead of connecting directly to each camera.

## Build

Requirements:

- JDK 17
- Android SDK with compile SDK 35

```bash
JAVA_HOME=/path/to/jdk-17 \
ANDROID_HOME=/path/to/android-sdk \
./gradlew clean assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Releases

Tags matching `v*` trigger the release workflow. It builds a signed universal
APK containing x86 and ARM ABIs,
generates a SHA-256 checksum, and attaches both files to a GitHub Release.
The project is currently in alpha; use tags such as `v0.3.0-alpha.2`. Alpha
tags are automatically marked as pre-releases on GitHub. Beta and stable
versions are intentionally deferred until the application matures. Published
APKs contain no camera configuration; add cameras on the device after
installation. Configure these repository secrets before publishing:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Encode the keystore with `base64 -w 0 release.keystore`. Never commit camera
credentials, the keystore, or signing passwords.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Changes are recorded in
[CHANGELOG.md](CHANGELOG.md).

## License

Licensed under the [MIT License](LICENSE).

## Security

Review the current accepted risks and planned hardening work in
[docs/SECURITY_RISK_REGISTER.md](docs/SECURITY_RISK_REGISTER.md). Do not expose
Frigate or go2rtc service ports to the internet.

The staged interface, camera-management, discovery, compatibility, testing,
branching, and commit plan is maintained in [docs/ROADMAP.md](docs/ROADMAP.md).
