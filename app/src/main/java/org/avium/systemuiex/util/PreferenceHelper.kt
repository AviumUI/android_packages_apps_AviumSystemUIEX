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
import androidx.core.content.edit
import android.os.SystemProperties

object PreferenceHelper {

    const val PREFS_NAME = "SystemUIEX_Prefs"
    private const val KEY_SELECTED_APPS_SET = "selected_apps"
    private const val KEY_SELECTED_APPS_LIST = "selected_apps_list"
    private const val ACTION_UPDATE_GESTURE_SETTINGS = "org.avium.UPDATE_GESTURE_SETTINGS"

    const val KEY_POPUP_DOUBLE_TAP_EXIT = "pop_up_view_double_tap_exit"
    const val KEY_POPUP_NOTIFICATION_PORTRAIT = "pop_up_view_notification_portrait"
    const val KEY_POPUP_NOTIFICATION_LANDSCAPE = "pop_up_view_notification_landscape"
    const val KEY_APP_LAUNCH_MODE = "app_launch_mode"

    const val SYS_PROP_POPUP_GESTURE = "persist.avium.popup_gesture"
    const val KEY_POPUP_GESTURE_FALLBACK = "pop_up_view_gesture_fallback"

    const val SYS_PROP_LAUNCHER_GESTURE = "persist.avium.launchergesture"
    const val KEY_LAUNCHER_GESTURE_FALLBACK = "launcher_gesture_fallback"

    const val SYS_PROP_GESTURE_AREA_HEIGHT = "persist.avium.gesture_area_height_dp"
    const val SYS_PROP_GESTURE_AREA_WIDTH = "persist.avium.gesture_area_width_dp"
    const val KEY_GESTURE_AREA_HEIGHT_FALLBACK = "gesture_area_height_fallback"
    const val KEY_GESTURE_AREA_WIDTH_FALLBACK = "gesture_area_width_fallback"

    const val SYS_PROP_POPUP_VIEW_MODE = "persist.avium.popup_view"
    const val KEY_POPUP_VIEW_MODE_FALLBACK = "popup_view_mode_fallback"
    const val KEY_POPUP_VIEW_MODE_FALLBACK_MODE = "popup_view_mode_fallback_mode"

    const val SYS_PROP_POPUP_VIEW_NOTIFS = "persist.avium.popup_view_notifs"
    const val KEY_POPUP_VIEW_NOTIFS_FALLBACK = "popup_view_notifs_fallback"

    const val SYS_PROP_BETA_FORCE_RELAUNCH = "persist.avium.beta_force_relaunch"
    const val KEY_BETA_FORCE_RELAUNCH_FALLBACK = "beta_force_relaunch_fallback"

    const val KEY_TRIGGER_WIDTH = "trigger_width"
    const val KEY_TRIGGER_HEIGHT = "trigger_height"
    const val KEY_SWIPE_TOLERANCE = "swipe_tolerance"

    const val MODE_BUBBLE = "bubble"
    const val MODE_FREEFORM = "free_window"
    const val MODE_POPUP_VIEW = "popup_view"
    private const val MODE_FREEFORM_LEGACY = "free window"

    fun setFloat(context: Context, key: String, value: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putFloat(key, value) }
    }

    fun getFloat(context: Context, key: String, defaultValue: Float): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(key, defaultValue)
    }

    fun saveSelectedApps(context: Context, selectedApps: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val joined = selectedApps.joinToString("|")
        prefs.edit {
            putString(KEY_SELECTED_APPS_LIST, joined)
            remove(KEY_SELECTED_APPS_SET)
        }
    }

    fun getSelectedApps(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_SELECTED_APPS_LIST, null)
        if (!stored.isNullOrEmpty()) {
            return stored.split("|").filter { it.isNotBlank() }
        }
        val legacySet = prefs.getStringSet(KEY_SELECTED_APPS_SET, emptySet()) ?: emptySet()
        if (legacySet.isEmpty()) {
            return emptyList()
        }
        val sorted = sortPackagesByLabel(context, legacySet.toList())
        saveSelectedApps(context, sorted)
        return sorted
    }

    private fun sortPackagesByLabel(context: Context, packages: List<String>): List<String> {
        val pm = context.packageManager
        return packages.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }
        })
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(key, value) }
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, defaultValue)
    }

    fun setPopupGestureEnabled(context: Context, enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        try {
            SystemProperties.set(SYS_PROP_POPUP_GESTURE, value)
            val readBack = SystemProperties.get(SYS_PROP_POPUP_GESTURE, if (enabled) "0" else "1")
            if (readBack != value) {
                setBoolean(context, KEY_POPUP_GESTURE_FALLBACK, enabled)
            } else {
                setBoolean(context, KEY_POPUP_GESTURE_FALLBACK, enabled)
            }
        } catch (e: Exception) {
            setBoolean(context, KEY_POPUP_GESTURE_FALLBACK, enabled)
        }
    }

    fun isPopupGestureEnabled(context: Context, defaultValue: Boolean = false): Boolean {
        return try {
            val prop = SystemProperties.get(SYS_PROP_POPUP_GESTURE, if (defaultValue) "1" else "0")
            when (prop) {
                "1", "true", "TRUE" -> true
                "0", "false", "FALSE" -> false
                else -> getBoolean(context, KEY_POPUP_GESTURE_FALLBACK, defaultValue)
            }
        } catch (e: Exception) {
            getBoolean(context, KEY_POPUP_GESTURE_FALLBACK, defaultValue)
        }
    }

    fun setLauncherGestureEnabled(context: Context, enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        try {
            SystemProperties.set(SYS_PROP_LAUNCHER_GESTURE, value)
            val readBack = SystemProperties.get(SYS_PROP_LAUNCHER_GESTURE, if (enabled) "0" else "1")
            if (readBack != value) {
                setBoolean(context, KEY_LAUNCHER_GESTURE_FALLBACK, enabled)
            } else {
                setBoolean(context, KEY_LAUNCHER_GESTURE_FALLBACK, enabled)
            }
        } catch (e: Exception) {
            setBoolean(context, KEY_LAUNCHER_GESTURE_FALLBACK, enabled)
        }
    }

    fun isLauncherGestureEnabled(context: Context, defaultValue: Boolean = false): Boolean {
        return try {
            val prop = SystemProperties.get(SYS_PROP_LAUNCHER_GESTURE, if (defaultValue) "1" else "0")
            when (prop) {
                "1", "true", "TRUE" -> true
                "0", "false", "FALSE" -> false
                else -> getBoolean(context, KEY_LAUNCHER_GESTURE_FALLBACK, defaultValue)
            }
        } catch (e: Exception) {
            getBoolean(context, KEY_LAUNCHER_GESTURE_FALLBACK, defaultValue)
        }
    }

    fun setGestureAreaHeight(context: Context, value: Float) {
        try {
            SystemProperties.set(SYS_PROP_GESTURE_AREA_HEIGHT, value.toString())
            val readBack = SystemProperties.get(SYS_PROP_GESTURE_AREA_HEIGHT, "0")
            if (readBack != value.toString()) {
                setFloat(context, KEY_GESTURE_AREA_HEIGHT_FALLBACK, value)
            } else {
                setFloat(context, KEY_GESTURE_AREA_HEIGHT_FALLBACK, value)
            }
        } catch (e: Exception) {
            setFloat(context, KEY_GESTURE_AREA_HEIGHT_FALLBACK, value)
        }
        val intent = Intent(ACTION_UPDATE_GESTURE_SETTINGS)
        context.sendBroadcast(intent)
    }

    fun getGestureAreaHeight(context: Context, defaultValue: Float = 20f): Float {
        return try {
            val prop = SystemProperties.get(SYS_PROP_GESTURE_AREA_HEIGHT, defaultValue.toString())
            prop.toFloatOrNull() ?: getFloat(context, KEY_GESTURE_AREA_HEIGHT_FALLBACK, defaultValue)
        } catch (e: Exception) {
            getFloat(context, KEY_GESTURE_AREA_HEIGHT_FALLBACK, defaultValue)
        }
    }

    fun setGestureAreaWidth(context: Context, value: Float) {
        try {
            SystemProperties.set(SYS_PROP_GESTURE_AREA_WIDTH, value.toString())
            val readBack = SystemProperties.get(SYS_PROP_GESTURE_AREA_WIDTH, "0")
            if (readBack != value.toString()) {
                setFloat(context, KEY_GESTURE_AREA_WIDTH_FALLBACK, value)
            } else {
                setFloat(context, KEY_GESTURE_AREA_WIDTH_FALLBACK, value)
            }
        } catch (e: Exception) {
            setFloat(context, KEY_GESTURE_AREA_WIDTH_FALLBACK, value)
        }
        val intent = Intent(ACTION_UPDATE_GESTURE_SETTINGS)
        context.sendBroadcast(intent)
    }

    fun getGestureAreaWidth(context: Context, defaultValue: Float = 20f): Float {
        return try {
            val prop = SystemProperties.get(SYS_PROP_GESTURE_AREA_WIDTH, defaultValue.toString())
            prop.toFloatOrNull() ?: getFloat(context, KEY_GESTURE_AREA_WIDTH_FALLBACK, defaultValue)
        } catch (e: Exception) {
            getFloat(context, KEY_GESTURE_AREA_WIDTH_FALLBACK, defaultValue)
        }
    }

    fun setAppLaunchMode(context: Context, mode: String) {
        val value = normalizeLaunchMode(mode)
        try {
            SystemProperties.set(SYS_PROP_POPUP_VIEW_MODE, value)
            val readBack = SystemProperties.get(SYS_PROP_POPUP_VIEW_MODE, "")
            if (readBack != value) {
                setString(context, KEY_POPUP_VIEW_MODE_FALLBACK_MODE, value)
            } else {
                setString(context, KEY_POPUP_VIEW_MODE_FALLBACK_MODE, value)
            }
        } catch (e: Exception) {
            setString(context, KEY_POPUP_VIEW_MODE_FALLBACK_MODE, value)
        }
    }

    fun getAppLaunchMode(context: Context, defaultMode: String = MODE_BUBBLE): String {
        val normalizedDefault = normalizeLaunchMode(defaultMode)
        return try {
            val prop = SystemProperties.get(SYS_PROP_POPUP_VIEW_MODE, normalizedDefault)
            val normalizedProp = normalizeLaunchModeOrNull(prop)
            if (normalizedProp != null) {
                normalizedProp
            } else {
                getFallbackLaunchMode(context, normalizedDefault)
            }
        } catch (e: Exception) {
            getFallbackLaunchMode(context, normalizedDefault)
        }
    }

    fun setPopupViewMode(context: Context, useBubbleMode: Boolean) {
        val value = if (useBubbleMode) MODE_BUBBLE else MODE_FREEFORM
        setAppLaunchMode(context, value)
        setBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, useBubbleMode)
    }

    fun getPopupViewMode(context: Context, defaultValue: Boolean = true): Boolean {
        val defaultMode = if (defaultValue) MODE_BUBBLE else MODE_FREEFORM
        return getAppLaunchMode(context, defaultMode) == MODE_BUBBLE
    }

    private fun getFallbackLaunchMode(context: Context, defaultMode: String): String {
        val stored = getString(context, KEY_POPUP_VIEW_MODE_FALLBACK_MODE, "")
        val normalizedStored = normalizeLaunchModeOrNull(stored)
        if (normalizedStored != null) {
            return normalizedStored
        }
        val legacy = getBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, defaultMode == MODE_BUBBLE)
        return if (legacy) MODE_BUBBLE else MODE_FREEFORM
    }

    private fun normalizeLaunchMode(mode: String): String {
        return normalizeLaunchModeOrNull(mode) ?: MODE_BUBBLE
    }

    private fun normalizeLaunchModeOrNull(mode: String): String? {
        return when (mode) {
            MODE_BUBBLE -> MODE_BUBBLE
            MODE_FREEFORM, MODE_FREEFORM_LEGACY -> MODE_FREEFORM
            MODE_POPUP_VIEW -> MODE_POPUP_VIEW
            else -> null
        }
    }

    private fun setString(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(key, value) }
    }

    private fun getString(context: Context, key: String, defaultValue: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun setPopupViewNotifsEnabled(context: Context, enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        try {
            SystemProperties.set(SYS_PROP_POPUP_VIEW_NOTIFS, value)
            val readBack = SystemProperties.get(SYS_PROP_POPUP_VIEW_NOTIFS, if (enabled) "0" else "1")
            if (readBack != value) {
                setBoolean(context, KEY_POPUP_VIEW_NOTIFS_FALLBACK, enabled)
            } else {
                setBoolean(context, KEY_POPUP_VIEW_NOTIFS_FALLBACK, enabled)
            }
        } catch (e: Exception) {
            setBoolean(context, KEY_POPUP_VIEW_NOTIFS_FALLBACK, enabled)
        }
    }

    fun isPopupViewNotifsEnabled(context: Context, defaultValue: Boolean = false): Boolean {
        return try {
            val prop = SystemProperties.get(SYS_PROP_POPUP_VIEW_NOTIFS, if (defaultValue) "1" else "0")
            when (prop) {
                "1", "true", "TRUE" -> true
                "0", "false", "FALSE" -> false
                else -> getBoolean(context, KEY_POPUP_VIEW_NOTIFS_FALLBACK, defaultValue)
            }
        } catch (e: Exception) {
            getBoolean(context, KEY_POPUP_VIEW_NOTIFS_FALLBACK, defaultValue)
        }
    }

    fun setBetaForceRelaunchEnabled(context: Context, enabled: Boolean) {
        val value = if (enabled) "true" else "false"
        try {
            SystemProperties.set(SYS_PROP_BETA_FORCE_RELAUNCH, value)
            val readBack = SystemProperties.get(SYS_PROP_BETA_FORCE_RELAUNCH, if (enabled) "false" else "true")
            if (readBack != value) {
                setBoolean(context, KEY_BETA_FORCE_RELAUNCH_FALLBACK, enabled)
            } else {
                setBoolean(context, KEY_BETA_FORCE_RELAUNCH_FALLBACK, enabled)
            }
        } catch (e: Exception) {
            setBoolean(context, KEY_BETA_FORCE_RELAUNCH_FALLBACK, enabled)
        }
    }

    fun isBetaForceRelaunchEnabled(context: Context, defaultValue: Boolean = false): Boolean {
        return try {
            val prop = SystemProperties.get(SYS_PROP_BETA_FORCE_RELAUNCH, if (defaultValue) "true" else "false")
            when (prop) {
                "1", "true", "TRUE" -> true
                "0", "false", "FALSE" -> false
                else -> getBoolean(context, KEY_BETA_FORCE_RELAUNCH_FALLBACK, defaultValue)
            }
        } catch (e: Exception) {
            getBoolean(context, KEY_BETA_FORCE_RELAUNCH_FALLBACK, defaultValue)
        }
    }
}
