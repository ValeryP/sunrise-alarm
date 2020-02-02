package com.sunrise_alarm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.ColorStateList
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chibatching.kotpref.Kotpref
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import com.sunrise_alarm.app.R
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_main.*
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback
import org.joda.time.DateTime


class MainActivity : AppCompatActivity() {

    private lateinit var bluetooth: Bluetooth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(app_toolbar)
        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayShowTitleEnabled(false)
            it.setIcon(R.drawable.ic_launcher_foreground)
        }

        Kotpref.init(applicationContext)

        invalidateTime()
        setupListeners()

        bluetooth = Bluetooth(this)
        bluetooth.setBluetoothCallback(statusCallback)
        bluetooth.setDiscoveryCallback(discoveryCallback)
        bluetooth.setDeviceCallback(connectionCallback)

        switchButton.setOnClickListener { bluetooth.send("1|") }
        switchButton.isEnabled = false
        switchButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorGrey))

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(
                CompositePermissionListener(
                    SnackbarOnDeniedPermissionListener.Builder
                        .with(switchButton, "Enable location to find closest bluetooth")
                        .withOpenSettingsButton("Settings")
                        .withDuration(5000)
                        .build(),
                    object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
                            if (!manager.isProviderEnabled(GPS_PROVIDER)) {
                                checkGPSEnable()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest?,
                            token: PermissionToken?
                        ) {
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {}
                    })
            )
            .check()
    }

    private fun checkGPSEnable() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Enable GPS to find closest bluetooth")
            .setCancelable(false)
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        val alert = dialogBuilder.create()
        alert.show()
    }

    // E.g: "0|202001250544 [0545-1001,1029-1010,]"
    private fun updateAlarmRanges() {
        val msg = "0|${DateTime.now().toString("yyyyMMddHHmm")} " +
                "[${SavedTime.timeFrom1()}-${SavedTime.timeTo1()}," +
                "${SavedTime.timeFrom2()}-${SavedTime.timeTo2()},]"
        bluetooth.send(msg.replace(":", ""))
    }

    override fun onResume() {
        super.onResume()
        Log.d("xxx", "onResume")
        bluetooth.onStart()
        if (bluetooth.isEnabled) {
            Log.d("xxx", "onStart -> bluetooth.isEnabled")
            val deviceAddress = "00:18:E4:40:00:06"
            if (!bluetooth.isConnected) {
                val device = bluetooth.pairedDevices.firstOrNull { it.address == deviceAddress }
                if (device == null) {
                    Log.d("xxx", "onStart -> startScanning")
//                    bluetooth.pair(,"1234")
                    TODO("connect when device unpaired and with turned off GPS")
                } else {
                    bluetooth.connectToAddress(deviceAddress)
                }
            }
        } else {
            bluetooth.showEnableDialog(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (bluetooth.isConnected) bluetooth.disconnect()
        bluetooth.onStop()
        Log.d("xxx", "onPause")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bluetooth.onActivityResult(requestCode, resultCode)
    }

    private val discoveryCallback = object : DiscoveryCallback {
        override fun onDiscoveryStarted() {
            Log.d("xxx", "pairingCallback -> onDiscoveryStarted")
        }

        override fun onDiscoveryFinished() {
            Log.d("xxx", "pairingCallback -> onDiscoveryFinished")
        }

        override fun onDeviceFound(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDeviceFound: ${device.name} | ${device.address}")
        }

        override fun onDevicePaired(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDevicePaired: ${device.name}")
        }

        override fun onDeviceUnpaired(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDeviceUnpaired: ${device.name}")
        }

        override fun onError(errorCode: Int) {
            Log.e("xxx", "pairingCallback -> onError: $errorCode")
        }
    }

    private val statusCallback: BluetoothCallback = object : BluetoothCallback {
        override fun onBluetoothTurningOn() {
            Log.d("xxx", "statusCallback -> onBluetoothTurningOn")
        }

        override fun onBluetoothTurningOff() {
            Log.d("xxx", "statusCallback -> onBluetoothTurningOff")
        }

        override fun onBluetoothOff() {
            Log.d("xxx", "statusCallback -> onBluetoothOff")
        }

        override fun onBluetoothOn() {
            Log.d("xxx", "statusCallback -> onBluetoothOn")
        }

        override fun onUserDeniedActivation() {
            Toast.makeText(
                this@MainActivity,
                "Allow bluetooth to use the app",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val connectionCallback = object : DeviceCallback {
        override fun onDeviceConnected(device: BluetoothDevice?) {
            Log.d("xxx", "connectionCallback -> onDeviceConnected")
            updateAlarmRanges()
        }

        override fun onDeviceDisconnected(device: BluetoothDevice?, message: String?) {
            Log.d("xxx", "connectionCallback -> onDeviceDisconnected")
        }

        override fun onMessage(message: ByteArray?) {
            message?.let {
                val text = String(it)
                Log.d("xxx", "connectionCallback -> onMessage: $text")
                if (text.contains("isAlarm", true)) {
                    runOnUiThread {
                        switchButton.isEnabled = true
                        if (text.contains("isAlarm: 1", true)) {
                            switchButton.imageTintList =
                                ColorStateList.valueOf(getColor(R.color.colorYellow))
                            viewLight.visibility = View.VISIBLE
                        } else {
                            switchButton.imageTintList =
                                ColorStateList.valueOf(getColor(android.R.color.black))
                            viewLight.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }

        override fun onError(errorCode: Int) {
            Log.d("xxx", "connectionCallback -> onError: $errorCode")
        }

        override fun onConnectError(device: BluetoothDevice?, message: String?) {
            Log.d("xxx", "connectionCallback -> onConnectError: $message")
        }
    }


    private fun setupListeners() {
        timeFrom1.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    SavedTime.hourFrom1 = hour
                    SavedTime.minFrom1 = minute
                    invalidateTime()
                    updateAlarmRanges()
                })
        }
        timeTo1.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    val timeFrom1 = DateTime.now()
                        .withHourOfDay(SavedTime.hourFrom1)
                        .withMinuteOfHour(SavedTime.minFrom1)
                    val timeSelected = DateTime.now()
                        .withHourOfDay(hour).withMinuteOfHour(minute)
                    if (timeSelected.isAfter(timeFrom1)) {
                        SavedTime.hourTo1 = hour
                        SavedTime.minTo1 = minute
                        invalidateTime()
                        updateAlarmRanges()
                    } else {
                        Toast.makeText(
                            this,
                            "Time should be after ${SavedTime.timeFrom1()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
        timeFrom2.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    val timeFrom1 = DateTime.now()
                        .withHourOfDay(SavedTime.hourFrom1)
                        .withMinuteOfHour(SavedTime.minFrom1)
                    val timeTo1 = DateTime.now()
                        .withHourOfDay(SavedTime.hourTo1)
                        .withMinuteOfHour(SavedTime.minTo1)
                    val timeSelected = DateTime.now()
                        .withHourOfDay(hour).withMinuteOfHour(minute)
                    if (timeSelected.isAfter(timeFrom1) && timeSelected.isBefore(timeTo1)) {
                        Toast.makeText(
                            this,
                            "Time should NOT be in a range ${SavedTime.timeFrom1()} – ${SavedTime.timeTo1()}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        SavedTime.hourTo1 = hour
                        SavedTime.minTo1 = minute
                        invalidateTime()
                        updateAlarmRanges()
                    }
                })
        }
        timeTo2.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    val timeFrom1 = DateTime.now()
                        .withHourOfDay(SavedTime.hourFrom1)
                        .withMinuteOfHour(SavedTime.minFrom1)
                    val timeTo1 = DateTime.now()
                        .withHourOfDay(SavedTime.hourTo1)
                        .withMinuteOfHour(SavedTime.minTo1)
                    val timeFrom2 = DateTime.now()
                        .withHourOfDay(SavedTime.hourFrom2)
                        .withMinuteOfHour(SavedTime.minFrom2)
                    val timeSelected = DateTime.now()
                        .withHourOfDay(hour).withMinuteOfHour(minute)
                    when {
                        timeSelected.isAfter(timeFrom1) && timeSelected.isBefore(timeTo1) -> {
                            Toast.makeText(
                                this,
                                "Time should NOT be in a range ${SavedTime.timeFrom1()} – ${SavedTime.timeTo1()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        timeSelected.isBefore(timeFrom2) -> {
                            Toast.makeText(
                                this,
                                "Time should be after $timeFrom2",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            SavedTime.hourTo1 = hour
                            SavedTime.minTo1 = minute
                            invalidateTime()
                            updateAlarmRanges()
                        }
                    }
                })
        }
    }

    private fun invalidateTime() {
        timeFrom1.text = SavedTime.timeFrom1()
        timeTo1.text = SavedTime.timeTo1()
        timeFrom2.text = SavedTime.timeFrom2()
        timeTo2.text = SavedTime.timeTo2()
    }

    private fun createTimePicker(callback: TimePickerDialog.OnTimeSetListener) {
        TimePickerDialog.newInstance(callback, true).apply {
            version = TimePickerDialog.Version.VERSION_1
            isThemeDark = true
        }.show(supportFragmentManager, "TimePickerDialog")
    }
}
