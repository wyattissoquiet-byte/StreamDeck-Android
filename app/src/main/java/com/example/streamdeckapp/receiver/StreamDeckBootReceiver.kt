package com.example.streamdeckapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.streamdeckapp.service.StreamDeckService

class StreamDeckBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("StreamDeckBootReceiver", "Boot completed, starting StreamDeckService in background")
            try {
                val serviceIntent = Intent(context, StreamDeckService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("StreamDeckBootReceiver", "Failed starting StreamDeckService on boot", e)
            }
        }
    }
}
