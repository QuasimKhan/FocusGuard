# Spec Delta

## Purpose

Provides a recurring local-time window that independently locks the apps selected for scheduled focus, without rewriting their normal app-control settings.

## ADDED Requirements

### Requirement: Independent Scheduled App Selection
The system MUST persist a Daily Auto-Lock app set independently from the global Open/Pause/Block app modes. Membership in this set MUST add a block only while the configured schedule is active; it MUST NOT change an app's global mode.

#### Scenario: Scheduled app outside its window
- **GIVEN** an app is selected only for the Daily Auto-Lock Window and its global mode is Open
- **WHEN** the local time is outside the enabled window
- **THEN** the schedule MUST NOT block the app, and the app remains usable

#### Scenario: Scheduled app inside its window
- **GIVEN** an app is selected for the Daily Auto-Lock Window
- **WHEN** the enabled window is active and the app becomes the user-facing foreground app
- **THEN** the system MUST block it with the Daily Auto-Lock Window as the reason

#### Scenario: Schedule selection does not rewrite global mode
- **GIVEN** an app has a global mode of Pause or Block
- **WHEN** the user adds or removes it from the scheduled app set
- **THEN** the global mode MUST remain unchanged

#### Scenario: Existing global block remains independent
- **GIVEN** an app is in the global Block mode and is also selected for a schedule
- **WHEN** the schedule is inactive
- **THEN** the global Block rule continues to apply independently of schedule membership

### Requirement: Minute-Resolution Daily Window
The system MUST support start and end times at minute resolution in the device's local timezone, including windows that cross midnight. The start minute is included and the end minute is excluded; equal start and end times retain the existing all-day-window meaning.

#### Scenario: Window starts
- **GIVEN** the enabled schedule starts at 18:37 local time
- **WHEN** the local clock reaches 18:37
- **THEN** the scheduled app set becomes subject to the schedule block without requiring the user to reopen FocusGuard

#### Scenario: Window ends
- **GIVEN** the enabled schedule ends at 21:12 local time
- **WHEN** the local clock reaches 21:12
- **THEN** the schedule block is no longer applied and schedule-only apps become usable, subject to their independent global modes

#### Scenario: Overnight window
- **GIVEN** the enabled schedule starts at 22:30 and ends at 06:15
- **WHEN** local time is 23:00 or 05:59
- **THEN** the schedule is active
- **WHEN** local time is 06:15
- **THEN** the schedule is inactive

#### Scenario: Equal start and end
- **GIVEN** start and end are both set to 08:00
- **WHEN** the schedule is enabled
- **THEN** the schedule is treated as active for the full local day, preserving current behavior

### Requirement: Schedule Boundary Recovery
The system MUST recompute the active schedule from persisted settings and current local time after a schedule edit, device reboot, date or clock change, or timezone change. If exact-alarm access is unavailable, the schedule state MUST still be evaluated on every qualifying foreground transition and the UI MUST disclose that boundary alerts or immediate mid-session transitions may be delayed.

#### Scenario: Schedule configuration changes
- **GIVEN** the schedule is enabled
- **WHEN** the user changes a selected app or either boundary time
- **THEN** pending boundary work is recalculated from the new settings

#### Scenario: Device time context changes
- **GIVEN** a schedule is enabled
- **WHEN** the device reboots or its local date, time, or timezone changes
- **THEN** the next local start/end boundaries are recalculated without changing the stored schedule

#### Scenario: Exact-alarm access is denied
- **GIVEN** the user has not granted exact-alarm special access
- **WHEN** a selected app becomes foreground during an active schedule
- **THEN** the current local-time schedule decision is still enforced and FocusGuard reports the reduced boundary-notification precision
