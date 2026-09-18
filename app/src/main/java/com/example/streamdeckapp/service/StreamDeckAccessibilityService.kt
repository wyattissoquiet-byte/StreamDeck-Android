package com.example.streamdeckapp.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class StreamDeckAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: StreamDeckAccessibilityService? = null
            private set

        fun isAccessibilityServiceEnabled(): Boolean = instance != null

        fun checkPermission(context: Context): Boolean {
            if (instance != null) return true
            return try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.contains(context.packageName, ignoreCase = true)) {
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }

        fun launchAppFromBackground(intent: Intent): Boolean {
            val service = instance ?: return false
            return try {
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                service.startActivity(intent)
                Log.d("StreamDeckAccessibility", "Successfully launched app via AccessibilityService")
                true
            } catch (e: Exception) {
                Log.e("StreamDeckAccessibility", "Failed to launch app via AccessibilityService", e)
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("StreamDeckAccessibility", "StreamDeckAccessibilityService connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive service used for privileged background app launching
    }

    override fun onInterrupt() {
        Log.d("StreamDeckAccessibility", "StreamDeckAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
