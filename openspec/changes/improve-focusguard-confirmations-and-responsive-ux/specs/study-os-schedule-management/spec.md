# Spec Delta

## Purpose

Lets users manage the Daily Auto-Lock app selection predictably, including when the schedule is currently active, without changing their separate global app rules.

## ADDED Requirements

### Requirement: Schedule membership remains editable during an active window
The system MUST let users add or remove an app from the Daily Auto-Lock selection during its active window after confirming the app-specific effect. A membership edit MUST NOT change the schedule times, schedule enablement, or the app's independent global mode.

#### Scenario: Add an app during the active window
- **GIVEN** the Daily Auto-Lock Window is active
- **WHEN** the user selects another app and confirms the change
- **THEN** FocusGuard adds that app to the schedule and immediately reevaluates it if it is already foregrounded

#### Scenario: Remove an app during the active window
- **GIVEN** an app is selected for the active Daily Auto-Lock Window
- **WHEN** the user confirms removing it from the schedule
- **THEN** FocusGuard removes only that schedule membership and immediately reevaluates the app against any remaining global rule

#### Scenario: Cancel an active-window membership edit
- **GIVEN** an add or removal confirmation is shown for the active schedule
- **WHEN** the user cancels or dismisses the confirmation
- **THEN** the scheduled app selection and its current enforcement remain unchanged

#### Scenario: Global rule remains after schedule removal
- **GIVEN** a scheduled app also has a global Pause or Block rule
- **WHEN** the user removes it from the active schedule
- **THEN** the global Pause or Block rule continues to apply independently
