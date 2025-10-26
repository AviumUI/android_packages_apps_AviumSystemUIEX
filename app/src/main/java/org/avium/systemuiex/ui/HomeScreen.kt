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
    onOpenAppSelection: () -> Unit
) {
    val context = LocalContext.current

    var doubleTapExit by remember { mutableStateOf(false) }
    var notifPortrait by remember { mutableStateOf(false) }
    var notifLandscape by remember { mutableStateOf(false) }

    val popupGestureEnabled = remember {
        mutableStateOf(
            PreferenceHelper.isPopupGestureEnabled(context, false)
        )
    }


    LaunchedEffect(Unit) {
        doubleTapExit = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_DOUBLE_TAP_EXIT, false)
        notifPortrait = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_PORTRAIT, true)
        notifLandscape = PreferenceHelper.getBoolean(context, PreferenceHelper.KEY_POPUP_NOTIFICATION_LANDSCAPE, true)
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
