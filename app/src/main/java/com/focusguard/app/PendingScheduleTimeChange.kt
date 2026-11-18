package com.focusguard.app

/** A schedule boundary selected in the picker but not yet persisted. */
internal class PendingScheduleTimeChange(
    val isStart: Boolean,
    val proposedMinutes: Int,
    private val save: (Boolean, Int) -> Unit
) {
    private val pendingAction = PendingUserAction {
        save(isStart, proposedMinutes.coerceIn(0, 1439))
    }

    fun confirm() = pendingAction.confirm()

    fun cancel() = pendingAction.cancel()
}
