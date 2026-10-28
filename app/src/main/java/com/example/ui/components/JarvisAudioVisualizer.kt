package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import com.example.ui.theme.JarvisAccentGold
import com.example.ui.theme.JarvisAccentGreen
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-tech JARVIS Audio Visualizer that dynamically reacts to microphone input levels (audioRms).
 * Features concentric holographic arc reactor rings, animated audio spectrum waveform bars,
 * and live decibel/intensity telemetry.
 */
@Composable
fun JarvisAudioVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    status: AssistantStatus,
    partialTranscript: String = "",
    modifier: Modifier = Modifier
) {
    // Smooth animated audio level to avoid abrupt jitter
    val smoothRms by animateFloatAsState(
        targetValue = audioRms.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "smooth_rms"
    )

    // Infinite rotation for sci-fi HUD radar ring
    val transition = rememberInfiniteTransition(label = "jarvis_visualizer")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 2000 else if (isSpeaking) 4000 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rotation"
    )

    val breathingPulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 600 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Arc Core with Pulsing Glow
        Box(
            modifier = Modifier.size(126.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f * 0.72f
                val dynamicRadius = baseRadius + (smoothRms * 18f)

                // 1. Outer Neon Aura Glow (expands with voice volume)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isListening) JarvisCyanGlow.copy(alpha = 0.5f) else JarvisCyan.copy(alpha = 0.2f),
                            if (isListening) JarvisBlue.copy(alpha = 0.25f) else Color.Transparent,
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius * breathingPulse * 1.35f
                    ),
                    radius = dynamicRadius * breathingPulse * 1.35f,
                    center = center
                )

                // 2. Outer Precision Ring with HUD Ticks
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.35f),
                    radius = baseRadius * 1.08f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Radial Tick Marks
                val tickCount = 16
                for (i in 0 until tickCount) {
                    val angle = Math.toRadians((rotation + (i * (360.0 / tickCount))))
                    val tickStart = center + Offset((baseRadius * 1.04f * cos(angle)).toFloat(), (baseRadius * 1.04f * sin(angle)).toFloat())
                    val tickEnd = center + Offset((baseRadius * 1.12f * cos(angle)).toFloat(), (baseRadius * 1.12f * sin(angle)).toFloat())
                    drawLine(
                        color = if (i % 4 == 0) JarvisCyanGlow else JarvisBlue.copy(alpha = 0.5f),
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = if (i % 4 == 0) 2.5f else 1.2f
                    )
                }

                // 3. Middle Cyberspace Ring
                drawCircle(
                    color = if (isListening) JarvisCyan else JarvisBlue.copy(alpha = 0.6f),
                    radius = baseRadius * 0.86f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // 4. Central Glowing Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, JarvisCyan, JarvisDarkNavy),
                        center = center,
                        radius = baseRadius * 0.45f
                    ),
                    radius = baseRadius * 0.45f * (if (isSpeaking || isListening) breathingPulse else 1f),
                    center = center
                )

                // 5. 8 Symmetrical Reactor Spokes
                val spokeCount = 8
                for (i in 0 until spokeCount) {
                    val angle = Math.toRadians((-rotation + (i * (360.0 / spokeCount))))
                    val startX = center.x + (baseRadius * 0.48f * cos(angle)).toFloat()
                    val startY = center.y + (baseRadius * 0.48f * sin(angle)).toFloat()
                    val endX = center.x + (baseRadius * 0.84f * cos(angle)).toFloat()
                    val endY = center.y + (baseRadius * 0.84f * sin(angle)).toFloat()

                    drawLine(
                        color = if (isListening) JarvisCyanGlow else JarvisBlue,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f
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

        Spacer(modifier = Modifier.height(6.dp))

        // Spectrum Waveform Equalizer Bars (Reacts directly to Microphone Input)
        AudioWaveformBars(
            isListening = isListening,
            isSpeaking = isSpeaking,
            audioRms = smoothRms,
            wavePhase = wavePhase
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Decibel Telemetry and Status Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val micLevelPercent = (smoothRms * 100).toInt()
            if (isListening) {
                Surface(
                    color = JarvisCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(JarvisCyanGlow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MIC LEVEL: $micLevelPercent%",
                            color = JarvisCyanGlow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = when {
                    isListening && partialTranscript.isNotBlank() -> "🎙️ \"$partialTranscript\""
                    isListening -> "LISTENING // SPEAK IN HINDI OR ENGLISH"
                    status == AssistantStatus.PROCESSING -> "THINKING // UNDERSTANDING INTENT"
                    status == AssistantStatus.EXECUTING -> "EXECUTING // CONTROLLING PHONE"
                    status == AssistantStatus.SPEAKING -> "SPEAKING // JARVIS AUDIO OUT"
                    else -> "JARVIS ONLINE // STANDBY"
                },
                color = if (isListening) JarvisCyanGlow else JarvisCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

/**
 * 28-Bar Equalizer Spectrum Visualizer reflecting voice frequency and input levels.
 */
@Composable
private fun AudioWaveformBars(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    wavePhase: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 28

    Canvas(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .height(34.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.35f
        val barWidth = (width - totalSpacing) / barCount
        val barSpacing = totalSpacing / (barCount - 1)
        val centerY = height / 2f

        for (i in 0 until barCount) {
            // Symmetrical frequency distribution from center outward
            val distanceFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val bellCurve = (1f - (distanceFromCenter * 0.65f)).coerceIn(0.2f, 1f)

            // Harmonic wave modulation
            val harmonic = sin(wavePhase + (i * 0.45f)).toFloat()

            val barHeight = when {
                isListening -> {
                    val baseMin = 4.dp.toPx()
                    val voiceImpact = audioRms * height * 0.95f * bellCurve
                    val animatedFluctuation = (harmonic * 5.dp.toPx() * audioRms)
                    (baseMin + voiceImpact + animatedFluctuation).coerceIn(4.dp.toPx(), height)
                }
                isSpeaking -> {
                    val speechWave = (sin(wavePhase * 1.5f + (i * 0.5f)).toFloat() * 0.5f + 0.5f)
                    (6.dp.toPx() + (speechWave * height * 0.6f * bellCurve)).coerceIn(4.dp.toPx(), height)
                }
                else -> {
                    // Gentle ambient idle breathing ripple
                    val idleWave = (sin(wavePhase * 0.7f + (i * 0.3f)).toFloat() * 0.5f + 0.5f)
                    (3.dp.toPx() + (idleWave * 5.dp.toPx())).coerceIn(3.dp.toPx(), 12.dp.toPx())
                }
            }

            val x = i * (barWidth + barSpacing)
            val top = centerY - (barHeight / 2f)

            // Color gradient from electric blue to glowing cyan and bright white peak
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    if (isListening && audioRms > 0.4f) Color.White else JarvisCyanGlow,
                    JarvisCyan,
                    JarvisBlue
                ),
                startY = top,
                endY = top + barHeight
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
