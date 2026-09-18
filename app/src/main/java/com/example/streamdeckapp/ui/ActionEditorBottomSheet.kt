package com.example.streamdeckapp.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.service.StreamDeckAccessibilityService
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionEditorBottomSheet(
    slotIndex: Int,
    currentAction: DeckAction,
    installedApps: List<InstalledAppInfo>,
    pageCount: Int,
    onDismiss: () -> Unit,
    onSaveAction: (DeckAction) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentAction.type) }
    var labelText by remember { mutableStateOf(currentAction.label) }
    var backgroundColor by remember { mutableStateOf(currentAction.backgroundColor) }
    var labelColor by remember { mutableStateOf(currentAction.labelColor) }
    var selectedAppPackage by remember { mutableStateOf(currentAction.targetPackageName) }
    var selectedAppName by remember { mutableStateOf(currentAction.targetAppName) }
    var shellCmdText by remember { mutableStateOf(currentAction.shellCommand) }
    var targetPageIdx by remember { mutableStateOf(currentAction.targetPageIndex) }
    var customImagePath by remember { mutableStateOf(currentAction.customImagePath) }
    var extraDataText by remember { mutableStateOf(currentAction.extraData) }
    var touchX by remember { mutableStateOf(if (currentAction.touchX > 0f) currentAction.touchX else 0.5f) }
    var touchY by remember { mutableStateOf(if (currentAction.touchY > 0f) currentAction.touchY else 0.5f) }
    var touchEndX by remember { mutableStateOf(if (currentAction.touchEndX > 0f) currentAction.touchEndX else 0.5f) }
    var touchEndY by remember { mutableStateOf(if (currentAction.touchEndY > 0f) currentAction.touchEndY else 0.25f) }
    var swipeDurationMs by remember { mutableStateOf(if (currentAction.swipeDurationMs > 0L) currentAction.swipeDurationMs else 300L) }
    var showFullscreenMapper by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(getCategoryForType(currentAction.type)) }
    var hexInputText by remember { mutableStateOf("#" + ((backgroundColor and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase())) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val iconDir = File(context.filesDir, "custom_icons")
                if (!iconDir.exists()) iconDir.mkdirs()
                val targetFile = File(iconDir, "icon_${System.currentTimeMillis()}.png")
                inputStream?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                customImagePath = targetFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var isAppDropdownExpanded by remember { mutableStateOf(false) }

    // Curated rich color palette matching car stereo aesthetics
    val presetBgColors = listOf(
        // Blacks & Steels
        0xFF121214L, 0xFF1C1C24L, 0xFF263238L, 0xFF37474FL,
        // Greens & Cyans
        0xFF004D20L, 0xFF007E33L, 0xFF00E676L, 0xFF00BFA5L,
        // Blues & Navies
        0xFF003366L, 0xFF0091EAL, 0xFF00B0FFL, 0xFF00E5FFL, 0xFF1A237EL,
        // Purples & Magentas
        0xFF4A0072L, 0xFF651FFFL, 0xFF7C4DFFL, 0xFF3D5AFEL, 0xFFE040FBL, 0xFFD500F9L,
        // Pinks & Roses
        0xFFFF4081L, 0xFFC2185BL,
        // Reds & Crimsons
        0xFF7A0019L, 0xFFB71C1CL, 0xFFD50000L, 0xFFFF1744L,
        // Oranges & Golds
        0xFFFF5722L, 0xFF8A3B00L, 0xFFFF6D00L, 0xFFFF9100L, 0xFFFFD600L
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configure Key #${slotIndex + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Quick clear button
                TextButton(onClick = {
                    selectedType = ActionType.NONE
                    labelText = ""
                    customImagePath = ""
                    selectedAppPackage = ""
                    selectedAppName = ""
                    shellCmdText = ""
                    backgroundColor = 0xFF1C1C24L
                }) {
                    Text("Clear Slot", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            // ==========================================
            // LIVE BUTTON PREVIEW (Matches physical LCD)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIVE LCD PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LiveButtonPreview(
                    type = selectedType,
                    label = labelText,
                    backgroundColor = backgroundColor,
                    labelColor = labelColor,
                    customImagePath = customImagePath,
                    targetPageIndex = targetPageIdx
                )
            }

            HorizontalDivider()

            // ==========================================
            // ACTION CATEGORY & TYPE SELECTOR
            // ==========================================
            Text("Action Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ActionCategory.values()) { category ->
                    val isCatSelected = selectedCategory == category
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            selectedCategory = category
                            val actionsInCat = getActionsForCategory(category)
                            if (selectedType !in actionsInCat && actionsInCat.isNotEmpty()) {
                                selectedType = actionsInCat.first()
                            }
                        },
                        label = { Text("${category.icon} ${category.title}", fontSize = 12.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Text("Select Action", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                getActionsForCategory(selectedCategory).forEach { type ->
                    val isSelected = selectedType == type
                    val color = getActionColor(type)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            if (labelText.isBlank() || labelText.matches(Regex("^[A-Z0-9 +/\\-]+$"))) {
                                labelText = when (type) {
                                    ActionType.SYSTEM_HOME -> "HOME"
                                    ActionType.SYSTEM_BACK -> "BACK"
                                    ActionType.SYSTEM_RECENTS -> "RECENTS"
                                    ActionType.SYSTEM_NOTIFICATIONS -> "NOTIF"
                                    ActionType.SYSTEM_QUICK_SETTINGS -> "QUICK"
                                    ActionType.SYSTEM_LOCK_SCREEN -> "LOCK"
                                    ActionType.SYSTEM_POWER_DIALOG -> "POWER"
                                    ActionType.SYSTEM_SPLIT_SCREEN -> "SPLIT"
                                    ActionType.SYSTEM_SCREENSHOT -> "SHOT"
                                    ActionType.SIMULATED_TAP -> "TAP"
                                    ActionType.SIMULATED_SWIPE -> "SWIPE"
                                    ActionType.SIMULATED_SWIPE_UP -> "SWIPE UP"
                                    ActionType.SIMULATED_SWIPE_DOWN -> "SWIPE DN"
                                    ActionType.SIMULATED_SWIPE_LEFT -> "SWIPE LT"
                                    ActionType.SIMULATED_SWIPE_RIGHT -> "SWIPE RT"
                                    ActionType.MEDIA_PLAY_PAUSE -> "PLAY/PAUSE"
                                    ActionType.MEDIA_PLAY -> "PLAY"
                                    ActionType.MEDIA_PAUSE -> "PAUSE"
                                    ActionType.MEDIA_STOP -> "STOP"
                                    ActionType.MEDIA_NEXT -> "NEXT"
                                    ActionType.MEDIA_PREV -> "PREV"
                                    ActionType.MEDIA_FAST_FORWARD -> "FWD"
                                    ActionType.MEDIA_REWIND -> "RWD"
                                    ActionType.VOLUME_UP -> "VOL +"
                                    ActionType.VOLUME_DOWN -> "VOL -"
                                    ActionType.VOLUME_MUTE_TOGGLE -> "MUTE"
                                    ActionType.BRIGHTNESS_UP -> "BRT +"
                                    ActionType.BRIGHTNESS_DOWN -> "BRT -"
                                    ActionType.SCREEN_OFF -> "SLEEP"
                                    ActionType.SETTINGS_BLUETOOTH -> "BT"
                                    ActionType.SETTINGS_WIFI -> "WIFI"
                                    ActionType.SETTINGS_SOUND -> "SOUND"
                                    ActionType.SETTINGS_DISPLAY -> "DISP"
                                    ActionType.SETTINGS_DATE_TIME -> "TIME"
                                    ActionType.SETTINGS_LOCATION -> "GPS"
                                    ActionType.SETTINGS_APPS -> "APPS"
                                    ActionType.SETTINGS_MAIN -> "SETTINGS"
                                    ActionType.TORCH_TOGGLE -> "TORCH"
                                    ActionType.OPEN_URL -> "WEB"
                                    ActionType.VOICE_ASSISTANT -> "VOICE"
                                    ActionType.DIAL_PHONE -> "PHONE"
                                    ActionType.NEXT_PAGE -> "NEXT"
                                    ActionType.PREV_PAGE -> "PREV"
                                    else -> ""
                                }
                            }
                        },
                        label = { Text(type.name.replace("_", " "), fontSize = 11.5.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    )
                }
            }

            // ==========================================
            // DYNAMIC ACTION CONFIGURATION PANELS
            // ==========================================
            when (selectedType) {
                ActionType.SIMULATED_TAP,
                ActionType.SIMULATED_SWIPE,
                ActionType.SIMULATED_SWIPE_UP,
                ActionType.SIMULATED_SWIPE_DOWN,
                ActionType.SIMULATED_SWIPE_LEFT,
                ActionType.SIMULATED_SWIPE_RIGHT -> {
                    Surface(
                        color = Color(0xFF161824),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C324A)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Screen Gesture Automation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF00E5FF)
                                )
                                Surface(
                                    color = Color(0xFF263238),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (selectedType == ActionType.SIMULATED_TAP) "TAP" else "SWIPE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF80D8FF),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Prominent Fullscreen Mapper Trigger Button
                            Button(
                                onClick = { showFullscreenMapper = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎯 Open Fullscreen Screen Mapper", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Coordinate Readout
                            val widthPx = context.resources.displayMetrics.widthPixels
                            val heightPx = context.resources.displayMetrics.heightPixels
                            val readout = if (selectedType == ActionType.SIMULATED_TAP) {
                                "Target: (${(touchX * widthPx).toInt()}px, ${(touchY * heightPx).toInt()}px) · ${(touchX * 100).toInt()}% X, ${(touchY * 100).toInt()}% Y"
                            } else {
                                "Path: (${(touchX * widthPx).toInt()}px, ${(touchY * heightPx).toInt()}px) ➔ (${(touchEndX * widthPx).toInt()}px, ${(touchEndY * heightPx).toInt()}px) · ${swipeDurationMs}ms"
                            }
                            Text(readout, fontSize = 11.5.sp, color = Color(0xFFB0BEC5))

                            // Test gesture on real screen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (selectedType == ActionType.SIMULATED_TAP) {
                                            StreamDeckAccessibilityService.simulateTap(touchX, touchY, context.resources.displayMetrics)
                                        } else {
                                            StreamDeckAccessibilityService.simulateSwipe(
                                                touchX, touchY, touchEndX, touchEndY, swipeDurationMs, context.resources.displayMetrics
                                            )
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test on Screen", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                ActionType.OPEN_URL -> {
                    Text("Website URL or App Deep Link", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = extraDataText,
                        onValueChange = { extraDataText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://maps.google.com or waze://") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                    )
                }

                ActionType.DIAL_PHONE -> {
                    Text("Phone Number to Dial", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = extraDataText,
                        onValueChange = { extraDataText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 555-123-4567") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                    )
                }

                ActionType.LAUNCH_APP -> {
                    Text("Select Application to Launch", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    ExposedDropdownMenuBox(
                        expanded = isAppDropdownExpanded,
                        onExpandedChange = { isAppDropdownExpanded = !isAppDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAppName.ifBlank { "Choose installed app..." },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAppDropdownExpanded) }
                        )

                        ExposedDropdownMenu(
                            expanded = isAppDropdownExpanded,
                            onDismissRequest = { isAppDropdownExpanded = false }
                        ) {
                            installedApps.forEach { appInfo ->
                                DropdownMenuItem(
                                    text = { Text(appInfo.appName) },
                                    onClick = {
                                        selectedAppPackage = appInfo.packageName
                                        selectedAppName = appInfo.appName
                                        if (labelText.isBlank()) labelText = appInfo.appName
                                        isAppDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ActionType.SHELL_COMMAND -> {
                    Text("Shell Command / Script", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = shellCmdText,
                        onValueChange = { shellCmdText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. input keyevent 26 or reboot") }
                    )
                }

                ActionType.GOTO_PAGE -> {
                    Text("Target Page Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (p in 0 until pageCount) {
                            FilterChip(
                                selected = targetPageIdx == p,
                                onClick = { targetPageIdx = p },
                                label = { Text("Page ${p + 1}") }
                            )
                        }
                    }
                }

                else -> {}
            }

            // Label Text Input
            Text("Button Label Text", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter button label...") }
            )

            // ==========================================
            // BUTTON BACKGROUND COLOR PALETTE (Realistic mini-buttons)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Button Background Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "#" + ((backgroundColor and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Realistic mini key tile swatches matching actual button finish
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetBgColors) { colorLong ->
                    val isSelected = backgroundColor == colorLong
                    val col = Color(colorLong.toInt())

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(col.copy(alpha = 0.95f), col.copy(alpha = 0.6f))
                                )
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                backgroundColor = colorLong
                                hexInputText = "#" + ((colorLong and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Hex Code Custom Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { text ->
                        hexInputText = text
                        val cleanHex = text.removePrefix("#").trim()
                        if (cleanHex.length == 6) {
                            val parsed = cleanHex.toLongOrNull(16)
                            if (parsed != null) {
                                backgroundColor = 0xFF000000L or parsed
                            }
                        }
                    },
                    label = { Text("Custom Hex Color (#RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(backgroundColor.toInt()))
                        .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                )
            }

            // Custom Image Picker
            Text("Custom Image / Icon Asset", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (customImagePath.isNotBlank()) "Change Custom Image" else "Choose Image (PNG/JPG)")
                }
                if (customImagePath.isNotBlank()) {
                    TextButton(onClick = { customImagePath = "" }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Save / Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val updated = currentAction.copy(
                            type = selectedType,
                            label = labelText,
                            backgroundColor = backgroundColor,
                            labelColor = labelColor,
                            targetPackageName = selectedAppPackage,
                            targetAppName = selectedAppName,
                            shellCommand = shellCmdText,
                            targetPageIndex = targetPageIdx,
                            customImagePath = customImagePath,
                            extraData = extraDataText,
                            touchX = touchX,
                            touchY = touchY,
                            touchEndX = touchEndX,
                            touchEndY = touchEndY,
                            swipeDurationMs = swipeDurationMs
                        )
                        onSaveAction(updated)
                        onDismiss()
                    }
                ) {
                    Text("Save Changes")
                }
            }
        }
    }

    // ==========================================
    // INTERACTIVE FULLSCREEN GESTURE MAPPER DIALOG
    // ==========================================
    if (showFullscreenMapper) {
        FullscreenTouchMapperDialog(
            initialType = selectedType,
            initialTouchX = touchX,
            initialTouchY = touchY,
            initialTouchEndX = touchEndX,
            initialTouchEndY = touchEndY,
            initialDurationMs = swipeDurationMs,
            onDismiss = { showFullscreenMapper = false },
            onConfirm = { newType, nx, ny, nex, ney, dur, suggestedLabel ->
                selectedType = newType
                touchX = nx
                touchY = ny
                touchEndX = nex
                touchEndY = ney
                swipeDurationMs = dur
                if (labelText.isBlank() || labelText.startsWith("TAP") || labelText.startsWith("SWIPE")) {
                    labelText = suggestedLabel
                }
                showFullscreenMapper = false
            }
        )
    }
}

enum class ActionCategory(val title: String, val icon: String) {
    NAVIGATION("System & Nav", "📱"),
    GESTURES("Touch & Swipe", "👆"),
    MEDIA("Media & Audio", "🎵"),
    DISPLAY("Display", "☀️"),
    SETTINGS("Settings", "⚙️"),
    TOOLS("Tools", "🛠️"),
    PAGES("Pages", "📄")
}

fun getCategoryForType(type: ActionType): ActionCategory {
    return when (type) {
        ActionType.SYSTEM_HOME, ActionType.SYSTEM_BACK, ActionType.SYSTEM_RECENTS,
        ActionType.SYSTEM_NOTIFICATIONS, ActionType.SYSTEM_QUICK_SETTINGS, ActionType.SYSTEM_LOCK_SCREEN,
        ActionType.SYSTEM_POWER_DIALOG, ActionType.SYSTEM_SPLIT_SCREEN, ActionType.SYSTEM_SCREENSHOT -> ActionCategory.NAVIGATION

        ActionType.SIMULATED_TAP, ActionType.SIMULATED_SWIPE, ActionType.SIMULATED_SWIPE_UP,
        ActionType.SIMULATED_SWIPE_DOWN, ActionType.SIMULATED_SWIPE_LEFT, ActionType.SIMULATED_SWIPE_RIGHT -> ActionCategory.GESTURES

        ActionType.MEDIA_PLAY_PAUSE, ActionType.MEDIA_PLAY, ActionType.MEDIA_PAUSE, ActionType.MEDIA_STOP,
        ActionType.MEDIA_NEXT, ActionType.MEDIA_PREV, ActionType.MEDIA_FAST_FORWARD, ActionType.MEDIA_REWIND,
        ActionType.VOLUME_UP, ActionType.VOLUME_DOWN, ActionType.VOLUME_MUTE_TOGGLE -> ActionCategory.MEDIA

        ActionType.BRIGHTNESS_UP, ActionType.BRIGHTNESS_DOWN, ActionType.SCREEN_OFF -> ActionCategory.DISPLAY

        ActionType.SETTINGS_BLUETOOTH, ActionType.SETTINGS_WIFI, ActionType.SETTINGS_SOUND,
        ActionType.SETTINGS_DISPLAY, ActionType.SETTINGS_DATE_TIME, ActionType.SETTINGS_LOCATION,
        ActionType.SETTINGS_APPS, ActionType.SETTINGS_MAIN -> ActionCategory.SETTINGS

        ActionType.TORCH_TOGGLE, ActionType.OPEN_URL, ActionType.VOICE_ASSISTANT,
        ActionType.DIAL_PHONE, ActionType.LAUNCH_APP, ActionType.SHELL_COMMAND -> ActionCategory.TOOLS

        ActionType.NEXT_PAGE, ActionType.PREV_PAGE, ActionType.GOTO_PAGE, ActionType.NONE -> ActionCategory.PAGES
    }
}

fun getActionsForCategory(category: ActionCategory): List<ActionType> {
    return ActionType.values().filter { getCategoryForType(it) == category }
}


@Composable
fun LiveButtonPreview(
    type: ActionType,
    label: String,
    backgroundColor: Long,
    labelColor: Long,
    customImagePath: String,
    targetPageIndex: Int
) {
    val baseBgColor = Color(backgroundColor.toInt())
    val signatureColor = getActionColor(type)

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            baseBgColor.copy(alpha = 0.95f),
            baseBgColor.copy(alpha = 0.65f)
        )
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradientBrush)
            .border(2.dp, signatureColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon or Custom Image
            if (customImagePath.isNotBlank() && File(customImagePath).exists()) {
                val bitmap = remember(customImagePath) {
                    BitmapFactory.decodeFile(customImagePath)?.asImageBitmap()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Custom Icon",
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    ColorfulActionIconBadge(
                        action = DeckAction(type = type, targetPageIndex = targetPageIndex),
                        signatureColor = signatureColor
                    )
                }
            } else {
                ColorfulActionIconBadge(
                    action = DeckAction(type = type, targetPageIndex = targetPageIndex),
                    signatureColor = signatureColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label text with pill
            val displayLabel = when {
                label.isNotBlank() -> label
                type != ActionType.NONE -> type.name.replace("_", " ")
                else -> ""
            }

            if (displayLabel.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = displayLabel,
                        color = Color(labelColor.toInt()),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
