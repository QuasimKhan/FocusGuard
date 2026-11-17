# Spec Delta

## Purpose

Makes FocusGuard's self-imposed uninstall friction less disruptive by allowing normal app-settings navigation and intervening only at an uninstall confirmation during an active protection lock.

## ADDED Requirements

### Requirement: Preserve App Info Navigation
The system MUST allow the user to open and browse FocusGuard's App Info and uninstall pages normally. Merely viewing FocusGuard's name, app details, or Device Admin settings MUST NOT trigger a redirect or block intervention.

#### Scenario: User opens FocusGuard App Info
- **GIVEN** a protection lock is active
- **WHEN** the user opens FocusGuard's App Info page
- **THEN** FocusGuard leaves the page accessible and does not send the user Home

#### Scenario: User opens another app's settings
- **GIVEN** a protection lock is active
- **WHEN** the user visits App Info for another app
- **THEN** FocusGuard does not treat that navigation as an attempt to uninstall FocusGuard

### Requirement: Intervene at FocusGuard Uninstall Confirmation
While a protection lock is active and the accessibility service is enabled, the system MUST identify an accessible uninstall confirmation as targeting FocusGuard, cancel that confirmation before removal, and explain that the active lock remains in effect. It MUST NOT claim that ordinary Device Admin enrollment guarantees uninstall prevention.

#### Scenario: FocusGuard uninstall confirmation during lock
- **GIVEN** a protection lock is active and the user confirms uninstalling FocusGuard
- **WHEN** the system exposes an accessible uninstall confirmation surface targeting FocusGuard
- **THEN** FocusGuard cancels the confirmation and presents a clear, dismissible explanation of the active lock

#### Scenario: Uninstall another app
- **GIVEN** a protection lock is active
- **WHEN** an uninstall confirmation targets a different app
- **THEN** FocusGuard does not cancel that confirmation

#### Scenario: No protection lock is active
- **GIVEN** no strict lock, active study window, or locked Pomodoro session is active
- **WHEN** the user confirms uninstalling FocusGuard
- **THEN** this self-imposed confirmation intervention does not block the system action

#### Scenario: Confirmation UI is not accessible
- **GIVEN** a device's installer does not expose a reliably identifiable confirmation surface
- **WHEN** the user tries to uninstall FocusGuard during a protection lock
- **THEN** FocusGuard reports that this best-effort protection is unavailable on that device and does not claim uninstall is guaranteed to be prevented
