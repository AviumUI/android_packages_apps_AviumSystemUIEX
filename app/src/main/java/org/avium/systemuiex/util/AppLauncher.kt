package org.avium.systemuiex.util

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import android.widget.Toast
import org.avium.systemuiex.R

object AppLauncher {

    /*
    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            showLaunchFailToast(context)
        }
    }
    */

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val miniWindowOptions = ActivityOptions.makeBasic().apply {
                setLaunchWindowingMode(102)
            }

            context.startActivity(intent, miniWindowOptions.toBundle())
        } else {
            showLaunchFailToast(context)
        }
    }

    private fun showLaunchFailToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.cannot_launch_app), Toast.LENGTH_SHORT).show()
    }
}
