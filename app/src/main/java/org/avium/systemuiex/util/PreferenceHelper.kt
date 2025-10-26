package org.avium.systemuiex.util

import android.content.Context
import androidx.core.content.edit
import android.os.SystemProperties

object PreferenceHelper {

    private const val PREFS_NAME = "SystemUIEX_Prefs"
    private const val KEY_SELECTED_APPS = "selected_apps"

    const val KEY_POPUP_DOUBLE_TAP_EXIT = "pop_up_view_double_tap_exit"
    const val KEY_POPUP_NOTIFICATION_PORTRAIT = "pop_up_view_notification_portrait"
    const val KEY_POPUP_NOTIFICATION_LANDSCAPE = "pop_up_view_notification_landscape"

    const val SYS_PROP_POPUP_GESTURE = "persist.avium.popup_gesture"
    const val KEY_POPUP_GESTURE_FALLBACK = "pop_up_view_gesture_fallback"

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
}
