# Proposal

## Why

The current block overlay deliberately opens Android's notification shade and removes itself for System UI events, allowing the blocked app to become usable. An active Daily Auto-Lock window also locks Apps-section mode controls, coupling two settings that need to remain independent.

## What Changes

- Keep Apps-section Open/Pause/Block controls editable when Daily Auto-Lock is the only active protection. The schedule continues blocking only apps selected in Study OS; selecting Open in Apps does not override an active schedule for an app still selected there.
- Allow users to remove an app from the active Daily Auto-Lock selection with confirmation; its separate global Apps rule remains in effect.
- Keep the full-screen intervention attached and touch-consuming while its block rule applies. Stop forwarding its edge swipes to Notifications or Quick Settings, dismiss the notification shade if Android opens it during a block, and do not remove the intervention for transient System UI events.
- Recreate the intervention using current display bounds after orientation changes, preserving the active target and Pause deadline. Keep the landscape layout compact and free of vertical scrolling.
- Do not add Do Not Disturb or notification-policy access. Android may still display OS-owned heads-up notifications above an accessibility overlay on some devices; such events must not clear the app lock or expose the blocked app beneath the overlay.

## Capabilities

### New Capabilities

- `daily-app-locks`: Keep the Study OS selection independent from Apps-section modes and allow schedule membership changes without rewriting global rules.
- `accessible-focus-interventions`: Keep the block surface active through notification gestures and transient system windows, and rebuild it correctly after rotation.

### Modified Capabilities

None. The repository has no published capabilities in `openspec/specs/`; these contracts currently exist in change deltas.

## Impact

Likely code areas are `FocusPrefs.kt`, `MainActivity.kt`, `FocusAccessibilityService.kt`, `InterventionOverlayRules.kt`, and `OverlayWindowFrame.kt`. The change should retain the existing policy and accessibility overlay without adding a dependency or notification-policy permission. The in-progress `improve-focusguard-confirmations-and-responsive-ux` change currently requires notification shade and Quick Settings gestures to remain usable; that conflicting acceptance must be reconciled before both changes are treated as final.
