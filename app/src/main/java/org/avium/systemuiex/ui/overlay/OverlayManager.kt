
package org.avium.systemuiex.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.widget.ImageView
import android.util.Log
import org.avium.systemuiex.R
import org.avium.systemuiex.util.PreferenceHelper

@SuppressLint("StaticFieldLeak")
object OverlayManager {

    private val TAG = "OverlayManager"
    @SuppressLint("StaticFieldLeak")
    private var overlayView: BaseAppCircleViewGroup? = null
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null

    fun show(context: Context, isLeft: Boolean) {

        this.context = context
        
        if (overlayView != null) {
            hide()
        }

        val selectedApps = PreferenceHelper.getSelectedApps(context)
        
        if (selectedApps.isEmpty()) {
            return
        }

        overlayView = if (isLeft) {
            LeftAppCircleViewGroup(context)
        } else {
            RightAppCircleViewGroup(context)
        }

        val pm = context.packageManager

        selectedApps.take(org.avium.systemuiex.util.Config.MAX_ICONS - 1).forEach { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val icon = appInfo.loadIcon(pm)
                val imageView = ImageView(context).apply {
                    setImageDrawable(icon)
                    setOnClickListener { _ ->
                        org.avium.systemuiex.util.AppLauncher.launchApp(context, packageName)
                        hide()
                    }
                }
                overlayView?.addView(imageView)
            } catch (e: Exception) {
                //do nothing
            }
        }

        val moreAppsButton = ImageView(context).apply {
            setImageResource(R.drawable.ic_more_app_list)
            setOnClickListener {
                val intent = Intent(context, org.avium.systemuiex.ui.selection.AppSelectionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                hide()
            }
        }
        overlayView?.addView(moreAppsButton)

        overlayView?.let { 
            it.setOnClickListener { hide() }
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.addView(it, BaseAppCircleViewGroup.groupLayoutParams)
            } catch (e: Exception) {
                overlayView = null
            }
        }
    }

    fun hide() {
        overlayView?.let {
            try {
                val ctx = context ?: it.context
                val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(it)
            } catch (e: Exception) {
                //do nothing
            } finally {
                overlayView = null
            }
        }
    }
}
