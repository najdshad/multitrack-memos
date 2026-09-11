package com.multitrackmemos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Keeps the recording session alive when the screen is off. Audio ownership is added in Phase 1. */
class RecordingService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Recording", NotificationManager.IMPORTANCE_LOW))
        startForeground(NOTIFICATION_ID, Notification.Builder(this, CHANNEL).setContentTitle("Sketch recording").setContentText("Ready").setSmallIcon(android.R.drawable.ic_btn_speak_now).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { private const val CHANNEL = "recording"; private const val NOTIFICATION_ID = 1 }
}
