<div align="center">

# 🎛️ StreamDeck Android

**Bridge Elgato Stream Deck hardware directly to Android via USB.**  
Optimized for automotive head units, tablets, and phones.

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Elgato](https://img.shields.io/badge/Hardware-Elgato_Stream_Deck-0078D7?style=flat-square)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

</div>

---

## 📖 Overview

**StreamDeck Android** interfaces directly with 15-key Elgato Stream Deck devices over USB using the Android Open Accessory / USB Host API. Designed from the ground up for **in-car infotainment systems** (FYT, UIS7862, Rockchip, Allwinner) as well as everyday Android devices, providing reliable macro triggers, custom dials, and dynamic key displays on the road.

---

## ✨ Features

- **Direct USB Host Control**: Zero root required. Communicates over raw USB endpoints via the Android USB Host API.
- **Auto-Detection & Launch Reconnect**: Automatically refreshes and connects to any attached Stream Deck on app open, resume, or boot with progressive retry polling (0s, 0.8s, 1.5s, 3s, 5s) to handle slow-enumerating USB ports on Android head units.
- **Multi-Device Support**: Full protocol handling across both generations of 15-key hardware:
  - **Stream Deck V1** (PID: `0x0060`): Raw HID output reports (BMP display format).
  - **Stream Deck V2 / MK.2** (PID: `0x006D`, `0x0080`): Native JPEG bulk-transfer pipeline.
- **Dynamic LCD Key Display**: Dual-tone gradient backings, specular gloss curves, radial rim glows, dark translucent text pills, 30+ vivid color options, and custom PNG/JPG image assets with live in-app preview.
- **3-Tier Background App Shortcuts**: Launches apps even when the Stream Deck app is minimized or closed (`PendingIntent` + `FLAG_ACTIVITY_NEW_TASK` + car stereo `monkey` shell fallback).
- **Desktop Configuration Web Panel**: Built-in HTTP server listening on port `8080` for drag-and-drop key configuration from any laptop, Mac, or PC on your car's Wi-Fi or hotspot.
- **360° Deck Orientation**: Rotate 0°, 90°, 180°, or 270° with dynamic LCD image rotation and key coordinate mapping.
- **Automotive-First Architecture**:
  - Auto-start on boot / vehicle ignition (`BOOT_COMPLETED`, `QUICKBOOT_POWERON`).
  - Quiet background connection on USB attachment without stealing focus from navigation or CarPlay.
  - Low-overhead background service designed to survive aggressive OEM memory managers.

---

## 🔌 Hardware Compatibility

| Model | Keys | Protocol | Image Format | Status |
| :--- | :---: | :---: | :---: | :---: |
| **Stream Deck V1** (PID: `0x0060`) | 15 | HID / Feature Reports | BMP | Supported |
| **Stream Deck V2** (PID: `0x006D`) | 15 | Bulk Transfer | JPEG | Supported |
| **Stream Deck MK.2** (PID: `0x0080`) | 15 | Bulk Transfer | JPEG | Supported |
| **Stream Deck Mini** | 6 | — | — | Planned |
| **Stream Deck XL** | 32 | — | — | Planned |

---

## 🚗 Target Platforms

- **Car Head Units**: FYT (UIS7862 / 7862S), Rockchip PX5 / PX6, Allwinner T3 / T8
- **Standard Devices**: Android phones and tablets (Android 8.0 Oreo or newer)
- **Requirements**: USB-OTG support and a compliant powered USB hub (recommended for automotive setups)

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0+ (API Level 26+)
- USB-OTG cable or dedicated vehicle USB port

### Installation
1. Download the latest **`StreamDeck-v1.0.1.apk`** from the [Releases](https://github.com/wyattissoquiet-byte/StreamDeck-Android/releases/latest) page.
2. Install on your Android device or car stereo head unit.
3. Plug in your 15-key Elgato Stream Deck via USB.
4. Grant the USB permission prompt (select *"Always open Stream Deck when this USB device is connected"*).
5. *(Optional)* Connect your PC/Mac to the car's Wi-Fi and open `http://<head-unit-ip>:8080` to configure keys visually!

---

## 📄 License

MIT License - free for personal and commercial use.