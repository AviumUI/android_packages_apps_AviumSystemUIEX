package org.avium.systemuiex.ui.selection

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.avium.systemuiex.R
import org.avium.systemuiex.model.AppInfo
import org.avium.systemuiex.ui.theme.SystemUIEXTheme
import org.avium.systemuiex.util.AppLauncher
import org.avium.systemuiex.util.AppListProvider
import org.avium.systemuiex.util.Config
import org.avium.systemuiex.util.PreferenceHelper

class AppSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SystemUIEXTheme {
                AppSelectionScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen() {
    val context = LocalContext.current
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedApps = remember { mutableStateOf(PreferenceHelper.getSelectedApps(context)) }
    val maxSelectionCount = Config.MAX_ICONS - 1

    fun updateSelection(newSelection: Set<String>) {
        selectedApps.value = newSelection
        PreferenceHelper.saveSelectedApps(context, newSelection)
        appList = appList.map { it.copy(isSelected = newSelection.contains(it.packageName)) }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        appList = AppListProvider.getLaunchableApps(context).map {
            it.copy(isSelected = selectedApps.value.contains(it.packageName))
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.manage_apps)) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SelectedAppsPreview(selectedApps.value) { packageName ->
                val currentSelected = selectedApps.value.toMutableSet()
                currentSelected.remove(packageName)
                updateSelection(currentSelected)
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(appList) { app ->
                        AppGridItem(
                            app = app,
                            onSelect = { packageName ->
                                val currentSelected = selectedApps.value.toMutableSet()
                                if (currentSelected.size < maxSelectionCount) {
                                    currentSelected.add(packageName)
                                    updateSelection(currentSelected)
                                } else {
                                    val toastText = context.getString(R.string.selection_limit_toast, maxSelectionCount)
                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedAppsPreview(selectedAppPackages: Set<String>, onDeselect: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val selectedAppInfo = remember(selectedAppPackages) {
        selectedAppPackages.mapNotNull { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                Pair(packageName, appInfo.loadIcon(pm))
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.selected_apps), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedAppInfo.isEmpty()) {
            Text(stringResource(R.string.no_apps_selected), modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(selectedAppInfo) { (packageName, icon) ->
                    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Image(
                            painter = rememberDrawablePainter(drawable = icon),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Deselect App",
                            tint = Color.Red,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clickable { onDeselect(packageName) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AppGridItem(
    app: AppInfo,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clickable { AppLauncher.launchApp(context, app.packageName) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Image(
                painter = rememberDrawablePainter(drawable = app.icon),
                contentDescription = app.appName,
                modifier = Modifier.fillMaxSize()
            )
            if (!app.isSelected) {
                 Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Select App",
                    tint = Color.Green,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clickable { onSelect(app.packageName) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.appName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}