package com.focusguard.app

enum class InterventionEdgeAction {
    NOTIFICATIONS,
    QUICK_SETTINGS,
    BACK,
    HOME
}

/** Maps full-screen overlay edge swipes to the Android actions normally reached at those edges. */
object InterventionEdgeGesture {
    fun actionForSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        displayWidth: Int,
        displayHeight: Int,
        safeFrame: OverlayWindowFrame,
        landscape: Boolean,
        minimumDistancePx: Float
    ): InterventionEdgeAction? {
        val dx = endX - startX
        val dy = endY - startY
        val leftInset = safeFrame.x.toFloat()
        val topInset = safeFrame.y.toFloat()
        val rightInset = (displayWidth - safeFrame.x - safeFrame.width).toFloat()
        val bottomInset = (displayHeight - safeFrame.y - safeFrame.height).toFloat()

        if (!landscape && startY < topInset && dy >= minimumDistancePx && dy > kotlin.math.abs(dx)) {
            return if (startX >= displayWidth * 0.72f) {
                InterventionEdgeAction.QUICK_SETTINGS
            } else {
                InterventionEdgeAction.NOTIFICATIONS
            }
        }

        if (landscape && startX < leftInset && dx >= minimumDistancePx && dx > kotlin.math.abs(dy)) {
            return if (startY >= displayHeight * 0.72f) {
                InterventionEdgeAction.QUICK_SETTINGS
            } else {
                InterventionEdgeAction.NOTIFICATIONS
            }
        }

        if (!landscape && startY >= displayHeight - bottomInset && -dy >= minimumDistancePx && -dy > kotlin.math.abs(dx)) {
            return InterventionEdgeAction.HOME
        }

        if (landscape && startX >= displayWidth - rightInset && -dx >= minimumDistancePx && -dx > kotlin.math.abs(dy)) {
            return InterventionEdgeAction.HOME
        }

        if (landscape && startY < topInset && dy >= minimumDistancePx && dy > kotlin.math.abs(dx)) {
            return InterventionEdgeAction.BACK
        }

        if (landscape && startY >= displayHeight - bottomInset && -dy >= minimumDistancePx && -dy > kotlin.math.abs(dx)) {
            return InterventionEdgeAction.BACK
        }

        if (!landscape && startX < leftInset && dx >= minimumDistancePx && dx > kotlin.math.abs(dy)) {
            return InterventionEdgeAction.BACK
        }

        if (!landscape && startX >= displayWidth - rightInset && -dx >= minimumDistancePx && -dx > kotlin.math.abs(dy)) {
            return InterventionEdgeAction.BACK
        }

        return null
    }
}
