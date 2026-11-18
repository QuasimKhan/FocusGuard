# Privacy Policy for FocusGuard

**Effective Date:** November 19, 2026  
**Developer:** Quasim Khan (`Quasim.™`)  
**Package Name:** `com.focusguard.app`  
**Contact:** quasimdotdev@gmail.com  
**Source Code:** https://github.com/quasimkhan/FocusGuard

## 1. Overview
FocusGuard is an open-source digital wellbeing and study focus application for Android. Your privacy is fundamental to the design of FocusGuard: **all app blocking, short-form video filtering, study schedules, and DNS filtering happen 100% locally on your device.** FocusGuard does not operate backend tracking servers, does not use third-party analytics or advertising SDKs, and never collects, sells, or transmits your personal data.

## 2. Information Stored Locally on Your Device
All settings and statistics are stored exclusively in private local storage (`SharedPreferences`) on your device and are never uploaded externally:
- **App Rules & Schedules:** The list of apps you choose to block, pause, or restrict during Daily Auto-Lock Windows, along with your Pomodoro timer and Strict Lock cooldown preferences.
- **Custom DNS Blocklist:** Any custom domain names or wildcard rules you manually add.
- **Focus Statistics:** Local counts of completed study sessions and focused minutes.

You can delete all locally stored data at any time by clearing the app's storage in Android Settings or uninstalling FocusGuard.

## 3. Sensitive Permissions & How They Are Used
To enforce your focus rules reliably on Android, FocusGuard requests specific system permissions. Each permission is used strictly on-device for its stated user-facing feature:

- **Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`):**  
  Used solely to detect when a user-blocked app, YouTube Shorts, Instagram Reels, or restricted system settings (during an active Strict Lock) are opened on screen so FocusGuard can display your block or mindful-pause intervention. FocusGuard does **not** record keystrokes, read messages, capture passwords, or transmit screen content off your device.
- **Local VPN Service (`BIND_VPN_SERVICE`):**  
  When you enable the DNS & Adult Content Shield, FocusGuard creates a local, on-device loopback VPN interface to inspect DNS queries against your local blocklist and drop blocked domains. **No network traffic is routed through any external VPN server**—all DNS filtering occurs entirely inside your phone's memory.
- **Query All Packages (`QUERY_ALL_PACKAGES`):**  
  Used exclusively to display the list of installed launcher apps inside FocusGuard so you can select which apps to pause, block, or include in your Daily Auto-Lock Window. This app list never leaves your device.
- **Device Administrator (`BIND_DEVICE_ADMIN`):**  
  Optional tamper-protection feature that warns before Device Admin is deactivated while a focus lock is active.
- **Alarms & Notifications (`SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`):**  
  Used locally to trigger exact Daily Auto-Lock Window boundaries, timer completions, and optional focus reminders.

## 4. Network Requests (Update Checks Only)
FocusGuard makes only one optional outbound network request: querying the public GitHub Releases API (`api.github.com/repos/quasimkhan/FocusGuard/releases/latest`) to check whether a newer version of the app is available. This request transmits no personal identifiers, device IDs, or usage analytics.

## 5. Children's Privacy
FocusGuard does not knowingly collect personal information from anyone, including children under the age of 13, as no personal data is collected or transmitted by the application.

## 6. Changes to This Privacy Policy
If future updates introduce any changes to how FocusGuard handles data or permissions, this Privacy Policy will be updated in the official repository with a revised effective date.

## 7. Contact
If you have any questions about this Privacy Policy or FocusGuard's open-source code, contact the developer at **quasimdotdev@gmail.com** or open an issue at https://github.com/quasimkhan/FocusGuard/issues.
EOF

git add PRIVACY.md
git commit -m "Add Privacy Policy (PRIVACY.md) for app store listings"
git push origin main
