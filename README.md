# FocusGuard by Quasim.™

[![Latest Release](https://img.shields.io/github/v/release/quasimkhan/FocusGuard?style=flat-square&color=38BDF8)](https://github.com/quasimkhan/FocusGuard/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/quasimkhan/FocusGuard/total?style=flat-square&color=10B981)](https://github.com/quasimkhan/FocusGuard/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-slate.svg?style=flat-square)](LICENSE)

A zero-bloat, 100% on-device Android distraction blocker, short-form video filter, local DNS adult firewall, and focus study lock with best-effort tamper friction.

---

## Why FocusGuard?

- **Block Shorts & Reels Only:** Automatically exits YouTube Shorts and Instagram Reels while keeping long-form lectures, search, and DMs working normally.
- **3-Mode App Control (`Open` · `15s Pause` · `Block`):** Hard-block addictive apps or add a 15-second mindful breathing pause before unlocking a 5-minute pass.
- **On-Device Adult & Web Shield (IPv4 + IPv6):** Blocks 130,000+ adult domains, rotating mirror domains, and custom `*wildcard*` rules across Chrome, Brave, Firefox, and Incognito tabs—with zero external VPN servers or internet slowdown.
- **Study OS:** Built-in Deep-Work Pomodoro timer (`25m` / `50m` / `90m`) with daily progress tracking and a recurring, independently configured auto-lock app list.
- **Armor:** Best-effort focus protection while FocusGuard's Accessibility Service is enabled, plus monotonic cooldown timers that cannot be bypassed by changing the phone's clock.
- **100% Private:** No accounts, no analytics, no cloud servers. Everything runs locally on your phone.

---

## How to Download & Install on Android

> **Note:** FocusGuard uses Android Accessibility and local VPN APIs for system-wide blocking, so it is distributed directly via verified GitHub Releases rather than the Google Play Store.

### Step 1: Download the APK

1. On your Android phone, open the [**Latest Release Page**](https://github.com/quasimkhan/FocusGuard/releases/latest).
2. Under **Assets**, tap the latest FocusGuard APK for your device architecture (the current build is `1.4.0`).
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

### Step 4: Activate Web Shield & Focus Protection

1. Open the **Web** tab in FocusGuard → switch **Local DNS Adult & Web Shield** ON and tap **OK** on Android's VPN connection prompt.
    - _Chrome Tip:_ Open **Chrome Settings → Privacy and security → Use secure DNS** and turn it **Off** so Chrome uses FocusGuard's local DNS shield.
2. In **Armor**, optionally enable the Device Admin warning. Regular Device Admin enrollment cannot guarantee uninstall prevention. While a focus lock is active and Accessibility is enabled, FocusGuard attempts to dismiss a clearly identified FocusGuard uninstall confirmation; some OEM installer screens do not expose enough information and may fail open.

### Daily Auto-Lock Window

Open **Study OS**, enable **Daily Auto-Lock Window**, set the start and end times, then search and select the apps for this schedule. This list is independent of the global Open / 15s Pause / Block modes in the Apps tab. Schedule-only apps are usable outside the window, while any global rule remains in effect. The start minute is included, the end minute is excluded, windows can cross midnight, and matching start/end times mean all day.

On Android 12 and later, allow **Alarms & reminders** access in Study OS for precise mid-session transitions and boundary alerts. If access is unavailable, FocusGuard still checks the local schedule when a foreground app changes, but an already-open app and its alert may update late.

On Android 13 and later, enabling the schedule or Comic Focus Nudges explains and requests notification permission. Choose **Not now** to keep using FocusGuard without alerts; blocking remains active. The Study OS status shows whether app notifications and the Daily Auto-Lock, App Block, and Comic Nudge channels are available. If a channel is off, open Android **App info → Notifications** and enable that channel. Notification settings do not change lock enforcement.

---

## Automatic Updates

FocusGuard checks GitHub Releases automatically every 24 hours (or manually via **Armor → Check Updates**). Whenever a new version is released, an **Update Available** banner appears at the top of the app—tap **Download** and install the APK over your existing app without losing any settings or blocklists.

## Building from Source

Debug builds need no signing setup. Release builds require `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` supplied through user-level Gradle properties (for example, `~/.gradle/gradle.properties`) or CI environment variables. Keep these values and the keystore outside the repository; a release build reports a clear error when configuration is missing.

---

## Support & Contact

- **Report a Bug or Request a Feature:** [Open a GitHub Issue](https://github.com/quasimkhan/FocusGuard/issues)
- **Email:** [quasimdotdev@gmail.com](mailto:quasimdotdev@gmail.com)
- **X (Twitter):** [@quasimdotdev](https://x.com/quasimdotdev)
- **LinkedIn:** [linkedin.com/in/quasimkhan](https://linkedin.com/in/quasimkhan)
- **License:** Released under the [MIT License](LICENSE) by **Quasim.™**
