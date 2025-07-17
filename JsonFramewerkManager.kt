package com.miniai.plugin

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import kotlin.random.Random

object JsonFramewerkManager {
    private const val TAG = "JsonFramewerkManager"
    private val eventQueue = mutableListOf<JSONObject>()
    private val queueMutex = Mutex()
    private val activeEvents = mutableMapOf<String, JSONObject>()
    private val eventsMutex = Mutex()
    private var isProcessing = true
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var processingJob: Job? = null
    private const val MAX_RETRIES = 3

    fun addEvent(event: JSONObject) {
        if (!event.has("eventType")) {
            Log.e(TAG, "Event missing eventType: $event")
            return
        }
        coroutineScope.launch {
            queueMutex.withLock {
                eventQueue.add(event)
            }
            Log.d(TAG, "Event added to queue: ${event.optString("eventType")}")
            processEventQueue()
        }
    }

    private fun processEventQueue() {
        if (processingJob?.isActive == true) return
        processingJob = coroutineScope.launch {
            while (isProcessing && queueMutex.withLock { eventQueue.isNotEmpty() }) {
                val event = queueMutex.withLock { eventQueue.removeFirstOrNull() } ?: continue
                val eventId = event.optString("eventId", "${Random.nextLong()}-${System.nanoTime()}")
                val retryCount = event.optInt("retryCount", 0)

                eventsMutex.withLock {
                    activeEvents[eventId] = event
                }

                try {
                    when (event.optString("eventType")) {
                        "initialize_app" -> handleInitialization(event)
                        "json_update" -> handleJsonUpdate(event)
                        "ai_command" -> handleAICommand(event)
                        "mini_ai_bridge_init" -> handleMiniAIBridge(event)
                        "voice_input" -> handleVoiceInput(event)
                        "pop_up_window" -> handlePopUpWindow(event)
                        else -> handleGenericEvent(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing event $eventId: ${e.message}")
                    if (retryCount < MAX_RETRIES) {
                        event.put("retryCount", retryCount + 1)
                        queueMutex.withLock { eventQueue.add(event) }
                        Log.d(TAG, "Event $eventId re-queued for retry ${retryCount + 1}")
                    } else {
                        Log.e(TAG, "Event $eventId failed after $MAX_RETRIES retries")
                    }
                } finally {
                    eventsMutex.withLock {
                        activeEvents.remove(eventId)
                    }
                }
            }
        }
    }

    private fun handleInitialization(event: JSONObject) {
        val data = event.optJSONObject("data")
        Log.d(TAG, "Handling initialization: $data")
        data?.let {
            val appName = it.optString("name")
            val version = it.optString("version")
            val platform = it.optString("platform")
            Log.d(TAG, "App: $appName, Version: $version, Platform: $platform")
            // Route to mini_ai_bridge for JasonViewActivity
            val bridgeEvent = JSONObject().apply {
                put("eventType", "mini_ai_bridge_init")
                put("data", data)
            }
            addEvent(bridgeEvent)
        }
    }

    private fun handleJsonUpdate(event: JSONObject) {
        Log.d(TAG, "Processing JSON update: ${event.optJSONObject("data")}")
    }

    private fun handleAICommand(event: JSONObject) {
        Log.d(TAG, "Executing AI command: ${event.optJSONObject("data")}")
    }

    private fun handleMiniAIBridge(event: JSONObject) {
        Log.d(TAG, "Routing to mini_ai_bridge: $event")
        // Mini AI renders JasonViewActivity UI
    }

    private fun handleVoiceInput(event: JSONObject) {
        Log.d(TAG, "Processing voice input: ${event.optJSONObject("data")}")
        // Mini AI handles voice data
    }

    private fun handlePopUpWindow(event: JSONObject) {
        Log.d(TAG, "Managing pop-up window: ${event.optJSONObject("data")}")
        // Mini AI controls visibility and content
    }

    private fun handleGenericEvent(event: JSONObject) {
        Log.d(TAG, "Processing generic event: $event")
    }

    fun shutdown() {
        isProcessing = false
        coroutineScope.cancel()
        processingJob = null
        Log.d(TAG, "JsonFramewerkManager shut down")
    }

    fun resume() {
        isProcessing = true
        processEventQueue()
        Log.d(TAG, "JsonFramewerkManager resumed")
    }

    fun getActiveEvents(): Map<String, JSONObject> {
        return activeEvents.toMap()
    }

    fun configure(config: JSONObject) {
        Log.d(TAG, "Configuring with: $config")
        // Mini AI parses plugins (e.g., mini_ai_bridge)
    }
}
