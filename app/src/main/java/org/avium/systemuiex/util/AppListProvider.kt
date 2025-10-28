package org.avium.systemuiex.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.avium.systemuiex.model.AppInfo

object AppListProvider {

    suspend fun getLaunchableApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA or PackageManager.MATCH_ALL)
        resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            if (packageName != null && appName != null && icon != null) {
                 AppInfo(appName, packageName, icon)
            } else {
                null
            }
        }.sortedBy { it.appName }
    }
}