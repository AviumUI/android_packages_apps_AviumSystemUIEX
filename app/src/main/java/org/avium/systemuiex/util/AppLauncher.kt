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

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.avium.systemuiex.R

object AppLauncher {

    fun launchApp(context: Context, selection: String) {
        val user =
            AppListProvider.resolveUser(context, selection) ?: return showLaunchFailToast(context)
        val packageName = selection.substringAfter(':')
        val useBubbleMode = PreferenceHelper.getPopupViewMode(context, true)

        try {
            if (useBubbleMode) {
                val intent = Intent("org.avium.LAUNCH_BUBBLE")
                intent.putExtra("package_name", packageName)
                intent.putExtra(Intent.EXTRA_USER_HANDLE, user.identifier)
                intent.setPackage("com.android.systemui")
                intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND)
                context.sendBroadcast(intent)
            } else {
                launchAppNormally(context, packageName, user)
            }
        } catch (_: SecurityException) {
            showLaunchFailToast(context)
        } catch (_: android.content.ActivityNotFoundException) {
            showLaunchFailToast(context)
        } catch (_: IllegalStateException) {
            showLaunchFailToast(context)
        }
    }

    private fun launchAppNormally(
        context: Context,
        packageName: String,
        user: android.os.UserHandle,
    ) {
        val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val activity = launcher.getActivityList(packageName, user).firstOrNull()
        if (activity != null) {

            val miniWindowOptions =
                ActivityOptions.makeBasic().apply { setLaunchWindowingMode(102) }

            launcher.startMainActivity(
                activity.componentName,
                user,
                null,
                miniWindowOptions.toBundle(),
            )
        } else {
            showLaunchFailToast(context)
        }
    }

    private fun showLaunchFailToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.cannot_launch_app), Toast.LENGTH_SHORT)
            .show()
    }
}
