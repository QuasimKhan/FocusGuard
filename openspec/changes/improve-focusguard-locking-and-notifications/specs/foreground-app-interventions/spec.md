# Spec Delta

## Purpose

Ensures FocusGuard acts on real user-facing app use and presents a clear, reversible intervention that explains the rule currently preventing access.

## ADDED Requirements

### Requirement: Confirm User-Facing Foreground Before App Blocking
The system MUST block an app only after confirming that it owns the active, focused user-facing application window. An event package name alone MUST NOT trigger an app block; transient system, notification, service, or background events MUST be ignored for app-block decisions.

#### Scenario: Background event for a blocked app
- **GIVEN** a different app owns the active focused window
- **WHEN** an accessibility event names a package that is globally blocked
- **THEN** FocusGuard MUST NOT close that app or launch a block intervention

#### Scenario: System notification window appears
- **GIVEN** an allowed app is active
- **WHEN** a notification shade or other transient system window produces an event
- **THEN** FocusGuard MUST NOT treat the event package as the foreground app to block

#### Scenario: Blocked app becomes active
- **GIVEN** an app has an effective block rule
- **WHEN** the app's own window becomes active and focused
- **THEN** FocusGuard MUST show the intervention for that app

### Requirement: Evaluate Current Lock Rules Before Intervention
The system MUST resolve the current master state, global app mode, schedule membership and local time, temporary pass, and active protection lock before blocking. The intervention reason MUST identify the rule that currently applies.

#### Scenario: Schedule-only app outside its window
- **GIVEN** an app is selected only for the Daily Auto-Lock Window and its global mode is Open
- **WHEN** its active window becomes foreground outside the schedule
- **THEN** FocusGuard MUST allow it and MUST NOT show a block intervention

#### Scenario: Schedule-only app during its window
- **GIVEN** an app is selected for the Daily Auto-Lock Window
- **WHEN** its active window becomes foreground during the schedule
- **THEN** FocusGuard MUST block it and identify the Daily Auto-Lock Window as the reason

#### Scenario: Mindful pause pass
- **GIVEN** an app uses the global Pause mode and has no active temporary pass
- **WHEN** the user completes the pause and chooses to proceed
- **THEN** FocusGuard MUST grant the existing temporary pass and allow that app until the pass expires

### Requirement: Full-Screen Block Intervention
The system MUST present a dedicated full-screen intervention instead of automatically sending the user to Home. It MUST identify the target app and applicable lock reason, show the remaining time when the rule has a known end, and provide an explicit safe-exit action.

#### Scenario: Timed schedule block
- **GIVEN** a schedule-selected app is blocked until the current window ends
- **WHEN** the intervention appears
- **THEN** it shows the schedule reason and time remaining until the window ends

#### Scenario: Untimed global block
- **GIVEN** an app is blocked by its global Block mode
- **WHEN** the intervention appears
- **THEN** it identifies the global app block and does not display a misleading countdown

#### Scenario: User chooses safe exit
- **GIVEN** the full-screen intervention is visible
- **WHEN** the user chooses its safe-exit action
- **THEN** FocusGuard returns the user to Home without automatically reopening or repeatedly blocking the target app

#### Scenario: Schedule ends while intervention is visible
- **GIVEN** the intervention is showing only because of a schedule rule
- **WHEN** the schedule ends
- **THEN** FocusGuard reevaluates the rule and dismisses the intervention if no independent global rule still blocks the app
