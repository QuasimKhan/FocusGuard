# Tasks

## 1. Consequential Setting Confirmations

- [x] 1.1 Add a reusable pending-action confirmation flow for enforcement controls, Armor Strict Lock, and app Pause/Block changes; verify focused state tests show confirm commits once, cancel/dismiss commits nothing, and optional nudges remain direct.
- [x] 1.2 Stage time-picker results and show the selected start/end time in a follow-up confirmation; verify tests prove confirm saves/reschedules and cancel preserves the previous time and boundary.
- [x] 1.3 Start the Android VPN consent flow only after the user confirms DNS shielding; verify authorization grant starts the service and denial leaves the shield reported inactive.

## 2. Active Schedule App Management

- [x] 2.1 Allow only scheduled-package membership edits while the Daily Auto-Lock Window is active, preserving guards on schedule enablement and time edits; verify preference tests cover add/remove during the window, unchanged global modes, and unchanged times.
- [x] 2.2 Add app-specific confirmations for active-window additions and removals, then update the schedule and foreground policy only after confirmation; verify cancel leaves selection unchanged and confirm immediately reevaluates an already-foreground app.
- [x] 2.3 Verify active-window removal preserves an independent global Pause or Block with policy regression cases, and document the immediate schedule effect in the confirmation copy.

## 3. Study OS Performance and Responsive Dashboard

- [x] 3.1 Load the launchable-app inventory asynchronously and render the bounded Study OS picker lazily with stable package identity and icon fallback; verify schedule/timer controls are usable before a large app list finishes loading and search/selection stay responsive.
- [x] 3.2 Adapt dashboard tabs and confirmation dialogs to available window size, system-bar insets, orientation, and larger text; verify no clipped controls at compact width, short landscape height, and increased Android text scale.
- [x] 3.3 Expand Accessibility-off guidance with feature impact and enabling steps, and refresh service status on return from Settings; verify both enabled and still-disabled return states.
- [x] 3.4 Restyle the Armor version/developer footer as centered and subdued near the bottom of its scroll content; verify it remains readable and does not cover settings on short screens.

## 4. Block Intervention and System UI Access

- [x] 4.1 Cover the full display with the accessibility block surface while keeping its panel safe-area constrained and scrollable; verify notification shade, Quick Settings, and navigation actions remain usable on compact and landscape screens.
- [x] 4.2 Reevaluate the blocked target after system UI is dismissed and restore the intervention only while its rule still applies; verify schedule expiry removes it and an independent global rule keeps it active.
- [x] 4.3 Keep enforcement persistent through missed events and configuration changes; verify a foreground watchdog restores the full-screen intervention only while the blocking rule still applies and clears it when another app becomes foreground.

## 5. Integrated Acceptance

- [ ] 5.1 Run the unit suite and debug build, then verify the full confirmation, active schedule edit, Accessibility setup, responsive layout, and block-overlay flows on API 26, 32, 33, and 35 across compact portrait, landscape, and large-text configurations; record device/OEM limitations alongside the existing intervention acceptance results.
