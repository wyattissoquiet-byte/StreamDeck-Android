package com.example.streamdeckapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.service.StreamDeckAccessibilityService
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenTouchMapperDialog(
    initialType: ActionType,
    initialTouchX: Float,
    initialTouchY: Float,
    initialTouchEndX: Float,
    initialTouchEndY: Float,
    initialDurationMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (type: ActionType, x: Float, y: Float, endX: Float, endY: Float, durationMs: Long, suggestedLabel: String) -> Unit
) {
    val context = LocalContext.current
    val displayMetrics = remember { context.resources.displayMetrics }

    val isInitialSwipe = initialType in listOf(
        ActionType.SIMULATED_SWIPE,
        ActionType.SIMULATED_SWIPE_UP,
        ActionType.SIMULATED_SWIPE_DOWN,
        ActionType.SIMULATED_SWIPE_LEFT,
        ActionType.SIMULATED_SWIPE_RIGHT
    )

    var isSwipeMode by remember { mutableStateOf(isInitialSwipe) }
    var touchX by remember { mutableStateOf(if (initialTouchX > 0f) initialTouchX else 0.5f) }
    var touchY by remember { mutableStateOf(if (initialTouchY > 0f) initialTouchY else 0.5f) }
    var touchEndX by remember { mutableStateOf(if (initialTouchEndX > 0f) initialTouchEndX else 0.5f) }
    var touchEndY by remember { mutableStateOf(if (initialTouchEndY > 0f) initialTouchEndY else 0.25f) }
    var durationMs by remember { mutableStateOf(if (initialDurationMs > 0L) initialDurationMs else 300L) }

    // Pulsing reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
        ) {
            // Interactive Gesture Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isSwipeMode) {
                        if (isSwipeMode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w > 0 && h > 0) {
                                        touchX = (offset.x / w).coerceIn(0.01f, 0.99f)
                                        touchY = (offset.y / h).coerceIn(0.01f, 0.99f)
                                        touchEndX = touchX
                                        touchEndY = touchY
                                    }
                                },
                                onDrag = { change, _ ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w > 0 && h > 0) {
                                        touchEndX = (change.position.x / w).coerceIn(0.01f, 0.99f)
                                        touchEndY = (change.position.y / h).coerceIn(0.01f, 0.99f)
                                    }
                                }
                            )
                        } else {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w > 0 && h > 0) {
                                    touchX = (offset.x / w).coerceIn(0.01f, 0.99f)
                                    touchY = (offset.y / h).coerceIn(0.01f, 0.99f)
                                }
                            }
                        }
                    }
            ) {
                // Background visual alignment grid & targets
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Subtle Alignment Grid Lines (every 20%)
                    for (i in 1..4) {
                        val gx = w * (i * 0.2f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 1..4) {
                        val gy = h * (i * 0.2f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1f
                        )
                    }

                    if (!isSwipeMode) {
                        // TAP RETICLE
                        val cx = touchX * w
                        val cy = touchY * h

                        // Outer pulsing halo
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                            radius = 42f * pulseScale,
                            center = Offset(cx, cy)
                        )
                        // Inner reticle ring
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 24f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.5f)
                        )
                        // Crosshair hairs
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx - 36f, cy),
                            end = Offset(cx - 10f, cy),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx + 10f, cy),
                            end = Offset(cx + 36f, cy),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx, cy - 36f),
                            end = Offset(cx, cy - 10f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(cx, cy + 10f),
                            end = Offset(cx, cy + 36f),
                            strokeWidth = 3f
                        )
                        // Center bullseye
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(cx, cy)
                        )
                    } else {
                        // SWIPE PATH ARROW
                        val sx = touchX * w
                        val sy = touchY * h
                        val ex = touchEndX * w
                        val ey = touchEndY * h

                        // Start Point Indicator
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 18f,
                            center = Offset(sx, sy)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 6f,
                            center = Offset(sx, sy)
                        )

                        // Path Line
                        drawLine(
                            color = Color(0xFF69F0AE),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 6f
                        )

                        // End Arrowhead
                        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                        val arrowLen = 32f
                        val p1 = Offset(
                            (ex - arrowLen * cos(angle - Math.PI / 6)).toFloat(),
                            (ey - arrowLen * sin(angle - Math.PI / 6)).toFloat()
                        )
                        val p2 = Offset(
                            (ex - arrowLen * cos(angle + Math.PI / 6)).toFloat(),
                            (ey - arrowLen * sin(angle + Math.PI / 6)).toFloat()
                        )

                        val arrowPath = Path().apply {
                            moveTo(ex, ey)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            close()
                        }
                        drawPath(arrowPath, Color(0xFF00E5FF))

                        // End Target Ring
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 16f,
                            center = Offset(ex, ey),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            // ==========================================
            // TOP HUD: Mode Selector & Title
            // ==========================================
            Surface(
                color = Color(0xFF141620).copy(alpha = 0.95f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3048)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSwipeMode) "🎯 Screen Swipe Mapper" else "🎯 Screen Tap Mapper",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isSwipeMode) "Drag across screen to map swipe path" else "Tap anywhere on screen to place target",
                            fontSize = 11.sp,
                            color = Color(0xFF90CAF9)
                        )
                    }

                    // Mode Toggle Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isSwipeMode,
                            onClick = { isSwipeMode = false },
                            label = { Text("Tap Mode", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = isSwipeMode,
                            onClick = { isSwipeMode = true },
                            label = { Text("Swipe Mode", fontSize = 11.sp) },
                            leadingIcon = { Text("⇄", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // ==========================================
            // BOTTOM HUD: Coordinates, Presets & Action Buttons
            // ==========================================
            Surface(
                color = Color(0xFF141620).copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3048)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Coordinates & Readout Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF0D0F17),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E44)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isSwipeMode) {
                                    val pxX = (touchX * displayMetrics.widthPixels).toInt()
                                    val pxY = (touchY * displayMetrics.heightPixels).toInt()
                                    Text(
                                        text = "TARGET: X=${pxX}px (${(touchX * 100).toInt()}%), Y=${pxY}px (${(touchY * 100).toInt()}%)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                } else {
                                    val sx = (touchX * displayMetrics.widthPixels).toInt()
                                    val sy = (touchY * displayMetrics.heightPixels).toInt()
                                    val ex = (touchEndX * displayMetrics.widthPixels).toInt()
                                    val ey = (touchEndY * displayMetrics.heightPixels).toInt()
                                    Text(
                                        text = "SWIPE: ($sx, $sy) ➔ ($ex, $ey)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF69F0AE)
                                    )
                                }
                            }
                        }

                        // Instant Live Test Button
                        Button(
                            onClick = {
                                if (!isSwipeMode) {
                                    StreamDeckAccessibilityService.simulateTap(touchX, touchY, displayMetrics)
                                } else {
                                    StreamDeckAccessibilityService.simulateSwipe(
                                        touchX, touchY,
                                        touchEndX, touchEndY,
                                        durationMs,
                                        displayMetrics
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF263238),
                                contentColor = Color(0xFF00E5FF)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Test", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test on Device", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Area Presets Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PRESETS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E95AA))

                        if (!isSwipeMode) {
                            PresetChip("Center") { touchX = 0.5f; touchY = 0.5f }
                            PresetChip("Top-Left") { touchX = 0.15f; touchY = 0.15f }
                            PresetChip("Top-Right") { touchX = 0.85f; touchY = 0.15f }
                            PresetChip("Bottom-Left") { touchX = 0.15f; touchY = 0.85f }
                            PresetChip("Bottom-Right") { touchX = 0.85f; touchY = 0.85f }
                        } else {
                            PresetChip("↑ Swipe Up") { touchX = 0.5f; touchY = 0.75f; touchEndX = 0.5f; touchEndY = 0.25f }
                            PresetChip("↓ Swipe Down") { touchX = 0.5f; touchY = 0.25f; touchEndX = 0.5f; touchEndY = 0.75f }
                            PresetChip("← Swipe Left") { touchX = 0.8f; touchY = 0.5f; touchEndX = 0.2f; touchEndY = 0.5f }
                            PresetChip("→ Swipe Right") { touchX = 0.2f; touchY = 0.5f; touchEndX = 0.8f; touchEndY = 0.5f }
                        }
                    }

                    // Bottom Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                val resolvedType = if (!isSwipeMode) {
                                    ActionType.SIMULATED_TAP
                                } else {
                                    // Detect if matches clean preset
                                    when {
                                        touchX in 0.45f..0.55f && touchEndX in 0.45f..0.55f && touchEndY < touchY -> ActionType.SIMULATED_SWIPE_UP
                                        touchX in 0.45f..0.55f && touchEndX in 0.45f..0.55f && touchEndY > touchY -> ActionType.SIMULATED_SWIPE_DOWN
                                        touchY in 0.45f..0.55f && touchEndY in 0.45f..0.55f && touchEndX < touchX -> ActionType.SIMULATED_SWIPE_LEFT
                                        touchY in 0.45f..0.55f && touchEndY in 0.45f..0.55f && touchEndX > touchX -> ActionType.SIMULATED_SWIPE_RIGHT
                                        else -> ActionType.SIMULATED_SWIPE
                                    }
                                }

                                val suggestedLabel = if (!isSwipeMode) {
                                    "TAP ${(touchX * 100).toInt()}%"
                                } else {
                                    when (resolvedType) {
                                        ActionType.SIMULATED_SWIPE_UP -> "SWIPE UP"
                                        ActionType.SIMULATED_SWIPE_DOWN -> "SWIPE DN"
                                        ActionType.SIMULATED_SWIPE_LEFT -> "SWIPE LT"
                                        ActionType.SIMULATED_SWIPE_RIGHT -> "SWIPE RT"
                                        else -> "SWIPE"
                                    }
                                }

                                onConfirm(resolvedType, touchX, touchY, touchEndX, touchEndY, durationMs, suggestedLabel)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Apply Target", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(name: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1E2130),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333852)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures { onClick() }
                }
        ) {
            Text(name, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCFD8DC))
        }
    }
}
