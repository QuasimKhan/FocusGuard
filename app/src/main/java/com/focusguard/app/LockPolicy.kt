package com.focusguard.app

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Pure lock decision input, separated from Android state and clocks for predictable enforcement. */
data class LockPolicyInput(
    val packageName: String,
    val now: LocalDateTime,
    val masterActive: Boolean,
    val protectionLocked: Boolean,
    val globalMode: Int,
    val scheduleEnabled: Boolean,
    val scheduledPackages: Set<String>,
    val scheduleStartMinute: Int,
    val scheduleEndMinute: Int,
    val temporaryPassActive: Boolean,
    val pauseEndsAt: Instant? = null,
    val timeZone: ZoneId = ZoneId.systemDefault(),
    val nowInstant: Instant? = null
)

enum class LockAction { ALLOW, PAUSE, BLOCK }

enum class LockReason { NONE, GLOBAL_BLOCK, DAILY_AUTO_LOCK, MINDFUL_PAUSE }

data class LockDecision(
    val action: LockAction,
    val reason: LockReason,
    val endsAt: Instant? = null
)

object LockPolicy {
    fun evaluate(input: LockPolicyInput): LockDecision {
        if (!input.masterActive && !input.protectionLocked) return allowed()

        if (input.globalMode == FocusPrefs.APP_MODE_BLOCKED) {
            return LockDecision(LockAction.BLOCK, LockReason.GLOBAL_BLOCK)
        }

        val schedule = activeSchedule(input)
        if (schedule.active) {
            return LockDecision(LockAction.BLOCK, LockReason.DAILY_AUTO_LOCK, schedule.endsAt)
        }

        if (input.globalMode == FocusPrefs.APP_MODE_PAUSE && !input.temporaryPassActive) {
            return LockDecision(LockAction.PAUSE, LockReason.MINDFUL_PAUSE, input.pauseEndsAt)
        }

        return allowed()
    }

    private data class ScheduleState(val active: Boolean, val endsAt: Instant?)

    private fun activeSchedule(input: LockPolicyInput): ScheduleState {
        if (!input.scheduleEnabled || input.packageName !in input.scheduledPackages) return ScheduleState(false, null)
        val start = input.scheduleStartMinute.coerceIn(0, 1439)
        val end = input.scheduleEndMinute.coerceIn(0, 1439)
        val currentMinute = input.now.hour * 60 + input.now.minute
        val active = when {
            start == end -> true
            start < end -> currentMinute >= start && currentMinute < end
            else -> currentMinute >= start || currentMinute < end
        }
        if (!active || start == end) return ScheduleState(active, null)

        val nowInstant = input.nowInstant ?: input.now.atZone(input.timeZone).toInstant()
        val nextBoundary = ScheduleBoundaryCalculator.nextBoundary(
            now = nowInstant,
            zone = input.timeZone,
            startMinute = start,
            endMinute = end
        )
        val endInstant = nextBoundary
            ?.takeIf { it.kind == ScheduleBoundaryKind.END || it.kind == ScheduleBoundaryKind.CLOCK_CHANGE }
            ?.at
        return ScheduleState(true, endInstant)
    }

    private fun allowed() = LockDecision(LockAction.ALLOW, LockReason.NONE)
}
