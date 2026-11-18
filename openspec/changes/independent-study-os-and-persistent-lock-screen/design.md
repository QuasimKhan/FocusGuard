# Design

## Context

See [proposal.md](proposal.md) for motivation and [the capability specs](specs/) for required behavior. `FocusPrefs` stores Daily Auto-Lock membership separately from Apps-section modes, but currently treats an active schedule as a global protection lock for editing those modes and removing schedule members. The accessibility overlay currently detaches for `com.android.systemui` and forwards edge swipes to global system actions. Its content already uses a compact landscape composition and a measured safe frame.

The in-progress changes `improve-focusguard-locking-and-notifications` and `improve-focusguard-confirmations-and-responsive-ux` touch these same paths. Refine the existing preference and overlay implementations rather than introducing duplicate storage or another overlay controller.

## Goals / Non-Goals

**Goals:**

- Keep the schedule package set as the only source for Daily Auto-Lock membership; retain each app's independent global mode.
- Allow Apps-section mode changes when Daily Auto-Lock is the only active lock. Keep selected apps blocked while the schedule window is active, regardless of their Apps-section mode.
- Allow confirmed removal from the active schedule without changing the app's global mode.
- Keep the full-screen intervention attached and consuming touch while transient System UI events occur; do not launch Notifications, Quick Settings, Back, or Home actions from edge swipes.
- Rebuild the intervention using current display bounds after orientation changes while preserving the active target and Pause deadline.

**Non-Goals:**

- Add Do Not Disturb or notification-policy access. Android may still draw an OS-owned heads-up notification above an accessibility overlay on some devices.
- Add device-owner lock task mode or change device-level navigation policy.
- Change schedule timing, global mode resolution, notifications, safe exit, or unrelated dashboard behavior.
- Add a dependency or replace the accessibility service.

## Decisions

### 1. Preserve the preference boundary and independent mode controls

Continue using the dedicated scheduled-package preference for Study OS selection. Every schedule picker write updates that set only; Apps continues to read and update global modes. `LockPolicy` remains the single resolver: schedule membership adds a block only during the active window, while independent global Pause or Block modes remain effective outside that window. Strict Lock and a locked Pomodoro continue preventing users from loosening restrictions.

The active Daily Auto-Lock window alone must not prevent changing an app's global mode. Setting a selected app to Open leaves it scheduled, so `LockPolicy` still blocks it until removal or window end. Removing an app during the active window requires confirmation and updates only schedule membership; the existing global mode remains an independent rule.

Deriving schedule membership from global blocked or paused sets was rejected because it would couple controls and change app modes when a schedule is edited.

### 2. Keep the intervention attached across transient system UI

Keep the overlay service-owned and attached while System UI events occur. Consume touches on the full-screen surface and do not dispatch global system actions from edge swipes. If Android still opens the notification shade, dismiss it while the target's block remains active. Recheck focus after transient System UI events, but do not treat them as an allow decision or clear the lock. Clear the target only after another app is confirmed foreground or the target's effective policy becomes Allow. The existing foreground watchdog remains the fallback for missed accessibility events.

Do Not Disturb and notification-policy access are excluded. Android may still show an OS-owned heads-up notification above the accessibility overlay on some devices; keep the overlay attached and the block active so the blocked app cannot be used when the notification leaves. Device-owner lock task mode is excluded because it requires kiosk setup and changes device-level navigation behavior.

### 3. Use a measured compact landscape composition

Continue using the full-display surface and safe content frame. Select the intervention layout from the current orientation and measured available width/height. In landscape, use a compact composition with tighter spacing, a reduced brand treatment, and side-by-side content/actions where space permits. Keep title, target, applicable reason/time, Exit, and the Pause Proceed action visible; allow labels to wrap and remove decorative copy before reducing essential information. Do not put the landscape composition in a vertical scroll container. Portrait can retain its established arrangement when its available height requires scrolling.

Rebuild the view on configuration changes while preserving the current pause deadline and active target; rotation must not restart a pause or clear its policy. A single fixed layout was rejected because usable height and insets vary across orientations and devices.

## Risks / Trade-offs

- [Android or an OEM does not deliver a useful focus event after transient System UI closes] → Revalidate on accessibility window events and keep the foreground watchdog so the intervention remains attached without reopening FocusGuard.
- [An OS-owned heads-up notification temporarily covers the block surface] → Keep the overlay attached and touch-consuming so the blocked app remains inaccessible after the notification leaves; FocusGuard does not suppress OS notifications.
- [Android handles a reserved system-bar gesture before the accessibility overlay receives it] → Do not dispatch notification or Quick Settings actions from the app; retain and revalidate the active block through any transient System UI window.
- [Landscape height, cutouts, or large text constrain the panel] → Measure the usable frame, reflow actions and labels, and keep required controls visible without scrolling in landscape.

## Migration Plan

No data migration is required. Keep existing schedule membership and global app-mode preferences intact; do not copy one set into the other. The overlay and layout changes are runtime-only. Rolling them back leaves current preferences readable and restores the previous presentation behavior.
