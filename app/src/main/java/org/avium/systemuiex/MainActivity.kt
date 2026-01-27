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

package org.avium.systemuiex

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.avium.systemuiex.ui.HomeScreen
import org.avium.systemuiex.ui.selection.AppSelectionActivity
import org.avium.systemuiex.ui.theme.SystemUIEXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SystemUIEXTheme {
                HomeScreen(
                    onOpenAppSelection = {
                        startActivity(Intent(this, AppSelectionActivity::class.java))
                    },
                    onOpenGlobalSidebar = {
                        openMiFreeform()
                    }
                )
            }
        }
    }

    private fun openMiFreeform() {
        val intent = Intent().apply {
            setComponent(
                ComponentName(
                    "com.sunshine.freeform",
                    "com.sunshine.freeform.ui.splash.SplashActivity"
                )
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
