package com.sunrise_alarm

import android.Manifest.permission.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.joda.time.DateTime


const val DEVICE_ADDRESS = "00:18:E4:40:00:06"

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager

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
        setupBluetooth()

        switchButton.setOnClickListener { bluetoothManager.send("1|") }
    }

    private fun setupBluetooth() {
        bluetoothManager = BluetoothManager(this)
        CoroutineScope(Dispatchers.Main).launch {
            bluetoothManager.discoveryChannel.openSubscription().consumeEach { Log.d("xxx", it.message) }
            bluetoothManager.bluetoothChannel.openSubscription().consumeEach { Log.d("xxx", it.message) }
            bluetoothManager.deviceChannel.openSubscription().consumeEach {
                Log.d("xxx", it.message)
                when (it) {
                    is BluetoothManager.BluetoothMessage -> onMessageReceived(it)
                    is BluetoothManager.BluetoothDeviceConnected -> onDevideConnected()
                }
            }
        }
    }

    private fun onMessageReceived(it: BluetoothManager.BluetoothMessage) {
        if (it.message.contains("isAlarm:", true) && motion_base.progress == 1f) {
            switchButton.isEnabled = true
            val isAlarm = it.message.contains("isAlarm: 1", true)
            val color = if (isAlarm) R.color.colorYellow else android.R.color.black
            switchButton.imageTintList = ColorStateList.valueOf(getColor(color))
            viewLight.visibility = if (isAlarm) VISIBLE else INVISIBLE
        }
    }

    private fun onDevideConnected() {
        updateAlarmRanges()
        motion_base.transitionToEnd()
        timeFrom1.isEnabled = true
        timeFrom2.isEnabled = true
        timeTo1.isEnabled = true
        timeTo2.isEnabled = true
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
        bluetoothManager.send(msg.replace(":", ""))
    }

    override fun onStart() {
        super.onStart()
        Log.d("xxx", "onStart")

        askPermissions()

        switchButton.isEnabled = false
        switchButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorGrey))
        timeFrom1.isEnabled = false
        timeFrom2.isEnabled = false
        timeTo1.isEnabled = false
        timeTo2.isEnabled = false

        bluetoothManager.start()
    }

    override fun onStop() {
        super.onStop()
        Log.d("xxx", "onStop")

        motion_base.progress = 0f
        viewLight.visibility = INVISIBLE
        switchButton.imageTintList = ColorStateList.valueOf(getColor(android.R.color.black))

        bluetoothManager.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bluetoothManager.onActivityResult(requestCode, resultCode)
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
