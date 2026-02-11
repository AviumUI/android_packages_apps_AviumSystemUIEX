/*
 *
 * Copyright (C) 2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.avium.systemuiex.ui.overlay.OverlayManager

class TouchCoordinatesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "org.avium.systemuiex.TOUCH_COORDINATES") {
            val x = intent.getFloatExtra("x", -1f)
            val y = intent.getFloatExtra("y", -1f)
            val isUp = intent.getBooleanExtra("isUp", false)
            if (x >= 0 && y >= 0) {
                OverlayManager.onTouchCoordinates(x, y, isUp)
            }
        }
    }
}
