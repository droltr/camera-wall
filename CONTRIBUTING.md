# Contributing

Thank you for improving Camera Wall.

## Development workflow

1. Fork the repository and create a focused branch.
2. Copy `camera.properties.example` to `camera.properties` and use test URLs.
3. Build with JDK 17 using `./gradlew clean assembleDebug`.
4. Test on an x86 Android 5.0+ device or emulator.
5. Open a pull request describing the change and how it was tested.

Do not commit RTSP credentials, private IP details, keystores, APKs, build
outputs, or local Android SDK settings. Keep changes compatible with API 21
and the x86 target unless a project decision explicitly changes that scope.

Bug reports should include Android version, device model, relevant logs with
credentials removed, reproduction steps, and expected versus actual behavior.
