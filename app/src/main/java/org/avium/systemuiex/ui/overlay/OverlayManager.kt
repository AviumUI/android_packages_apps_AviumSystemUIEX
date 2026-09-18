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


package org.avium.systemuiex.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import android.util.Log
import org.avium.systemuiex.R
import org.avium.systemuiex.util.PreferenceHelper
import java.util.concurrent.ConcurrentLinkedQueue

@SuppressLint("StaticFieldLeak")
object OverlayManager {

    private val TAG = "OverlayManager"
    @SuppressLint("StaticFieldLeak")
    private var overlayView: BaseAppCircleViewGroup? = null
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val touchCoordinatesQueue = ConcurrentLinkedQueue<Triple<Float, Float, Boolean>>()
    private var isGestureActive = false

    private var receiverContext: Context? = null
    private val profileReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            hide()
        }
    }

    fun show(context: Context, isLeft: Boolean, initialTouchX: Float = -1f, initialTouchY: Float = -1f) {

        this.context = context
        
        if (overlayView != null) {
            hide()
        }

        val selectedApps = PreferenceHelper.getSelectedApps(context)
        
        if (selectedApps.isEmpty()) {
            return
        }

        receiverContext = context.applicationContext.also {
            it.registerReceiver(profileReceiver, android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_PROFILE_INACCESSIBLE)
                addAction(Intent.ACTION_PROFILE_REMOVED)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            }, Context.RECEIVER_EXPORTED)
        }

        overlayView = if (isLeft) {
            LeftAppCircleViewGroup(context)
        } else {
            RightAppCircleViewGroup(context)
        }

        val visibleApps = mutableListOf<String>()

        selectedApps.take(org.avium.systemuiex.util.Config.MAX_ICONS - 1).forEach { packageName ->
            try {
                val appInfo = org.avium.systemuiex.util.AppListProvider.resolveApp(context, packageName)
                    ?: return@forEach
                val icon = appInfo.icon
                visibleApps.add(packageName)
                val imageView = ImageView(context).apply {
                    setImageDrawable(icon)
                }
                overlayView?.addView(imageView)
            } catch (e: Exception) {
                //do nothing
            }
        }

        val moreAppsButton = ImageView(context).apply {
            setImageResource(R.drawable.ic_more_app_list)
        }
        overlayView?.addView(moreAppsButton)

        overlayView?.let { 
            it.setOnIconLaunchListener { index ->
                val apps = visibleApps
                if (index < apps.size) {
                    org.avium.systemuiex.util.AppLauncher.launchApp(context, apps[index])
                } else {
                    val intent = Intent(context,
                        org.avium.systemuiex.ui.selection.AppLaunchPickerActivity::class.java)
                        .putExtra(org.avium.systemuiex.ui.selection.AppSelectionActivity.EXTRA_MODE,
                            org.avium.systemuiex.ui.selection.AppSelectionActivity.MODE_LAUNCH)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Tear down the gesture surface before the picker starts sampling the
                    // background for blur. Defer launch until this input callback returns.
                    hide()
                    mainHandler.post { context.startActivity(intent) }
                    return@setOnIconLaunchListener
                }
                hide()
            }
            
            it.setOnDismissListener {
                hide()
            }
            
            if (initialTouchX >= 0 && initialTouchY >= 0) {
                it.setInitialTouchPoint(initialTouchX, initialTouchY)
            }
            
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.addView(it, BaseAppCircleViewGroup.groupLayoutParams)
                
                while (touchCoordinatesQueue.isNotEmpty()) {
                    val touch = touchCoordinatesQueue.poll()
                    touch?.let { (x, y, isUp) ->
                        it.dispatchTouchCoordinates(x, y, isUp)
                    }
                }
            } catch (e: Exception) {
                overlayView = null
            }
        }
    }

    fun hide() {
        receiverContext?.unregisterReceiver(profileReceiver)
        receiverContext = null
        overlayView?.let {
            try {
                val ctx = context ?: it.context
                val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(it)
            } catch (e: Exception) {
                //do nothing
            } finally {
                overlayView = null
                isGestureActive = false
                touchCoordinatesQueue.clear()
            }
        }
    }
    
    fun onTouchCoordinates(x: Float, y: Float, isUp: Boolean) {
        if (!isGestureActive && !isUp) {
            isGestureActive = true
        }
        
        if (overlayView == null) {
            touchCoordinatesQueue.add(Triple(x, y, isUp))
        } else {
            mainHandler.post {
                overlayView?.dispatchTouchCoordinates(x, y, isUp)
            }
        }
        
        if (isUp) {
            isGestureActive = false
            touchCoordinatesQueue.clear()
        }
    }
}
