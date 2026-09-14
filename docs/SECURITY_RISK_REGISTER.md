# Security Risk Register

This document records known deployment risks without publishing private
addresses, camera names, credentials, or network topology.

## RISK-001: Unauthenticated camera and service access on the local network

- **Status:** Temporarily accepted
- **Severity:** High
- **Scope:** Current trusted-LAN deployment
- **Review trigger:** Before allowing guest or untrusted devices onto the LAN,
  exposing any service through a router/VPN/reverse proxy, or shipping the next
  production release

### Observed condition

The deployed Frigate/go2rtc instance currently exposes an RTSP restream without
authentication. Its go2rtc HTTP API and Frigate's internal unauthenticated API
are also reachable from the local network. Home Assistant login protection does
not protect clients that connect directly to those service ports.

### Impact

An untrusted device on the same network may be able to watch known or discovered
camera streams. Unauthenticated management or configuration endpoints may also
reveal sensitive camera configuration and credentials or permit privileged API
operations. Internet exposure would substantially increase the risk.

### Temporary controls

- No router port forwarding for Frigate/go2rtc service ports
- Treat the LAN as trusted and restrict guest/IoT lateral access where possible
- Keep camera URLs and credentials out of Git
- Distribute private APK builds only to controlled devices

These controls reduce likelihood but do not remove the vulnerability.

### Planned remediation

1. Stop publishing Frigate's internal unauthenticated API port to the LAN.
2. Stop publishing the go2rtc HTTP API, or bind and authenticate it appropriately.
3. Enable a dedicated username and strong password on the go2rtc RTSP server.
4. Configure Camera Wall with its dedicated RTSP credentials.
5. Restrict RTSP access at the firewall to approved viewer devices where practical.
6. Rotate upstream camera credentials after closing unauthenticated configuration access.
7. Verify that no related ports are forwarded from the internet.

### Application readiness

Camera Wall supports optional `username` and `password` fields per camera. Empty
fields preserve today's unauthenticated behavior. This allows server hardening
to be tested camera by camera without changing source code or disrupting the
current deployment.

### Closure criteria

- Unauthenticated RTSP playback from an ordinary LAN client fails.
- Unauthenticated go2rtc configuration/API requests fail or are unreachable.
- Frigate's internal unauthenticated API is reachable only by intended internal services.
- The tablet continues to display all configured streams using dedicated credentials.
- Camera and service credentials have been rotated and remain absent from Git history.
