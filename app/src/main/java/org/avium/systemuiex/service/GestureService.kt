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

package org.avium.systemuiex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import org.avium.systemuiex.R
import org.avium.systemuiex.ui.overlay.OverlayManager
import androidx.core.net.toUri

class GestureService : Service() {

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "GestureServiceChannel"

    companion object {
        fun startViaPendingIntent(context: Context, isLeft: Boolean, touchX: Float = -1f, touchY: Float = -1f) {

            val serviceIntent = Intent(context, GestureService::class.java).apply {
                putExtra("isLeft", isLeft)
                putExtra("touchX", touchX)
                putExtra("touchY", touchY)
            }

            val pendingIntent =
                PendingIntent.getForegroundService(
                    context,
                    0,
                    serviceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            try {
                pendingIntent.send()
            } catch (e: Exception) {
                //do nothing
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gesture Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "tmp"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val isLeft = intent.getBooleanExtra("isLeft", true)
        val touchX = intent.getFloatExtra("touchX", -1f)
        val touchY = intent.getFloatExtra("touchY", -1f)

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        OverlayManager.show(this, isLeft, touchX, touchY)
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, org.avium.systemuiex.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(" ")
                .setContentText(" ")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setSilent(true)
                .build()

    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayManager.hide()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        OverlayManager.hide()
        stopSelf()
    }
}