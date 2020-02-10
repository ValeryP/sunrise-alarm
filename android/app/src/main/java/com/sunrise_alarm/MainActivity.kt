package com.sunrise_alarm

import android.Manifest.permission.*
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.ColorStateList
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chibatching.kotpref.Kotpref
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sunrise_alarm.app.R
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_main.*
import me.aflak.bluetooth.Bluetooth
import me.aflak.bluetooth.interfaces.BluetoothCallback
import me.aflak.bluetooth.interfaces.DeviceCallback
import me.aflak.bluetooth.interfaces.DiscoveryCallback
import org.joda.time.DateTime


const val DEVICE_ADDRESS = "00:18:E4:40:00:06"

class MainActivity : AppCompatActivity() {
    private lateinit var bluetooth: Bluetooth
    private var arduino: BluetoothDevice? = null

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
        bluetooth.setCallbackOnUI(this)
        bluetooth.setBluetoothCallback(statusCallback)
        bluetooth.setDiscoveryCallback(discoveryCallback)
        bluetooth.setDeviceCallback(connectionCallback)

        switchButton.setOnClickListener { bluetooth.send("1|") }
    }

    private fun askPermissions() {
        Dexter.withActivity(this)
            .withPermissions(BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_COARSE_LOCATION)
            .withListener(
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) =
                        if (report.areAllPermissionsGranted()) {
                            checkGPSEnable()
                        } else {
                            askPermissions()
                        }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: List<PermissionRequest>, token: PermissionToken
                    ) = token.continuePermissionRequest()
                })
            .check()
    }

    private fun checkGPSEnable() {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this).setMessage("Enable GPS to find closest bluetooth")
                .setCancelable(false)
                .setPositiveButton("Enable") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .create()
                .show()
        }
    }

    // E.g: "0|202001250544 [0545-1001,1029-1010,]"
    private fun updateAlarmRanges() {
        val msg = "0|${DateTime.now().toString("yyyyMMddHHmm")} " +
                "[${SavedTime.timeFrom1()}-${SavedTime.timeTo1()}," +
                "${SavedTime.timeFrom2()}-${SavedTime.timeTo2()},]"
        bluetooth.send(msg.replace(":", ""))
    }

    override fun onStart() {
        super.onStart()
        Log.d("xxx", "onStart")

        askPermissions()

        bluetooth.onStart()
        if (bluetooth.isEnabled) {
            Log.d("xxx", "onStart -> bluetooth.isEnabled")
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
            bluetooth.showEnableDialog(this)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("xxx", "onStop")

        if (bluetooth.isConnected) bluetooth.disconnect()
        bluetooth.onStop()

        switchButton.isEnabled = false
        switchButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorGrey))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bluetooth.onActivityResult(requestCode, resultCode)
    }

    private val discoveryCallback = object : DiscoveryCallback {
        override fun onDiscoveryStarted() {
            Log.d("xxx", "pairingCallback -> onDiscoveryStarted")
            arduino = null
        }

        override fun onDiscoveryFinished() {
            Log.d("xxx", "pairingCallback -> onDiscoveryFinished")
            val device = bluetooth.pairedDevices.firstOrNull { it.address == DEVICE_ADDRESS }
            if (arduino == null) {
                makeText(this@MainActivity, "You're too far from device", LENGTH_LONG).show()
            } else if (device == null) {
                pair()
            }
        }

        override fun onDeviceFound(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDeviceFound: ${device.name} | ${device.address}")
            if (device.address == DEVICE_ADDRESS) {
                arduino = device
                pair()
            }
        }

        override fun onDevicePaired(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDevicePaired: ${device.name}")
            bluetooth.connectToAddress(DEVICE_ADDRESS)
        }

        override fun onDeviceUnpaired(device: BluetoothDevice) {
            Log.d("xxx", "pairingCallback -> onDeviceUnpaired: ${device.name}")
        }

        override fun onError(errorCode: Int) {
            Log.e("xxx", "pairingCallback -> onError: $errorCode")
        }
    }

    private fun pair() {
        AlertDialog.Builder(this)
            .setTitle("Pair with device")
            .setMessage("After you click OK the app will insert the pin code by itself. Don't do anything and just wait 3 sec.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> bluetooth.pair(arduino, "1234") }
            .create()
            .show()
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
            makeText(this@MainActivity, "Allow bluetooth to use the app", LENGTH_LONG).show()
        }
    }

    private val connectionCallback = object : DeviceCallback {
        override fun onDeviceConnected(device: BluetoothDevice?) {
            Log.d("xxx", "connectionCallback -> onDeviceConnected")
            updateAlarmRanges()
            motion_base.transitionToEnd()
        }

        override fun onDeviceDisconnected(device: BluetoothDevice?, message: String?) {
            Log.d("xxx", "connectionCallback -> onDeviceDisconnected")
        }

        override fun onMessage(message: ByteArray?) {
            message?.let {
                val text = String(it)
                Log.d("xxx", "connectionCallback -> onMessage: $text")
                if (text.contains("isAlarm:", true)) {
                    switchButton.isEnabled = true
                    if (text.contains("isAlarm: 1", true)) {
                        switchButton.imageTintList =
                            ColorStateList.valueOf(getColor(R.color.colorYellow))
                        viewLight.visibility = VISIBLE
                    } else {
                        switchButton.imageTintList =
                            ColorStateList.valueOf(getColor(android.R.color.black))
                        viewLight.visibility = INVISIBLE
                    }
                }
            }
        }

        override fun onError(errorCode: Int) {
            Log.d("xxx", "connectionCallback -> onError: $errorCode")
        }

        override fun onConnectError(device: BluetoothDevice?, message: String?) {
            Log.d("xxx", "connectionCallback -> onConnectError: $message")
            bluetooth.connectToAddress(DEVICE_ADDRESS)
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
                        makeText(
                            this,
                            "Time should be after ${SavedTime.timeFrom1()}",
                            LENGTH_LONG
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
                        makeText(
                            this,
                            "Time should NOT be in a range ${SavedTime.timeFrom1()} – ${SavedTime.timeTo1()}",
                            LENGTH_LONG
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
                            makeText(
                                this,
                                "Time should NOT be in a range ${SavedTime.timeFrom1()} – ${SavedTime.timeTo1()}",
                                LENGTH_LONG
                            ).show()
                        }
                        timeSelected.isBefore(timeFrom2) -> {
                            makeText(
                                this,
                                "Time should be after $timeFrom2",
                                LENGTH_LONG
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
