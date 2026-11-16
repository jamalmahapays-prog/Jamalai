package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.AppStrings
import com.example.ui.components.AiOutputCard
import com.example.ui.components.PremiumCard
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletPrimary
import java.util.Locale

@Composable
fun VoiceAssistantScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    errorMessage: String?,
    onRunVoicePrompt: (toolType: String, prompt: String, systemInstruction: String) -> Unit
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = if (strings.isArabic) Locale.forLanguageTag("ar") else Locale.US
            }
        }
        engine
    }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer, tts) {
        onDispose {
            runCatching { speechRecognizer?.destroy() }
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            speechError = "Speech recognition service unavailable on this device. You can type below."
            return
        }
        speechError = null
        val localeTag = if (strings.isArabic) "ar-SA" else "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                speechError = "Speech error code: $error"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    transcript = spoken
                    val langNote = if (strings.isArabic) "Respond concisely and naturally in Arabic for voice playback." else "Respond concisely and naturally in English for voice playback."
                    onRunVoicePrompt("voice", spoken, langNote)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) transcript = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            speechError = strings.voicePermissionNeeded
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(strings.voiceTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.voiceSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isListening) listOf(RoseError, VioletPrimary)
                                        else listOf(VioletPrimary, CyanSecondary)
                                    )
                                )
                                .clickable {
                                    if (isListening) {
                                        speechRecognizer?.stopListening()
                                        isListening = false
                                    } else {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            startListening()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("voice_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = strings.voiceTapToSpeak,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isListening) strings.voiceListening else strings.voiceTapToSpeak,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isListening) RoseError else CyanSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        label = { Text(strings.chatPlaceholder) },
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val langNote = if (strings.isArabic) "Respond concisely and naturally in Arabic for voice playback." else "Respond concisely and naturally in English for voice playback."
                            onRunVoicePrompt("voice", transcript, langNote)
                        },
                        enabled = !isLoading && transcript.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.send)
                        }
                    }
                }
            }

            val err = errorMessage ?: speechError
            if (!err.isNullOrBlank()) {
                item {
                    Surface(
                        color = RoseError.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(err, color = RoseError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (output.isNotBlank()) {
                item {
                    AiOutputCard(
                        title = strings.voiceTitle,
                        text = output,
                        strings = strings,
                        context = context
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                tts.language = if (strings.isArabic) Locale.forLanguageTag("ar") else Locale.US
                                tts.speak(output, TextToSpeech.QUEUE_FLUSH, null, "myai_tts")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.voiceSpeakReply)
                        }
                        OutlinedButton(
                            onClick = { tts.stop() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.voiceStopSpeaking)
                        }
                    }
                }
            }
        }
    }
}
