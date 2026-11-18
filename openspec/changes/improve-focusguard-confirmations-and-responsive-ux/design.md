# Design

## Context

See [proposal.md](proposal.md) for motivation and the four spec deltas for user-visible behavior. The dashboard and tabs are Compose functions in `MainActivity.kt`; the app already uses Material 3 and `BoxWithConstraints`, but several sections depend on fixed spacing and the main dashboard only applies status-bar insets. Study OS queries launchable apps while composing, builds every picker row in a scrollable `Column`, and resolves each app icon in the row composable. `FocusPrefs` guards schedule membership edits whenever any protection is active. The block intervention is a match-parent `TYPE_ACCESSIBILITY_OVERLAY` using `FLAG_LAYOUT_NO_LIMITS`, so it spans system gesture regions.

The in-progress `improve-focusguard-locking-and-notifications` change introduced foreground validation, schedule policy, and the service-owned overlay. This change refines those paths and should be verified against that change's remaining device-acceptance matrix.

## Goals / Non-Goals

**Goals:**

- Stage enforcement and schedule mutations so settings change only after the user confirms.
- Let users edit schedule membership during an active window without unlocking unrelated controls or changing global app modes.
- Keep dashboard, dialogs, and the service overlay usable in constrained layouts and system text scales.
- Keep package inventory and icon work off the first interactive frame of Study OS and render only the app rows the user can see.
- Preserve existing preference keys, schedule policy, notification behavior, and Android permission boundaries.

**Non-Goals:**

- Replacing Compose navigation, the current visual identity, the DNS engine, or the Accessibility Service enforcement model.
- Adding an appearance setting, a new settings screen, a new external dependency, or device-owner controls.
- Adding confirmations to optional comic nudges or unrelated low-impact preferences.

## Decisions

### 1. Treat each confirmation as a pending action

Keep one pending action in the Compose UI state, with enough information to display the affected control/app and the effect. A reusable confirmation dialog presents that description and has explicit confirm/cancel actions. Do not call `FocusPrefs` setters or start an Android permission flow when the initial control is tapped; call the existing setter only after confirmation. Dismissing the dialog follows the cancel path. Optional nudges stay on their direct path to avoid unnecessary prompts. Armor's Strict Lock action uses the same staged flow and commits its selected cooldown only after confirmation.

For schedule time changes, retain the value returned by the platform time picker as a candidate. Show it in a second dialog and only persist it and reschedule after confirmation. For DNS shielding, begin Android's VPN consent flow after the app-level confirmation and reflect the actual service state if the system request is declined.

A single generic warning screen was considered but would hide the specific consequence of each action. Separate hand-written dialogs per control were rejected because their cancel and commit behavior could drift.

### 2. Relax only active schedule membership edits

Add a narrow schedule-membership update path that remains available during an active Daily Auto-Lock Window. Keep schedule enablement, start/end times, and other protection settings behind their current lock guards. The UI confirms the affected app and immediate effect before using this path; after a confirmed edit, recompute the boundary and ask the connected Accessibility Service to reevaluate its active window. `LockPolicy` remains the source of the effective result, so a global Pause or Block continues to apply after schedule membership is removed.

Allowing every Study OS setting to become editable during a lock was considered and rejected: the request is specifically to manage the app list, and unrestricted schedule edits could weaken the whole active window unexpectedly.

### 3. Load Study OS app data without blocking interaction

Move launchable-app inventory work out of the initial UI composition and into an asynchronous load with a stable result state. Render the bounded picker with lazy, package-keyed rows; resolve/cache icons as rows become visible and retain the existing neutral fallback when icon loading fails. Search and selection changes operate on the loaded snapshot and update the changed row without rebuilding the entire eager list. Keep this in-memory and use existing Android/Compose APIs rather than adding a cache or image library dependency.

Keeping the nested eager `Column` was rejected because it constructs every row and performs package icon conversion even when most rows are off-screen. Loading app labels/icons before showing any Study OS controls was rejected because it preserves the perceived startup stall.

### 4. Base responsive layout on the actual window and insets

Use the available content width and height, system-bar/navigation insets, and text wrapping to size the existing layouts. Keep primary dashboard navigation available while allowing the selected tab body to scroll. The block surface covers the full display, while its interactive panel stays inside the usable content area and scrolls when large text or short landscape height requires it. Use wrapping/stacked actions in narrow layouts instead of shrinking controls.

For the Armor tab, keep its settings in the existing scrollable content and present version/developer credit as a subdued, centered bottom treatment with navigation-bar spacing. Do not pin a footer over actions on short screens.

Width-only breakpoints were considered insufficient because short landscape windows and large system text can overflow at otherwise wide widths. A new adaptive-layout dependency is unnecessary for this single-activity phone UI.

### 5. Preserve system gesture areas around the block intervention

Keep the intervention service-owned and retain its current foreground validation and lock reevaluation. Draw an opaque full-display surface so the blocked app is never visible around the intervention. Keep the explanatory panel within the content-safe region and make it scrollable. Route system-edge gestures to Android's notification, Quick Settings, back, or home actions; when a System UI window appears, remove the overlay and keep checking foreground state. Restore the full-screen intervention only after confirming the blocked app is foreground and its rule still applies. Reevaluate at a bounded interval while an intervention is active so a missed accessibility event cannot leave the block absent or stale.

Leaving transparent strips around the intervention was rejected because the blocked app remains visible and the block looks incomplete. Leaving an interactive overlay above system UI was rejected because it can hide notifications. The full-display visual surface therefore yields temporarily to system windows and is restored from a foreground and policy check.

### 6. Keep setup help in the existing service status surface

Expand the current Accessibility-off banner with concise, actionable Android settings steps and the features that depend on the service. Recheck `FocusAccessibilityService.isConnected` when the Activity resumes from system settings so the guidance reflects the actual state. Do not add a separate onboarding flow or imply that FocusGuard can enable Accessibility on the user's behalf.

## Risks / Trade-offs

- [Confirmation dialogs add friction] → Limit them to enforcement changes, active-window schedule membership edits, and schedule time saves; keep optional nudges direct.
- [Removing a scheduled app changes the active lock immediately] → Name the app and effect in the confirmation, preserve independent global rules, and reevaluate the foreground policy only after commit.
- [OEM gesture and accessibility-window behavior differs] → Validate notification shade, Quick Settings, navigation gestures, and return-to-block behavior on representative API levels and OEM devices before acceptance.
- [Asynchronous package/icon loading can produce missing or stale entries] → Keep a loading/empty state, use neutral icon fallbacks, and refresh the inventory when returning from system app changes if needed.
- [Large text and short windows constrain dialogs and overlays] → Allow content scrolling and verify all confirmation actions remain reachable at large font scales and in landscape.

## Migration Plan

No persisted data migration is needed. Keep existing settings and preference keys intact; previously enabled protections continue without a startup confirmation. New confirmation state exists only while a pending action is open. If the change is rolled back, the existing preferences remain readable and the prior schedule and enforcement behavior can resume.
