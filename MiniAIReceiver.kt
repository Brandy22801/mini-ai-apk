package com.miniai.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

class MiniAIReceiver: BroadcastReceiver() {
    companion object {
        private const val TAG = "MiniAIReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received event: ${intent.action}")

        // Corrected typo: com.miniai.Plugin.EVENT -> com.miniai.plugin.EVENT
        if (intent.action == "com.miniai.plugin.EVENT") {
            val eventJson = intent.getStringExtra("event") ?: return
            val event = JSONObject(eventJson)

            // Process the event using JsonFramewerkManager
            JsonFramewerkManager.addEvent(event)

            // Send response back to host app
            // Corrected typo: responseïntent -> responseIntent
            val responseIntent = Intent("com.miniai.host.RESPONSE")
            responseIntent.putExtra("response", "Event processed")
            // Corrected typo: caller_pack age -> caller_package
            responseIntent.setPackage(intent.getStringExtra("caller_package"))
            context.sendBroadcast(responseIntent)
        }
    }
}



