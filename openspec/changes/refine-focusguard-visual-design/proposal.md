# Proposal

## Why

FocusGuard already has a recognizable shield-and-search logo and a cohesive dark palette, but its screens rely on dense custom rows and small controls, the main header lacks the brand mark, and app choices and the block overlay do not show app icons. A visual-only refinement will make the existing controls easier to scan and the intervention screen feel intentional while preserving every current feature and behavior.

## What Changes

- Establish a classic, minimal visual system with consistent neutral surfaces, one restrained accent, readable typography, spacing, cards, borders, and touch target presentation across light and dark appearances.
- Integrate the existing FocusGuard mark in the dashboard header, blocked overlay, and relevant empty states.
- Refine app list and picker rows with the installed app icon, clear name hierarchy, rounded icon containers, and a restrained indication of each existing mode or selection state.
- Restyle the blocked overlay to present FocusGuard branding, target app icon and name, the existing lock reason and timer, and its existing actions in a calm, readable layout.
- Keep all changes presentational: preserve existing state, rules, timers, actions, permissions, notifications, and data. Do not add screens, settings, or interactions.

## Capabilities

### New Capabilities

- `focusguard-visual-presentation`: Defines the visual presentation of the dashboard, app rows and pickers, empty states, and existing blocked-app overlay.

### Modified Capabilities

None. The repository currently has no main OpenSpec capabilities; this change defines a visual presentation contract without changing existing app functionality.

## Impact

- Compose UI and theme definitions in `app/src/main/java/com/focusguard/app/MainActivity.kt`.
- Existing native view construction for the accessibility block overlay in `FocusAccessibilityService.kt`, limited to rendering and styling.
- Existing launcher brand artwork in `app/src/main/res/drawable/ic_launcher_foreground.xml` and adaptive launcher resources; reuse these without external image dependencies.
- No business logic, data model, service triggers, timers, permissions, notifications, or dependencies are in scope.
