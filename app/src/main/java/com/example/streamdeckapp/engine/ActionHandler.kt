package com.example.streamdeckapp.engine
 
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.service.StreamDeckAccessibilityService

class ActionHandler(
    private val context: Context,
    private val onPageNavigationRequested: (targetPageIndex: Int?, isRelativeNext: Boolean?, isRelativePrev: Boolean?) -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTorchOn = false

    private val displayMetrics: DisplayMetrics
        get() = context.resources.displayMetrics

    fun executeAction(action: DeckAction) {
        try {
            when (action.type) {
                // System & Navigation
                ActionType.SYSTEM_HOME -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_HOME, KeyEvent.KEYCODE_HOME)
                }
                ActionType.SYSTEM_BACK -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_BACK, KeyEvent.KEYCODE_BACK)
                }
                ActionType.SYSTEM_RECENTS -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_RECENTS, KeyEvent.KEYCODE_APP_SWITCH)
                }
                ActionType.SYSTEM_NOTIFICATIONS -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                }
                ActionType.SYSTEM_QUICK_SETTINGS -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                }
                ActionType.SYSTEM_LOCK_SCREEN -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, KeyEvent.KEYCODE_POWER)
                }
                ActionType.SYSTEM_POWER_DIALOG -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
                }
                ActionType.SYSTEM_SPLIT_SCREEN -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
                ActionType.SYSTEM_SCREENSHOT -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                }

                // Touch & Gesture Simulation
                ActionType.SIMULATED_TAP -> {
                    StreamDeckAccessibilityService.simulateTap(action.touchX, action.touchY, displayMetrics)
                }
                ActionType.SIMULATED_SWIPE -> {
                    StreamDeckAccessibilityService.simulateSwipe(
                        action.touchX, action.touchY,
                        action.touchEndX, action.touchEndY,
                        action.swipeDurationMs,
                        displayMetrics
                    )
                }
                ActionType.SIMULATED_SWIPE_UP -> {
                    StreamDeckAccessibilityService.simulateSwipe(0.5f, 0.75f, 0.5f, 0.25f, 300L, displayMetrics)
                }
                ActionType.SIMULATED_SWIPE_DOWN -> {
                    StreamDeckAccessibilityService.simulateSwipe(0.5f, 0.25f, 0.5f, 0.75f, 300L, displayMetrics)
                }
                ActionType.SIMULATED_SWIPE_LEFT -> {
                    StreamDeckAccessibilityService.simulateSwipe(0.8f, 0.5f, 0.2f, 0.5f, 300L, displayMetrics)
                }
                ActionType.SIMULATED_SWIPE_RIGHT -> {
                    StreamDeckAccessibilityService.simulateSwipe(0.2f, 0.5f, 0.8f, 0.5f, 300L, displayMetrics)
                }

                // Media & Audio Controls
                ActionType.MEDIA_PLAY_PAUSE -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
                ActionType.MEDIA_PLAY -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
                }
                ActionType.MEDIA_PAUSE -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
                }
                ActionType.MEDIA_STOP -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
                }
                ActionType.MEDIA_NEXT -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                }
                ActionType.MEDIA_PREV -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
                ActionType.MEDIA_FAST_FORWARD -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                }
                ActionType.MEDIA_REWIND -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND)
                }
                ActionType.VOLUME_UP -> {
                    try {
                        audioManager?.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    } catch (e: Exception) {
                        Log.e("ActionHandler", "Error volume up", e)
                    }
                }
                ActionType.VOLUME_DOWN -> {
                    try {
                        audioManager?.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                    } catch (e: Exception) {
                        Log.e("ActionHandler", "Error volume down", e)
                    }
                }
                ActionType.VOLUME_MUTE_TOGGLE -> {
                    try {
                        audioManager?.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE,
                            AudioManager.FLAG_SHOW_UI
                        )
                    } catch (e: Exception) {
                        Log.e("ActionHandler", "Error mute toggle", e)
                    }
                }

                // Brightness & Display
                ActionType.BRIGHTNESS_UP -> {
                    adjustBrightness(true)
                }
                ActionType.BRIGHTNESS_DOWN -> {
                    adjustBrightness(false)
                }
                ActionType.SCREEN_OFF -> {
                    performSystemGlobal(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, KeyEvent.KEYCODE_POWER)
                }

                // Settings Shortcuts
                ActionType.SETTINGS_BLUETOOTH -> openSettingsScreen(Settings.ACTION_BLUETOOTH_SETTINGS)
                ActionType.SETTINGS_WIFI -> openSettingsScreen(Settings.ACTION_WIFI_SETTINGS)
                ActionType.SETTINGS_SOUND -> openSettingsScreen(Settings.ACTION_SOUND_SETTINGS)
                ActionType.SETTINGS_DISPLAY -> openSettingsScreen(Settings.ACTION_DISPLAY_SETTINGS)
                ActionType.SETTINGS_DATE_TIME -> openSettingsScreen(Settings.ACTION_DATE_SETTINGS)
                ActionType.SETTINGS_LOCATION -> openSettingsScreen(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                ActionType.SETTINGS_APPS -> openSettingsScreen(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                ActionType.SETTINGS_MAIN -> openSettingsScreen(Settings.ACTION_SETTINGS)

                // Tools & Vehicle
                ActionType.TORCH_TOGGLE -> {
                    toggleTorch()
                }
                ActionType.OPEN_URL -> {
                    openBrowserUrl(action.extraData)
                }
                ActionType.VOICE_ASSISTANT -> {
                    triggerVoiceAssistant()
                }
                ActionType.DIAL_PHONE -> {
                    openDialer(action.extraData)
                }
                ActionType.LAUNCH_APP -> {
                    if (action.targetPackageName.isNotBlank()) {
                        launchAppFromBackground(action.targetPackageName, action.targetAppName)
                    }
                }
                ActionType.SHELL_COMMAND -> {
                    if (action.shellCommand.isNotBlank()) {
                        try {
                            Runtime.getRuntime().exec(action.shellCommand)
                            showToast("Executed: ${action.shellCommand}")
                        } catch (e: Exception) {
                            showToast("Command failed: ${e.message}")
                        }
                    }
                }

                // Page Navigation
                ActionType.NEXT_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(null, true, null) }
                }
                ActionType.PREV_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(null, null, true) }
                }
                ActionType.GOTO_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(action.targetPageIndex, null, null) }
                }
                ActionType.NONE -> {
                    // No action
                }
            }
        } catch (e: Throwable) {
            Log.e("ActionHandler", "Failed executing action ${action.type}", e)
        }
    }

    private fun sendKeyEvent(keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
        } catch (e: Exception) {
            Log.e("ActionHandler", "Error sending media key event $keyCode", e)
        }
    }

    private fun launchAppFromBackground(packageName: String, appName: String) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // Method 0: AccessibilityService background launch (completely exempt from Android background activity launch restrictions)
                if (StreamDeckAccessibilityService.isAccessibilityServiceEnabled()) {
                    val launched = StreamDeckAccessibilityService.launchAppFromBackground(launchIntent)
                    if (launched) {
                        return
                    }
                }

                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )

                // Method 1: PendingIntent send (bypasses some background restrictions)
                try {
                    val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(context, (System.currentTimeMillis() % 10000).toInt(), launchIntent, flags)
                    pendingIntent.send()
                    return
                } catch (e: Exception) {
                    Log.w("ActionHandler", "PendingIntent send failed, trying startActivity", e)
                }

                // Method 2: Direct startActivity
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w("ActionHandler", "startActivity failed, falling back to shell", e)
                }
            }

            // Method 3: Shell command fallback for Android Car Stereos
            try {
                Runtime.getRuntime().exec(arrayOf("monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1"))
            } catch (e: Exception) {
                showToast("Cannot launch: $appName")
            }
        } catch (e: Exception) {
            Log.e("ActionHandler", "Failed to launch app $packageName", e)
        }
    }

    private fun performSystemGlobal(actionId: Int, fallbackKeyCode: Int? = null) {
        if (StreamDeckAccessibilityService.performGlobal(actionId)) {
            return
        }
        if (fallbackKeyCode != null) {
            try {
                Runtime.getRuntime().exec(arrayOf("input", "keyevent", fallbackKeyCode.toString()))
            } catch (e: Exception) {
                Log.w("ActionHandler", "Fallback keyevent $fallbackKeyCode failed", e)
            }
        }
    }

    private fun adjustBrightness(increase: Boolean) {
        try {
            val cr = context.contentResolver
            val cur = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
            val delta = 30
            val next = if (increase) minOf(255, cur + delta) else maxOf(10, cur - delta)
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, next)
            } else {
                val keycode = if (increase) 221 else 220
                Runtime.getRuntime().exec(arrayOf("input", "keyevent", keycode.toString()))
            }
        } catch (e: Exception) {
            val keycode = if (increase) 221 else 220
            try {
                Runtime.getRuntime().exec(arrayOf("input", "keyevent", keycode.toString()))
            } catch (e2: Exception) {
                Log.w("ActionHandler", "Brightness adjustment failed", e2)
            }
        }
    }

    private fun toggleTorch() {
        try {
            val cm = cameraManager ?: return
            val id = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cm.cameraIdList.firstOrNull() ?: return
            isTorchOn = !isTorchOn
            cm.setTorchMode(id, isTorchOn)
        } catch (e: Exception) {
            Log.w("ActionHandler", "Toggle torch failed", e)
        }
    }

    private fun openSettingsScreen(action: String) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (StreamDeckAccessibilityService.isAccessibilityServiceEnabled()) {
            StreamDeckAccessibilityService.launchAppFromBackground(intent)
        } else {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w("ActionHandler", "Cannot open settings: $action", e)
            }
        }
    }

    private fun openBrowserUrl(rawUrl: String) {
        val url = if (rawUrl.isNotBlank()) rawUrl.trim() else "https://google.com"
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (StreamDeckAccessibilityService.isAccessibilityServiceEnabled()) {
            StreamDeckAccessibilityService.launchAppFromBackground(intent)
        } else {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                showToast("Cannot open URL")
            }
        }
    }

    private fun triggerVoiceAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (StreamDeckAccessibilityService.isAccessibilityServiceEnabled()) {
            StreamDeckAccessibilityService.launchAppFromBackground(intent)
        } else {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val searchIntent = Intent("android.intent.action.WEB_SEARCH").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(searchIntent) } catch (e2: Exception) {}
            }
        }
    }

    private fun openDialer(phoneNum: String) {
        val num = phoneNum.trim()
        val uri = if (num.isNotBlank()) Uri.parse("tel:$num") else Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (StreamDeckAccessibilityService.isAccessibilityServiceEnabled()) {
            StreamDeckAccessibilityService.launchAppFromBackground(intent)
        } else {
            try { context.startActivity(intent) } catch (e: Exception) {}
        }
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            try {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isAudioPlaying(): Boolean {
        return try {
            audioManager?.isMusicActive ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun isAudioMuted(): Boolean {
        return try {
            audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
