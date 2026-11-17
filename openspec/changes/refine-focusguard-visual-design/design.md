# Design

## Context

See [proposal.md](proposal.md) for motivation and [the visual presentation spec](specs/focusguard-visual-presentation/spec.md) for the user-visible contract. The dashboard, all four tabs, app rows, and schedule picker are Compose functions in `MainActivity.kt`; the current theme is a single hard-coded dark scheme. The brand mark is the vector launcher foreground drawable. The app-mode list has text-only rows, and Study OS uses checkbox rows without app icons. The intercepted app and mindful-pause surfaces are constructed from native Android views in `FocusAccessibilityService.showInterventionOverlay`.

The visual-only guardrail means the existing callbacks and state sources remain authoritative. The overlay may be edited to arrange and style views, but its lock evaluation, countdown, pause gating, dismissal behavior, button callbacks, and window lifecycle are outside this design.

## Goals / Non-Goals

**Goals:**

- Give Compose screens a consistent neutral surface and typography hierarchy that follows the system's light or dark appearance.
- Reuse the existing vector logo mark and installed-app icons in the specified visual anchors.
- Make existing selected modes, scheduled-app selection, and overlay information easy to scan while retaining the current controls.
- Keep rendering changes local, with no new image dependencies, persisted data, or changes to user state.

**Non-Goals:**

- Changing app blocking, schedule or timer behavior, permissions, notifications, accessibility event handling, or data storage.
- Adding new onboarding flows, settings, navigation, or app-selection actions.
- Redesigning or replacing the FocusGuard logo or changing the launcher icon.

## Decisions

1. **Use a system-aware Material color scheme, with semantic status colors kept separate.** Define coordinated light and dark schemes for the existing `MaterialTheme` entry point and replace decorative hard-coded panel, text, and divider colors with scheme roles. Keep distinct success, warning, and error accents only where they already communicate status. This keeps all Compose tabs aligned and avoids adding a theme preference. An always-dark scheme was considered, but would not meet the requested balanced light/dark presentation; a new in-app appearance setting was also considered and rejected because it adds state and interaction outside the visual scope.

2. **Use the existing launcher foreground vector as the in-app brand mark.** Render the mark at a compact scale in the dashboard header and in an appropriately larger, quiet treatment on the overlay; use a smaller treatment in app-list empty states. Preserve the launcher resources and avoid a second logo asset or external image library. A text-only title was considered but does not provide the requested brand continuity.

3. **Render installed-app icons without changing persisted or domain models.** Resolve each icon from the existing package name through `PackageManager`, convert it for Compose presentation, and cache only for the current UI composition as needed. Use a rounded neutral icon holder, a stable visual fallback if an installed icon cannot be loaded, and the existing app label. Keep mode controls and schedule checkboxes wired to their present callbacks. Extending persisted app records or introducing a dependency-backed drawable painter was considered and rejected as unnecessary for a presentation-only update.

4. **Treat the block overlay as a native-view restyling, not a service redesign.** Reorganize its existing view tree into a centered brand area, a contained target-app identity, readable reason and timer, and the same existing buttons. Use resource-backed or configuration-aware colors and rounded drawable backgrounds so this surface can follow light/dark appearance despite being outside Compose. Keep all existing view references needed by the current ticker and change only presentation attributes and layout parameters. Replacing the overlay with an Activity or Compose host was rejected because that would change window/lifecycle behavior and exceed scope.

5. **Standardize spacing and control presentation through small shared UI helpers.** Use common card shapes, borders, insets, title/body/caption styles, and at least 48 dp interactive hit areas where custom clickable chips or rows currently fall below a comfortable target. Preserve all control labels, modes, selection state, enablement, and callback behavior. Where three mode controls compete for width, allow responsive spacing or wrapping rather than shrinking hit targets.

6. **Present empty results inline in existing lists.** When an existing app search yields no entries, show a short no-results message and a restrained brand mark in the list area. Do not add a separate screen or alter filtering/search state.

## Risks / Trade-offs

- **Installed app icons can be missing or visually inconsistent** → Use a stable neutral fallback and keep icon sizing and holder treatment uniform; do not let icon resolution affect row state or actions.
- **Compose Material colors and native overlay colors could drift** → Define corresponding light/dark token values in one documented palette and verify both screen types together.
- **A denser row can wrap on narrow devices or with long app names** → Ellipsize names as they are today, preserve the current search, and use responsive row layout without reducing touch targets.
- **Visual edits near service code could accidentally alter blocking behavior** → Keep changes to overlay construction/style declarations and verify the existing decision, timer, dismissal, and click-handler logic remains unchanged in review.

## Migration Plan

No migration is required: the design introduces no persisted values, permissions, dependencies, or changes to existing app state. Rollback is a source-level revert of the Compose theme/components and overlay presentation changes; no user data cleanup is needed.
