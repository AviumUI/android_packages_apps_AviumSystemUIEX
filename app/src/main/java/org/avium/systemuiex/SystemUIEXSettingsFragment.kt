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
    }

    private fun bindSwitchPreferences() {
        val context = requireContext()

        bindSwitch(KEY_POPUP_GESTURE, PreferenceHelper.isPopupGestureEnabled(context, false)) {
            PreferenceHelper.setPopupGestureEnabled(context, it)
        }

        bindSwitch(KEY_LAUNCHER_GESTURE, PreferenceHelper.isLauncherGestureEnabled(context, false)) {
            PreferenceHelper.setLauncherGestureEnabled(context, it)
        }

        bindSwitch(KEY_APP_LAUNCH_MODE, PreferenceHelper.getPopupViewMode(context, true)) { enabled ->
            PreferenceHelper.setBoolean(context, PreferenceHelper.KEY_APP_LAUNCH_MODE, enabled)
            PreferenceHelper.setPopupViewMode(context, enabled)
        }

        bindSwitch(KEY_POPUP_VIEW_NOTIFS, PreferenceHelper.isPopupViewNotifsEnabled(context, false)) {
            PreferenceHelper.setPopupViewNotifsEnabled(context, it)
        }
    }

    private fun bindSwitch(key: String, initialValue: Boolean, onChange: (Boolean) -> Unit) {
        val pref = findPreference<SwitchPreferenceCompat>(key) ?: return
        pref.isPersistent = false
        pref.isChecked = initialValue

        if (key == KEY_APP_LAUNCH_MODE) {
            updateLaunchModeSummary(pref, initialValue)
        }

        pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            onChange(enabled)
            if (key == KEY_APP_LAUNCH_MODE) {
                updateLaunchModeSummary(pref, enabled)
            }
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

    private fun updateLaunchModeSummary(pref: SwitchPreferenceCompat, useBubbleMode: Boolean) {
        pref.summary = getString(
            if (useBubbleMode) {
                R.string.app_launch_mode_bubble
            } else {
                R.string.app_launch_mode_lightweight
            }
        )
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
        private const val KEY_APP_LAUNCH_MODE = "app_launch_mode"
        private const val KEY_POPUP_VIEW_NOTIFS = "popup_view_notifs"
        private const val KEY_GESTURE_AREA_WIDTH = "gesture_area_width"
        private const val KEY_GESTURE_AREA_HEIGHT = "gesture_area_height"
    }
}
