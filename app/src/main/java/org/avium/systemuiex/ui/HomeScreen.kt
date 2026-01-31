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

package org.avium.systemuiex.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.avium.systemuiex.R
import org.avium.systemuiex.util.PreferenceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAppSelection: () -> Unit,
    onOpenGlobalSidebar: () -> Unit
) {
    val context = LocalContext.current

    var doubleTapExit by remember { mutableStateOf(false) }
    var notifPortrait by remember { mutableStateOf(false) }
    var notifLandscape by remember { mutableStateOf(false) }
    var useBubbleMode by remember { mutableStateOf(true) }

    val popupGestureEnabled = remember {
        mutableStateOf(
            PreferenceHelper.isPopupGestureEnabled(context, false)
        )
    }

    val launcherGestureEnabled = remember {
        mutableStateOf(
            PreferenceHelper.isLauncherGestureEnabled(context, false)
        )
    }

    val popupViewNotifsEnabled = remember {
        mutableStateOf(
            PreferenceHelper.isPopupViewNotifsEnabled(context, false)
        )
    }

    var gestureAreaWidth by remember { mutableStateOf(20f) }
    var gestureAreaHeight by remember { mutableStateOf(20f) }


    LaunchedEffect(Unit) {
        doubleTapExit = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_DOUBLE_TAP_EXIT, false)
        notifPortrait = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_PORTRAIT, true)
        notifLandscape = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_LANDSCAPE, true)
        useBubbleMode = PreferenceHelper.getPopupViewMode(context, true)
        gestureAreaWidth = PreferenceHelper.getGestureAreaWidth(context, 20f)
        gestureAreaHeight = PreferenceHelper.getGestureAreaHeight(context, 20f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.top_bar_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_page),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            )
            Text(
                text = stringResource(R.string.main_hint),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.main_ps),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenAppSelection() }
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .alpha(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.manage_label_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.manage_label_summary),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenGlobalSidebar() }
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .alpha(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.global_sidebar_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.global_sidebar_summary),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingSwitchRow(
                title = stringResource(R.string.popup_gesture),
                summary = stringResource(R.string.popup_gesture_summary),
                checked = popupGestureEnabled.value,
                onCheckedChange = {
                    popupGestureEnabled.value = it
                    PreferenceHelper.setPopupGestureEnabled(context, it)
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.launcher_gesture_title),
                summary = stringResource(R.string.launcher_gesture_summary),
                checked = launcherGestureEnabled.value,
                onCheckedChange = {
                    launcherGestureEnabled.value = it
                    PreferenceHelper.setLauncherGestureEnabled(context, it)
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.app_launch_mode_title),
                summary = if (useBubbleMode) stringResource(R.string.app_launch_mode_bubble) else stringResource(R.string.app_launch_mode_lightweight),
                checked = useBubbleMode,
                onCheckedChange = { checked ->
                    useBubbleMode = checked
                    PreferenceHelper.setBoolean(context, PreferenceHelper.KEY_APP_LAUNCH_MODE, checked)
                    PreferenceHelper.setPopupViewMode(context, checked)
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.popup_view_notifs_title),
                summary = stringResource(R.string.popup_view_notifs_summary),
                checked = popupViewNotifsEnabled.value,
                onCheckedChange = {
                    popupViewNotifsEnabled.value = it
                    PreferenceHelper.setPopupViewNotifsEnabled(context, it)
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.gesture_area_warning),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingSliderRow(
                title = stringResource(R.string.gesture_area_width_title),
                value = gestureAreaWidth,
                valueRange = 20f..100f,
                onValueChange = { value ->
                    gestureAreaWidth = value
                    PreferenceHelper.setGestureAreaWidth(context, value)
                }
            )

            SettingSliderRow(
                title = stringResource(R.string.gesture_area_height_title),
                value = gestureAreaHeight,
                valueRange = 20f..100f,
                onValueChange = { value ->
                    gestureAreaHeight = value
                    PreferenceHelper.setGestureAreaHeight(context, value)
                }
            )

            /* 
            SettingSwitchRow(
                title = stringResource(R.string.popup_double_tap_exit_title),
                summary = stringResource(R.string.popup_double_tap_exit_summary),
                checked = doubleTapExit,
                onCheckedChange = { checked ->
                    doubleTapExit = checked
                    PreferenceHelper.setBoolean(context, PreferenceHelper.KEY_POPUP_DOUBLE_TAP_EXIT, checked)
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.popup_notification_portrait_title),
                summary = stringResource(R.string.popup_notification_portrait_summary),
                checked = notifPortrait,
                onCheckedChange = { checked ->
                    notifPortrait = checked
                    PreferenceHelper.setBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_PORTRAIT, checked)
                }
            )

            SettingSwitchRow(
                title = stringResource(R.string.popup_notification_landscape_title),
                summary = stringResource(R.string.popup_notification_landscape_summary),
                checked = notifLandscape,
                onCheckedChange = { checked ->
                    notifLandscape = checked
                    PreferenceHelper.setBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_LANDSCAPE, checked)
                }
            )
            */

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value.toInt().toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 80
        )
    }
}
