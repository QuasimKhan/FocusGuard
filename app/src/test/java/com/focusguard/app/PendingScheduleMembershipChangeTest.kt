package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingScheduleMembershipChangeTest {
    @Test fun confirmationPersistsTheSelectionAndReevaluatesForegroundOnce() {
        var selected = setOf("com.example.existing")
        var reevaluations = 0
        val change = PendingScheduleMembershipChange(
            nextPackages = selected + "com.example.new",
            persist = { selected = it },
            reevaluateForeground = { reevaluations++ }
        )

        change.confirm()
        change.confirm()

        assertEquals(setOf("com.example.existing", "com.example.new"), selected)
        assertEquals(1, reevaluations)
    }

    @Test fun cancellationLeavesSelectionAndForegroundUntouched() {
        var selected = setOf("com.example.existing")
        var reevaluations = 0
        val change = PendingScheduleMembershipChange(
            nextPackages = emptySet(),
            persist = { selected = it },
            reevaluateForeground = { reevaluations++ }
        )

        change.cancel()
        change.confirm()

        assertEquals(setOf("com.example.existing"), selected)
        assertEquals(0, reevaluations)
    }
}
