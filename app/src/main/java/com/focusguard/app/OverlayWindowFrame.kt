package com.focusguard.app

data class OverlayInsetsPx(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)

data class OverlayWindowFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

object OverlayWindowFrameCalculator {
    fun calculate(
        displayWidthPx: Int,
        displayHeightPx: Int,
        systemBars: OverlayInsetsPx,
        systemGestures: OverlayInsetsPx,
        minimumGestureInsetPx: Int
    ): OverlayWindowFrame {
        val left = maxOf(systemBars.left, systemGestures.left, minimumGestureInsetPx)
        val top = maxOf(systemBars.top, systemGestures.top, minimumGestureInsetPx)
        val right = maxOf(systemBars.right, systemGestures.right, minimumGestureInsetPx)
        val bottom = maxOf(systemBars.bottom, systemGestures.bottom, minimumGestureInsetPx)

        val safeLeft = left.coerceAtMost((displayWidthPx - 1).coerceAtLeast(0))
        val safeTop = top.coerceAtMost((displayHeightPx - 1).coerceAtLeast(0))
        val safeRight = right.coerceAtMost((displayWidthPx - safeLeft - 1).coerceAtLeast(0))
        val safeBottom = bottom.coerceAtMost((displayHeightPx - safeTop - 1).coerceAtLeast(0))

        return OverlayWindowFrame(
            x = safeLeft,
            y = safeTop,
            width = (displayWidthPx - safeLeft - safeRight).coerceAtLeast(1),
            height = (displayHeightPx - safeTop - safeBottom).coerceAtLeast(1)
        )
    }
}
