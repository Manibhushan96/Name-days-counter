package com.mani.elapsed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Chronometer bases live in elapsedRealtime, which resets on reboot - so re-render. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ElapsedWidget.refreshAll(context)
    }
}
