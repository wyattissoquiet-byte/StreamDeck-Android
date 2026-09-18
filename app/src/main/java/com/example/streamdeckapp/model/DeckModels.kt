package com.example.streamdeckapp.model

import java.util.UUID

enum class ActionType {
    NONE,

    // System & Navigation
    SYSTEM_HOME,
    SYSTEM_BACK,
    SYSTEM_RECENTS,
    SYSTEM_NOTIFICATIONS,
    SYSTEM_QUICK_SETTINGS,
    SYSTEM_LOCK_SCREEN,
    SYSTEM_POWER_DIALOG,
    SYSTEM_SPLIT_SCREEN,
    SYSTEM_SCREENSHOT,

    // Screen Touch & Gesture Automation
    SIMULATED_TAP,
    SIMULATED_SWIPE,
    SIMULATED_SWIPE_UP,
    SIMULATED_SWIPE_DOWN,
    SIMULATED_SWIPE_LEFT,
    SIMULATED_SWIPE_RIGHT,

    // Media & Audio Controls
    MEDIA_PLAY_PAUSE,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_STOP,
    MEDIA_NEXT,
    MEDIA_PREV,
    MEDIA_FAST_FORWARD,
    MEDIA_REWIND,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE_TOGGLE,

    // Brightness & Display
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    SCREEN_OFF,

    // Settings Shortcuts
    SETTINGS_BLUETOOTH,
    SETTINGS_WIFI,
    SETTINGS_SOUND,
    SETTINGS_DISPLAY,
    SETTINGS_DATE_TIME,
    SETTINGS_LOCATION,
    SETTINGS_APPS,
    SETTINGS_MAIN,

    // Tools & Vehicle
    TORCH_TOGGLE,
    OPEN_URL,
    VOICE_ASSISTANT,
    DIAL_PHONE,
    LAUNCH_APP,
    SHELL_COMMAND,

    // Page Navigation
    NEXT_PAGE,
    PREV_PAGE,
    GOTO_PAGE
}

data class DeckAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType = ActionType.NONE,
    val label: String = "",
    val labelColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0xFF1C1C24,
    val iconName: String = "",
    val targetPackageName: String = "",
    val targetAppName: String = "",
    val shellCommand: String = "",
    val targetPageIndex: Int = 0,
    val customImagePath: String = "",
    val extraData: String = "",
    val touchX: Float = 0.5f,
    val touchY: Float = 0.5f,
    val touchEndX: Float = 0.5f,
    val touchEndY: Float = 0.5f,
    val swipeDurationMs: Long = 300L
)

data class DeckPage(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Page 1",
    val slots: MutableList<DeckAction> = MutableList(15) { DeckAction() }
)

data class DeckProfile(
    val id: String = "default_profile",
    val name: String = "Default Profile",
    val pages: MutableList<DeckPage> = mutableListOf(),
    var activePageIndex: Int = 0,
    var rotationDegrees: Int = 0 // 0, 90, 180, 270
) {
    companion object {
        fun mapPhysicalToLogical(physicalIndex: Int, rotationDegrees: Int): Int {
            if (physicalIndex !in 0..14) return physicalIndex
            val r = physicalIndex / 5
            val c = physicalIndex % 5
            return when (rotationDegrees) {
                90 -> (c * 3 + (2 - r)).coerceIn(0, 14)
                180 -> 14 - physicalIndex
                270 -> ((4 - c) * 3 + r).coerceIn(0, 14)
                else -> physicalIndex
            }
        }

        fun mapLogicalToPhysical(logicalIndex: Int, rotationDegrees: Int): Int {
            if (logicalIndex !in 0..14) return logicalIndex
            return when (rotationDegrees) {
                90 -> {
                    val c = logicalIndex / 3
                    val r = 2 - (logicalIndex % 3)
                    (r * 5 + c).coerceIn(0, 14)
                }
                180 -> 14 - logicalIndex
                270 -> {
                    val c = 4 - (logicalIndex / 3)
                    val r = logicalIndex % 3
                    (r * 5 + c).coerceIn(0, 14)
                }
                else -> logicalIndex
            }
        }
    }
}
