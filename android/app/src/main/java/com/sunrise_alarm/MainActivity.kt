package com.sunrise_alarm

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.chibatching.kotpref.Kotpref
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.google.firebase.messaging.FirebaseMessaging
import com.sunrise_alarm.BluetoothManager.BluetoothDeviceConnected
import com.sunrise_alarm.BluetoothManager.BluetoothMessage
import com.sunrise_alarm.Const.LOG_TAG
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.joda.time.DateTime


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
        bluetoothManager = BluetoothManager(this, true)

        refreshUi()
        setupListeners()
        subscribeFCMTopic()
    }

    private fun subscribeFCMTopic() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this) { instanceIdResult: InstanceIdResult ->
            val newToken = instanceIdResult.token
            Log.i(LOG_TAG, "newToken: $newToken")
        }
        Log.i(LOG_TAG, "subscribeFCMTopic")
        FirebaseMessaging.getInstance().subscribeToTopic("light")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(LOG_TAG, "subscribed")
                } else {
                    Log.i(LOG_TAG, "subscribed failded")
                }
            }
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "onStart")
        disableUi()

        ServiceManager.askPermissions(this) {
            Log.d(LOG_TAG, "success()")
            CoroutineScope(Main).launch {
                bluetoothManager.subscribe().consumeEach {
                    when (it) {
                        is BluetoothMessage -> processBluetoothMessage(it)
                        is BluetoothDeviceConnected -> enableUi()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "onStop")

        disableUi()
        bluetoothManager.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bluetoothManager.onActivityResult(requestCode, resultCode)
    }

    private fun processBluetoothMessage(it: BluetoothMessage) {
        if (it.message.contains("isAlarm:", true) && motion_base.progress == 1f) {
            switchButton.isEnabled = true
            val isAlarm = it.message.contains("isAlarm: 1", true)
            val color = if (isAlarm) R.color.colorYellow else android.R.color.black
            switchButton.imageTintList =
                ColorStateList.valueOf(ResourcesCompat.getColor(resources, color, null))
            viewLight.visibility = if (isAlarm) VISIBLE else INVISIBLE
        }
    }

    private fun enableUi() {
        bluetoothManager.updateAlarmRanges()
        motion_base.transitionToEnd()
        timeFrom1.isEnabled = true
        timeFrom2.isEnabled = true
        timeTo1.isEnabled = true
        timeTo2.isEnabled = true
        shimmer_view_container.hideShimmer()
    }

    private fun disableUi() {
        motion_base.progress = 0f
        viewLight.visibility = INVISIBLE
        switchButton.isEnabled = false
        switchButton.backgroundTintList =
            ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.colorGrey, null))
        timeFrom1.isEnabled = false
        timeFrom2.isEnabled = false
        timeTo1.isEnabled = false
        timeTo2.isEnabled = false
        shimmer_view_container.showShimmer(true)
    }

    private fun refreshUi() {
        timeFrom1.text = SavedTime.timeFrom1()
        timeTo1.text = SavedTime.timeTo1()
        timeFrom2.text = SavedTime.timeFrom2()
        timeTo2.text = SavedTime.timeTo2()
    }

    private fun setupListeners() {
        fun createTimePicker(callback: TimePickerDialog.OnTimeSetListener) {
            TimePickerDialog.newInstance(callback, true).apply {
                version = TimePickerDialog.Version.VERSION_1
                isThemeDark = true
            }.show(supportFragmentManager, "TimePickerDialog")
        }

        switchButton.setOnClickListener { bluetoothManager.switchLight() }

        timeFrom1.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    SavedTime.hourFrom1 = hour
                    SavedTime.minFrom1 = minute
                    refreshUi()
                    bluetoothManager.updateAlarmRanges()
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
                        refreshUi()
                        bluetoothManager.updateAlarmRanges()
                    } else {
                        makeText(
                            this,
                            getString(R.string.time_should_be_after_value) + SavedTime.timeFrom1(),
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
                            getString(R.string.time_should_be_in_range_value) + SavedTime.timeFrom1() + " – " + SavedTime.timeTo1(),
                            LENGTH_LONG
                        ).show()
                    } else {
                        SavedTime.hourFrom2 = hour
                        SavedTime.minFrom2 = minute
                        refreshUi()
                        bluetoothManager.updateAlarmRanges()
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
                                getString(R.string.time_should_be_not_be_in_range_value) + SavedTime.timeFrom1() + " – " + SavedTime.timeTo1(),
                                LENGTH_LONG
                            ).show()
                        }
                        timeSelected.isBefore(timeFrom2) -> {
                            makeText(
                                this,
                                getString(R.string.time_should_be_after_value) + timeFrom2,
                                LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            SavedTime.hourTo2 = hour
                            SavedTime.minTo2 = minute
                            refreshUi()
                            bluetoothManager.updateAlarmRanges()
                        }
                    }
                })
        }
    }
}
