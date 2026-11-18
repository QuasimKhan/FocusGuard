# Spec Delta

## Purpose

Shows a persistent full-screen block while the blocked app is foregrounded, while letting users reach Android's notification shade and system navigation without bypassing the active app rule.

## ADDED Requirements

### Requirement: Full-screen intervention remains persistent and yields to system UI
The intervention MUST visually cover the entire display while its target app is confirmed foreground and its blocking rule remains active. Its explanatory content and actions MUST remain within the usable content area and scroll when necessary. Notification and navigation gestures MUST remain usable; FocusGuard MUST yield the overlay while Android system UI is foreground and MUST reevaluate the active rule before restoring the full-screen block.

#### Scenario: Blocked app receives a full-screen intervention
- **GIVEN** a blocking rule applies to the foreground app
- **WHEN** FocusGuard displays the intervention
- **THEN** the full display is covered by the block surface, while its panel stays within safe system-bar and gesture insets and scrolls to expose every action

#### Scenario: Intervention persists while the rule remains active
- **GIVEN** the blocked app remains foreground and its rule is active
- **WHEN** an accessibility event is delayed or missed or the display configuration changes
- **THEN** FocusGuard restores or retains the full-screen intervention after checking the current foreground and policy

#### Scenario: Open the notification shade from a blocked app
- **GIVEN** the intervention is visible for a blocked foreground app
- **WHEN** the user swipes from the status bar to open notifications or Quick Settings
- **THEN** Android opens and controls the system shade, and FocusGuard yields its overlay while the shade is foreground

#### Scenario: Return to the still-blocked app
- **GIVEN** the notification shade was opened while a blocked app remains underneath
- **WHEN** the user dismisses the shade and returns to that app
- **THEN** FocusGuard reevaluates the app rule and restores the full-screen intervention only if it still blocks the foreground app

#### Scenario: Use system navigation
- **GIVEN** the intervention is visible on a device with gesture or button navigation
- **WHEN** the user uses an Android system navigation gesture or control
- **THEN** the system receives that action, and FocusGuard keeps the block full-screen whenever the blocked app is foregrounded again
