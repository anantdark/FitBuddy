package com.anant.fitbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke: confirms the app package is installed on device.
 * Debug builds use applicationIdSuffix ".debug".
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(
            "unexpected package ${appContext.packageName}",
            appContext.packageName == "com.anant.fitbuddy" ||
                appContext.packageName == "com.anant.fitbuddy.debug"
        )
    }
}
