package com.swathi.queue_app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Log.d("FCM", "New Token: $token")

        // we have to Send this token to our Node.js backend
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "Queue App"
        val body = message.notification?.body ?: ""

        Log.d("FCM", "Title: $title")
        Log.d("FCM", "Body: $body")

        val notification = NotificationCompat.Builder(this, "queue_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "queue_channel",
                "Queue Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(channel)
        }

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification.build()
        )
    }
}