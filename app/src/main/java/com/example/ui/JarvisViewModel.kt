package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiProvider
import com.example.ai.CloudAiService
import com.example.data.repository.JarvisCloudRepository
import com.example.executor.DeviceActionExecutor
import com.example.executor.ExecutionResult
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.ChatMessage
import com.example.model.JarvisCommandResult
import com.example.service.JarvisAccessibilityService
import com.example.speech.VoiceManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AssistantStatus {
    STANDBY,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceManager = VoiceManager(application)
    private val cloudAiService = CloudAiService()
    private val cloudRepo = JarvisCloudRepository(application)

    private val _assistantStatus = MutableStateFlow(AssistantStatus.STANDBY)
    val assistantStatus: StateFlow<AssistantStatus> = _assistantStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "Namaste! Main JARVIS hoon. Aap YouTube, Instagram, Phone calls, Screen scroll, ya device settings control kar sakte hain. Kahiye, main kya karoon?",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<JarvisCommandResult?>(null)
    val pendingConfirmation: StateFlow<JarvisCommandResult?> = _pendingConfirmation.asStateFlow()

    private val _selectedAiProvider = MutableStateFlow(AiProvider.AUTO)
    val selectedAiProvider: StateFlow<AiProvider> = _selectedAiProvider.asStateFlow()

    private val _customOpenAiKey = MutableStateFlow("")
    val customOpenAiKey: StateFlow<String> = _customOpenAiKey.asStateFlow()

    private val _statusBannerText = MutableStateFlow<String?>(null)
    val statusBannerText: StateFlow<String?> = _statusBannerText.asStateFlow()

    private val _screenReadContent = MutableStateFlow<List<String>?>(null)
    val screenReadContent: StateFlow<List<String>?> = _screenReadContent.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val audioRms: StateFlow<Float> = voiceManager.audioRms
    val partialTranscript: StateFlow<String> = voiceManager.partialTranscript
    val selectedLanguage: StateFlow<com.example.speech.SpeechLanguage> = voiceManager.selectedLanguage
    val isAccessibilityActive: StateFlow<Boolean> = JarvisAccessibilityService.isServiceActive

    private val _currentUser = MutableStateFlow(Firebase.auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    init {
        Firebase.auth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    fun setSpeechLanguage(language: com.example.speech.SpeechLanguage) {
        voiceManager.setSpeechLanguage(language)
    }

    fun setAiProvider(provider: AiProvider) {
        _selectedAiProvider.value = provider
    }

    fun setCustomOpenAiKey(key: String) {
        _customOpenAiKey.value = key
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
            _assistantStatus.value = AssistantStatus.STANDBY
        } else {
            _assistantStatus.value = AssistantStatus.LISTENING
            voiceManager.startListening(
                onResult = { text ->
                    handleUserCommand(text)
                },
                onError = { errorMsg ->
                    _assistantStatus.value = AssistantStatus.STANDBY
                    _statusBannerText.value = errorMsg
                }
            )
        }
    }

    fun handleUserCommand(rawText: String) {
        if (rawText.isBlank()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = rawText,
            isUser = true
        )
        _messages.value = _messages.value + userMsg
        _assistantStatus.value = AssistantStatus.PROCESSING
        _statusBannerText.value = "Analyzing command..."

        viewModelScope.launch {
            try {
                val (result, aiSource) = cloudAiService.processCommand(
                    rawCommand = rawText,
                    provider = _selectedAiProvider.value,
                    customOpenAiKey = _customOpenAiKey.value.ifBlank { null }
                )

                if (result.requiresConfirmation) {
                    _pendingConfirmation.value = result
                    _assistantStatus.value = AssistantStatus.STANDBY
                    _statusBannerText.value = "Authorization required for action"

                    val jarvisMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "${result.spokenResponseHindi} (AI: $aiSource)",
                        isUser = false,
                        actionResult = result
                    )
                    _messages.value = _messages.value + jarvisMsg

                    voiceManager.speak(result.spokenResponseHindi, preferHindi = true)
                } else {
                    executeAndCompleteAction(result, aiSource)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command", e)
                _assistantStatus.value = AssistantStatus.STANDBY
                _statusBannerText.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun confirmPendingAction() {
        val command = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            executeAndCompleteAction(command.copy(status = ActionStatus.PARSED), "Confirmed by User")
        }
    }

    fun cancelPendingAction() {
        val command = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        val cancelledMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Action cancelled as requested.",
            isUser = false,
            actionResult = command.copy(status = ActionStatus.CANCELLED)
        )
        _messages.value = _messages.value + cancelledMsg
        _assistantStatus.value = AssistantStatus.STANDBY
        _statusBannerText.value = "Action cancelled"
        voiceManager.speak("Thik hai, cancel kar diya.", preferHindi = true)
    }

    private suspend fun executeAndCompleteAction(result: JarvisCommandResult, aiSource: String) {
        _assistantStatus.value = AssistantStatus.EXECUTING
        _statusBannerText.value = "Executing: ${result.actionType.name}"

        val execResult: ExecutionResult = DeviceActionExecutor.execute(getApplication(), result)

        val finalStatus = if (execResult.success) ActionStatus.EXECUTED else ActionStatus.FAILED
        val finalCommand = result.copy(status = finalStatus)

        if (execResult.readScreenContent != null) {
            _screenReadContent.value = execResult.readScreenContent
        }

        val speakContent = if (result.actionType == ActionType.DEVICE_STATUS && execResult.success) {
            execResult.message
        } else {
            result.spokenResponseHindi
        }

        val assistantText = if (execResult.success) {
            if (result.actionType == ActionType.DEVICE_STATUS) {
                execResult.message
            } else {
                "${result.spokenResponseHindi} [${execResult.message}]"
            }
        } else {
            "Action couldn't complete: ${execResult.message}"
        }

        val jarvisMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = assistantText,
            isUser = false,
            actionResult = finalCommand
        )
        _messages.value = _messages.value + jarvisMsg

        // Speak reply
        _assistantStatus.value = AssistantStatus.SPEAKING
        voiceManager.speak(speakContent, preferHindi = true)

        // Save to Cloud Firestore if user is authenticated
        cloudRepo.saveCommand(finalCommand, aiSource)

        _statusBannerText.value = if (execResult.success) "Command completed successfully" else execResult.message
        _assistantStatus.value = AssistantStatus.STANDBY
    }

    fun replayMessageSpeech(text: String) {
        voiceManager.speak(text, preferHindi = true)
    }

    fun clearScreenReadContent() {
        _screenReadContent.value = null
    }

    fun openAccessibilitySettings(context: Context) {
        DeviceActionExecutor.openAccessibilitySettings(context)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }

    companion object {
        private const val TAG = "JarvisViewModel"
    }
}
