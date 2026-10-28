package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechLanguage(val displayName: String, val tag: String, val extraLangs: Array<String>) {
    AUTO("Auto (Hindi/Hinglish/English)", "hi-IN", arrayOf("en-IN", "en-US", "hi-IN")),
    HINDI("Hindi (हिन्दी)", "hi-IN", arrayOf("hi-IN", "en-IN")),
    HINGLISH("Hinglish (English India)", "en-IN", arrayOf("en-IN", "hi-IN")),
    ENGLISH("English (US)", "en-US", arrayOf("en-US", "en-IN"))
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(SpeechLanguage.AUTO)
    val selectedLanguage: StateFlow<SpeechLanguage> = _selectedLanguage.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onSpeechErrorCallback: ((String) -> Unit)? = null

    private var isTtsInitialized = false
    private val hindiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()

    init {
        mainHandler.post {
            tts = TextToSpeech(context.applicationContext, this)
            initSpeechRecognizer()
        }
    }

    fun setSpeechLanguage(language: SpeechLanguage) {
        _selectedLanguage.value = language
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(hindiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isTtsInitialized = true
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device/emulator.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialTranscript.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        _partialTranscript.value = "Hearing speech..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS decibels to 0f..1f for visualization
                        _audioRms.value = (rmsdB / 10f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioRms.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRms.value = 0f
                        _partialTranscript.value = ""

                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly or tap quick commands."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue while contacting speech recognition service."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Please check microphone."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy. Resetting..."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Speech recognition code: $error"
                        }
                        Log.w(TAG, "SpeechRecognizer error: $error ($msg)")
                        onSpeechErrorCallback?.invoke(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRms.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull() ?: ""
                        _partialTranscript.value = spokenText
                        if (spokenText.isNotBlank()) {
                            onSpeechResultCallback?.invoke(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val liveText = matches?.firstOrNull()
                        if (!liveText.isNullOrBlank()) {
                            _partialTranscript.value = liveText
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
        }
    }

    /**
     * Starts listening with support for Hindi, Hinglish, and English
     */
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopSpeaking()
        onSpeechResultCallback = onResult
        onSpeechErrorCallback = onError
        _partialTranscript.value = "Starting listening..."

        mainHandler.post {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            if (speechRecognizer == null) {
                onError("Speech recognition service is not available on this device.")
                _isListening.value = false
                return@post
            }

            val lang = _selectedLanguage.value

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang.tag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", lang.extraLangs)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS is listening (${lang.displayName})...")
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                _isListening.value = false
                Log.e(TAG, "Error starting speech recognition", e)
                onError("Could not start SpeechRecognizer: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _audioRms.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun speak(text: String, preferHindi: Boolean = true) {
        if (!isTtsInitialized || text.isBlank()) return
        stopListening()
        mainHandler.post {
            if (preferHindi) {
                val res = tts?.setLanguage(hindiLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            } else {
                tts?.setLanguage(Locale.US)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "JarvisVoiceManager"
    }
}
