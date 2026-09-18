package com.example.streamdeckapp.engine

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction

class ActionHandler(
    private val context: Context,
    private val onPageNavigationRequested: (targetPageIndex: Int?, isRelativeNext: Boolean?, isRelativePrev: Boolean?) -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    fun executeAction(action: DeckAction) {
        try {
            when (action.type) {
                ActionType.MEDIA_PLAY_PAUSE -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
                ActionType.MEDIA_NEXT -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                }
                ActionType.MEDIA_PREV -> {
                    sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
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
                ActionType.NEXT_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(null, true, null) }
                }
                ActionType.PREV_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(null, null, true) }
                }
                ActionType.GOTO_PAGE -> {
                    mainHandler.post { onPageNavigationRequested(action.targetPageIndex, null, null) }
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
            showToast("Failed to launch app: ${e.message}")
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
