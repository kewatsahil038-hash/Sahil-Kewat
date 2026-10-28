package com.example.ai

import android.util.Log
import com.example.data.remote.openai.OpenAiChatRequest
import com.example.data.remote.openai.OpenAiClient
import com.example.data.remote.openai.OpenAiMessage
import com.example.data.remote.openai.OpenAiResponseFormat
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.JarvisCommandResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiProvider(val displayName: String) {
    AUTO("Auto (NLU + Cloud)"),
    GEMINI("Gemini 3.8 Flash"),
    CHATGPT("ChatGPT (OpenAI)")
}

class CloudAiService {

    // Lazy initialization for Firebase AI Logic
    private val geminiModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic init error", e)
            null
        }
    }

    /**
     * Interprets user command using selected AI provider with fallback to on-device NLU.
     */
    suspend fun processCommand(
        rawCommand: String,
        provider: AiProvider,
        customOpenAiKey: String? = null
    ): Pair<JarvisCommandResult, String> {
        val nluFallback = JarvisNluEngine.parseCommand(rawCommand)

        return when (provider) {
            AiProvider.AUTO -> {
                // If it's a known device intent (like open app, youtube, screen control),
                // NLU is instantaneous and exact. If conversational or unknown, query Gemini.
                if (nluFallback.actionType != ActionType.UNKNOWN && nluFallback.actionType != ActionType.GENERAL_REPLY) {
                    Pair(nluFallback, "Jarvis Local NLU")
                } else {
                    val geminiResult = queryGemini(rawCommand)
                    if (geminiResult != null) {
                        Pair(geminiResult, "Gemini 3.8 Flash")
                    } else {
                        Pair(nluFallback, "Jarvis Local NLU")
                    }
                }
            }
            AiProvider.GEMINI -> {
                val geminiResult = queryGemini(rawCommand)
                if (geminiResult != null) {
                    Pair(geminiResult, "Gemini 3.8 Flash")
                } else {
                    Pair(nluFallback, "Jarvis Local NLU (Fallback)")
                }
            }
            AiProvider.CHATGPT -> {
                val gptResult = queryChatGPT(rawCommand, customOpenAiKey)
                if (gptResult != null) {
                    Pair(gptResult, "ChatGPT (Retrofit)")
                } else {
                    Pair(nluFallback, "Jarvis Local NLU (Fallback)")
                }
            }
        }
    }

    /**
     * Query Firebase AI Logic (Gemini 3.8 Flash)
     */
    private suspend fun queryGemini(rawCommand: String): JarvisCommandResult? {
        return try {
            val model = geminiModel ?: return null
            val prompt = buildSystemPrompt(rawCommand)
            val response = withTimeout(25_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                parseAiJsonResponse(rawCommand, text)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini call failed, falling back", e)
            null
        }
    }

    /**
     * Query ChatGPT via Retrofit OpenAiApiService
     */
    private suspend fun queryChatGPT(rawCommand: String, apiKey: String?): JarvisCommandResult? {
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "OpenAI API key not provided for ChatGPT")
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildSystemPrompt(rawCommand)
                val request = OpenAiChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = "You are JARVIS Android AI Assistant. Return valid JSON only with keys: actionType, target, details, spokenResponseHindi, spokenResponseEnglish, requiresConfirmation."
                        ),
                        OpenAiMessage(role = "user", content = prompt)
                    ),
                    temperature = 0.3,
                    responseFormat = OpenAiResponseFormat("json_object")
                )

                val response = OpenAiClient.apiService.createChatCompletion(
                    authHeader = "Bearer ${apiKey.trim()}",
                    request = request
                )

                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (!content.isNullOrBlank()) {
                        parseAiJsonResponse(rawCommand, content)
                    } else {
                        Log.w(TAG, "OpenAI returned empty choice message")
                        null
                    }
                } else {
                    val err = response.errorBody()?.string()
                    Log.e(TAG, "OpenAI Retrofit error code ${response.code()}: $err")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Retrofit ChatGPT call error", e)
                null
            }
        }
    }

    private fun buildSystemPrompt(userText: String): String {
        return """
            You are JARVIS, an Android Personal AI Agent. The user gives commands in Hindi, Hinglish, or English.
            Analyze the command and return a single valid JSON object with NO MARKDOWN OR BACKTICKS:
            {
              "actionType": "YOUTUBE_SEARCH" | "YOUTUBE_OPEN" | "INSTAGRAM_SEARCH" | "INSTAGRAM_CHAT" | "OPEN_APP" | "OPEN_SETTINGS" | "OPEN_CAMERA" | "OPEN_CHROME" | "SET_ALARM" | "SET_TIMER" | "MAKE_CALL" | "SEND_WHATSAPP" | "NAVIGATE_HOME" | "NAVIGATE_BACK" | "SCROLL_DOWN" | "SCROLL_UP" | "READ_SCREEN" | "GENERAL_REPLY",
              "target": "target app name, search query, contact name, or time",
              "details": "short description or message content",
              "spokenResponseHindi": "Natural Hindi / Hinglish response to speak to user",
              "spokenResponseEnglish": "English translation for response",
              "requiresConfirmation": boolean (true ONLY for sensitive actions like MAKE_CALL or SEND_WHATSAPP/SEND_MESSAGE)
            }
            User Command: "$userText"
        """.trimIndent()
    }

    private fun parseAiJsonResponse(rawCommand: String, aiOutput: String): JarvisCommandResult? {
        return try {
            val cleanJson = aiOutput
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val obj = JSONObject(cleanJson)
            val actionTypeName = obj.optString("actionType", "UNKNOWN")
            val actionType = try {
                ActionType.valueOf(actionTypeName)
            } catch (e: Exception) {
                ActionType.UNKNOWN
            }
            val target = obj.optString("target", "")
            val details = obj.optString("details", "")
            val spokenHindi = obj.optString("spokenResponseHindi", "")
            val spokenEnglish = obj.optString("spokenResponseEnglish", "")
            val requiresConfirm = obj.optBoolean("requiresConfirmation", false)

            JarvisCommandResult(
                rawCommand = rawCommand,
                actionType = actionType,
                target = target,
                details = details,
                spokenResponseHindi = spokenHindi.ifEmpty { "Aapka aadesh poora kar raha hoon." },
                spokenResponseEnglish = spokenEnglish.ifEmpty { "Executing your command." },
                requiresConfirmation = requiresConfirm,
                confirmationPrompt = if (requiresConfirm) "Please confirm executing: $actionType ($target)" else "",
                status = if (requiresConfirm) ActionStatus.PENDING_CONFIRMATION else ActionStatus.PARSED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI JSON response: $aiOutput", e)
            null
        }
    }

    companion object {
        private const val TAG = "CloudAiService"
    }
}
