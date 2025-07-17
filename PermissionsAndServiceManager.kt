package com.miniai.plugin

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.Locale

class PermissionAndServiceManager(private val context: Context) {
    companion object {
        private const val TAG = "PermissionAndServiceMgr"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
    }

    fun checkAndRequestPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty() && context is Activity) {
            ActivityCompat.requestPermissions(context, missing.toTypedArray(), 1001)
        } else {
            broadcastStatus("permissions_granted")
        }
    }

    fun onRequestPermissionsResult(grantResults: IntArray) {
        if (grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            broadcastStatus("permissions_granted")
        } else {
            broadcastStatus("permissions_denied")
        }
    }

    fun startBackgroundService() {
        val intent = Intent(context, MiniAIBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        broadcastStatus("background_service_started")
    }

    private fun broadcastStatus(status: String) {
        val response = JSONObject().apply {
            put("eventType", status)
            put("timestamp", System.currentTimeMillis())
        }
        val intent = Intent("com.miniai.host.RESPONSE")
        intent.putExtra("response", response.toString())
        context.sendBroadcast(intent)
        Log.d(TAG, "Broadcasted status: $status")
    }
}

class MiniAIBackgroundService : Service() {
    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    private var isListeningForWakeWord = true
    private var conversationTimeoutHandler: android.os.Handler? = null
    private var conversationTimeoutRunnable: Runnable? = null
    private val CONVERSATION_TIMEOUT_MS = 20000L // 20 seconds

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startWakeWordListener()
        return START_STICKY
    }

    private fun startWakeWordListener() {
        isListeningForWakeWord = true
        if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this)
            val recognizerIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (isListeningForWakeWord) {
                        if (matches != null && matches.any { it.contains("hey mini", ignoreCase = true) }) {
                            broadcastWakeWordDetected()
                            isListeningForWakeWord = false
                            startConversationMode()
                        } else {
                            startWakeWordListener()
                        }
                    }
                }
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { if (isListeningForWakeWord) startWakeWordListener() }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            speechRecognizer?.startListening(recognizerIntent)
        }
    }

    private fun startConversationMode() {
        startVoiceRecognition()
        conversationTimeoutHandler = android.os.Handler(mainLooper)
        conversationTimeoutRunnable = Runnable {
            isListeningForWakeWord = true
            startWakeWordListener()
            Log.d("MiniAIBackgroundService", "Conversation timeout, returning to wake word mode")
        }
        conversationTimeoutHandler?.postDelayed(conversationTimeoutRunnable!!, CONVERSATION_TIMEOUT_MS)
    }

    private fun startVoiceRecognition() {
        val recognizerIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                val userInput = matches?.firstOrNull() ?: ""
                val aiResponse = processAI(userInput)
                broadcastAIResponse(aiResponse)
                // Reset the timeout for each new command
                conversationTimeoutHandler?.removeCallbacks(conversationTimeoutRunnable!!)
                conversationTimeoutHandler?.postDelayed(conversationTimeoutRunnable!!, CONVERSATION_TIMEOUT_MS)
                startVoiceRecognition() // Keep listening for more commands
            }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // On error, reset timeout and keep listening
                conversationTimeoutHandler?.removeCallbacks(conversationTimeoutRunnable!!)
                conversationTimeoutHandler?.postDelayed(conversationTimeoutRunnable!!, CONVERSATION_TIMEOUT_MS)
                startVoiceRecognition()
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun processAI(input: String): String {
        // deploy engagement.json from the directory files stored under assets/plugin_assets/engagement.json
        return when {
            input.contains("weather", ignoreCase = true) -> "The weather is sunny."
            input.contains("hello", ignoreCase = true) -> "Hello! How can I help you?"
            input.isNotBlank() -> "You said: $input"
            else -> "Sorry, I didn't understand that."
        }
    }

    private fun broadcastWakeWordDetected() {
        val response = JSONObject().apply {
            put("eventType", "wake_word_detected")
            put("timestamp", System.currentTimeMillis())
        }
        val intent = Intent("com.miniai.host.RESPONSE")
        intent.putExtra("response", response.toString())
        sendBroadcast(intent)
        Log.d("MiniAIBackgroundService", "Wake word detected and broadcasted")
    }

    private fun broadcastAIResponse(response: String) {
        // Speak the response aloud
        speakResponse(response, TextToSpeech.QUEUE_FLUSH, null, null)
        val json = JSONObject().apply {
            put("eventType", "ai_response")
            put("data", response)
            put("timestamp", System.currentTimeMillis())
        }
        val intent = Intent("com.miniai.host.RESPONSE")
        intent.putExtra("response", json.toString())
        sendBroadcast(intent)
        Log.d("MiniAIBackgroundService", "AI response broadcasted: $response")
    }

    private fun speakResponse(text: String, queueMode: Int, params: android.os.Bundle?, utteranceId: String?) {
        TextToSpeech(this@MiniAIBackgroundService, object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {
                handleTtsInit(status, text, queueMode, params, utteranceId, this)
            }
        })
    }

    private fun handleTtsInit(
        status: Int,
        text: String,
        queueMode: Int,
        params: android.os.Bundle?,
        utteranceId: String?,
        listener: TextToSpeech.OnInitListener
    ) {
        if (status == TextToSpeech.SUCCESS && listener is TextToSpeech) {
            listener.language = Locale.getDefault()
            listener.speak(text, queueMode, params, utteranceId)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
