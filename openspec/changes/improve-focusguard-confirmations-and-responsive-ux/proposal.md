# Proposal

## Why

FocusGuard currently applies several blocking and schedule changes immediately, gives little guidance when Accessibility is disabled, and has a Study OS app picker that eagerly builds and loads icons for the full installed-app list. Its full-screen intervention also covers system gesture areas, while some layouts and the Armor footer need to adapt better to different phone sizes and text scales.

## What Changes

- Add clear warning and confirmation dialogs before consequential enforcement is enabled or changed, including app Pause/Block rules, Shorts blocking, DNS shielding, the Daily Auto-Lock Window, locked Pomodoro sessions, and Armor's Strict Lock. Confirmed schedule time selections show the proposed time before saving; cancelling leaves the saved setting unchanged. Optional comic nudges remain direct.
- Allow users to remove an app from the Daily Auto-Lock selection during an active window after confirming that the schedule block for that app will end immediately; keep the schedule and the app's independent global rule intact.
- Make dashboard tabs, dialogs, and the block intervention adapt to narrow, short, landscape, and large-text layouts. Keep the block screen visually full-screen and persistent while its target app is foregrounded, while preserving system notification and navigation gestures and resuming enforcement when the blocked app is again foregrounded.
- Reduce Study OS picker lag by avoiding eager composition and icon loading for every installed app, and make selection updates responsive.
- Add actionable Accessibility setup guidance when its service is disabled, and move the Armor branding/version to a subdued, centered footer.
- Review the touched paths for directly related usability and state-handling defects and include regression coverage for confirmed behaviors.

## Capabilities

### New Capabilities

- `app-control-confirmations`: Confirmation behavior for consequential enforcement changes and schedule-time saves.
- `study-os-schedule-management`: Responsive schedule app selection, including explicit removal during an active window.
- `responsive-focusguard-ui`: Responsive dashboard and dialog layouts, setup guidance, app-picker responsiveness, and Armor footer presentation.
- `accessible-focus-interventions`: Access to notification and navigation system UI while the block intervention remains active.

### Modified Capabilities

None. The repository has no published OpenSpec capabilities yet. The in-progress `improve-focusguard-locking-and-notifications` change already defines the initial full-screen intervention and daily schedule contracts; this proposal refines those behaviors and should be coordinated with that change's remaining device acceptance.

## Impact

Primary code areas are `MainActivity.kt`, `FocusPrefs.kt`, `FocusAccessibilityService.kt`, and focused UI/state tests. The proposal uses existing Compose and Android APIs and assumes no new dependency. The accessibility overlay changes must be validated alongside the in-progress intervention work, especially across notification shade gestures, navigation, and device sizes.
