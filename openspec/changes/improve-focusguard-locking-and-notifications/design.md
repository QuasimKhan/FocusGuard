# Design

## Context

`MainActivity` owns the Compose tabs and polls lock state once per second. `FocusPrefs` stores global app modes, daily schedule times, Pomodoro state, and lock state in `SharedPreferences`. `FocusAccessibilityService` currently handles blocking directly from broad window/content events, sends users Home, then starts `MainActivity` for an intervention. There is no independent schedule app set or schedule-boundary receiver. `FocusNotifier` creates comic-nudge and update channels, while `MainActivity` requests notification permission at startup without consuming the result. `BootReceiver` restores the VPN and adjusts reboot-sensitive timers, but does not reschedule daily transitions.

The app supports API 26+, targets API 35, already uses Compose Material 3 and AndroidX Core, and has no test source tree. The relevant contracts are the four new capabilities in the delta specs.

## Goals / Non-Goals

**Goals:**

- Make schedule membership and its effective lock decision explicit, persistent, and independent from global app modes.
- Use one policy result for accessibility enforcement, intervention copy, countdowns, and notifications.
- Keep blocking functional if optional notification or exact-alarm access is denied, while reporting the degraded experience.
- Make anti-uninstall UX accurately describe the best-effort protection available to a regular installed app.
- Resolve release blockers found in the scoped audit, including source-controlled signing credentials and inaccurate user-facing protection claims.

**Non-Goals:**

- Replacing the local VPN/DNS design, blocklist, update service, or general Compose navigation.
- Turning FocusGuard into a device/profile owner or promising protection after the user disables its Accessibility Service.
- Broad refactoring unrelated to the five requested behaviors or reproducible release blockers in the affected paths.

## Decisions

### 1. Persist schedule selections separately and centralize policy evaluation

Add a dedicated scheduled-package set in `FocusPrefs`; keep the existing global blocked and paused sets intact. Extract a side-effect-free lock policy that receives the package, current local time, master state, protection-lock state, global mode, schedule set, and temporary-pass state, then returns an allow, pause, or block decision with a reason and optional end time.

Precedence is: enforcement disabled permits the app; an active global Block remains a global block; an active schedule blocks packages in its own set; global Pause applies when no valid pass exists; otherwise the app is allowed. The schedule adds no restriction outside its window, but it does not erase an independently configured global Block or Pause mode. An active schedule continues to count as a protection lock for existing anti-tamper controls.

The Study OS tab will reuse the launchable-app inventory pattern from the Apps tab for its separate selection list and use a minute-resolution Compose Material time picker, retaining quick time adjustments if useful. The schedule remains local wall-clock time, includes its start minute, excludes its end minute, supports overnight windows, and preserves the existing equal-times-means-all-day behavior. Schedule selection defaults empty on upgrade; existing global modes and schedule enable/time values are preserved rather than copied or rewritten. The UI must explain the new independent list.

### 2. Coordinate one-shot schedule boundaries and local-time recovery

Introduce a schedule coordinator that computes the next start or end instant from persisted local time, current date, and device timezone. It schedules one-shot boundaries rather than a repeating alarm, recalculating after schedule edits, boot, app replacement, local date/time/timezone changes, and exact-alarm access changes. A boundary receiver records an idempotent boundary token, posts the corresponding notification, and asks a connected accessibility service to reevaluate its active window; it does not launch an activity from the receiver.

For precise boundary delivery, use `AlarmManager` exact alarms only after checking and explaining the user-granted `SCHEDULE_EXACT_ALARM` special access. Android does not pre-grant this to fresh installs targeting API 33+, and it can be revoked; reschedule after its state-change broadcast. If access is denied, use an inexact idle-compatible fallback for alerts and always evaluate the current local-time policy on qualifying foreground transitions. The Study OS UI reports that exact mid-session transitions and alerts may be delayed. See [Android alarm scheduling guidance](https://developer.android.com/develop/background-work/services/alarms).

The schedule coordinator owns start/end idempotency so a time change, reboot, or duplicate receiver delivery cannot produce repeated notifications for one boundary. Timezone and daylight-saving changes recalculate from local wall time instead of shifting stored minute values.

### 3. Gate enforcement on the active application window and render a service-owned overlay

Separate app-block decisions from page-content checks. Hard app blocks and pauses are considered only on a foreground/window-state transition after checking the event package against the active focused application window and its root package. Continue to use content-change events for Shorts and browser URL checks, but only when the same foreground validation succeeds. Ignore FocusGuard's own overlay, launcher/system UI, notification shade, IME, and unrelated transient windows. The service already requests interactive-window access; use active/focused window information rather than trusting `event.packageName` alone. Android documents the active window as the one being touched or holding input focus in the [AccessibilityService API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

Render a full-screen Compose intervention from a service-owned `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` window. This is restricted to a connected Accessibility Service and avoids requiring the separate draw-over-other-apps permission; see the [window type reference](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY). A small overlay controller owns attachment, lifecycle, and removal. It receives the policy result, shows target label, lock reason, known remaining time, and friendly copy, and reevaluates while visible. When the schedule ends, it removes itself if no independent lock remains. The safe-exit button explicitly sends the user Home; enforcement never does so automatically. Remember the dismissed target until focus leaves it to prevent a redirect/re-overlay loop.

The existing 15-second Pause behavior remains a full-screen mindful-pause state: after its countdown, Proceed grants the existing temporary pass and opens the app; Exit safely returns Home. A blocked app without a known timed end shows no fabricated countdown. Schedule-boundary broadcasts may trigger a rescan only through the connected service, which can inspect the still-visible app and show/remove the overlay as policy changes.

### 4. Narrow uninstall interception to the actual confirmation

Remove the generic `focusguard` text match as a reason to leave Settings/App Info. Retain the existing protection for private DNS, date/time, and Device Admin settings. When a protection lock is active, inspect only known installer confirmation surfaces and cancel only when accessible text identifies FocusGuard as the uninstall target; prefer the dialog's cancel action and use Back only after confirming the target. Then show a dismissible protection explanation. Never block uninstall confirmation for another package.

Do not call `DevicePolicyManager.setUninstallBlocked` or claim that ordinary Device Admin enrollment can guarantee uninstall prevention: Android documents that API for device/profile owners or a specially delegated caller, not a regular active admin ([DevicePolicyManager API](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setUninstallBlocked(android.content.ComponentName,java.lang.String,boolean))). Update Armor labels and README language to distinguish this best-effort accessibility intervention from OS policy. If an OEM installer does not expose a reliable target identity, fail open and show the limitation rather than cancelling an ambiguous dialog.

### 5. Make notifications permission-aware, distinct, and deduplicated

Keep existing update and comic-nudge channel IDs so Android user channel preferences remain intact. Add stable channels for schedule transitions and app-block alerts. Use AndroidX `NotificationCompat` already available through the current Core dependency, with a valid monochrome FocusGuard small icon; app-specific alerts use the target app's icon as a large icon and fall back safely if unavailable. Use a stable per-package notification ID and persisted boundary token to replace or deduplicate repeat events.

Move `POST_NOTIFICATIONS` handling out of startup side effects. Explain the benefit when the user enables scheduled alerts or nudges, request through the Activity Result API on API 33+, inspect the result, and expose disabled app/channel state with a Settings link. Denial never changes enforcement. Android 13+ can suppress non-exempt notifications after denial; see [notification permission guidance](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

Post lock/unlock notifications from the boundary receiver and app-specific block notifications from the validated intervention path. Use a small pool of friendly Hinglish copy, including both examples in the spec; avoid shaming language. Do not create an unbounded stream for repeated attempts. Respect users who disable a channel because Android owns channel importance after creation.

### 6. Keep release-readiness work bounded and reproducible

Remove release signing credentials from tracked Gradle configuration and read them from ignored local Gradle properties or environment-provided release secrets. Release builds fail with a clear message when credentials are absent; debug builds remain unaffected. Update README and Armor text only for claims contradicted by the implemented behavior or Android platform limits. Other audit findings enter this change only when reproducible and directly block acceptance of the specified schedule, blocking, uninstall UX, or notification behavior.

## Risks / Trade-offs

- [Exact-alarm access is denied or later revoked] → Continue clock-based foreground enforcement, use the inexact alert fallback, reschedule on permission changes, and show degraded precision in Study OS.
- [Accessibility event order or OEM installer views differ] → Validate both event package and active root/window, fail open on ambiguous system UI, and verify representative API levels and OEM variants.
- [A full-screen accessibility overlay can obscure the target app] → Attach it only after a confirmed effective lock, expose an explicit safe-exit control, remove it immediately when its rule expires, and test accessibility focus/readout.
- [The overlay is unavailable when Accessibility Service is disabled] → Keep the existing service-status banner and explain that app blocking and this self-imposed overlay require the service.
- [Android suppresses notifications or users change channel settings] → Keep enforcement independent, inspect permission/channel state, and report unavailable alerts without claiming delivery.
- [Best-effort uninstall detection is bypassable on some devices] → Scope the claim to active protection plus an enabled service, fail open on ambiguous dialogs, and document the limitation rather than escalating to unsupported ownership APIs.
- [An empty new schedule list surprises existing users] → Preserve old global app modes unchanged and explain that scheduled selections are now configured separately; do not silently migrate global blocks into the new set.

## Migration Plan

1. Add the scheduled-package preference with an empty default; retain current start/end, enablement, global app modes, temporary passes, and existing notification preferences.
2. Keep existing notification channel IDs and add new IDs for lock and block events. Do not delete or recreate user-configured channels.
3. On upgrade, explain that users must choose apps in the new Daily Auto-Lock list. Do not copy the old global list or change whether those apps are globally Open, Paused, or Blocked.
4. Make release signing configuration external to source control. Local release builds must supply the signing values; debug builds and already-installed APK data remain unaffected.
5. Rollback is additive: old SharedPreferences keys and channels remain readable, and removing the new schedule/overlay code does not delete user data.
