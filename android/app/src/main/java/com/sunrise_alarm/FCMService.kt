package com.sunrise_alarm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i("xxx", "From: " + remoteMessage.from)

        if (remoteMessage.data.isNotEmpty()) {
            Log.i("xxx", "Message data payload: " + remoteMessage.data)
            // TODO: 03.05.2020 do the job
        }

        if (remoteMessage.notification != null) {
            Log.i("xxx", "Message Notification Body: " + remoteMessage.notification!!.body)
        }
    }

    // Don't need to update the token because the app uses topic subscription
    override fun onNewToken(token: String) {
        Log.i("xxx", "Refreshed token: $token")
    }
}