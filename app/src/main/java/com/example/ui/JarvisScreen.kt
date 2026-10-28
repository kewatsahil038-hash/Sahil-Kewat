package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.AiProvider
import com.example.model.ActionStatus
import com.example.model.ActionType
import com.example.model.ChatMessage
import com.example.model.JarvisCommandResult
import com.example.speech.SpeechLanguage
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val assistantStatus by viewModel.assistantStatus.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val audioRms by viewModel.audioRms.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedAiProvider by viewModel.selectedAiProvider.collectAsStateWithLifecycle()
    val customOpenAiKey by viewModel.customOpenAiKey.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerText.collectAsStateWithLifecycle()
    val screenReadContent by viewModel.screenReadContent.collectAsStateWithLifecycle()

    var showInfoSheet by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleVoiceListening()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisDarkNavy,
        topBar = {
            JarvisTopBar(
                status = assistantStatus,
                currentUser = currentUser,
                selectedLanguage = selectedLanguage,
                onLanguageClick = { showLanguageMenu = true },
                onInfoClick = { showInfoSheet = true },
                onAiConfigClick = { showAiConfigDialog = true },
                onGoogleSignIn = {
                    signInWithGoogle(context, coroutineScope)
                },
                onSignOut = {
                    signOutGoogle(context, coroutineScope)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // JARVIS Dynamic Audio Visualizer with Microphone Reactivity & Equalizer
            com.example.ui.components.JarvisAudioVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                audioRms = audioRms,
                status = assistantStatus,
                partialTranscript = partialTranscript
            )

            // Accessibility Warning Banner if screen control service is inactive
            if (!isAccessibilityActive) {
                AccessibilityBanner(
                    onEnableClick = { viewModel.openAccessibilitySettings(context) }
                )
            }

            // Quick Preset Commands Bar (User requested samples)
            QuickCommandBar(
                onCommandSelected = { cmd ->
                    viewModel.handleUserCommand(cmd)
                }
            )

            // Conversation and Command Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onReplaySpeech = { viewModel.replayMessageSpeech(msg.text) }
                    )
                }
            }

            // Status notification banner
            if (statusBanner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JarvisSurfaceDark.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusBanner ?: "",
                        color = JarvisCyanGlow,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom Input Controls with Language Toggle
            BottomInputBar(
                textInput = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.handleUserCommand(textInput)
                        textInput = ""
                    }
                },
                isListening = isListening,
                selectedLanguage = selectedLanguage,
                onLanguageClick = { showLanguageMenu = true },
                onMicClick = {
                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                },
                selectedProvider = selectedAiProvider,
                onProviderClick = { showAiConfigDialog = true }
            )
        }

        // Sensitive Action Confirmation Dialog (Calls, WhatsApp messages)
        if (pendingConfirmation != null) {
            ActionConfirmationModal(
                command = pendingConfirmation!!,
                onConfirm = { viewModel.confirmPendingAction() },
                onCancel = { viewModel.cancelPendingAction() }
            )
        }

        // Screen Read Results Dialog
        if (screenReadContent != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearScreenReadContent() },
                containerColor = JarvisSurfaceDark,
                title = {
                    Text("Screen Content Scanned", color = JarvisCyan, fontWeight = FontWeight.Bold)
                },
                text = {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(screenReadContent!!) { line ->
                            Text("• $line", color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearScreenReadContent() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                    ) {
                        Text("Close", color = Color.Black)
                    }
                }
            )
        }

        // Language Selection Dialog / Menu
        if (showLanguageMenu) {
            SpeechLanguageSelectionDialog(
                currentLanguage = selectedLanguage,
                onSelect = { lang ->
                    viewModel.setSpeechLanguage(lang)
                    showLanguageMenu = false
                },
                onDismiss = { showLanguageMenu = false }
            )
        }

        // How-It-Works & Device Controls Bottom Sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = JarvisSurfaceDark,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                DeviceControlInfoContent(
                    isAccessibilityActive = isAccessibilityActive,
                    onOpenSettings = { viewModel.openAccessibilitySettings(context) }
                )
            }
        }

        // AI Provider & ChatGPT Configuration Dialog
        if (showAiConfigDialog) {
            AiProviderConfigDialog(
                currentProvider = selectedAiProvider,
                customKey = customOpenAiKey,
                onSave = { provider, key ->
                    viewModel.setAiProvider(provider)
                    viewModel.setCustomOpenAiKey(key)
                    showAiConfigDialog = false
                },
                onDismiss = { showAiConfigDialog = false }
            )
        }
    }
}

@Composable
fun JarvisTopBar(
    status: AssistantStatus,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    selectedLanguage: SpeechLanguage,
    onLanguageClick: () -> Unit,
    onInfoClick: () -> Unit,
    onAiConfigClick: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = JarvisSurfaceDark,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status glowing dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                AssistantStatus.LISTENING -> JarvisBlue
                                AssistantStatus.PROCESSING -> JarvisAccentGold
                                AssistantStatus.EXECUTING -> JarvisAccentGreen
                                AssistantStatus.SPEAKING -> JarvisCyan
                                AssistantStatus.STANDBY -> JarvisCyan
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = JarvisCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Voice Agent",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speech Language Pill
                Surface(
                    color = JarvisCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                    modifier = Modifier
                        .clickable { onLanguageClick() }
                        .padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = JarvisCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (selectedLanguage) {
                                SpeechLanguage.AUTO -> "Hindi/Eng"
                                SpeechLanguage.HINDI -> "हिन्दी"
                                SpeechLanguage.HINGLISH -> "Hinglish"
                                SpeechLanguage.ENGLISH -> "English"
                            },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Cloud Sync Status / Google Sign-In
                if (currentUser != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisCardBg)
                            .clickable { onSignOut() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Connected",
                            tint = JarvisAccentGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUser.displayName?.split(" ")?.firstOrNull() ?: "Cloud",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("google_signin_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Sign in with Google",
                            tint = JarvisCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud", color = JarvisCyan, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onAiConfigClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = "AI Engine Configuration",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help and Device Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ArcReactorCore(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    partialTranscript: String,
    status: AssistantStatus
) {
    val transition = rememberInfiniteTransition(label = "arc_reactor")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 700 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f * 0.75f
                val dynamicRadius = baseRadius + (audioRms * 16f)

                // Outer pulsing glow circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isListening) JarvisCyanGlow.copy(alpha = 0.45f) else JarvisCyan.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius * pulse * 1.3f
                    ),
                    radius = dynamicRadius * pulse * 1.3f,
                    center = center
                )

                // Concentric Tech Rings
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.35f),
                    radius = baseRadius * 1.05f,
                    center = center,
                    style = Stroke(width = 2f)
                )

                drawCircle(
                    color = if (isListening) JarvisCyanGlow else JarvisBlue.copy(alpha = 0.6f),
                    radius = baseRadius * 0.85f,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Inner core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, JarvisCyan, JarvisDarkNavy),
                        center = center,
                        radius = baseRadius * 0.45f
                    ),
                    radius = baseRadius * 0.45f * (if (isSpeaking || isListening) pulse else 1f),
                    center = center
                )

                // 8 Arc Spokes
                val spokeCount = 8
                for (i in 0 until spokeCount) {
                    val angle = Math.toRadians((rotation + (i * (360.0 / spokeCount))))
                    val startX = center.x + (baseRadius * 0.5f * Math.cos(angle)).toFloat()
                    val startY = center.y + (baseRadius * 0.5f * Math.sin(angle)).toFloat()
                    val endX = center.x + (baseRadius * 0.82f * Math.cos(angle)).toFloat()
                    val endY = center.y + (baseRadius * 0.82f * Math.sin(angle)).toFloat()

                    drawLine(
                        color = if (isListening) JarvisCyanGlow else JarvisBlue,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }
            }

            // Center status symbol
            Icon(
                imageVector = when {
                    isListening -> Icons.Default.Mic
                    isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                    else -> Icons.Outlined.SmartToy
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Status or Live Partial Speech Recognition text
        if (isListening && partialTranscript.isNotBlank()) {
            Text(
                text = "🎙️ $partialTranscript",
                color = JarvisCyanGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Text(
                text = when (status) {
                    AssistantStatus.STANDBY -> "JARVIS ONLINE // STANDBY"
                    AssistantStatus.LISTENING -> "LISTENING // HINDI, HINGLISH & ENGLISH"
                    AssistantStatus.PROCESSING -> "PROCESSING AI INTENT..."
                    AssistantStatus.EXECUTING -> "EXECUTING DEVICE ACTION..."
                    AssistantStatus.SPEAKING -> "SPEAKING RESPONSE..."
                },
                color = JarvisCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AccessibilityBanner(onEnableClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGold.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Screen Control Inactive",
                    color = JarvisAccentGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enable JARVIS in Accessibility Settings for screen scroll, home & back navigation.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAccentGold),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Enable", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickCommandBar(onCommandSelected: (String) -> Unit) {
    val sampleCommands = listOf(
        "YouTube kholo aur Arijit Singh ka gana chalao",
        "Instagram kholo aur Rahul ki chat dhoondo",
        "Rahul ko message likho: main baad mein call karunga",
        "Screen ko neeche scroll karo",
        "Settings kholo",
        "Wapas jao aur home screen par aao",
        "5 minute ka timer lagao",
        "Subah 7 baje ka alarm lagao",
        "Mummy ko call lagao",
        "Camera kholo",
        "Battery aur phone status batao"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "QUICK VOICE COMMANDS (TAP TO TEST):",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            letterSpacing = 1.sp
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleCommands) { cmd ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = JarvisSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                    modifier = Modifier.clickable { onCommandSelected(cmd) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cmd,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onReplaySpeech: () -> Unit
) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUser) "YOU" else "JARVIS",
                color = if (isUser) JarvisBlue else JarvisCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(if (isUser) Color(0xFF0C2442) else JarvisCardBg)
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisBlue.copy(alpha = 0.5f) else JarvisCardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Action Result Tag if present
                if (message.actionResult != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ActionCard(result = message.actionResult)
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onReplaySpeech,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen to speech",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(result: JarvisCommandResult) {
    Surface(
        color = JarvisSurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (result.actionType) {
                        ActionType.YOUTUBE_SEARCH, ActionType.YOUTUBE_OPEN -> Icons.Default.PlayArrow
                        ActionType.MAKE_CALL -> Icons.Default.Call
                        ActionType.SEND_WHATSAPP -> Icons.AutoMirrored.Filled.Send
                        ActionType.SET_TIMER, ActionType.SET_ALARM -> Icons.Default.Alarm
                        ActionType.OPEN_SETTINGS -> Icons.Default.Settings
                        ActionType.NAVIGATE_HOME, ActionType.NAVIGATE_BACK, ActionType.SCROLL_DOWN, ActionType.SCROLL_UP -> Icons.Default.Smartphone
                        else -> Icons.Default.Navigation
                    },
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = result.actionType.name.replace("_", " "),
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.target.isNotEmpty()) {
                        Text(
                            text = result.target,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Status chip
            Surface(
                color = when (result.status) {
                    ActionStatus.EXECUTED -> JarvisAccentGreen.copy(alpha = 0.2f)
                    ActionStatus.PENDING_CONFIRMATION -> JarvisAccentGold.copy(alpha = 0.2f)
                    ActionStatus.FAILED -> JarvisAccentRed.copy(alpha = 0.2f)
                    ActionStatus.CANCELLED -> TextMuted.copy(alpha = 0.2f)
                    ActionStatus.PARSED -> JarvisBlue.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = result.status.name,
                    color = when (result.status) {
                        ActionStatus.EXECUTED -> JarvisAccentGreen
                        ActionStatus.PENDING_CONFIRMATION -> JarvisAccentGold
                        ActionStatus.FAILED -> JarvisAccentRed
                        ActionStatus.CANCELLED -> TextMuted
                        ActionStatus.PARSED -> JarvisBlue
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ActionConfirmationModal(
    command: JarvisCommandResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = JarvisSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = JarvisAccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Authorization",
                    color = JarvisAccentGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "JARVIS requires your permission to perform this sensitive action:",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = JarvisCardBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Action: ${command.actionType.name}",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (command.target.isNotEmpty()) {
                            Text(
                                text = "Target: ${command.target}",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        if (command.details.isNotEmpty()) {
                            Text(
                                text = "Details: \"${command.details}\"",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Authorize & Run", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentRed)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = JarvisAccentRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel", color = JarvisAccentRed)
            }
        }
    )
}

@Composable
fun BottomInputBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    selectedLanguage: SpeechLanguage,
    onLanguageClick: () -> Unit,
    onMicClick: () -> Unit,
    selectedProvider: AiProvider,
    onProviderClick: () -> Unit
) {
    Surface(
        color = JarvisSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Selected AI Provider & Speech Language chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLanguageClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Voice: ${selectedLanguage.displayName}",
                        color = JarvisCyanGlow,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "AI: ${selectedProvider.displayName}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onProviderClick() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Mic Button
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = if (isListening) JarvisAccentRed else JarvisCyan,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_mic_button"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input Microphone"
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Text Input for typing commands
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    placeholder = {
                        Text("Type command in Hindi, Hinglish, or English...", color = TextMuted, fontSize = 12.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedContainerColor = JarvisCardBg,
                        unfocusedContainerColor = JarvisCardBg
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(onClick = onSend) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Command",
                                    tint = JarvisCyan
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SpeechLanguageSelectionDialog(
    currentLanguage: SpeechLanguage,
    onSelect: (SpeechLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceDark,
        title = {
            Text("Speech Recognition Language", color = JarvisCyan, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Select primary language for SpeechRecognizer:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SpeechLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (lang == currentLanguage) JarvisCardBg else Color.Transparent)
                            .clickable { onSelect(lang) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (lang == currentLanguage) JarvisCyan else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(lang.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "Code: ${lang.tag}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Text("Close", color = Color.Black)
            }
        }
    )
}

@Composable
fun AiProviderConfigDialog(
    currentProvider: AiProvider,
    customKey: String,
    onSave: (AiProvider, String) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf(currentProvider) }
    var key by remember { mutableStateOf(customKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceDark,
        title = {
            Text("AI Brain & Cloud Settings", color = JarvisCyan, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Select active AI intelligence engine:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                AiProvider.entries.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (provider == item) JarvisCardBg else Color.Transparent)
                            .clickable { provider = item }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (provider == item) JarvisCyan else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.displayName, color = TextPrimary, fontSize = 13.sp)
                    }
                }

                if (provider == AiProvider.CHATGPT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("OpenAI API Key (Optional):", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-...", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(provider, key) },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeviceControlInfoContent(
    isAccessibilityActive: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "JARVIS Device Control Architecture",
            color = JarvisCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "How voice commands connect to Android features:",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureInfoRow(
            title = "1. YouTube & Instagram Control",
            desc = "Uses official Android Deep Links and View Intents to open videos, searches, and profiles directly in native apps with web fallback."
        )

        FeatureInfoRow(
            title = "2. Android Screen Control (Accessibility)",
            desc = "Uses Android AccessibilityService to perform global actions (Home, Back, Recents), dispatch swipe scroll gestures, and read on-screen text."
        )

        FeatureInfoRow(
            title = "3. Phone Calls & WhatsApp Messages",
            desc = "Enforces zero-trust confirmation prompts before placing calls or sending messages, protecting privacy and preventing accidental dispatches."
        )

        FeatureInfoRow(
            title = "4. Alarms, Timers, Camera & Settings",
            desc = "Utilizes AlarmClock Intents, MediaStore camera intents, and system settings intents."
        )

        FeatureInfoRow(
            title = "5. SpeechRecognizer & Multilingual Support",
            desc = "Utilizes Android SpeechRecognizer API with real-time partial transcription, supporting Hindi (hi-IN), Hinglish (en-IN), and English (en-US)."
        )

        FeatureInfoRow(
            title = "6. Cloud Sync & ChatGPT Intelligence",
            desc = "Syncs command history to Cloud Firestore securely under your Google account, with multilingual NLU, Gemini 3.8 Flash, and ChatGPT integration."
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAccessibilityActive) "Accessibility Enabled ✓" else "Open Accessibility Settings",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FeatureInfoRow(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, color = JarvisCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(desc, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

// Google Sign-In helper via CredentialManager
fun signInWithGoogle(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        return
    }
    val credentialManager = CredentialManager.create(context)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisAuth", "Sign-in error: ${e.message}")
        }
    }
}

fun signOutGoogle(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    Firebase.auth.signOut()
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}
    }
}
