/*
 * Copyright (C) 2025 The AviumUI Project
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
 */

package org.avium.systemuiex

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SliderPreference
import org.avium.systemuiex.ui.selection.AppSelectionActivity
import org.avium.systemuiex.util.PreferenceHelper
import kotlin.math.roundToInt

class SystemUIEXSettingsFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PreferenceHelper.PREFS_NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.systemuiex_settings, rootKey)

        bindActionPreferences()
        bindBehaviorPreferences()
        bindSwitchPreferences()
        bindSliderPreferences()
    }

    private fun bindActionPreferences() {
        findPreference<Preference>(KEY_MANAGE_APPS)?.apply {
            isPersistent = false
            setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AppSelectionActivity::class.java))
                true
            }
        }

        findPreference<Preference>(KEY_GLOBAL_SIDEBAR)?.apply {
            isPersistent = false
            setOnPreferenceClickListener {
                openMiFreeform()
                true
            }
        }

        findPreference<Preference>(KEY_POPUP_NOTIFICATION_BLACKLIST)?.apply {
            isPersistent = false
            setOnPreferenceClickListener {
                openBlacklistSelection()
                true
            }
        }
    }

    private fun bindBehaviorPreferences() {
        val context = requireContext()

        bindSwitch(KEY_POPUP_NOTIFICATION_JUMP_PORTRAIT, PreferenceHelper.isPopupNotificationJumpPortrait(context, false)) {
            PreferenceHelper.setPopupNotificationJumpPortrait(context, it)
        }

        bindSwitch(KEY_POPUP_NOTIFICATION_JUMP_LANDSCAPE, PreferenceHelper.isPopupNotificationJumpLandscape(context, false)) {
            PreferenceHelper.setPopupNotificationJumpLandscape(context, it)
        }

        bindListPreference(KEY_POPUP_SINGLE_TAP_ACTION, PreferenceHelper.getPopupSingleTapAction(context, 1).toString()) {
            PreferenceHelper.setPopupSingleTapAction(context, it.toInt())
        }

        bindListPreference(KEY_POPUP_DOUBLE_TAP_ACTION, PreferenceHelper.getPopupDoubleTapAction(context, 2).toString()) {
            PreferenceHelper.setPopupDoubleTapAction(context, it.toInt())
        }
    }

    private fun bindListPreference(key: String, initialValue: String, onChange: (String) -> Unit) {
        val pref = findPreference<ListPreference>(key) ?: return
        pref.isPersistent = false
        pref.value = initialValue

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val selectedValue = newValue as String
            onChange(selectedValue)
            true
        }
    }

    private fun openBlacklistSelection() {
        val intent = Intent(requireContext(), AppSelectionActivity::class.java).apply {
            putExtra(AppSelectionActivity.EXTRA_MODE, AppSelectionActivity.MODE_BLACKLIST)
        }
        startActivity(intent)
    }

    private fun bindSwitchPreferences() {
        val context = requireContext()

        bindSwitch(KEY_POPUP_GESTURE, PreferenceHelper.isPopupGestureEnabled(context, false)) {
            PreferenceHelper.setPopupGestureEnabled(context, it)
        }

        bindSwitch(KEY_LAUNCHER_GESTURE, PreferenceHelper.isLauncherGestureEnabled(context, false)) {
            PreferenceHelper.setLauncherGestureEnabled(context, it)
        }

        bindLaunchModePreference()
    }

    private fun bindSwitch(key: String, initialValue: Boolean, onChange: (Boolean) -> Unit) {
        val pref = findPreference<SwitchPreferenceCompat>(key) ?: return
        pref.isPersistent = false
        pref.isChecked = initialValue

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            onChange(enabled)
            true
        }
    }

    private fun bindSliderPreferences() {
        val context = requireContext()

        bindSlider(KEY_GESTURE_AREA_WIDTH, PreferenceHelper.getGestureAreaWidth(context, 20f)) {
            PreferenceHelper.setGestureAreaWidth(context, it.toFloat())
        }

        bindSlider(KEY_GESTURE_AREA_HEIGHT, PreferenceHelper.getGestureAreaHeight(context, 20f)) {
            PreferenceHelper.setGestureAreaHeight(context, it.toFloat())
        }
    }

    private fun bindSlider(key: String, initialValue: Float, onChange: (Int) -> Unit) {
        val pref = findPreference<SliderPreference>(key) ?: return
        pref.isPersistent = false
        val value = initialValue.roundToInt().coerceIn(pref.min, pref.max)
        pref.value = value
        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            onChange(newValue as Int)
            true
        }
    }

    private fun bindLaunchModePreference() {
        val pref = findPreference<ListPreference>(KEY_APP_LAUNCH_MODE_PREF) ?: return
        pref.isPersistent = false

        val useBubbleMode = PreferenceHelper.getPopupViewMode(requireContext(), true)
        pref.value = if (useBubbleMode) VALUE_LAUNCH_MODE_BUBBLE else VALUE_LAUNCH_MODE_FREE_WINDOW

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val selectedValue = newValue as String
            val useBubble = selectedValue == VALUE_LAUNCH_MODE_BUBBLE
            PreferenceHelper.setBoolean(requireContext(), PreferenceHelper.KEY_APP_LAUNCH_MODE, useBubble)
            PreferenceHelper.setPopupViewMode(requireContext(), useBubble)
            true
        }
    }

    private fun openMiFreeform() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.sunshine.freeform",
                "com.sunshine.freeform.ui.splash.SplashActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val KEY_MANAGE_APPS = "manage_apps"
        private const val KEY_GLOBAL_SIDEBAR = "global_sidebar"
        private const val KEY_POPUP_GESTURE = "popup_gesture"
        private const val KEY_LAUNCHER_GESTURE = "launcher_gesture"
        private const val KEY_APP_LAUNCH_MODE_PREF = "app_launch_mode_pref"
        private const val KEY_GESTURE_AREA_WIDTH = "gesture_area_width"
        private const val KEY_GESTURE_AREA_HEIGHT = "gesture_area_height"
        private const val VALUE_LAUNCH_MODE_BUBBLE = "bubble"
        private const val VALUE_LAUNCH_MODE_FREE_WINDOW = "free_window"

        private const val KEY_POPUP_NOTIFICATION_JUMP_PORTRAIT = "popup_notification_jump_portrait"
        private const val KEY_POPUP_NOTIFICATION_JUMP_LANDSCAPE = "popup_notification_jump_landscape"
        private const val KEY_POPUP_NOTIFICATION_BLACKLIST = "popup_notification_blacklist"
        private const val KEY_POPUP_SINGLE_TAP_ACTION = "popup_single_tap_action"
        private const val KEY_POPUP_DOUBLE_TAP_ACTION = "popup_double_tap_action"
    }
}
