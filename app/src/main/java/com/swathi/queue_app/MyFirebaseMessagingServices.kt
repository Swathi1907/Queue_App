package com.swathi.queue_app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Log.d("FCM", "New Token: $token")

        // TODO: Send this token to your Node.js backend
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "Title: ${message.notification?.title}")
        Log.d("FCM", "Body: ${message.notification?.body}")

        // Later we'll show a notification here
    }
}