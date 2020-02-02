package com.sunrise_alarm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chibatching.kotpref.Kotpref
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
        bluetooth.setDiscoveryCallback(pairingCallback)
        bluetooth.setDeviceCallback(connectionCallback)

        switchButton.setOnClickListener { bluetooth.send("1|") }
        switchButton.isEnabled = false
        switchButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorGrey))
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
        bluetooth.onStart()
        if (bluetooth.isEnabled) {
            Log.w("xxx", "onStart -> bluetooth.isEnabled")
            if (!bluetooth.isConnected) {
                bluetooth.connectToAddress("00:18:E4:40:00:06")
            }
        } else {
            bluetooth.showEnableDialog(this)
        }
    }

    override fun onPause() {
        super.onPause()
        bluetooth.disconnect()
        bluetooth.onStop()
        Log.w("xxx", "onPause")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bluetooth.onActivityResult(requestCode, resultCode)
    }

    private val pairingCallback = object : DiscoveryCallback {
        override fun onDiscoveryStarted() {}
        override fun onDiscoveryFinished() {}
        override fun onDeviceFound(device: BluetoothDevice) {}
        override fun onDevicePaired(device: BluetoothDevice) {}
        override fun onDeviceUnpaired(device: BluetoothDevice) {}
        override fun onError(errorCode: Int) {
            Log.e("xxx", "pairingCallback -> onError: $errorCode")
        }
    }

    private val statusCallback: BluetoothCallback = object : BluetoothCallback {
        override fun onBluetoothTurningOn() {}
        override fun onBluetoothTurningOff() {}
        override fun onBluetoothOff() {}
        override fun onBluetoothOn() {
            Log.w("xxx", "statusCallback -> onBluetoothOn")
        }

        override fun onUserDeniedActivation() {
            Toast.makeText(
                this@MainActivity,
                "Time should be after ${SavedTime.timeFrom1()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val connectionCallback = object : DeviceCallback {
        override fun onDeviceConnected(device: BluetoothDevice?) {
            Log.w("xxx", "connectionCallback -> onDeviceConnected")
            updateAlarmRanges()
        }

        override fun onDeviceDisconnected(device: BluetoothDevice?, message: String?) {
            Log.w("xxx", "connectionCallback -> onDeviceDisconnected")
        }

        override fun onMessage(message: ByteArray?) {
            message?.let {
                val text = String(it)
                Log.w("xxx", "connectionCallback -> onMessage: $text")
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

        override fun onError(errorCode: Int) {}
        override fun onConnectError(device: BluetoothDevice?, message: String?) {}
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
        }.show(supportFragmentManager, "TimePickerDialog")
    }
}
