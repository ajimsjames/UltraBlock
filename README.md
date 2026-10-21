# UltraBlock 🛡️🚫

[![Android](https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV-blue.svg?style=flat-square&logo=android)](https://github.com/ajimsjames/UltraBlock)
[![License](https://img.shields.io/badge/License-GPL%20v3-red.svg?style=flat-square)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/ajimsjames/UltraBlock?style=flat-square)](https://github.com/ajimsjames/UltraBlock/releases)
[![Rules Blocked](https://img.shields.io/badge/Rules%20Sinkholed-563%2C061-orange.svg?style=flat-square)]()

A high-performance **Local VPN Ad & Telemetry Blocker** built specifically for **Android TV, Google TV, and Android Smartphones & Tablets**.

---

## 🌟 Key Features

- **Zero-Latency Local DNS Sinkhole**: Blocks ads, tracking domains, and OEM analytics on-device without routing data through third-party remote servers.
- **563,000+ Blocked Hosts**: Preloaded with high-impact ad, spyware, and TV telemetry filter lists (Sony BRAVIA, MediaTek, Google TV telemetry, Hotstar/Jio ads).
- **Dual TV & Touch Interaction**:
  - **Android TV Remote Friendly**: Toggle individual apps or global protection effortlessly using D-Pad `OK` keys.
  - **Mobile Touch Ready**: Full touch swipe & tap support for Android phones and tablets.
- **Android Quick Settings Tile**: Includes `UltraTileService` to easily turn protection ON/OFF directly from the system notification shade.
- **Granular Per-App Bypass**: Allows unblocking or bypassing specific streaming apps that require unhindered DNS resolution.
- **Real-Time Sinkhole Counter**: Live telemetry counters showing total blocked requests and blocked domains.

---

## 🚀 Installation

### Quick Install via ADB
```bash
adb connect <DEVICE_IP>:5555
adb install -r UltraBlock-v1.0.apk
```

### Build from Source
```bash
git clone https://github.com/ajimsjames/UltraBlock.git
cd UltraBlock
./gradlew assembleDebug
```

---

## 📜 License
GPL v3.0. Private, open-source telemetry filtering for all Android devices.
