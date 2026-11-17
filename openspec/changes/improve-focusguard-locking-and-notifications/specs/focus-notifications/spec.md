# Spec Delta

## Purpose

Provides permission-aware local notifications for schedule transitions and app-block interventions, with clear delivery status, useful app identity, and FocusGuard's friendly Hinglish voice.

## ADDED Requirements

### Requirement: Permission-Aware Notification Delivery
The system MUST request notification permission contextually on Android 13 and later, report when app notifications or a relevant channel are disabled, and keep blocking and lock enforcement operational when notifications are unavailable.

#### Scenario: Notification permission is not granted
- **GIVEN** the user enables scheduled notifications on Android 13 or later
- **WHEN** notification permission has not been granted
- **THEN** FocusGuard explains the benefit before requesting permission and shows the resulting enabled or denied state

#### Scenario: Notification permission is denied
- **GIVEN** notification permission is denied or a relevant channel is disabled
- **WHEN** a schedule or block notification is due
- **THEN** FocusGuard does not claim it was delivered, keeps app enforcement active, and exposes the notification setting state in the UI

### Requirement: Reliable Schedule Boundary Notifications
The system MUST post one lock-start and one unlock notification for each enabled daily-window transition when notification permission is available. It MUST recalculate one-shot boundaries after schedule edits, reboot, date/time changes, timezone changes, and exact-alarm permission changes.

#### Scenario: Scheduled window begins
- **GIVEN** an enabled schedule reaches its local start boundary
- **WHEN** notification permission and the lock-events channel are available
- **THEN** FocusGuard posts one lock-start notification for that boundary

#### Scenario: Scheduled window ends
- **GIVEN** an enabled schedule reaches its local end boundary
- **WHEN** notification permission and the lock-events channel are available
- **THEN** FocusGuard posts one unlock notification for that boundary

#### Scenario: Exact-alarm access is unavailable
- **GIVEN** exact-alarm special access is denied
- **WHEN** a schedule boundary alert is due
- **THEN** FocusGuard uses its documented inexact fallback, avoids duplicate alerts, and reports that delivery may be delayed

#### Scenario: Device time context changes
- **GIVEN** a daily schedule is enabled
- **WHEN** the device reboots or local date, time, timezone, or exact-alarm access changes
- **THEN** FocusGuard recalculates the next boundary from current settings and does not replay an already-delivered transition

### Requirement: App-Specific Block Alerts
When notification permission is available, the system MUST provide an app-specific alert for a blocked foreground attempt, identify the target app, and avoid repeated alerts for the same package during one continuous block episode.

#### Scenario: App is blocked
- **GIVEN** a foreground app has an effective block rule
- **WHEN** FocusGuard shows its block intervention
- **THEN** the related notification names the app and opens relevant FocusGuard details when tapped

#### Scenario: Repeated blocked attempts
- **GIVEN** a package remains blocked and the user repeatedly attempts to open it
- **WHEN** multiple attempts occur during one continuous block episode
- **THEN** FocusGuard does not produce an unbounded stream of duplicate notifications

### Requirement: Notification Identity and Hinglish Copy
The system MUST use a valid FocusGuard small icon and stable notification channels. App-specific alerts MUST include the target app's icon as a large icon when available and use a safe FocusGuard fallback when it is not. Focus nudges MUST use friendly, non-shaming Hinglish copy that includes the requested examples in its message pool.

#### Scenario: App icon is available
- **GIVEN** a block notification targets an installed app with a loadable icon
- **WHEN** FocusGuard posts the notification
- **THEN** the notification shows the FocusGuard small icon and the target app's icon as its large icon

#### Scenario: App icon cannot be loaded
- **GIVEN** the target package or its icon is unavailable
- **WHEN** FocusGuard posts a block notification
- **THEN** the notification uses a safe FocusGuard icon and remains deliverable

#### Scenario: Hinglish nudge is selected
- **GIVEN** a focus nudge is due
- **WHEN** FocusGuard selects its message
- **THEN** the copy is friendly and non-shaming, and the pool includes “Phone rakh de bhai, abhi focus ka time hai!” and “Bas kar pagle, kaam pe dhyan de!”
