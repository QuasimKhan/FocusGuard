# Spec Delta

## Purpose

Keeps a clear, accessible app intervention active whenever a configured block applies, including after transient Android system UI interruptions and across screen orientations.

## ADDED Requirements

### Requirement: Active Block Intercepts Notification Gestures
The intervention MUST remain attached and consume interaction while its block rule applies. It MUST NOT invoke Notifications or Quick Settings actions or remove itself in response to transient System UI events.

#### Scenario: Swipe from a system edge while blocked
- **GIVEN** the intervention is visible for an app with an active block rule
- **WHEN** the user swipes from a system edge within the intervention's touch surface
- **THEN** FocusGuard does not invoke Notifications or Quick Settings, and the blocked app remains inaccessible behind the intervention

#### Scenario: The notification shade opens during a block
- **GIVEN** the intervention is visible for an app with an active block rule
- **WHEN** Android opens the notification shade while that rule remains active
- **THEN** FocusGuard dismisses the shade and keeps the intervention attached over the blocked app

#### Scenario: A heads-up notification appears
- **GIVEN** the intervention is visible for an app with an active block rule
- **WHEN** Android displays a heads-up notification or another transient system window
- **THEN** the block decision and overlay target remain active, and the intervention remains available without reopening FocusGuard or changing tabs

#### Scenario: User exits the intervention
- **GIVEN** the intervention is visible for a blocked app
- **WHEN** the user selects its Exit to Home action
- **THEN** FocusGuard returns Home and does not repeatedly reopen the intervention until the blocked app is foregrounded again

### Requirement: Rotation Rebuilds the Active Intervention
The system MUST rebuild the intervention from current display bounds after an orientation change while preserving the active target, effective rule, and Pause deadline.

#### Scenario: Rotate while the blocked app is foreground
- **GIVEN** a blocked app owns the foreground and its intervention is visible
- **WHEN** the device rotates between portrait and landscape
- **THEN** the intervention immediately adapts to the new safe display bounds and continues blocking without requiring the user to reopen FocusGuard or change tabs

#### Scenario: Landscape intervention fits without scrolling
- **GIVEN** a block or pause intervention is displayed in landscape
- **WHEN** the display has a short landscape height
- **THEN** its title, target, reason, applicable remaining time, and available actions are visible without vertical scrolling

#### Scenario: Android draws a notification above the overlay
- **GIVEN** Android displays an OS-owned notification surface above the accessibility intervention
- **WHEN** the notification appears or disappears
- **THEN** the active app-lock decision remains unchanged and the blocked app is not exposed for interaction
