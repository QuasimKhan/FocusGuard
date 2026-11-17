# Tasks

## 1. Schedule Policy and Stored State

- [x] 1.1 Add a side-effect-free schedule/app lock decision model that returns allow, pause, or block with reason and optional end time; verify unit cases for master/protection state, global modes, temporary passes, inclusive start, exclusive end, overnight windows, equal-times all-day, and local-time boundaries.
- [x] 1.2 Persist a separate scheduled-package set without mutating existing global modes; verify migration coverage that preserves prior schedule times/enabled state and leaves old global Open/Pause/Block settings unchanged.

## 2. Daily Auto-Lock Configuration UX

- [x] 2.1 Update Study OS with a searchable installed-app selection list and minute-resolution start/end time picker; verify users can select schedule-only apps and save non-30-minute times without changing global app modes.
- [x] 2.2 Show schedule semantics, active-window status, and exact-alarm/notification availability in Study OS, and update README setup instructions; verify displayed status matches the policy at start, end, overnight, and all-day boundaries.

## 3. Schedule Boundary Coordination

- [x] 3.1 Implement one-shot local start/end boundary scheduling with exact-alarm access checks and an inexact fallback; verify tests cover overnight and daylight-saving/timezone boundary calculations and permission-denied behavior.
- [x] 3.2 Reschedule after preference edits, boot, app replacement, date/time/timezone changes, and exact-alarm permission changes; verify duplicate receiver delivery produces one transition and one scheduled notification event per boundary token.
- [x] 3.3 Notify a connected Accessibility Service to reevaluate its active window at each boundary without starting an Activity from the receiver; verify a schedule start/end while a scheduled app is already foreground updates the lock state and overlay when exact access is available.

## 4. Foreground App Blocking Stability

- [x] 4.1 Refactor hard app blocking and mindful pause to require a matching active/focused application window and root package, and use the shared lock decision; verify event/root mismatch, system UI, notification shade, IME, and FocusGuard-owned windows cannot trigger an app block.
- [x] 4.2 Preserve validated content checks for Shorts and browser URLs and add focused regression coverage; verify WhatsApp remains open when only background/content/system events mention it, while a genuinely foreground blocked app is intercepted.

## 5. Full-Screen Intervention Overlay

- [x] 5.1 Add a service-owned full-screen accessibility overlay with target app identity, lock reason, known remaining time, and friendly copy; verify the overlay appears only after a confirmed effective lock and does not require draw-over-other-apps permission.
- [x] 5.2 Integrate safe exit, mindful-pause countdown/proceed pass, dismissal-loop suppression, and policy reevaluation while visible; verify explicit Exit returns Home, Proceed opens the app with a temporary pass, and a schedule-only overlay disappears when its window ends.

## 6. Anti-Uninstall UX

- [x] 6.1 Allow normal App Info navigation and limit accessibility intervention to a verified FocusGuard uninstall confirmation during an active protection lock; verify FocusGuard is not confused with another package and ambiguous/OEM-specific dialogs fail open.
- [x] 6.2 Present a clear dismissible protection explanation and correct Armor wording and README claims to describe best-effort behavior; verify no claim implies regular Device Admin enrollment can guarantee uninstall prevention.

## 7. Reliable Notifications

- [x] 7.1 Replace startup-only notification prompting with contextual API 33+ permission handling and explicit app/channel state; verify permission grant, denial, returning from Settings, and disabled-channel states leave lock enforcement active and accurately reported.
- [x] 7.2 Add stable schedule/block channels, a valid FocusGuard small icon, target-app large icons with fallback, and friendly Hinglish message selection; verify icon fallback, both requested sample messages, and existing update/nudge channel preferences remain intact.
- [x] 7.3 Connect start/end transitions and validated app-block events to deduplicated notifications; verify schedule boundaries notify once, repeat attempts do not spam, and exact-alarm denial uses the inexact fallback with a visible precision status.
- [x] 7.4 Document notification permission, exact-alarm access, and channel controls in README or in-app guidance; verify each permission prompt explains its purpose and denial behavior.

## 8. Bounded Release-Readiness Audit

- [x] 8.1 Move release signing values out of tracked Gradle configuration and require external local/CI values for release builds; verify debug builds still assemble, no signing secret remains in tracked build files, and release builds fail with a clear missing-configuration message.
- [x] 8.2 Review the touched enforcement, schedule, notification, and Armor paths for reproducible acceptance blockers, fixing only issues directly affecting this change; verify each added fix has a regression check and user-facing claims match implemented behavior.

## 9. Integrated Acceptance

- [ ] 9.1 Verify end-to-end behavior on API 26, API 32, API 33, and API 35, including reboot/timezone changes, notification and exact-alarm denial, real foreground transitions, overlay dismissal, and representative OEM installer UI; record any unsupported OEM behavior as a documented best-effort limitation.
