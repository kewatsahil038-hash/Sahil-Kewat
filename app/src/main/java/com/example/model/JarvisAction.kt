package com.example.model

enum class ActionType {
    YOUTUBE_SEARCH,
    YOUTUBE_OPEN,
    INSTAGRAM_SEARCH,
    INSTAGRAM_CHAT,
    INSTAGRAM_OPEN,
    OPEN_APP,
    OPEN_SETTINGS,
    OPEN_CAMERA,
    OPEN_CHROME,
    SET_ALARM,
    SET_TIMER,
    MAKE_CALL,
    SEND_WHATSAPP,
    NAVIGATE_HOME,
    NAVIGATE_BACK,
    NAVIGATE_RECENTS,
    SCROLL_DOWN,
    SCROLL_UP,
    READ_SCREEN,
    DEVICE_STATUS,
    GENERAL_REPLY,
    UNKNOWN
}

enum class ActionStatus {
    PARSED,
    PENDING_CONFIRMATION,
    EXECUTED,
    FAILED,
    CANCELLED
}

data class JarvisCommandResult(
    val rawCommand: String,
    val actionType: ActionType,
    val target: String = "",
    val details: String = "",
    val spokenResponseHindi: String,
    val spokenResponseEnglish: String,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String = "",
    val status: ActionStatus = ActionStatus.PARSED
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionResult: JarvisCommandResult? = null
)
