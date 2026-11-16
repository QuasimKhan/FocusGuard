# FocusGuard by Quasim.™

A zero-bloat, 100% on-device Android focus engine, short-form feed blocker, local DNS adult & distraction firewall, and tamper-resistant study lock built with native **Kotlin** and **Jetpack Compose**.

---

## Core Capabilities

- **3-Mode App Control (`Open` · `15s Pause` · `Blocked`):** Hard-block pure distractions or add a 15-second mindful breathing pause before granting a 5-minute pass to semi-useful apps.
- **YouTube Shorts & Instagram Reels Surgeon:** Blocks addictive short-form feeds at the UI node level while keeping long-form YouTube lectures, search, and Instagram DMs working normally.
- **On-Device IPv4 + IPv6 DNS Sinkhole:** Blocks 130,000+ adult domains, DNS-over-HTTPS endpoints, rotating numbered mirror domains (`*xhamester*`, etc.), and custom `*wildcard*` rules across Chrome, Brave, Firefox, and Incognito tabs—with zero external VPN servers.
- **Study OS:** Includes a Deep-Work Pomodoro timer (`25m` / `50m` / `90m`) with daily focus stats and an automatic daily study schedule window.
- **Tamper-Resistant Armor:** Uses Android Device Admin to grey out the Uninstall button, a Settings Watchdog to block tampering with Accessibility/Private DNS/Date & Time during active locks, and monotonic uptime (`SystemClock.elapsedRealtime()`) cooldown timers that cannot be bypassed by changing the system clock.

---

## How to Download & Install on Android

1. Open the [**Releases**](https://github.com/quasimkhan/FocusGuard/releases/latest) page on your Android phone.
2. Under **Assets**, tap **`FocusGuard-v1.0-Quasim.apk`** to download it.
3. Open the downloaded APK and allow **Install unknown apps** for your browser/file manager if prompted.
4. **Enable Accessibility Service (Android 13–16 Restricted Settings):**
    - Open FocusGuard and tap **Enable** on the Accessibility banner.
    - If Android greys out FocusGuard under _Downloaded apps_, open **Settings → Apps → FocusGuard → tap the 3-dot menu (`⋮`) in the top-right → Allow restricted settings**, then return to Accessibility and turn **FocusGuard** ON.
5. **Prevent Background Battery Killing (Xiaomi / Samsung / OnePlus / Vivo):**
    - Long-press the **FocusGuard** app icon → **App info** → enable **Autostart** (if available) and set **Battery** to **Unrestricted / No restrictions**.
6. **Activate Web Shield & Uninstall Protection:**
    - Open the **Web** tab → turn on **Local DNS Adult & Web Shield** (and turn **Off** _Use secure DNS_ in Chrome Settings).
    - Open the **Armor** tab → tap **Activate Device Admin Uninstall Lock**.

---

## App Updates

FocusGuard checks `github.com/quasimkhan/FocusGuard/releases/latest` automatically once every 24 hours (and manually via **Armor → Check Updates**). When a new version is published, an in-app **Update Available** banner appears with a one-tap download button. Installing the new APK updates FocusGuard in place while preserving all blocklists, schedules, and active cooldowns.

---

## Build From Source

Requires **JDK 17** and **Android SDK 35**:

```bash
git clone [https://github.com/quasimkhan/FocusGuard.git](https://github.com/quasimkhan/FocusGuard.git)
cd FocusGuard
echo "sdk.dir=$HOME/android-sdk" > local.properties
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
