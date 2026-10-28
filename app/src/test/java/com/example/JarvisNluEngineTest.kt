package com.example

import com.example.ai.JarvisNluEngine
import com.example.model.ActionStatus
import com.example.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisNluEngineTest {

    @Test
    fun testYouTubeSearch() {
        val result = JarvisNluEngine.parseCommand("Jarvis, YouTube kholo aur Arijit Singh ka gana chalao.")
        assertEquals(ActionType.YOUTUBE_SEARCH, result.actionType)
        assertTrue(result.target.contains("Arijit Singh", ignoreCase = true))
    }

    @Test
    fun testInstagramChat() {
        val result = JarvisNluEngine.parseCommand("Instagram kholo aur Rahul ki chat dhoondo.")
        assertEquals(ActionType.INSTAGRAM_CHAT, result.actionType)
        assertEquals("Rahul", result.target)
    }

    @Test
    fun testMessageRequiresConfirmation() {
        val result = JarvisNluEngine.parseCommand("Rahul ko message likho: main baad mein call karunga.")
        assertEquals(ActionType.SEND_WHATSAPP, result.actionType)
        assertEquals("Rahul", result.target)
        assertEquals("main baad mein call karunga.", result.details)
        assertTrue(result.requiresConfirmation)
        assertEquals(ActionStatus.PENDING_CONFIRMATION, result.status)
    }

    @Test
    fun testScreenScrollDown() {
        val result = JarvisNluEngine.parseCommand("Screen ko neeche scroll karo.")
        assertEquals(ActionType.SCROLL_DOWN, result.actionType)
    }

    @Test
    fun testOpenSettings() {
        val result = JarvisNluEngine.parseCommand("Settings kholo.")
        assertEquals(ActionType.OPEN_SETTINGS, result.actionType)
    }

    @Test
    fun testNavigateHome() {
        val result = JarvisNluEngine.parseCommand("Wapas jao aur home screen par aao.")
        assertEquals(ActionType.NAVIGATE_HOME, result.actionType)
    }

    @Test
    fun testSetTimer() {
        val result = JarvisNluEngine.parseCommand("5 minute ka timer lagao.")
        assertEquals(ActionType.SET_TIMER, result.actionType)
        assertEquals("300", result.target)
    }

    @Test
    fun testMakeCallRequiresConfirmation() {
        val result = JarvisNluEngine.parseCommand("Mummy ko call lagao.")
        assertEquals(ActionType.MAKE_CALL, result.actionType)
        assertTrue(result.requiresConfirmation)
        assertEquals(ActionStatus.PENDING_CONFIRMATION, result.status)
    }

    @Test
    fun testDeviceStatusRecognition() {
        val resultHindi = JarvisNluEngine.parseCommand("Phone ki battery kitni hai?")
        assertEquals(ActionType.DEVICE_STATUS, resultHindi.actionType)

        val resultEnglish = JarvisNluEngine.parseCommand("Check device status and wifi network")
        assertEquals(ActionType.DEVICE_STATUS, resultEnglish.actionType)
    }
}
