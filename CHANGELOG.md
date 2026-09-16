# Changelog

Notable changes are documented here. This project follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed

- Synchronize the camera list with go2rtc and collapse stream aliases that use
  the same producer source.
- Make section navigation clearer with larger labeled tabs and custom icons.
- Support portrait and landscape rotation, swiping between camera pages, and zoom controls in single-camera view.
- Use labeled compact selectors for wall size, automatic paging, and page duration; explain network buffer in milliseconds and seconds.
- Keep camera management reorderable by drag-and-drop and searchable as the saved list grows.

### Added

- Let users select discovered RTSP and ONVIF candidates to save locally, then add credentials later if needed.
- Add password-encrypted settings/camera backup and restore plus an About screen.
- Add regression tests for backup encryption, wrong passwords, tampering, and passphrase validation.

## [0.3.0-alpha.2] - 2026-09-16

### Added

- Switchable 4-camera and 8-camera wall layouts with saved preference
- Hikvision channel restream routing through the locally configured go2rtc host
- New Camera Wall logo and refreshed camera, navigation, and settings screens

### Changed

- Use Hikvision low-bandwidth substreams and software decoding for older tablets
- Keep local camera configuration and device session notes out of GitHub releases
- Target Android 16 (API 36) while retaining Android 5.0 (API 21) support
- Upgrade LibVLC to 3.7.0 for current native playback support

## [0.3.0-alpha.1] - 2026-09-14

### Added

- Persisted application and go2rtc server settings with masked credentials
- go2rtc stream import, ONVIF discovery, bounded local RTSP scanning, and launcher icon
- ASUS K012 alpha stability smoke-test record

## [0.2.0-alpha.1] - 2026-09-14

### Added

- Reusable libVLC camera player shared by wall and fullscreen views
- Private ordered camera repository seeded from the build configuration
- Compact Home, Cameras, and Settings bottom navigation
- Tap-to-fullscreen single-camera view with visible and system Back support
- Optional per-camera RTSP username and password fields for future server hardening
- Security risk register for the current trusted-LAN deployment
- Staged roadmap for navigation, camera management, discovery, and device-safe delivery
- Public development-status table and Issue-to-release workflow documentation
- Build-time camera configuration through an ignored local properties file
- Android CI and signed GitHub Release workflows
- Dependabot configuration, contribution guide, and issue templates
- MIT license

### Changed

- Documentation now accurately states the three-second page interval and x86/ARM ABI support

## [0.1.0-alpha.1] - 2026-09-13

### Added

- Initial 2x2 RTSP camera wall application

[Unreleased]: https://github.com/droltr/camera-wall/compare/v0.3.0-alpha.2...HEAD
[0.3.0-alpha.2]: https://github.com/droltr/camera-wall/compare/v0.3.0-alpha.1...v0.3.0-alpha.2
[0.3.0-alpha.1]: https://github.com/droltr/camera-wall/compare/v0.2.0-alpha.1...v0.3.0-alpha.1
[0.2.0-alpha.1]: https://github.com/droltr/camera-wall/compare/v0.1.0-alpha.1...v0.2.0-alpha.1
[0.1.0-alpha.1]: https://github.com/droltr/camera-wall/releases/tag/v0.1.0-alpha.1
