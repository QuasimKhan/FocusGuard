# FocusGuard by Quasim.™

[![Latest Release](https://img.shields.io/github/v/release/quasimkhan/FocusGuard?style=flat-square&color=38BDF8)](https://github.com/quasimkhan/FocusGuard/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/quasimkhan/FocusGuard/total?style=flat-square&color=10B981)](https://github.com/quasimkhan/FocusGuard/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-slate.svg?style=flat-square)](LICENSE)

A zero-bloat, 100% on-device Android distraction blocker, short-form video filter, local DNS adult firewall, and tamper-resistant study lock crafted for deep focus.

---

## Why FocusGuard?

- **Block Shorts & Reels Only:** Automatically exits YouTube Shorts and Instagram Reels while keeping long-form lectures, search, and DMs working normally.
- **3-Mode App Control (`Open` · `15s Pause` · `Block`):** Hard-block addictive apps or add a 15-second mindful breathing pause before unlocking a 5-minute pass.
- **On-Device Adult & Web Shield (IPv4 + IPv6):** Blocks 130,000+ adult domains, rotating mirror domains, and custom `*wildcard*` rules across Chrome, Brave, Firefox, and Incognito tabs—with zero external VPN servers or internet slowdown.
- **Study OS:** Built-in Deep-Work Pomodoro timer (`25m` / `50m` / `90m`) with daily progress tracking and recurring daily auto-lock schedules.
- **Tamper-Resistant Armor:** Prevents app uninstallation via Device Admin, blocks Settings tampering during active sessions, and enforces monotonic cooldown timers that cannot be bypassed by changing the phone's clock.
- **100% Private:** No accounts, no analytics, no cloud servers. Everything runs locally on your phone.

---

## How to Download & Install on Android

> **Note:** FocusGuard uses Android Accessibility and local VPN APIs for system-wide blocking, so it is distributed directly via verified GitHub Releases rather than the Google Play Store.

### Step 1: Download the APK

1. On your Android phone, open the [**Latest Release Page**](https://github.com/quasimkhan/FocusGuard/releases/latest).
2. Under **Assets**, tap **`FocusGuard-v1.1.0-Quasim.apk`** (or the latest version) to download it.
3. Open the downloaded `.apk` file and tap **Install** (allow **Install unknown apps** for your browser or file manager if prompted).

### Step 2: Enable the Accessibility Engine (Android 13–16)

1. Open **FocusGuard** and tap **Enable** on the _Accessibility Service Offline_ banner.
2. Under **Downloaded apps**, tap **FocusGuard**.
    - _If FocusGuard is greyed out ("Restricted setting"):_ Open **Phone Settings → Apps → FocusGuard → tap the 3-dot menu (`⋮`) in the top-right corner → Allow restricted settings**, then return to Accessibility and switch **FocusGuard** ON.

### Step 3: Keep FocusGuard Running in the Background

Android phone brands aggressively close background services unless exempted once:

1. Long-press the **FocusGuard** icon on your home screen → tap **App info**.
2. Set **Battery** to **Unrestricted / No restrictions**.
3. On **Xiaomi / POCO / Redmi / Realme / Vivo / Oppo**, also turn **Autostart** ON.

### Step 4: Activate Web Shield & Uninstall Protection

1. Open the **Web** tab in FocusGuard → switch **Local DNS Adult & Web Shield** ON and tap **OK** on Android's VPN connection prompt.
    - _Chrome Tip:_ Open **Chrome Settings → Privacy and security → Use secure DNS** and turn it **Off** so Chrome uses FocusGuard's local DNS shield.
2. Open the **Armor** tab → tap **Activate Device Admin Uninstall Lock** to prevent impulsive uninstallation.

---

## Automatic Updates

FocusGuard checks GitHub Releases automatically every 24 hours (or manually via **Armor → Check Updates**). Whenever a new version is released, an **Update Available** banner appears at the top of the app—tap **Download** and install the APK over your existing app without losing any settings or blocklists.

---

## Support & Contact

- **Report a Bug or Request a Feature:** [Open a GitHub Issue](https://github.com/quasimkhan/FocusGuard/issues)
- **Email:** [quasimdotdev@gmail.com](mailto:quasimdotdev@gmail.com)
- **X (Twitter):** [@quasimdotdev](https://x.com/quasimdotdev)
- **LinkedIn:** [linkedin.com/in/quasimkhan](https://linkedin.com/in/quasimkhan)
- **License:** Released under the [MIT License](LICENSE) by **Quasim.™**
