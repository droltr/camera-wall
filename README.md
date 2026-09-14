# Camera Wall

[![Android CI](https://github.com/droltr/camera-wall/actions/workflows/android.yml/badge.svg)](https://github.com/droltr/camera-wall/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://developer.android.com/about/versions/lollipop)

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

## Compatibility

The application requires Android 5.0 (API 21) or newer. The current build is
deliberately restricted to the **x86 ABI** for the target tablet. It will not
install on ARM-only devices.

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

Tags matching `v*` trigger the release workflow. It builds a signed x86 APK,
generates a SHA-256 checksum, and attaches both files to a GitHub Release.
The project is currently in alpha; use tags such as `v0.1.0-alpha.1`. Alpha
tags are automatically marked as pre-releases on GitHub. Beta and stable
versions are intentionally deferred until the application matures.
Configure these repository secrets first:

- `CAMERA_CONFIG_BASE64`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Encode `camera.properties` and the keystore with
`base64 -w 0 camera.properties` and `base64 -w 0 release.keystore`. Never
commit camera credentials, the keystore, or signing passwords.

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
