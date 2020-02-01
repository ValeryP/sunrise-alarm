package com.sunrise_alarm

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.chibatching.kotpref.Kotpref
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.sunrise_alarm", appContext.packageName)
    }

    @Test
    fun useAppContext1() {
        val appContext = InstrumentationRegistry.getTargetContext()
        Kotpref.init(appContext)
        assertEquals("5:55 - 9:10, 18:0 - 21:30", SavedTime.toString())
    }
}
