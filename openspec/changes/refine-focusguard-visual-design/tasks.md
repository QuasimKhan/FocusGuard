# Tasks

## 1. Compose Theme and Shared Presentation

- [x] 1.1 Add coordinated light and dark Material color schemes plus shared typography, spacing, and card treatments in the existing Compose UI; verify each dashboard tab remains readable in both system appearances.
- [x] 1.2 Add the existing FocusGuard vector mark to the dashboard title row and to empty app-search results; verify the header retains its status/master control and empty results remain within the existing list.

## 2. App Lists and Schedule Picker

- [x] 2.1 Restyle Apps-tab rows with package icons, rounded holders, readable names, and explicit Open/15s Pause/Block state treatments; verify all three existing mode callbacks and enabled/disabled states are unchanged.
- [x] 2.2 Restyle Study OS schedule-picker rows with package icons, readable names, and clear selected/unselected states with comfortable touch targets; verify selecting and clearing an app still uses the existing checkbox behavior.

## 3. Block Overlay Presentation

- [x] 3.1 Restyle the existing native block and mindful-pause overlay with the FocusGuard mark, target-app icon/name, contained reason/timer text, and the current buttons; verify global block, schedule lock, and pause overlays show their current copy and actions.
- [x] 3.2 Add matching light/dark presentation tokens and stable icon fallbacks for the overlay; verify it remains legible in both system appearances and inspect the diff to confirm lock evaluation, timer, dismissal, and click-handler logic are unchanged.

## 4. Integration Verification

- [x] 4.1 Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` and confirm the visual changes compile without changing existing rule or service tests.
- [x] 4.2 Inspect the dashboard, both app lists, empty-search state, and block overlay on a device or emulator in light and dark appearances; confirm text contrast, icon alignment, responsive row spacing, and comfortable control targets.
