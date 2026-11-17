# Proposal

## Why

FocusGuard currently couples its daily study window to global app controls, reacts to broad accessibility events by sending users home, and has no scheduled lock/unlock notifications. These gaps make scheduled focus unpredictable and interventions abrupt; the existing Device Admin and notification UX also overstates or underdelivers what the app can enforce.

## What Changes

- Give the daily window its own persisted app selection and minute-resolution start/end controls. Schedule membership adds a block only while the window is active; it does not alter an app's separate global Open/Pause/Block setting.
- Centralize effective app-lock decisions and require evidence that a selected app owns the user-facing active window before enforcing a block. Replace automatic home redirects with a dedicated full-screen intervention that explains the active rule, shows remaining schedule time when available, and offers an explicit safe exit.
- Allow normal navigation through FocusGuard's App Info page. During an active protection lock, detect and dismiss the system uninstall confirmation when accessible, then explain why; describe this as best-effort protection rather than an OS-level uninstall guarantee.
- Add schedule-boundary and app-block notifications with explicit runtime-permission handling, stable channels, target-app large icons, deduplication, and a friendly Hinglish message pool. Use one-shot boundary scheduling and recover it after reboot, time/date changes, timezone changes, and schedule edits.
- Include a bounded release-readiness pass for defects directly affecting these behaviors and reproducible blockers found in the touched app paths. This includes removing hard-coded release-signing credentials from tracked build configuration and aligning user-facing claims with platform limits; it does not authorize an unrelated app-wide redesign.

## Capabilities

### New Capabilities

- `daily-app-locks`: Independent app selection, minute-level daily schedule, boundary behavior, and schedule-specific lock reasons.
- `foreground-app-interventions`: Reliable foreground verification, effective lock evaluation, and full-screen block/pause interventions.
- `anti-uninstall-ux`: Non-disruptive App Info navigation and best-effort cancellation of uninstall confirmation during an active protection lock.
- `focus-notifications`: Permission-aware, scheduled lock/unlock and throttled app-block notifications with app icons and Hinglish copy.

### Modified Capabilities

None. The project has no published OpenSpec capabilities yet.

## Impact

Primary code areas are `MainActivity.kt`, `FocusPrefs.kt`, `FocusAccessibilityService.kt`, `FocusNotifier.kt`, `BootReceiver.kt`, the Android manifest, and device-admin/accessibility resources. The schedule may add an `AlarmManager` receiver and exact-alarm special-access flow; the design must retain a truthful fallback when that access or notification permission is denied. No new library is assumed. The release-readiness pass also touches `app/build.gradle.kts` and README claims. The project currently has no test source tree, so focused policy/schedule verification and an Android API/device acceptance matrix are part of implementation planning.
