/*
 *
 * Copyright (C) 2025 The AviumUI Project
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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