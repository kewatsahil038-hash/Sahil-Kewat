package com.example.ai

import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.JarvisCommandResult
import java.util.Locale
import java.util.regex.Pattern

object JarvisNluEngine {

    /**
     * Parses Hindi, Hinglish, and English commands into structured actions.
     */
    fun parseCommand(rawText: String): JarvisCommandResult {
        val text = rawText.trim()
        val lower = text.lowercase(Locale.ROOT)
            .replace("jarvis", "")
            .replace("जार्विस", "")
            .replace(",", " ")
            .trim()

        // 1. YouTube Control
        // e.g. "YouTube kholo aur Arijit Singh ka gana chalao", "play lofi on youtube", "youtube open karo"
        if (lower.contains("youtube") || lower.contains("यू ट्यूब") || lower.contains("यूट्यूब")) {
            val query = extractYouTubeQuery(lower)
            return if (query.isNotEmpty()) {
                JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.YOUTUBE_SEARCH,
                    target = query,
                    details = "YouTube Search for: $query",
                    spokenResponseHindi = "YouTube par $query chala raha hoon.",
                    spokenResponseEnglish = "Playing $query on YouTube."
                )
            } else {
                JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.YOUTUBE_OPEN,
                    target = "YouTube",
                    details = "Opening YouTube",
                    spokenResponseHindi = "YouTube khol raha hoon.",
                    spokenResponseEnglish = "Opening YouTube."
                )
            }
        }

        // 2. Instagram Control
        if (lower.contains("instagram") || lower.contains("इंस्टाग्राम") || lower.contains("insta")) {
            val user = extractInstagramUser(text)
            return if (lower.contains("chat") || lower.contains("message") || lower.contains("dm")) {
                val targetUser = if (user.isNotEmpty()) user else "Direct Messages"
                JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.INSTAGRAM_CHAT,
                    target = targetUser,
                    details = "Open chat with $targetUser",
                    spokenResponseHindi = "Instagram par $targetUser ki chat dhoondh raha hoon.",
                    spokenResponseEnglish = "Opening Instagram chat for $targetUser."
                )
            } else if (user.isNotEmpty()) {
                JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.INSTAGRAM_SEARCH,
                    target = user,
                    details = "Search profile: $user",
                    spokenResponseHindi = "Instagram par $user ka profile khol raha hoon.",
                    spokenResponseEnglish = "Opening Instagram profile of $user."
                )
            } else {
                JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.INSTAGRAM_OPEN,
                    target = "Instagram",
                    details = "Opening Instagram",
                    spokenResponseHindi = "Instagram khol raha hoon.",
                    spokenResponseEnglish = "Opening Instagram."
                )
            }
        }

        // 3. Message / WhatsApp Sending (Requires Confirmation for Safety!)
        // e.g. "Rahul ko message likho: main baad mein call karunga", "whatsapp message bhejo rohit ko"
        if (lower.contains("message") || lower.contains("msg") || lower.contains("whatsapp") || lower.contains("संदेश")) {
            val (recipient, messageContent) = extractRecipientAndMessage(text)
            if (recipient.isNotEmpty() || messageContent.isNotEmpty()) {
                val targetRecipient = if (recipient.isNotEmpty()) recipient else "Selected Contact"
                return JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.SEND_WHATSAPP,
                    target = targetRecipient,
                    details = messageContent,
                    spokenResponseHindi = "Kya aap $targetRecipient ko ye message bhejna chahte hain: \"$messageContent\"?",
                    spokenResponseEnglish = "Do you want to send this message to $targetRecipient: \"$messageContent\"?",
                    requiresConfirmation = true,
                    confirmationPrompt = "Confirm sending message to $targetRecipient:\n\"$messageContent\"",
                    status = ActionStatus.PENDING_CONFIRMATION
                )
            }
        }

        // 4. Calling Tasks (Requires Confirmation for Safety!)
        // e.g. "Mummy ko call lagao", "Call 9876543210", "Papa ko phone karo"
        if (lower.contains("call") || lower.contains("फोन") || lower.contains("कॉल") || lower.contains("dial")) {
            val contact = extractCallTarget(text)
            val target = if (contact.isNotEmpty()) contact else "Contact"
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.MAKE_CALL,
                target = target,
                details = "Call to $target",
                spokenResponseHindi = "Kya aap $target ko call lagana chahte hain?",
                spokenResponseEnglish = "Would you like me to place a call to $target?",
                requiresConfirmation = true,
                confirmationPrompt = "Confirm initiating call to: $target",
                status = ActionStatus.PENDING_CONFIRMATION
            )
        }

        // 5. Screen Control Actions (Accessibility Service)
        // Scroll Down: "Screen ko neeche scroll karo", "scroll down"
        if (lower.contains("scroll down") || lower.contains("neeche scroll") || lower.contains("niche scroll") || lower.contains("नीचे स्क्रॉल")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.SCROLL_DOWN,
                details = "Scroll Down",
                spokenResponseHindi = "Screen ko neeche scroll kar raha hoon.",
                spokenResponseEnglish = "Scrolling screen down."
            )
        }

        // Scroll Up: "Screen ko upar scroll karo", "scroll up"
        if (lower.contains("scroll up") || lower.contains("upar scroll") || lower.contains("ऊपर स्क्रॉल")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.SCROLL_UP,
                details = "Scroll Up",
                spokenResponseHindi = "Screen ko upar scroll kar raha hoon.",
                spokenResponseEnglish = "Scrolling screen up."
            )
        }

        // Home Navigation (checked before back if both mentioned): "Home screen par aao", "home jao", "go home"
        if (lower.contains("home") || lower.contains("होम") || lower.contains("mukhya prashth")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.NAVIGATE_HOME,
                details = "Home Screen",
                spokenResponseHindi = "Home screen par ja raha hoon.",
                spokenResponseEnglish = "Navigating to home screen."
            )
        }

        // Back / Return: "Wapas jao", "go back", "back karo"
        if (lower.contains("wapas") || lower.contains("वापस") || lower.contains("back") || lower.contains("peechhe")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.NAVIGATE_BACK,
                details = "Back Navigation",
                spokenResponseHindi = "Wapas ja raha hoon.",
                spokenResponseEnglish = "Navigating back."
            )
        }

        // Recents / Multitasking: "recent apps", "multitask"
        if (lower.contains("recent") || lower.contains("recents") || lower.contains("multitasking")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.NAVIGATE_RECENTS,
                details = "Recent Apps",
                spokenResponseHindi = "Recent apps screen khol raha hoon.",
                spokenResponseEnglish = "Opening recent apps."
            )
        }

        // Read Screen: "Screen padho", "Screen ko read karo", "read screen"
        if (lower.contains("read screen") || lower.contains("screen padho") || lower.contains("स्क्रीन पढ़ो") || lower.contains("screen par kya")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.READ_SCREEN,
                details = "Read Screen Content",
                spokenResponseHindi = "Screen ka content scan kar raha hoon.",
                spokenResponseEnglish = "Reading screen content."
            )
        }

        // Device Status: Battery, Network, Charging check
        // e.g. "Battery kitni hai", "phone status kya hai", "network check karo", "device status"
        if (lower.contains("battery") || lower.contains("बैटरी") || lower.contains("चार्ज") ||
            lower.contains("charging") || lower.contains("network") || lower.contains("नेटवर्क") ||
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("device status") ||
            lower.contains("phone status") || lower.contains("phone ki battery")
        ) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.DEVICE_STATUS,
                target = "Device Status",
                details = "Battery & Network Status",
                spokenResponseHindi = "Phone ki battery aur network status check kar raha hoon.",
                spokenResponseEnglish = "Checking battery and network status."
            )
        }

        // 6. Device Apps & Utilities
        // Settings: "Settings kholo", "open settings"
        if (lower.contains("settings") || lower.contains("setting") || lower.contains("सेटिंग")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.OPEN_SETTINGS,
                target = "Settings",
                details = "Open Settings",
                spokenResponseHindi = "Settings khol raha hoon.",
                spokenResponseEnglish = "Opening device Settings."
            )
        }

        // Camera: "Camera kholo", "open camera", "photo khicho"
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo") || lower.contains("tasveer")) {
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.OPEN_CAMERA,
                target = "Camera",
                details = "Open Camera",
                spokenResponseHindi = "Camera chalu kar raha hoon.",
                spokenResponseEnglish = "Launching camera."
            )
        }

        // Chrome / Web: "Chrome kholo", "browser kholo"
        if (lower.contains("chrome") || lower.contains("browser") || lower.contains("क्रोम") || lower.contains("google search")) {
            val query = extractWebQuery(lower)
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.OPEN_CHROME,
                target = query.ifEmpty { "Chrome" },
                details = if (query.isNotEmpty()) "Search: $query" else "Open Chrome",
                spokenResponseHindi = if (query.isNotEmpty()) "Chrome mein $query search kar raha hoon." else "Chrome khol raha hoon.",
                spokenResponseEnglish = if (query.isNotEmpty()) "Searching $query in Chrome." else "Opening Chrome browser."
            )
        }

        // Timer: "5 minute ka timer lagao", "set timer 10 seconds"
        if (lower.contains("timer") || lower.contains("टाइमर")) {
            val seconds = extractTimerSeconds(lower)
            val minDisplay = if (seconds >= 60) "${seconds / 60} minute" else "$seconds second"
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.SET_TIMER,
                target = seconds.toString(),
                details = "$minDisplay timer",
                spokenResponseHindi = "$minDisplay ka timer laga diya hai.",
                spokenResponseEnglish = "Setting a timer for $minDisplay."
            )
        }

        // Alarm: "7 baje ka alarm lagao", "alarm set karo"
        if (lower.contains("alarm") || lower.contains("अलार्म")) {
            val (hour, minute) = extractAlarmTime(lower)
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.SET_ALARM,
                target = "$hour:$minute",
                details = "Alarm at %02d:%02d".format(hour, minute),
                spokenResponseHindi = "$hour baje $minute minute ka alarm set kar diya hai.",
                spokenResponseEnglish = "Alarm set for %02d:%02d.".format(hour, minute)
            )
        }

        // Generic Open App: "WhatsApp kholo", "Spotify kholo", "Calculator kholo"
        if (lower.contains("kholo") || lower.contains("open") || lower.contains("खोलो") || lower.contains("start karo")) {
            val appName = extractAppName(lower)
            if (appName.isNotEmpty()) {
                return JarvisCommandResult(
                    rawCommand = text,
                    actionType = ActionType.OPEN_APP,
                    target = appName,
                    details = "Open $appName",
                    spokenResponseHindi = "$appName open kar raha hoon.",
                    spokenResponseEnglish = "Opening $appName."
                )
            }
        }

        // 7. General Conversational / Greetings
        if (lower.contains("hello") || lower.contains("namaste") || lower.contains("namaskar") || lower.contains("kaise ho") || lower.contains("who are you") || lower.contains("kaun ho")) {
            val replyHi = "Namaste! Main JARVIS hoon, aapka personal Android AI assistant. YouTube, Instagram, phone calls aur screen navigation ke liye taiyar hoon."
            val replyEn = "Hello! I am JARVIS, your personal Android AI assistant, ready for device control, navigation, and tasks."
            return JarvisCommandResult(
                rawCommand = text,
                actionType = ActionType.GENERAL_REPLY,
                details = "Greeting",
                spokenResponseHindi = replyHi,
                spokenResponseEnglish = replyEn
            )
        }

        // Fallback Unknown
        return JarvisCommandResult(
            rawCommand = text,
            actionType = ActionType.UNKNOWN,
            target = text,
            details = "Unrecognized command",
            spokenResponseHindi = "Maaf kijiye, main ye command samajh nahi paya. Kripya dohrayein ya quick commands dekhein.",
            spokenResponseEnglish = "I didn't quite catch that. Please repeat or check the quick commands."
        )
    }

    private fun extractYouTubeQuery(text: String): String {
        val cleaned = text
            .replace("youtube", "")
            .replace("यू ट्यूब", "")
            .replace("यूट्यूब", "")
            .replace("kholo", "")
            .replace("aur", "")
            .replace("open", "")
            .replace("par", "")
            .replace("chalao", "")
            .replace("play", "")
            .replace("search", "")
            .replace("gana", "")
            .replace("song", "")
            .replace("video", "")
            .replace("ka", "")
            .replace("ki", "")
            .replace("ke", "")
            .trim()
        return cleaned
    }

    private fun extractInstagramUser(text: String): String {
        // e.g. "Instagram kholo aur Rahul ki chat dhoondo"
        val m = Pattern.compile("(?:aur|par|for|find|dhoondo|search|profile|user|@|ki)\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE).matcher(text)
        while (m.find()) {
            val word = m.group(1)?.lowercase(Locale.ROOT) ?: ""
            if (word !in listOf("chat", "message", "dm", "kholo", "open", "dhoondo", "ki", "ka")) {
                return m.group(1)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } ?: ""
            }
        }
        return ""
    }

    private fun extractRecipientAndMessage(text: String): Pair<String, String> {
        // Pattern 1: "Rahul ko message likho: main baad mein call karunga"
        val colonSplit = text.split(":", limit = 2)
        if (colonSplit.size == 2) {
            val left = colonSplit[0].lowercase(Locale.ROOT)
            val msg = colonSplit[1].trim()
            val recipient = left
                .replace("message", "")
                .replace("likho", "")
                .replace("bhejo", "")
                .replace("send", "")
                .replace("ko", "")
                .replace("to", "")
                .replace("whatsapp", "")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            return Pair(recipient, msg)
        }

        // Pattern 2: "... ko message ..."
        val koIndex = text.indexOf(" ko ", ignoreCase = true)
        if (koIndex != -1) {
            val recipient = text.substring(0, koIndex).trim().split(" ").last()
            val right = text.substring(koIndex + 4)
                .replace("message", "", ignoreCase = true)
                .replace("likho", "", ignoreCase = true)
                .replace("bhejo", "", ignoreCase = true)
                .trim()
            return Pair(recipient, right)
        }

        return Pair("", text)
    }

    private fun extractCallTarget(text: String): String {
        val cleaned = text
            .replace("call", "", ignoreCase = true)
            .replace("कॉल", "", ignoreCase = true)
            .replace("phone", "", ignoreCase = true)
            .replace("फोन", "", ignoreCase = true)
            .replace("lagao", "", ignoreCase = true)
            .replace("karo", "", ignoreCase = true)
            .replace("ko", "", ignoreCase = true)
            .replace("to", "", ignoreCase = true)
            .trim()
        return cleaned
    }

    private fun extractWebQuery(text: String): String {
        return text
            .replace("chrome", "")
            .replace("browser", "")
            .replace("kholo", "")
            .replace("open", "")
            .replace("mein", "")
            .replace("search", "")
            .replace("karo", "")
            .trim()
    }

    private fun extractTimerSeconds(text: String): Int {
        val m = Pattern.compile("(\\d+)\\s*(minute|min|sec|second|ghante|hour)?").matcher(text)
        if (m.find()) {
            val num = m.group(1)?.toIntOrNull() ?: 60
            val unit = m.group(2) ?: "minute"
            return when {
                unit.startsWith("sec") -> num
                unit.startsWith("hour") || unit.startsWith("ghante") -> num * 3600
                else -> num * 60
            }
        }
        return 300 // default 5 mins
    }

    private fun extractAlarmTime(text: String): Pair<Int, Int> {
        val m = Pattern.compile("(\\d+)\\s*(?::(\\d+))?\\s*(baje|am|pm|o'clock)?").matcher(text)
        if (m.find()) {
            var hour = m.group(1)?.toIntOrNull() ?: 7
            val min = m.group(2)?.toIntOrNull() ?: 0
            val modifier = m.group(3) ?: ""
            if (modifier.contains("pm") && hour < 12) hour += 12
            return Pair(hour, min)
        }
        return Pair(7, 0)
    }

    private fun extractAppName(text: String): String {
        return text
            .replace("kholo", "")
            .replace("open", "")
            .replace("chalao", "")
            .replace("start", "")
            .replace("app", "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
