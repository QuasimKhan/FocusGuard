package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayWindowFrameCalculatorTest {
    @Test fun portraitFrameLeavesStatusNavigationAndSideGestureInsets() {
        val frame = OverlayWindowFrameCalculator.calculate(
            displayWidthPx = 1080,
            displayHeightPx = 2400,
            systemBars = OverlayInsetsPx(top = 94, bottom = 24),
            systemGestures = OverlayInsetsPx(left = 54, right = 54, bottom = 35),
            minimumGestureInsetPx = 66
        )

        assertEquals(OverlayWindowFrame(x = 66, y = 94, width = 948, height = 2240), frame)
    }

    @Test fun landscapeFrameLeavesRotatedStatusNavigationAndGestureInsets() {
        val frame = OverlayWindowFrameCalculator.calculate(
            displayWidthPx = 2400,
            displayHeightPx = 1080,
            systemBars = OverlayInsetsPx(left = 94, bottom = 116),
            systemGestures = OverlayInsetsPx(right = 54, bottom = 35),
            minimumGestureInsetPx = 66
        )

        assertEquals(OverlayWindowFrame(x = 94, y = 66, width = 2240, height = 898), frame)
    }

    @Test fun compactFramesNeverExtendPastDisplayBounds() {
        val frame = OverlayWindowFrameCalculator.calculate(
            displayWidthPx = 640,
            displayHeightPx = 960,
            systemBars = OverlayInsetsPx(top = 80, bottom = 48),
            systemGestures = OverlayInsetsPx(left = 100, right = 100, bottom = 80),
            minimumGestureInsetPx = 72
        )

        assertEquals(100, frame.x)
        assertEquals(80, frame.y)
        assertEquals(440, frame.width)
        assertEquals(800, frame.height)
    }
}
