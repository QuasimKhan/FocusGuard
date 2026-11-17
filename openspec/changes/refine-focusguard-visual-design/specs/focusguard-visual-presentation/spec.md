# Spec Delta

## Purpose

Defines a calm, consistent visual presentation for FocusGuard's existing screens and app interventions, using the product's current identity while keeping all controls and behavior familiar.

## ADDED Requirements

### Requirement: Consistent light and dark visual hierarchy
The interface SHALL use readable typography, clear spacing, neutral backgrounds and card surfaces, and one restrained primary accent, adapting the same hierarchy to the device's current light or dark appearance.

#### Scenario: Dashboard is shown in light appearance
- **WHEN** the device uses light appearance and the user opens any dashboard tab
- **THEN** the screen, cards, text, dividers, and controls use a coordinated light palette with clear foreground contrast and distinct interactive states

#### Scenario: Dashboard is shown in dark appearance
- **WHEN** the device uses dark appearance and the user opens any dashboard tab
- **THEN** the screen, cards, text, dividers, and controls use a coordinated dark palette with clear foreground contrast and distinct interactive states

### Requirement: FocusGuard mark appears at key visual anchors
The interface SHALL reuse the existing FocusGuard brand mark in the main header and blocked-app overlay, and use it subtly in relevant app-list empty states without adding external image assets or dependencies.

#### Scenario: Main dashboard header is rendered
- **WHEN** the user views the main dashboard
- **THEN** a compact FocusGuard mark appears beside the FocusGuard title without displacing the existing status or master control

#### Scenario: Block overlay is rendered
- **WHEN** FocusGuard displays its existing blocked-app or mindful-pause overlay
- **THEN** the FocusGuard mark is a clear, prominent part of the screen's visual hierarchy

#### Scenario: App search has no matches
- **WHEN** a search in an existing app list or picker returns no matching apps
- **THEN** a concise empty message and subtle FocusGuard mark are shown in place of a blank list

### Requirement: App rows clearly communicate identity and current state
App rows in the Apps tab and Study OS schedule picker SHALL present the installed app icon in a rounded container, a legible app name, and the existing mode or selection state using text and visual treatment that remains distinguishable without color alone.

#### Scenario: Global app modes are displayed
- **WHEN** the user views the Apps tab
- **THEN** each app row shows its icon and name and clearly identifies its existing Open, 15s Pause, or Block mode

#### Scenario: Scheduled app selection is displayed
- **WHEN** the user views the Study OS app picker
- **THEN** each row shows its icon and name and clearly distinguishes whether it is currently selected for the schedule

#### Scenario: App row is used with touch input
- **WHEN** the user interacts with a mode, checkbox, or app-row control
- **THEN** the current actions remain easy to identify and have comfortable, consistent touch targets

### Requirement: Block overlay presents its existing information calmly
The blocked-app overlay SHALL visually group the FocusGuard mark, target app icon and name, existing lock explanation and remaining-time text, and the actions already available for that decision in a readable, uncluttered layout.

#### Scenario: App is blocked by an active rule
- **WHEN** the existing overlay is shown for a blocked app
- **THEN** the target app identity, current lock explanation, applicable remaining-time text, and existing safe-exit action are visually distinct and readable

#### Scenario: Mindful-pause overlay is shown
- **WHEN** the existing overlay is shown for a mindful pause
- **THEN** the target identity, pause text and countdown, and existing exit and proceed controls are visually distinct while preserving their current enabled states

### Requirement: Visual refinement preserves existing functionality
Visual updates SHALL preserve the existing UI state, control actions, availability rules, and resulting app behavior; they SHALL NOT introduce or remove a feature or interaction.

#### Scenario: Existing control is restyled
- **WHEN** a user activates an existing switch, app mode, schedule checkbox, timer control, or overlay button after the visual update
- **THEN** the control performs the same action and observes the same state and availability rules as before the visual update
