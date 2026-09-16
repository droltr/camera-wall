# ASUS K012 Alpha Stability Test

This is the pre-beta verification record for the Android 5 ASUS MeMO Pad 7
K012 (Wi-Fi ADB). Beta and stable releases remain deferred.

## Automated and real-device checks

- Debug APK builds successfully with the pinned JDK/Gradle toolchain.
- Application launched on the K012 and remained foreground for a 40-second
  smoke run.
- Home → Cameras → Settings → Home navigation completed on-device.
- No `FATAL EXCEPTION` or process crash was present in the captured logcat.
- Existing camera repository data remained available after navigation.

## Extended run checklist

The following should be repeated before a release candidate: boot launch,
30-minute playback, automatic paging, repeated fullscreen entry/exit,
disconnect/reconnect, settings persistence, and memory/CPU observation. Record
the APK commit, start/end time, stream set, and any recovery action here.

Known limitation: this smoke run is not evidence that the app is ready for
beta; it is a safe alpha checkpoint while the unauthenticated go2rtc risk
remains accepted.
