package com.sunrise_alarm

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback

/*
 * @author Valeriy Palamarchuk
 * @email valeriij.palamarchuk@gmail.com
 * Created on 13.02.2020
 */
@ExperimentalCoroutinesApi
class BluetoothManager(val context: Activity) {
    private var bluetooth = Bluetooth(context)
    private var arduino: BluetoothDevice? = null

    interface BluetoothEvent {
        val message: String
    }

    data class BluetoothError(override val message: String) : BluetoothEvent
    data class BluetoothMessage(override val message: String) : BluetoothEvent
    data class BluetoothDeviceConnected(override val message: String) : BluetoothEvent

    var bluetoothChannel = ConflatedBroadcastChannel<BluetoothEvent>()
    var discoveryChannel = ConflatedBroadcastChannel<BluetoothEvent>()
    var deviceChannel = ConflatedBroadcastChannel<BluetoothEvent>()
    private val pairDialog = AlertDialog.Builder(context)
        .setTitle("Pair with device")
        .setMessage("After you click OK the app will insert the pin code by itself. Don't do anything and just wait 5 sec.")
        .setCancelable(false)
        .setPositiveButton("OK") { _, _ -> bluetooth.pair(arduino, "1234") }
        .create()

    init {
        bluetooth.setCallbackOnUI(context)
    }

    fun start() {
        bluetoothChannel = ConflatedBroadcastChannel()
        discoveryChannel = ConflatedBroadcastChannel()
        deviceChannel = ConflatedBroadcastChannel()
        registerBluetoothChannel()
        registerDiscoveryChannel()
        registerDeviceChannel()

        bluetooth.onStart()
        if (bluetooth.isEnabled) {
            emitMessage(bluetoothChannel, "onStart -> bluetooth.isEnabled")
            if (!bluetooth.isConnected) {
                val device = bluetooth.pairedDevices.firstOrNull { it.address == DEVICE_ADDRESS }
                if (device == null) {
                    Log.d("xxx", "onStart -> startScanning")
                    bluetooth.startScanning()
                } else {
                    bluetooth.connectToAddress(DEVICE_ADDRESS)
                }
            }
        } else {
            bluetooth.showEnableDialog(context as Activity?)
        }
    }

    fun stop() {
        bluetoothChannel.cancel()
        discoveryChannel.cancel()
        deviceChannel.cancel()

        if (bluetooth.isConnected) bluetooth.disconnect()
        bluetooth.onStop()
        arduino = null
    }

    fun send(message: String) {
        bluetooth.send(message)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        bluetooth.onActivityResult(requestCode, resultCode)
    }

    private fun emitMessage(channel: BroadcastChannel<BluetoothEvent>, message: String) {
        if (!channel.isClosedForSend) {
            GlobalScope.launch(Dispatchers.Main) { channel.send(BluetoothMessage(message)) }
        } else {
            Log.d("xxx", "emitMessage - channel.isClosedForSend: ${channel.isClosedForSend}")
        }
    }

    private fun emitError(channel: BroadcastChannel<BluetoothEvent>, message: String) {
        if (!channel.isClosedForSend) {
            GlobalScope.launch(Dispatchers.Main) { channel.send(BluetoothError(message)) }
        } else {
            Log.d("xxx", "emitError - channel.isClosedForSend: ${channel.isClosedForSend}")
        }
    }

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
                makeText(context, "Allow bluetooth to use the app", LENGTH_LONG).show()
            }
        })
    }

    private fun registerDiscoveryChannel() {
        bluetooth.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
                emitMessage(discoveryChannel, "pairingCallback -> onDiscoveryStarted")
                arduino = null
            }

            override fun onDiscoveryFinished() {
                emitMessage(discoveryChannel, "pairingCallback -> onDiscoveryFinished")
                val device = bluetooth.pairedDevices.firstOrNull { it.address == DEVICE_ADDRESS }
                if (arduino == null) {
                    makeText(context, "You're too far from device", LENGTH_LONG).show()
                } else if (device == null) {
                    if (!pairDialog.isShowing) {
                        emitMessage(discoveryChannel, "pairingCallback -> onDiscoveryFinished (dialog)")
                        pairDialog.show()
                    }
                }
            }

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

    private fun registerDeviceChannel() {
        bluetooth.setDeviceCallback(object : DeviceCallback {
            override fun onDeviceConnected(device: BluetoothDevice?) {
                if (!deviceChannel.isClosedForSend) {
                    GlobalScope.launch(Dispatchers.Main) {
                        deviceChannel.send(BluetoothDeviceConnected("connectionCallback -> onDeviceConnected"))
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