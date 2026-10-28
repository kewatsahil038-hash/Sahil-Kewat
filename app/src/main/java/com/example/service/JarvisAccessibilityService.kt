package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d(TAG, "JarvisAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track active window or content events if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "JarvisAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        Log.d(TAG, "JarvisAccessibilityService destroyed")
    }

    /**
     * Navigates back.
     */
    fun performGoBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Navigates to the home screen.
     */
    fun performGoHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Opens the recents / multitasking screen.
     */
    fun performRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Opens notifications drawer.
     */
    fun performNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Scrolls screen downwards (swipe up gesture or ACTION_SCROLL_FORWARD).
     */
    fun scrollDown(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                return true
            }
        }
        // Fallback to gesture swipe
        return dispatchSwipe(fromYRatio = 0.7f, toYRatio = 0.3f)
    }

    /**
     * Scrolls screen upwards (swipe down gesture or ACTION_SCROLL_BACKWARD).
     */
    fun scrollUp(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                return true
            }
        }
        // Fallback to gesture swipe
        return dispatchSwipe(fromYRatio = 0.3f, toYRatio = 0.7f)
    }

    /**
     * Reads visible text elements from current active window.
     */
    fun readScreenText(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val texts = mutableListOf<String>()
        extractText(root, texts)
        return texts
    }

    private fun extractText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrEmpty() && !list.contains(text)) {
            list.add(text)
        } else if (!desc.isNullOrEmpty() && !list.contains(desc)) {
            list.add(desc)
        }
        for (i in 0 until node.childCount) {
            extractText(node.getChild(i), list)
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun dispatchSwipe(fromYRatio: Float, toYRatio: Float): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels.toFloat()
            val height = displayMetrics.heightPixels.toFloat()
            val startX = width / 2f
            val startY = height * fromYRatio
            val endY = height * toYRatio

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 300)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }

    companion object {
        private const val TAG = "JarvisAccessibility"
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        var instance: JarvisAccessibilityService? = null
            private set
    }
}
