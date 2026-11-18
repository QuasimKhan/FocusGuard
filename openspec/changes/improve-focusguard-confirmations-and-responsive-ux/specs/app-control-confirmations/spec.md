# Spec Delta

## Purpose

Confirms consequential protection changes before they take effect, so users can understand and cancel a lock or filtering action without leaving partially updated settings.

## ADDED Requirements

### Requirement: Confirm consequential enforcement changes
The system MUST explain and confirm an action before it enables or strengthens app blocking, focus locking, Shorts blocking, or DNS filtering. Optional comic nudges and other non-enforcement preferences MUST remain directly changeable.

#### Scenario: Enable an enforcement control
- **GIVEN** a supported enforcement control is off
- **WHEN** the user turns on master protection, Shorts blocking, DNS shielding, the Daily Auto-Lock Window, locked Pomodoro mode, or Armor's Strict Lock
- **THEN** FocusGuard shows a warning that explains the effect and offers explicit confirm and cancel actions before applying the change

#### Scenario: Strengthen an app rule
- **GIVEN** an app is Open or in Pause mode
- **WHEN** the user changes it to Pause or Block
- **THEN** FocusGuard explains the resulting restriction and applies the new mode only after confirmation

#### Scenario: Cancel an enforcement change
- **GIVEN** a confirmation is open for an enforcement change
- **WHEN** the user cancels or dismisses it
- **THEN** the prior app-owned setting remains unchanged and no dependent system permission flow starts

#### Scenario: Android VPN authorization is declined
- **GIVEN** the user confirms enabling DNS shielding and Android requests VPN authorization
- **WHEN** the user declines or dismisses Android's authorization screen
- **THEN** the VPN service remains off and FocusGuard reports it as inactive

### Requirement: Confirm a selected schedule time before saving
After the user accepts a time in the schedule picker, the system MUST show the selected boundary time and save it only after a second explicit confirmation.

#### Scenario: Confirm a new schedule time
- **GIVEN** the user selected a start or end time in the time picker
- **WHEN** the user confirms the displayed proposed time
- **THEN** FocusGuard saves that boundary and recalculates the next schedule transition

#### Scenario: Cancel a proposed schedule time
- **GIVEN** the user selected a start or end time in the time picker
- **WHEN** the user cancels the follow-up confirmation
- **THEN** the previously saved boundary remains in effect and the schedule is not rescheduled from the discarded value
