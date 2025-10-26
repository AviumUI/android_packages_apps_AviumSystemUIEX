package org.avium.systemuiex.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.avium.systemuiex.service.GestureService

class GestureReceiver : BroadcastReceiver() {
    
    private val TAG = "GestureReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received gesture broadcast: $intent")
        
        if (intent.action == "org.avium.systemuiex.GESTURE_ACTION") {
            val isLeft = intent.getBooleanExtra("isLeft", true)
            GestureService.startViaPendingIntent(context, isLeft)
        }
    }
}