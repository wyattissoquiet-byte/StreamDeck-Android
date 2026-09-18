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
                ActionType.MEDIA_PLAY_PAUSE -> Icon(Icons.Default.PlayArrow, null, tint = signatureColor, modifier = Modifier.size(16.dp))
                ActionType.MEDIA_NEXT -> Icon(Icons.Default.FastForward, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.MEDIA_PREV -> Icon(Icons.Default.FastRewind, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_UP -> Icon(Icons.Default.VolumeUp, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_DOWN -> Icon(Icons.Default.VolumeDown, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.VOLUME_MUTE_TOGGLE -> Icon(Icons.Default.VolumeOff, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.NEXT_PAGE -> Icon(Icons.Default.ArrowForward, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.PREV_PAGE -> Icon(Icons.Default.ArrowBack, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.GOTO_PAGE -> Text("${action.targetPageIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.LAUNCH_APP -> Icon(Icons.Default.Apps, null, tint = signatureColor, modifier = Modifier.size(14.dp))
                ActionType.SHELL_COMMAND -> Text(">_", fontSize = 11.sp, fontWeight = FontWeight.Black, color = signatureColor)
                ActionType.NONE -> Box(modifier = Modifier.size(4.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(50)))
            }
        }
    }
}

fun getActionColor(type: ActionType): Color {
    return when (type) {
        ActionType.MEDIA_PLAY_PAUSE -> Color(0xFF00E676)       // Bright Emerald
        ActionType.MEDIA_NEXT, ActionType.MEDIA_PREV -> Color(0xFF00E5FF) // Electric Cyan
        ActionType.VOLUME_UP -> Color(0xFFFFD600)              // Radiant Gold
        ActionType.VOLUME_DOWN -> Color(0xFFFF6D00)            // Deep Coral
        ActionType.VOLUME_MUTE_TOGGLE -> Color(0xFFFF1744)     // Neon Red
        ActionType.NEXT_PAGE, ActionType.PREV_PAGE -> Color(0xFFE040FB) // Electric Magenta
        ActionType.GOTO_PAGE -> Color(0xFF7C4DFF)              // Royal Purple
        ActionType.LAUNCH_APP -> Color(0xFFFF4081)             // Neon Pink
        ActionType.SHELL_COMMAND -> Color(0xFF00E676)          // Matrix Green
        ActionType.NONE -> Color(0xFF78909C)                   // Slate
    }
}
