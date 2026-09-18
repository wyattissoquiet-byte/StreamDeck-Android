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
            // ACTION TYPE SELECTOR
            // ==========================================
            Text("Action Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionType.values().forEach { type ->
                    val isSelected = selectedType == type
                    val color = getActionColor(type)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            if (labelText.isBlank()) {
                                labelText = when (type) {
                                    ActionType.MEDIA_PLAY_PAUSE -> "PLAY/PAUSE"
                                    ActionType.MEDIA_NEXT -> "NEXT"
                                    ActionType.MEDIA_PREV -> "PREV"
                                    ActionType.VOLUME_UP -> "VOL +"
                                    ActionType.VOLUME_DOWN -> "VOL -"
                                    ActionType.VOLUME_MUTE_TOGGLE -> "MUTE"
                                    ActionType.NEXT_PAGE -> "NEXT PAGE"
                                    ActionType.PREV_PAGE -> "PREV PAGE"
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

            // Dynamic parameter inputs
            when (selectedType) {
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
                            customImagePath = customImagePath
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
