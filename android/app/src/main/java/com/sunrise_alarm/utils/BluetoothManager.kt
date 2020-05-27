package com.sunrise_alarm.utils

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import com.sunrise_alarm.R
import com.sunrise_alarm.data.SavedTime
import com.sunrise_alarm.utils.Const.BLUETOOTH_PIN
import com.sunrise_alarm.utils.Const.DEVICE_ADDRESS
import com.sunrise_alarm.utils.Const.LOG_TAG
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback
import org.joda.time.DateTime

/*
 * @author Valeriy Palamarchuk
 * @email valeriij.palamarchuk@gmail.com
 * Created on 13.02.2020
 */

/**
 * Incapsulate all interactions with Arduino's bluetooth
 */
@ExperimentalCoroutinesApi
class BluetoothManager(val context: Context, val logsEnabled: Boolean = false) {
    private var bluetooth = Bluetooth(context)
    private var arduino: BluetoothDevice? = null

    /**
     * Single interface to unify the messages' format
     */
    interface BluetoothEvent {
        val message: String
    }

    // Errors custom type for bluetooth interactions
    data class BluetoothError(override val message: String) :
        BluetoothEvent

    data class BluetoothMessage(override val message: String) :
        BluetoothEvent

    data class BluetoothDeviceConnected(override val message: String) :
        BluetoothEvent

    // Interface which act as hot observables pattern to broadcast the bluetooth signals
    var bluetoothChannel = ConflatedBroadcastChannel<BluetoothEvent>()
    var discoveryChannel = ConflatedBroadcastChannel<BluetoothEvent>()
    var deviceChannel = ConflatedBroadcastChannel<BluetoothEvent>()

    private val pairDialog = AlertDialog.Builder(context)
        .setTitle(R.string.pair_with_device)
        .setMessage(R.string.pair_with_device_message)
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok) { _, _ -> bluetooth.pair(arduino, BLUETOOTH_PIN) }
        .create()

    /**
     * Establish bluetooth connection and start listening for the events
     */
    fun subscribe(): ReceiveChannel<BluetoothEvent> {
        if (logsEnabled) Log.d(LOG_TAG, "subscribe -> subscribe()")

        bluetoothChannel = ConflatedBroadcastChannel()
        discoveryChannel = ConflatedBroadcastChannel()
        deviceChannel = ConflatedBroadcastChannel()
        registerBluetoothChannel()
        registerDiscoveryChannel()
        registerDeviceChannel()

        bluetooth.onStart()
        if (bluetooth.isEnabled) {
            emitMessage(bluetoothChannel, "subscribe -> bluetooth.isEnabled")
            if (!bluetooth.isConnected) {
                val device = bluetooth.pairedDevices.firstOrNull { it.address == DEVICE_ADDRESS }
                if (device == null) {
                    if (logsEnabled) Log.d(LOG_TAG, "subscribe -> startScanning")
                    bluetooth.startScanning()
                } else {
                    bluetooth.connectToAddress(DEVICE_ADDRESS)
                }
            }
        } else {
            bluetooth.showEnableDialog(context as Activity?)
        }

        return deviceChannel.openSubscription()
    }

    /**
     * Cancel all hot channels and stop listening for the events
     */
    fun cancel() {
        bluetoothChannel.cancel()
        discoveryChannel.cancel()
        deviceChannel.cancel()

        if (bluetooth.isConnected) bluetooth.disconnect()
        bluetooth.onStop()
        arduino = null
    }

    /**
     * Change the state of the lights
     */
    fun switchLight() {
        bluetooth.send("1|")
    }

    /**
     * Update the values of the active time ranges on the Arduino
     *
     * E.g: "0|202001250544 [0545-1001,1029-1010,]"
     */
    fun updateAlarmRanges() {
        val msg = "0|${DateTime.now().toString("yyyyMMddHHmm")} " +
                "[${SavedTime.timeFrom1()}-${SavedTime.timeTo1()}," +
                "${SavedTime.timeFrom2()}-${SavedTime.timeTo2()},]"
        bluetooth.send(msg.replace(":", ""))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        bluetooth.onActivityResult(requestCode, resultCode)
    }

    /**
     * Send a signal on selected broadcasting channel
     */
    private fun emitMessage(channel: BroadcastChannel<BluetoothEvent>, message: String) {
        if (!channel.isClosedForSend) {
            if (logsEnabled) Log.d(LOG_TAG, "emitMessage -> $message")
            GlobalScope.launch(Main) {
                channel.send(
                    BluetoothMessage(
                        message
                    )
                )
            }
        } else {
            Log.w(LOG_TAG, "emitMessage - channel.isClosedForSend: ${channel.isClosedForSend}")
        }
    }

    /**
     * Send an error on selected broadcasting channel
     */
    private fun emitError(channel: BroadcastChannel<BluetoothEvent>, message: String) {
        if (!channel.isClosedForSend) {
            if (logsEnabled) Log.d(LOG_TAG, "emitError -> $message")
            GlobalScope.launch(Main) {
                channel.send(
                    BluetoothError(
                        message
                    )
                )
            }
        } else {
            Log.w(LOG_TAG, "emitError - channel.isClosedForSend: ${channel.isClosedForSend}")
        }
    }

    /**
     * Register a channel and listen for bluetooth state events
     */
    private fun registerBluetoothChannel() {
        bluetooth.setBluetoothCallback(object : BluetoothCallback {
            override fun onBluetoothTurningOn() {
                emitMessage(bluetoothChannel, "statusCallback -> onBluetoothTurningOn")
            }

            override fun onBluetoothTurningOff() {
                emitMessage(bluetoothChannel, "statusCallback -> onBluetoothTurningOff")
            }

            override fun onBluetoothOff() {
                emitMessage(bluetoothChannel, "statusCallback -> onBluetoothOff")
            }

            override fun onBluetoothOn() {
                emitMessage(bluetoothChannel, "statusCallback -> onBluetoothOn")
            }

            override fun onUserDeniedActivation() {
                emitMessage(bluetoothChannel, "statusCallback -> onUserDeniedActivation")
                makeText(
                    context,
                    context.getString(R.string.allow_bt_permission),
                    LENGTH_LONG
                ).show()
            }
        })
    }

    /**
     * Register a channel and listen for bluetooth connection events
     */
    private fun registerDiscoveryChannel() {
        bluetooth.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
                emitMessage(discoveryChannel, "pairingCallback -> onDiscoveryStarted")
                arduino = null
            }

            /**
             * Triggered once the required device was discovered
             */
            override fun onDiscoveryFinished() {
                emitMessage(discoveryChannel, "pairingCallback -> onDiscoveryFinished")
                val device = bluetooth.pairedDevices.firstOrNull { it.address == DEVICE_ADDRESS }
                if (arduino == null) {
                    makeText(
                        context,
                        context.getString(R.string.message_too_far_from_device),
                        LENGTH_LONG
                    ).show()
                } else if (device == null) {
                    if (!pairDialog.isShowing) {
                        emitMessage(
                            discoveryChannel,
                            "pairingCallback -> onDiscoveryFinished (dialog)"
                        )
                        pairDialog.show()
                    }
                }
            }

            /**
             * Triggered once the user accepted to connect
             */
            override fun onDeviceFound(device: BluetoothDevice) {
                emitMessage(
                    discoveryChannel,
                    "pairingCallback -> onDeviceFound: ${device.name} | ${device.address}"
                )
                if (device.address == DEVICE_ADDRESS) {
                    arduino = device
                    if (!pairDialog.isShowing) {
                        emitMessage(discoveryChannel, "pairingCallback -> onDeviceFound (dialog)")
                        pairDialog.show()
                    }
                }
            }

            /**
             * Triggered once the device was successfully connected
             */
            override fun onDevicePaired(device: BluetoothDevice) {
                emitMessage(discoveryChannel, "pairingCallback -> onDevicePaired: ${device.name}")
                bluetooth.connectToAddress(DEVICE_ADDRESS)
            }

            override fun onDeviceUnpaired(device: BluetoothDevice) {
                emitMessage(discoveryChannel, "pairingCallback -> onDeviceUnpaired: ${device.name}")
            }

            override fun onError(errorCode: Int) {
                emitError(discoveryChannel, "pairingCallback -> onError: $errorCode")
            }
        })
    }

    /**
     * Register a channel and listen for bluetooth message events
     */
    private fun registerDeviceChannel() {
        bluetooth.setDeviceCallback(object : DeviceCallback {
            override fun onDeviceConnected(device: BluetoothDevice?) {
                if (!deviceChannel.isClosedForSend) {
                    GlobalScope.launch(Main) {
                        deviceChannel.send(
                            BluetoothDeviceConnected("connectionCallback -> onDeviceConnected")
                        )
                        if (logsEnabled) Log.d(
                            LOG_TAG,
                            "registerDeviceChannel -> onDeviceConnected"
                        )
                    }
                }
            }

            override fun onDeviceDisconnected(device: BluetoothDevice?, message: String?) {
                emitMessage(deviceChannel, "connectionCallback -> onDeviceDisconnected")
            }

            override fun onMessage(message: ByteArray?) {
                message?.let {
                    emitMessage(deviceChannel, "connectionCallback -> onMessage: ${String(it)}")
                }
            }

            override fun onError(errorCode: Int) {
                emitMessage(deviceChannel, "connectionCallback -> onError: $errorCode")
            }

            override fun onConnectError(device: BluetoothDevice?, message: String?) {
                emitError(deviceChannel, "connectionCallback -> onConnectError: $message")
                bluetooth.connectToAddress(DEVICE_ADDRESS)
            }
        })
    }
}