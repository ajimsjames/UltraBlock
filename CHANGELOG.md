# Changelog

All notable changes to **UltraBlock** will be documented in this file.
Version numbering follows Semantic Versioning (`+0.0.1` per release).

---

## [1.0.2] - 2026-10-22
### Added
- **Operation Mode Selector (Root vs Local VPN)**:
  - Added support for rooted devices (`su` / KernelSU / Magisk) to apply system-level `/etc/hosts` sinkholing without maintaining an active VPN profile.
  - Quick mode toggle pill on the main dashboard (`MODE: VPN ▾` vs `MODE: ROOT ▾`).
- **Interactive Live Query Inspector**:
  - Tapping any domain in the Live Query list now opens an instant actions modal.
  - One-tap **Always Allow / Whitelist** domain (overrides all blocklists).
  - One-tap **Always Block / Blacklist** domain.
  - One-tap **Copy Domain Name** to clipboard.
- **Custom DNS / NextDNS / Pi-hole Upstream Input**:
  - Added custom resolver option allowing users to enter private DNS IPs (e.g., local home Pi-hole `192.168.1.100` or custom NextDNS IPv4).

---

## [1.0.1] - 2026-10-21
### Added
- **Material Day & Night Monochrome Theming**:
  - Crisp Ceramic White canvas for Day mode.
  - Deep Pitch Obsidian Black for Night mode.
  - Full automatic adaptation to Android system dark mode toggles.
- **Dedicated Firewall Modules Page**:
  - Relocated OEM telemetry & ad-network category switches into a dedicated full-screen `ModulesActivity`.
- **Snooze / Pause Shield**:
  - 5-minute, 15-minute, and 1-hour temporary pause buttons.
- **Upstream DNS Presets**:
  - Cloudflare (1.1.1.1), Cloudflare Security (1.1.1.2), AdGuard DNS (94.140.14.14), Quad9 (9.9.9.9), Google (8.8.8.8).
- **Auto-Start & Battery Fix**:
  - Added `BootReceiver` for auto-start on device boot.
  - Standby battery optimization exemption request.

---

## [1.0.0] - 2026-10-21
### Initial Release
- High-performance local DNS VPN firewall with 563k+ preloaded rules.
- Dual Android TV (D-Pad remote) & Mobile Touchscreen architecture.
- Android Quick Settings toggle tile (`UltraTileService`).
- Per-app ad and tracker unblocking controls.
