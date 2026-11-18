# Tasks

## 1. Preserve Daily Auto-Lock Independence

- [x] 1.1 Keep Study OS selection reads and writes on the dedicated scheduled-app set; confirm picker callbacks do not change Apps-section modes.
- [x] 1.2 Keep schedule membership and global modes independent in effective lock resolution; a schedule-only app is restricted only during its active window.
- [x] 1.3 Allow changing Apps-section Open/Pause/Block modes when Daily Auto-Lock is the only active lock; retain Strict Lock and locked Pomodoro restrictions.
- [x] 1.4 Allow confirmed removal from an active Study OS schedule, preserve the global app mode, and reevaluate the foreground policy.

## 2. Keep the Intervention Active Through System UI

- [x] 2.1 Keep the overlay attached and preserve its target and policy when transient System UI events occur.
- [x] 2.2 Consume touches on the full-screen intervention without dispatching Notifications, Quick Settings, Back, or Home actions; dismiss the shade if System UI still opens it.
- [x] 2.3 Preserve the active target and Pause deadline across view rebuilds and orientation changes.

## 3. Fit the Existing Intervention to Landscape

- [x] 3.1 Keep a compact landscape composition with no vertical scroll container and visible title, target, reason, time, and applicable actions.
- [x] 3.2 Retain the existing visual identity and adapt portrait/landscape content to current display bounds and safe insets.
