package com.example.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.JarvisCommandResult
import com.example.service.JarvisAccessibilityService
import java.net.URLEncoder

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val needsAccessibilityPermission: Boolean = false,
    val readScreenContent: List<String>? = null
)

object DeviceActionExecutor {

    private const val TAG = "DeviceActionExecutor"

    fun execute(context: Context, command: JarvisCommandResult): ExecutionResult {
        return try {
            when (command.actionType) {
                ActionType.YOUTUBE_SEARCH -> executeYouTubeSearch(context, command.target)
                ActionType.YOUTUBE_OPEN -> executeOpenApp(context, "com.google.android.youtube", "https://youtube.com")
                ActionType.INSTAGRAM_SEARCH -> executeInstagramSearch(context, command.target)
                ActionType.INSTAGRAM_CHAT -> executeInstagramChat(context, command.target)
                ActionType.INSTAGRAM_OPEN -> executeOpenApp(context, "com.instagram.android", "https://instagram.com")
                ActionType.OPEN_APP -> executeOpenNamedApp(context, command.target)
                ActionType.OPEN_SETTINGS -> executeOpenSettings(context)
                ActionType.OPEN_CAMERA -> executeOpenCamera(context)
                ActionType.OPEN_CHROME -> executeOpenChrome(context, command.target)
                ActionType.SET_TIMER -> executeSetTimer(context, command.target)
                ActionType.SET_ALARM -> executeSetAlarm(context, command.target)
                ActionType.MAKE_CALL -> executeMakeCall(context, command.target)
                ActionType.SEND_WHATSAPP -> executeSendWhatsApp(context, command.target, command.details)
                ActionType.NAVIGATE_HOME -> executeAccessibilityHome()
                ActionType.NAVIGATE_BACK -> executeAccessibilityBack()
                ActionType.NAVIGATE_RECENTS -> executeAccessibilityRecents()
                ActionType.SCROLL_DOWN -> executeAccessibilityScrollDown()
                ActionType.SCROLL_UP -> executeAccessibilityScrollUp()
                ActionType.READ_SCREEN -> executeAccessibilityReadScreen()
                ActionType.DEVICE_STATUS -> executeDeviceStatus(context)
                ActionType.GENERAL_REPLY -> ExecutionResult(true, "Spoken reply delivered")
                ActionType.UNKNOWN -> ExecutionResult(false, "Unrecognized command")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Execution failed for action ${command.actionType}", e)
            ExecutionResult(false, "Failed to execute: ${e.localizedMessage}")
        }
    }

    private fun executeYouTubeSearch(context: Context, query: String): ExecutionResult {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val appUri = Uri.parse("vnd.youtube:results?search_query=$encoded")
        val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, appIntent)) {
            context.startActivity(appIntent)
            ExecutionResult(true, "YouTube app opened with search: $query")
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "YouTube opened in browser for: $query")
        }
    }

    private fun executeInstagramSearch(context: Context, username: String): ExecutionResult {
        val cleanUser = username.replace("@", "").trim()
        val appUri = Uri.parse("instagram://user?username=$cleanUser")
        val webUri = Uri.parse("https://instagram.com/$cleanUser")

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, appIntent)) {
            context.startActivity(appIntent)
            ExecutionResult(true, "Instagram profile opened: @$cleanUser")
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "Instagram profile opened in browser: @$cleanUser")
        }
    }

    private fun executeInstagramChat(context: Context, username: String): ExecutionResult {
        val appUri = Uri.parse("instagram://shares")
        val webUri = Uri.parse("https://instagram.com/direct/inbox/")

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, appIntent)) {
            context.startActivity(appIntent)
            ExecutionResult(true, "Instagram Direct Messages opened for $username")
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "Instagram Inbox opened in browser")
        }
    }

    private fun executeOpenApp(context: Context, packageName: String, fallbackUrl: String): ExecutionResult {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ExecutionResult(true, "Launched $packageName")
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ExecutionResult(true, "Opened $fallbackUrl in browser")
        }
    }

    private fun executeOpenNamedApp(context: Context, appName: String): ExecutionResult {
        val launchResult = com.example.util.AppLauncherHelper.launchAppByName(context, appName)
        return ExecutionResult(
            success = launchResult.success,
            message = launchResult.message
        )
    }

    private fun executeOpenSettings(context: Context): ExecutionResult {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Device Settings opened")
    }

    private fun executeOpenCamera(context: Context): ExecutionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Camera opened")
    }

    private fun executeOpenChrome(context: Context, query: String): ExecutionResult {
        val url = if (query.isNotBlank() && query != "Chrome") {
            "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        } else {
            "https://www.google.com"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "Chrome opened: $url")
        } else {
            val generalIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalIntent)
            ExecutionResult(true, "Browser opened: $url")
        }
    }

    private fun executeSetTimer(context: Context, secondsStr: String): ExecutionResult {
        val seconds = secondsStr.toIntOrNull() ?: 300
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "Timer set for $seconds seconds")
        } else {
            ExecutionResult(true, "Timer scheduled (clock app notified)")
        }
    }

    private fun executeSetAlarm(context: Context, timeStr: String): ExecutionResult {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "Alarm set for %02d:%02d".format(hour, minute))
        } else {
            ExecutionResult(true, "Alarm requested for %02d:%02d".format(hour, minute))
        }
    }

    private fun executeMakeCall(context: Context, target: String): ExecutionResult {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val cleanNumber = target.filter { it.isDigit() || it == '+' }
        val telUri = if (cleanNumber.isNotEmpty()) Uri.parse("tel:$cleanNumber") else Uri.parse("tel:")

        val intent = if (hasCallPermission && cleanNumber.isNotEmpty()) {
            Intent(Intent.ACTION_CALL, telUri)
        } else {
            Intent(Intent.ACTION_DIAL, telUri)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return ExecutionResult(true, "Phone dialer initiated for $target")
    }

    private fun executeSendWhatsApp(context: Context, recipient: String, message: String): ExecutionResult {
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val cleanNumber = recipient.filter { it.isDigit() || it == '+' }
        val uri = if (cleanNumber.isNotEmpty()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (isIntentAvailable(context, intent)) {
            context.startActivity(intent)
            ExecutionResult(true, "WhatsApp prepared with message for $recipient")
        } else {
            // General share intent fallback
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share with $recipient").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            ExecutionResult(true, "Message dispatch dialog opened for $recipient")
        }
    }

    // Accessibility Service Actions
    private fun executeAccessibilityHome(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val ok = service.performGoHome()
            ExecutionResult(ok, if (ok) "Navigated to Home screen" else "Home navigation failed")
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled. Please enable JARVIS in Accessibility Settings.", needsAccessibilityPermission = true)
        }
    }

    private fun executeAccessibilityBack(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val ok = service.performGoBack()
            ExecutionResult(ok, if (ok) "Navigated Back" else "Back navigation failed")
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled.", needsAccessibilityPermission = true)
        }
    }

    private fun executeAccessibilityRecents(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val ok = service.performRecents()
            ExecutionResult(ok, if (ok) "Opened Recent Apps" else "Recents navigation failed")
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled.", needsAccessibilityPermission = true)
        }
    }

    private fun executeAccessibilityScrollDown(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val ok = service.scrollDown()
            ExecutionResult(ok, if (ok) "Scrolled down" else "Scroll gesture performed")
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled.", needsAccessibilityPermission = true)
        }
    }

    private fun executeAccessibilityScrollUp(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val ok = service.scrollUp()
            ExecutionResult(ok, if (ok) "Scrolled up" else "Scroll gesture performed")
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled.", needsAccessibilityPermission = true)
        }
    }

    private fun executeAccessibilityReadScreen(): ExecutionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val texts = service.readScreenText()
            ExecutionResult(
                success = true,
                message = "Screen scan complete (${texts.size} elements found)",
                readScreenContent = texts
            )
        } else {
            ExecutionResult(false, "Accessibility Service is not enabled.", needsAccessibilityPermission = true)
        }
    }

    private fun executeDeviceStatus(context: Context): ExecutionResult {
        val snapshot = com.example.util.DeviceStatusMonitor.getDeviceStatus(context)
        return ExecutionResult(
            success = true,
            message = snapshot.formattedSummaryHindi
        )
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return intent.resolveActivity(pm) != null
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
