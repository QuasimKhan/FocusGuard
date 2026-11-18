# Verification

## Automated checks

- `./gradlew :app:testDebugUnitTest :app:assembleDebug` passed on 2026-11-18 (`BUILD SUCCESSFUL`, 43 actionable tasks).
- `git diff --check` passed after the implementation changes.
- Unit coverage exercises confirmation commits/cancellation, active schedule edits, overlay edge-gesture mapping, and foreground-policy restoration.

## Device acceptance

Tested on one Xiaomi 22101320I running Android 14 (API 34), native display 1080 × 2400 at 440 dpi.

- Installed the debug build after confirming no FocusGuard package was installed. With Accessibility temporarily enabled for QA, opening blocked 2048 showed an opaque block surface across the full portrait display, with the warning panel centered inside the safe area.
- A top-edge swipe opened Android's notification shade while 2048 remained the underlying activity. Dismissing the shade restored the intervention.
- Back and Home edge gestures left the blocked app and cleared the overlay. Reopening 2048 restored the block. A forced landscape orientation produced a 2400 × 1080 full-display intervention; its content scrolled to expose “Exit to Home” in the short window.
- Landscape side-edge gesture yielded to Android's notification shade. Quick Settings controls were also opened and dismissed through Android's status-bar service commands. On this MIUI build, a routed Quick Settings edge action presented the notification panel first; the OEM-specific one-swipe transition from that panel was not confirmed.
- The orientation and display-size overrides were reset. The temporary debug FocusGuard package was uninstalled, the Accessibility service list was cleared, Accessibility was disabled, auto-rotation restored, and the display returned to its native size.

The available phone covers API 34 only. API 26, 32, and 35 device checks, large-text device checks, and the wider confirmation/dashboard acceptance matrix remain pending; task 5.1 stays open.
