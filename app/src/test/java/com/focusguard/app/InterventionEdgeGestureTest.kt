package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionEdgeGestureTest {
    private val portrait = OverlayWindowFrame(x = 66, y = 94, width = 948, height = 2240)
    private val landscape = OverlayWindowFrame(x = 94, y = 66, width = 2240, height = 898)

    @Test fun portraitStatusAndNavigationGesturesRemainAvailable() {
        assertEquals(
            InterventionEdgeAction.NOTIFICATIONS,
            InterventionEdgeGesture.actionForSwipe(120f, 20f, 120f, 300f, 1080, 2400, portrait, false, 120f)
        )
        assertEquals(
            InterventionEdgeAction.QUICK_SETTINGS,
            InterventionEdgeGesture.actionForSwipe(980f, 20f, 980f, 300f, 1080, 2400, portrait, false, 120f)
        )
        assertEquals(
            InterventionEdgeAction.HOME,
            InterventionEdgeGesture.actionForSwipe(540f, 2380f, 540f, 1900f, 1080, 2400, portrait, false, 120f)
        )
        assertEquals(
            InterventionEdgeAction.BACK,
            InterventionEdgeGesture.actionForSwipe(12f, 900f, 180f, 900f, 1080, 2400, portrait, false, 120f)
        )
    }

    @Test fun landscapeStatusAndNavigationGesturesRemainAvailable() {
        assertEquals(
            InterventionEdgeAction.NOTIFICATIONS,
            InterventionEdgeGesture.actionForSwipe(20f, 300f, 250f, 300f, 2400, 1080, landscape, true, 120f)
        )
        assertEquals(
            InterventionEdgeAction.QUICK_SETTINGS,
            InterventionEdgeGesture.actionForSwipe(20f, 900f, 250f, 900f, 2400, 1080, landscape, true, 120f)
        )
        assertEquals(
            InterventionEdgeAction.HOME,
            InterventionEdgeGesture.actionForSwipe(2380f, 500f, 1900f, 500f, 2400, 1080, landscape, true, 120f)
        )
        assertEquals(
            InterventionEdgeAction.BACK,
            InterventionEdgeGesture.actionForSwipe(500f, 12f, 500f, 180f, 2400, 1080, landscape, true, 120f)
        )
    }

    @Test fun incompleteOrNonEdgeSwipesAreNotRouted() {
        assertEquals(
            null,
            InterventionEdgeGesture.actionForSwipe(300f, 300f, 400f, 300f, 1080, 2400, portrait, false, 120f)
        )
        assertEquals(
            null,
            InterventionEdgeGesture.actionForSwipe(120f, 20f, 120f, 80f, 1080, 2400, portrait, false, 120f)
        )
    }
}
