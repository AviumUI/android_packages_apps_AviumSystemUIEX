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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.avium.systemuiex.model.AppInfo

object AppListProvider {
    // Preserve existing package-only selections for the current user. Profile selections use
    // serial numbers so a removed profile cannot accidentally target a reused user ID.
    fun key(context: Context, packageName: String, user: android.os.UserHandle): String {
        if (user == android.os.Process.myUserHandle()) return packageName
        val users = context.getSystemService(android.os.UserManager::class.java)
        return "${users.getSerialNumberForUser(user)}:$packageName"
    }

    fun resolveUser(context: Context, selection: String): android.os.UserHandle? {
        if (!selection.contains(':')) return android.os.Process.myUserHandle()
        val serial = selection.substringBefore(':').toLongOrNull() ?: return null
        val users = context.getSystemService(android.os.UserManager::class.java)
        val user = users.getUserForSerialNumber(serial) ?: return null
        val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
        return user.takeIf {
            launcher.profiles.contains(it) &&
                !users.isQuietModeEnabled(it) &&
                users.isUserUnlocked(it)
        }
    }

    fun keepSelection(context: Context, selection: String): Boolean {
        val users = context.getSystemService(android.os.UserManager::class.java)
        val user = if (selection.contains(':')) {
            val serial = selection.substringBefore(':').toLongOrNull() ?: return false
            users.getUserForSerialNumber(serial) ?: return false
        } else android.os.Process.myUserHandle()
        // Keep temporarily unavailable profiles, but reclaim slots for uninstalled apps.
        if (users.isQuietModeEnabled(user) || !users.isUserUnlocked(user)) return true
        return try {
            context.getSystemService(android.content.pm.LauncherApps::class.java)
                .getActivityList(selection.substringAfter(':'), user).isNotEmpty()
        } catch (_: SecurityException) {
            true
        } catch (_: IllegalStateException) {
            true
        }
    }

    fun resolveApp(context: Context, selection: String): AppInfo? =
        try {
            resolveAvailableApp(context, selection)
        } catch (_: SecurityException) {
            null // Profile may have been locked or removed while the menu was open.
        } catch (_: IllegalStateException) {
            null
        }

    private fun resolveAvailableApp(context: Context, selection: String): AppInfo? {
        val user = resolveUser(context, selection) ?: return null
        val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
        val activity =
            launcher.getActivityList(selection.substringAfter(':'), user).firstOrNull()
                ?: return null
        return AppInfo(
            context.packageManager.getUserBadgedLabel(activity.label, user).toString(),
            activity.componentName.packageName,
            activity.getBadgedIcon(context.resources.displayMetrics.densityDpi),
            selectionKey = key(context, activity.componentName.packageName, user),
        )
    }

    suspend fun getLaunchableApps(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val launcher = context.getSystemService(android.content.pm.LauncherApps::class.java)
            val users = context.getSystemService(android.os.UserManager::class.java)
            launcher.profiles
                .filter { !users.isQuietModeEnabled(it) && users.isUserUnlocked(it) }
                .flatMap { user ->
                    val activities = try {
                        launcher.getActivityList(null, user)
                    } catch (_: SecurityException) {
                        emptyList()
                    } catch (_: IllegalStateException) {
                        emptyList()
                    }
                    activities.map { activity ->
                        AppInfo(
                            context.packageManager
                                .getUserBadgedLabel(activity.label, user)
                                .toString(),
                            activity.componentName.packageName,
                            activity.getBadgedIcon(context.resources.displayMetrics.densityDpi),
                            selectionKey = key(context, activity.componentName.packageName, user),
                        )
                    }
                }
                .distinctBy { it.selectionKey }
                .sortedBy { it.appName }
        }
}
