package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

@Stable
class VoiceDictationController(
    val isListening: Boolean,
    val partialTranscript: String,
    val errorMessage: String?,
    val startOrStop: () -> Unit,
    val clearError: () -> Unit
)

@Composable
fun rememberVoiceDictationController(
    onTranscriptCommitted: (String) -> Unit
): VoiceDictationController {
    val context = LocalContext.current
    val latestCommit by rememberUpdatedState(onTranscriptCommitted)

    var isListening by remember { mutableStateOf(false) }
    var partialTranscript by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Narra la descripción de la escena…")
        }
    }

    val speechActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        partialTranscript = ""
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = matches?.firstOrNull()?.trim()
            if (!best.isNullOrEmpty()) {
                latestCommit(best)
            } else {
                errorMessage = "No se detectó voz en la grabación. Inténtalo de nuevo."
            }
        }
    }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    errorMessage = null
                    isListening = true
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                }

                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    partialTranscript = ""
                    errorMessage = mapSpeechError(error)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    partialTranscript = ""
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim()
                    if (!best.isNullOrEmpty()) {
                        latestCommit(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        partialTranscript = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        onDispose {
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
        }
    }

    fun launchDictation() {
        errorMessage = null
        if (speechRecognizer != null) {
            try {
                isListening = true
                partialTranscript = ""
                speechRecognizer.startListening(recognizerIntent)
                return
            } catch (_: Exception) {
                isListening = false
            }
        }
        try {
            speechActivityLauncher.launch(recognizerIntent)
        } catch (_: Exception) {
            errorMessage = "El reconocimiento de voz no está disponible en este dispositivo o emulador. Activa el servicio de voz de Google o escribe/pega la descripción."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchDictation()
        } else {
            errorMessage = "Permiso de micrófono denegado. Habilita el micrófono en Ajustes para dictar escenas."
        }
    }

    return VoiceDictationController(
        isListening = isListening,
        partialTranscript = partialTranscript,
        errorMessage = errorMessage,
        startOrStop = {
            if (isListening) {
                isListening = false
                runCatching { speechRecognizer?.stopListening() }
            } else {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    launchDictation()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
        clearError = { errorMessage = null }
    )
}

private fun mapSpeechError(errorCode: Int): String = when (errorCode) {
    SpeechRecognizer.ERROR_AUDIO -> "Error de captura de audio del micrófono."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono requerido para dictar por voz."
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Se requiere conexión de red o paquete de voz offline para transcribir."
    SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció habla clara. Acerca el micrófono e inténtalo de nuevo."
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera agotado sin detectar voz."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El micrófono está ocupado. Espera un instante e inténtalo de nuevo."
    else -> "No se pudo iniciar el dictado por voz (código $errorCode)."
}
