package com.example.streamdeckapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.model.DeckPage

@Composable
fun VirtualGridScreen(
    currentPage: DeckPage?,
    pressedKeyIndex: Int?,
    rotationDegrees: Int = 0,
    onRotateRequested: () -> Unit = {},
    onKeyTrigger: (slotIndex: Int) -> Unit,
    onKeyEditRequested: (slotIndex: Int) -> Unit
) {
    if (currentPage == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
    val numRows = if (isPortrait) 5 else 3
    val numCols = if (isPortrait) 3 else 5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status & Orientation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentPage.name} • 15 Keys",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Rotation chip
            AssistChip(
                onClick = onRotateRequested,
                label = { Text("Rotate: $rotationDegrees°", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }

        // Dynamic Responsive Grid (3x5 or 5x3)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (rowIndex in 0 until numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (colIndex in 0 until numCols) {
                        val slotIndex = rowIndex * numCols + colIndex
                        val action = currentPage.slots.getOrNull(slotIndex) ?: DeckAction()
                        val isPressed = pressedKeyIndex == slotIndex

                        KeySlotTile(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp, max = 100.dp)
                                .aspectRatio(1.1f),
                            slotIndex = slotIndex,
                            action = action,
                            isPressed = isPressed,
                            onClick = { onKeyTrigger(slotIndex) },
                            onLongClick = { onKeyEditRequested(slotIndex) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun KeySlotTile(
    modifier: Modifier = Modifier,
    slotIndex: Int,
    action: DeckAction,
    isPressed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val signatureColor = getActionColor(action.type)
    val baseBgColor = Color(action.backgroundColor.toInt())

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isPressed) Color.White else signatureColor.copy(alpha = 0.7f),
        label = "BorderAnim"
    )

    val labelTextColor = Color(action.labelColor.toInt())

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            baseBgColor.copy(alpha = 0.95f),
            baseBgColor.copy(alpha = 0.65f)
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) signatureColor else Color.Transparent)
            .background(gradientBrush)
            .border(
                width = if (isPressed) 2.5.dp else 1.2.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Action Type Icon Badge
            ColorfulActionIconBadge(action = action, signatureColor = signatureColor)

            Spacer(modifier = Modifier.height(2.dp))

            // Label text
            val displayLabel = when {
                action.label.isNotBlank() -> action.label
                action.type != ActionType.NONE -> action.type.name.replace("_", " ")
                else -> ""
            }

            if (displayLabel.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = displayLabel,
                        color = labelTextColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Small slot index number overlay
        Text(
            text = "${slotIndex + 1}",
            fontSize = 8.5.sp,
            color = Color.White.copy(alpha = 0.45f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
        )

        // Small edit indicator
        IconButton(
            onClick = onLongClick,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Key",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun ColorfulActionIconBadge(action: DeckAction, signatureColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = signatureColor.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(1.dp, signatureColor.copy(alpha = 0.6f)),
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (action.type) {
                // System & Navigation
                ActionType.SYSTEM_HOME -> Icon(Icons.Default.Home, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_BACK -> Icon(Icons.Default.ArrowBack, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_RECENTS -> Icon(Icons.Default.Layers, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_NOTIFICATIONS -> Icon(Icons.Default.Notifications, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_QUICK_SETTINGS -> Icon(Icons.Default.Tune, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_LOCK_SCREEN -> Icon(Icons.Default.Lock, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_POWER_DIALOG -> Icon(Icons.Default.PowerSettingsNew, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_SPLIT_SCREEN -> Icon(Icons.Default.VerticalSplit, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SYSTEM_SCREENSHOT -> Icon(Icons.Default.CameraAlt, null, tint = signatureColor, modifier = Modifier.size(14.dp))

                // Touch & Gestures
                ActionType.SIMULATED_TAP -> Icon(Icons.Default.TouchApp, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SIMULATED_SWIPE -> Text("⇄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = signatureColor)
                ActionType.SIMULATED_SWIPE_UP -> Text("↑", fontSize = 13.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.SIMULATED_SWIPE_DOWN -> Text("↓", fontSize = 13.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.SIMULATED_SWIPE_LEFT -> Text("←", fontSize = 13.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.SIMULATED_SWIPE_RIGHT -> Text("→", fontSize = 13.sp, fontWeight = FontWeight.Black, color = signatureColor)

                // Media & Audio
                ActionType.MEDIA_PLAY_PAUSE -> Icon(Icons.Default.PlayArrow, null, tint = signatureColor, modifier = Modifier.size(16.dp))
                ActionType.MEDIA_PLAY -> Icon(Icons.Default.PlayArrow, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_PAUSE -> Icon(Icons.Default.Pause, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_STOP -> Icon(Icons.Default.Stop, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_NEXT -> Icon(Icons.Default.FastForward, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_PREV -> Icon(Icons.Default.FastRewind, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_FAST_FORWARD -> Icon(Icons.Default.FastForward, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_REWIND -> Icon(Icons.Default.FastRewind, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_UP -> Icon(Icons.Default.VolumeUp, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_DOWN -> Icon(Icons.Default.VolumeDown, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_MUTE_TOGGLE -> Icon(Icons.Default.VolumeOff, null, tint = signatureColor, modifier = Modifier.size(14.dp))

                // Brightness & Display
                ActionType.BRIGHTNESS_UP -> Icon(Icons.Default.BrightnessHigh, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.BRIGHTNESS_DOWN -> Icon(Icons.Default.BrightnessLow, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SCREEN_OFF -> Icon(Icons.Default.Bedtime, null, tint = signatureColor, modifier = Modifier.size(14.dp))

                // Settings Shortcuts
                ActionType.SETTINGS_BLUETOOTH -> Icon(Icons.Default.Bluetooth, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_WIFI -> Icon(Icons.Default.Wifi, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_SOUND -> Icon(Icons.Default.VolumeUp, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_DISPLAY -> Icon(Icons.Default.Tv, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_DATE_TIME -> Icon(Icons.Default.Schedule, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_LOCATION -> Icon(Icons.Default.Place, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_APPS -> Icon(Icons.Default.Apps, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SETTINGS_MAIN -> Icon(Icons.Default.Settings, null, tint = signatureColor, modifier = Modifier.size(14.dp))

                // Tools & Vehicle
                ActionType.TORCH_TOGGLE -> Icon(Icons.Default.FlashlightOn, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.OPEN_URL -> Icon(Icons.Default.Language, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOICE_ASSISTANT -> Icon(Icons.Default.Mic, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.DIAL_PHONE -> Icon(Icons.Default.Call, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.LAUNCH_APP -> Icon(Icons.Default.Apps, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SHELL_COMMAND -> Text(">_", fontSize = 11.sp, fontWeight = FontWeight.Black, color = signatureColor)

                // Page Navigation
                ActionType.NEXT_PAGE -> Icon(Icons.Default.ArrowForward, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.PREV_PAGE -> Icon(Icons.Default.ArrowBack, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.GOTO_PAGE -> Text("${action.targetPageIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.NONE -> Box(modifier = Modifier.size(4.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(50)))
            }
        }
    }
}

fun getActionColor(type: ActionType): Color {
    return when (type) {
        // System & Navigation
        ActionType.SYSTEM_HOME -> Color(0xFF29B6F6)
        ActionType.SYSTEM_BACK -> Color(0xFFAB47BC)
        ActionType.SYSTEM_RECENTS -> Color(0xFF26A69A)
        ActionType.SYSTEM_NOTIFICATIONS -> Color(0xFFFFA726)
        ActionType.SYSTEM_QUICK_SETTINGS -> Color(0xFF7E57C2)
        ActionType.SYSTEM_LOCK_SCREEN -> Color(0xFFEF5350)
        ActionType.SYSTEM_POWER_DIALOG -> Color(0xFFD32F2F)
        ActionType.SYSTEM_SPLIT_SCREEN -> Color(0xFF5C6BC0)
        ActionType.SYSTEM_SCREENSHOT -> Color(0xFFEC407A)

        // Screen Gestures
        ActionType.SIMULATED_TAP -> Color(0xFF00E5FF)
        ActionType.SIMULATED_SWIPE,
        ActionType.SIMULATED_SWIPE_UP,
        ActionType.SIMULATED_SWIPE_DOWN,
        ActionType.SIMULATED_SWIPE_LEFT,
        ActionType.SIMULATED_SWIPE_RIGHT -> Color(0xFF69F0AE)

        // Media & Audio
        ActionType.MEDIA_PLAY_PAUSE -> Color(0xFF00E676)
        ActionType.MEDIA_PLAY -> Color(0xFF00E676)
        ActionType.MEDIA_PAUSE -> Color(0xFFFF9100)
        ActionType.MEDIA_STOP -> Color(0xFFFF1744)
        ActionType.MEDIA_NEXT, ActionType.MEDIA_PREV -> Color(0xFF00E5FF)
        ActionType.MEDIA_FAST_FORWARD, ActionType.MEDIA_REWIND -> Color(0xFF1DE9B6)
        ActionType.VOLUME_UP -> Color(0xFFFFD600)
        ActionType.VOLUME_DOWN -> Color(0xFFFF6D00)
        ActionType.VOLUME_MUTE_TOGGLE -> Color(0xFFFF1744)

        // Brightness & Display
        ActionType.BRIGHTNESS_UP, ActionType.BRIGHTNESS_DOWN -> Color(0xFFFFD700)
        ActionType.SCREEN_OFF -> Color(0xFF78909C)

        // Settings Shortcuts
        ActionType.SETTINGS_BLUETOOTH -> Color(0xFF2979FF)
        ActionType.SETTINGS_WIFI -> Color(0xFF00E676)
        ActionType.SETTINGS_SOUND -> Color(0xFFFF9100)
        ActionType.SETTINGS_DISPLAY -> Color(0xFFFFEA00)
        ActionType.SETTINGS_DATE_TIME -> Color(0xFFAB47BC)
        ActionType.SETTINGS_LOCATION -> Color(0xFFFF5252)
        ActionType.SETTINGS_APPS -> Color(0xFFE040FB)
        ActionType.SETTINGS_MAIN -> Color(0xFF90A4AE)

        // Tools & Vehicle
        ActionType.TORCH_TOGGLE -> Color(0xFFFFF176)
        ActionType.OPEN_URL -> Color(0xFF40C4FF)
        ActionType.VOICE_ASSISTANT -> Color(0xFFEA4335)
        ActionType.DIAL_PHONE -> Color(0xFF34A853)
        ActionType.LAUNCH_APP -> Color(0xFFFF4081)
        ActionType.SHELL_COMMAND -> Color(0xFF00E676)

        // Page Navigation
        ActionType.NEXT_PAGE, ActionType.PREV_PAGE -> Color(0xFFE040FB)
        ActionType.GOTO_PAGE -> Color(0xFF7C4DFF)
        ActionType.NONE -> Color(0xFF78909C)
    }
}
