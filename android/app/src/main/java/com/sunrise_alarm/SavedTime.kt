package com.sunrise_alarm

import com.chibatching.kotpref.KotprefModel

/*
 * @author Valeriy Palamarchuk
 * @email valeriij.palamarchuk@gmail.com
 * Created on 2020-02-01
 */
object SavedTime : KotprefModel() {
    var hourFrom1 by intPref(default = 5)
    var minFrom1 by intPref(default = 55)
    var hourTo1 by intPref(default = 9)
    var minTo1 by intPref(default = 10)
    var hourFrom2 by intPref(default = 18)
    var minFrom2 by intPref(default = 0)
    var hourTo2 by intPref(default = 21)
    var minTo2 by intPref(default = 30)

    fun timeFrom1(): String {
        return "${fix(hourFrom1)}:${fix(minFrom1)}"
    }

    fun timeTo1(): String {
        return "${fix(hourTo1)}:${fix(minTo1)}"
    }

    fun timeFrom2(): String {
        return "${fix(hourFrom2)}:${fix(minFrom2)}"
    }

    fun timeTo2(): String {
        return "${fix(hourTo2)}:${fix(minTo2)}"
    }

    private fun fix(value: Int): String {
        return if (value in 0..9) "0$value" else "$value"
    }

    override fun toString(): String {
        return "$hourFrom1:$minFrom1 - $hourTo1:$minTo1, $hourFrom2:$minFrom2 - $hourTo2:$minTo2"
    }
}