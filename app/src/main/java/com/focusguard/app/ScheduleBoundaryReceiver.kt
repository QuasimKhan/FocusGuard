package com.focusguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ScheduleCoordinator.ACTION_BOUNDARY) return
        ScheduleCoordinator.onBoundaryDelivered(
            context,
            intent.getStringExtra(ScheduleCoordinator.EXTRA_BOUNDARY_TOKEN),
            intent.getStringExtra(ScheduleCoordinator.EXTRA_BOUNDARY_KIND)
        )
    }
}
