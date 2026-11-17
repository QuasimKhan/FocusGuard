package com.focusguard.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class FocusDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "FocusGuard Device Admin is active. Disabling it also removes this warning."
    }
}
