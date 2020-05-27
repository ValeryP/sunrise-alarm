package com.sunrise_alarm.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sunrise_alarm.utils.BluetoothManager
import com.sunrise_alarm.utils.BluetoothManager.BluetoothMessage
import com.sunrise_alarm.utils.Const.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Service to receive FCM notifications triggered by voice
 *
 * Responsible for receiving FCM messages (look on firebase-cloud-fun module) and switching the
 * light even if the app is on the background. Voice integration implemented via IFTTT.
 */
class FCMService : FirebaseMessagingService() {
    @ExperimentalCoroutinesApi
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i(LOG_TAG, "From: " + remoteMessage.from)

        if (remoteMessage.data.isNotEmpty()) {
            Log.i(LOG_TAG, "Message data payload: " + remoteMessage.data)
            CoroutineScope(Main).launch {
                BluetoothManager(this@FCMService, true).apply {
                    val channel = subscribe()
                    var counter = 0
                    channel.consumeEach {
                        if (it is BluetoothMessage) {
                            val isCommandLightOn = remoteMessage.data["light"]?.toBoolean() ?: false
                            val isLightOn = it.message.contains("isAlarm: 1", true)
                            val isLightOff = it.message.contains("isAlarm: 0", true)
                            // received command, execute
                            if (isLightOn && !isCommandLightOn || isLightOff && isCommandLightOn) {
                                this.switchLight()
                                this.cancel()
                            } else if (isLightOn || isLightOff) { // command mismatch (light is already on, command: "lights on")
                                if (counter > 5) {
                                    Log.i(LOG_TAG, "counter > 5 - off")
                                    this.cancel()
                                } else {
                                    Log.i(LOG_TAG, "counting: $counter")
                                    counter++
                                }
                            } else {
                                Log.i(LOG_TAG, "received other message: " + it.message)
                            }
                        }
                    }
                }
            }
        }

        if (remoteMessage.notification != null) {
            Log.i(LOG_TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
        }
    }

    // Don't need to update the token because the app uses topic subscription
    override fun onNewToken(token: String) {
        Log.i(LOG_TAG, "Refreshed token: $token")
    }
}