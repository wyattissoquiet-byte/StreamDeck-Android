package com.example.streamdeckapp.model

import java.util.UUID

enum class ActionType {
    NONE,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREV,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE_TOGGLE,
    NEXT_PAGE,
    PREV_PAGE,
    GOTO_PAGE,
    LAUNCH_APP,
    SHELL_COMMAND
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
    val customImagePath: String = ""
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
