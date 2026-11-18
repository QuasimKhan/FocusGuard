package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingUserActionTest {
    @Test fun confirmationCommitsAtMostOnce() {
        var commits = 0
        val action = PendingUserAction { commits++ }

        action.confirm()
        action.confirm()

        assertEquals(1, commits)
    }

    @Test fun cancellationNeverCommits() {
        var commits = 0
        val action = PendingUserAction { commits++ }

        action.cancel()
        action.confirm()

        assertEquals(0, commits)
    }
}
