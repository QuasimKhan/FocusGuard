# Spec Delta

## Purpose

Defines the recurring local-time set of apps subject to Daily Auto-Lock independently of each app's persistent mode in the Apps section.

## ADDED Requirements

### Requirement: Independent Daily Auto-Lock Selection
The system MUST maintain Daily Auto-Lock app membership separately from the Apps-section Open, Pause, and Block modes. Changing schedule membership MUST NOT change an app's global mode.

#### Scenario: Add an app to Daily Auto-Lock
- **GIVEN** an app has a global Pause or Block mode
- **WHEN** the user selects it for Daily Auto-Lock in Study OS
- **THEN** it is added to the schedule selection and its global mode remains unchanged

#### Scenario: Scheduled app outside the active window
- **GIVEN** an app is selected only for Daily Auto-Lock, its global mode is Open, and the schedule is outside its active window
- **WHEN** the app becomes the foreground app
- **THEN** the schedule does not block the app and its global mode remains Open

#### Scenario: Scheduled app inside the active window
- **GIVEN** an app is selected for Daily Auto-Lock and its global mode is Open
- **WHEN** the app becomes the foreground app during the active window
- **THEN** the app is blocked by Daily Auto-Lock without changing its global mode

### Requirement: Daily Auto-Lock Does Not Lock Apps-Section Mode Controls
The system MUST allow users to change an app's global Open, Pause, or Block mode while Daily Auto-Lock is the only active protection. The active schedule MUST continue blocking selected apps independently of those modes.

#### Scenario: Set a scheduled app to Open during the active window
- **GIVEN** Daily Auto-Lock is active, an app is selected in Study OS, and no other protection lock is active
- **WHEN** the user sets that app's Apps-section mode to Open
- **THEN** the global mode is Open and Daily Auto-Lock continues blocking the app until it is removed from Study OS or the window ends

#### Scenario: Set an unselected app to Open during the active window
- **GIVEN** Daily Auto-Lock is active, an app is not selected in Study OS, and its global mode is Pause or Block
- **WHEN** the user sets that app's Apps-section mode to Open
- **THEN** the global mode becomes Open and Daily Auto-Lock does not block the app

### Requirement: Remove Apps From An Active Daily Auto-Lock Selection
The system MUST allow the user to remove an app from Study OS during an active Daily Auto-Lock window after confirmation, without changing its Apps-section mode. Other active protection locks MAY continue restricting this action.

#### Scenario: Confirm removal during the active window
- **GIVEN** an app is selected in Study OS and Daily Auto-Lock is active
- **WHEN** the user confirms removing it
- **THEN** it is removed from schedule membership, its global mode remains unchanged, and the active lock decision is reevaluated

#### Scenario: Cancel removal during the active window
- **GIVEN** an app is selected in Study OS and Daily Auto-Lock is active
- **WHEN** the user cancels the removal confirmation
- **THEN** it remains selected and continues to be blocked by the schedule
