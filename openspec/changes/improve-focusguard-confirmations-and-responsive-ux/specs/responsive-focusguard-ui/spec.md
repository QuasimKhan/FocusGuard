# Spec Delta

## Purpose

Keeps FocusGuard's existing dashboard usable and readable across phone sizes, orientations, system text scales, and the app's current light and dark appearances.

## ADDED Requirements

### Requirement: Dashboard content adapts to available space
The system MUST keep each dashboard tab's controls readable, reachable, and free from clipping or overlap across supported phone widths, heights, orientations, and system text scales.

#### Scenario: Compact or short phone layout
- **GIVEN** FocusGuard is displayed on a compact phone or in a short landscape window
- **WHEN** the user opens any dashboard tab or confirmation dialog
- **THEN** content fits the available area, scrolls when needed, and leaves primary controls reachable without horizontal clipping

#### Scenario: Large system text scale
- **GIVEN** the user has increased Android's display text size
- **WHEN** the user opens a dashboard tab, dialog, or app intervention
- **THEN** labels and actions remain readable and do not overlap, disappear, or become inaccessible

#### Scenario: System bars and navigation insets
- **GIVEN** the device displays status or navigation system bars
- **WHEN** the user views or scrolls a dashboard tab
- **THEN** important content and bottom actions remain visible above system UI and respond to system insets

### Requirement: Study OS app selection remains responsive
The system MUST keep Study OS controls, app search, and selection responsive when the device has a large installed-app list, including while app identities and icons are resolved.

#### Scenario: Open Study OS with many installed apps
- **GIVEN** the device has many launchable apps
- **WHEN** the user opens Study OS
- **THEN** schedule and timer controls become usable without waiting for every app row and icon to be rendered

#### Scenario: Search and change an app selection
- **GIVEN** the Study OS app picker contains many apps
- **WHEN** the user searches, scrolls, or changes one app selection
- **THEN** the interface remains responsive and the changed selection is reflected without disrupting other rows

### Requirement: Disabled Accessibility service has actionable guidance
When FocusGuard's Accessibility Service is disabled, the system MUST explain which features depend on it and provide clear steps for enabling it and returning to FocusGuard.

#### Scenario: User opens FocusGuard with Accessibility disabled
- **GIVEN** the Accessibility Service is disabled
- **WHEN** the user sees the service status guidance
- **THEN** FocusGuard explains that app blocking and the intervention overlay need the service, gives the Android settings path, and provides an action to open Accessibility settings

#### Scenario: User returns after enabling Accessibility
- **GIVEN** the user opened Android Accessibility settings from FocusGuard
- **WHEN** the user returns to the app
- **THEN** FocusGuard refreshes the service status and shows the corresponding active or still-disabled guidance

### Requirement: Armor branding is a subdued footer
The Armor tab MUST show the installed app version and developer branding centered near the bottom of its content with reduced visual emphasis.

#### Scenario: Armor tab reaches its footer
- **GIVEN** the user views the Armor tab
- **WHEN** the user reaches the end of its content
- **THEN** the FocusGuard version and developer credit appear centered with lower emphasis than settings and actions
