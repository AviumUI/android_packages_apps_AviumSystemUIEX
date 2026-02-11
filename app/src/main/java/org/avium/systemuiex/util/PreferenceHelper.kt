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
    private const val KEY_SELECTED_APPS = "selected_apps"
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

    const val SYS_PROP_POPUP_VIEW_NOTIFS = "persist.avium.popup_view_notifs"
    const val KEY_POPUP_VIEW_NOTIFS_FALLBACK = "popup_view_notifs_fallback"

    const val KEY_TRIGGER_WIDTH = "trigger_width"
    const val KEY_TRIGGER_HEIGHT = "trigger_height"
    const val KEY_SWIPE_TOLERANCE = "swipe_tolerance"

    fun setFloat(context: Context, key: String, value: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putFloat(key, value) }
    }

    fun getFloat(context: Context, key: String, defaultValue: Float): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(key, defaultValue)
    }

    fun saveSelectedApps(context: Context, selectedApps: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(KEY_SELECTED_APPS, selectedApps) }
    }

    fun getSelectedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
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

    fun setPopupViewMode(context: Context, useBubbleMode: Boolean) {
        val value = if (useBubbleMode) "bubble" else "free window"
        try {
            SystemProperties.set(SYS_PROP_POPUP_VIEW_MODE, value)
            val readBack = SystemProperties.get(SYS_PROP_POPUP_VIEW_MODE, "")
            if (readBack != value) {
                setBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, useBubbleMode)
            } else {
                setBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, useBubbleMode)
            }
        } catch (e: Exception) {
            setBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, useBubbleMode)
        }
    }

    fun getPopupViewMode(context: Context, defaultValue: Boolean = true): Boolean {
        return try {
            val prop = SystemProperties.get(SYS_PROP_POPUP_VIEW_MODE, if (defaultValue) "bubble" else "free window")
            when (prop) {
                "bubble" -> true
                "free window" -> false
                else -> getBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, defaultValue)
            }
        } catch (e: Exception) {
            getBoolean(context, KEY_POPUP_VIEW_MODE_FALLBACK, defaultValue)
        }
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
}
