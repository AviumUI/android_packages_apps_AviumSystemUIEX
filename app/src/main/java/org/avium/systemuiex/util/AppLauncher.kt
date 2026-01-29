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

package org.avium.systemuiex.util

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import android.widget.Toast
import org.avium.systemuiex.R

object AppLauncher {

    /*
    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            showLaunchFailToast(context)
        }
    }
    */

    fun launchApp(context: Context, packageName: String) {
        val useBubbleMode = PreferenceHelper.getPopupViewMode(context, true)
        
        if (useBubbleMode) {
            val intent = Intent("org.avium.LAUNCH_BUBBLE")
            intent.putExtra("package_name", packageName)
            intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND)
            context.sendBroadcast(intent)
        } else {
            launchAppNormally(context, packageName)
        }
    }

    private fun launchAppNormally(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            val componentName = launchIntent.component
            if (componentName != null) {
                val activityName = componentName.className
                val intent = Intent("com.sunshine.freeform.start_freeform").apply {
                    setPackage("com.sunshine.freeform")
                    putExtra("packageName", packageName)
                    putExtra("activityName", activityName)
                    putExtra("userId", 0)
                    putExtra(Intent.EXTRA_INTENT, launchIntent)
                }
                context.sendBroadcast(intent)
            } else {
                showLaunchFailToast(context)
            }
        } else {
            showLaunchFailToast(context)
        }
    }

    private fun showLaunchFailToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.cannot_launch_app), Toast.LENGTH_SHORT).show()
    }
}
