package com.example.streamdeckapp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
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

        fun performGlobal(actionId: Int): Boolean {
            val service = instance ?: return false
            return try {
                val result = service.performGlobalAction(actionId)
                Log.d("StreamDeckAccessibility", "performGlobalAction($actionId) returned $result")
                result
            } catch (e: Exception) {
                Log.e("StreamDeckAccessibility", "Failed to perform global action $actionId", e)
                false
            }
        }

        fun simulateTap(touchX: Float, touchY: Float, displayMetrics: DisplayMetrics): Boolean {
            val actualX = if (touchX in 0f..1f) touchX * displayMetrics.widthPixels else touchX
            val actualY = if (touchY in 0f..1f) touchY * displayMetrics.heightPixels else touchY

            val service = instance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && service != null) {
                return try {
                    val path = Path().apply { moveTo(actualX, actualY) }
                    val stroke = GestureDescription.StrokeDescription(path, 0, 60)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()
                    val dispatched = service.dispatchGesture(gesture, null, null)
                    Log.d("StreamDeckAccessibility", "dispatchGesture tap ($actualX, $actualY): $dispatched")
                    dispatched
                } catch (e: Exception) {
                    Log.e("StreamDeckAccessibility", "dispatchGesture tap failed", e)
                    fallbackShellTap(actualX, actualY)
                }
            }
            return fallbackShellTap(actualX, actualY)
        }

        fun simulateSwipe(
            startX: Float, startY: Float,
            endX: Float, endY: Float,
            durationMs: Long,
            displayMetrics: DisplayMetrics
        ): Boolean {
            val actualStartX = if (startX in 0f..1f) startX * displayMetrics.widthPixels else startX
            val actualStartY = if (startY in 0f..1f) startY * displayMetrics.heightPixels else startY
            val actualEndX = if (endX in 0f..1f) endX * displayMetrics.widthPixels else endX
            val actualEndY = if (endY in 0f..1f) endY * displayMetrics.heightPixels else endY
            val actualDuration = durationMs.coerceIn(50L, 2500L)

            val service = instance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && service != null) {
                return try {
                    val path = Path().apply {
                        moveTo(actualStartX, actualStartY)
                        lineTo(actualEndX, actualEndY)
                    }
                    val stroke = GestureDescription.StrokeDescription(path, 0, actualDuration)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()
                    val dispatched = service.dispatchGesture(gesture, null, null)
                    Log.d("StreamDeckAccessibility", "dispatchGesture swipe ($actualStartX, $actualStartY) -> ($actualEndX, $actualEndY): $dispatched")
                    dispatched
                } catch (e: Exception) {
                    Log.e("StreamDeckAccessibility", "dispatchGesture swipe failed", e)
                    fallbackShellSwipe(actualStartX, actualStartY, actualEndX, actualEndY, actualDuration)
                }
            }
            return fallbackShellSwipe(actualStartX, actualStartY, actualEndX, actualEndY, actualDuration)
        }

        private fun fallbackShellTap(x: Float, y: Float): Boolean {
            return try {
                Runtime.getRuntime().exec(arrayOf("input", "tap", x.toInt().toString(), y.toInt().toString()))
                true
            } catch (e: Exception) {
                Log.w("StreamDeckAccessibility", "Shell tap fallback failed", e)
                false
            }
        }

        private fun fallbackShellSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
            return try {
                Runtime.getRuntime().exec(arrayOf(
                    "input", "swipe",
                    x1.toInt().toString(), y1.toInt().toString(),
                    x2.toInt().toString(), y2.toInt().toString(),
                    duration.toString()
                ))
                true
            } catch (e: Exception) {
                Log.w("StreamDeckAccessibility", "Shell swipe fallback failed", e)
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
