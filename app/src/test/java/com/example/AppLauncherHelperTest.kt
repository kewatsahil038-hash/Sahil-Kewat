package com.example

import com.example.util.AppLauncherHelper
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLauncherHelperTest {

    @Test
    fun testAppLauncherResolvesOrHandlesGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = AppLauncherHelper.launchAppByName(context, "Calculator")
        assertNotNull(result)
        assertNotNull(result.message)
    }

    @Test
    fun testAppLauncherWithHindiPrefix() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = AppLauncherHelper.launchAppByName(context, "kholo WhatsApp")
        assertNotNull(result)
    }
}
