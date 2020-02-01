package com.sunrise_alarm

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chibatching.kotpref.Kotpref
import com.sunrise_alarm.app.R
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Kotpref.init(applicationContext)

        invalidateTime()
        setupListeners()
    }

    private fun setupListeners() {
        timeFrom1.setOnClickListener {
            createTimePicker(
                TimePickerDialog.OnTimeSetListener { _: TimePickerDialog?, hour: Int, minute: Int, _: Int ->
                    SavedTime.hourFrom1 = hour
                    SavedTime.minFrom1 = minute
                    invalidateTime()
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
