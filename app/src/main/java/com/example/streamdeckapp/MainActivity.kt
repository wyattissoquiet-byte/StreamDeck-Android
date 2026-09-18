package com.example.streamdeckapp

import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.ui.ActionEditorBottomSheet
import com.example.streamdeckapp.ui.MainViewModel
import com.example.streamdeckapp.ui.PageManagerScreen
import com.example.streamdeckapp.ui.VirtualGridScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle "when connected the app connects to it without launching":
        // If launched due to USB attachment, connect immediately in background and exit UI quietly!
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            viewModel.connectUsbDevice()
            moveTaskToBack(true)
            finish()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C4DFF),
                    onPrimary = Color.White,
                    surface = Color(0xFF121216),
                    background = Color(0xFF0A0A0C),
                    onBackground = Color.White,
                    surfaceVariant = Color(0xFF1E1E26),
                    onSurfaceVariant = Color.White
                )
            ) {
                val activeProfile by viewModel.activeProfile.collectAsState()
                val pressedKeyIndex by viewModel.pressedKeyIndex.collectAsState()
                val isConnected by viewModel.isConnected.collectAsState()
                val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
                val installedApps by viewModel.installedApps.collectAsState()
                val serverUrl by viewModel.serverUrl.collectAsState()

                var selectedTab by remember { mutableStateOf(0) } // 0 = Virtual Grid, 1 = Pages Manager
                var editingSlotIndex by remember { mutableStateOf<Int?>(null) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Stream Deck", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Connection Status Pill
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isConnected) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // Rotate Deck Button
                                val currentRotation = activeProfile?.rotationDegrees ?: 0
                                FilledTonalButton(
                                    onClick = { viewModel.rotateDeck() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate Deck",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${currentRotation}°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // USB Reconnect Button
                                IconButton(onClick = { viewModel.connectUsbDevice() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reconnect USB", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.GridOn, contentDescription = "Virtual Grid") },
                                label = { Text("Virtual Grid") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Layers, contentDescription = "Pages") },
                                label = { Text("Page Manager") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                val currentPage = activeProfile?.pages?.getOrNull(activeProfile?.activePageIndex ?: 0)
                                VirtualGridScreen(
                                    currentPage = currentPage,
                                    pressedKeyIndex = pressedKeyIndex,
                                    rotationDegrees = activeProfile?.rotationDegrees ?: 0,
                                    onRotateRequested = { viewModel.rotateDeck() },
                                    onKeyTrigger = { slotIdx -> viewModel.onVirtualKeyClicked(slotIdx) },
                                    onKeyEditRequested = { slotIdx -> editingSlotIndex = slotIdx }
                                )
                            }
                            1 -> {
                                PageManagerScreen(
                                    profile = activeProfile,
                                    serverUrl = serverUrl,
                                    onPageSelected = { pageIdx ->
                                        viewModel.switchPage(pageIdx)
                                        selectedTab = 0
                                    },
                                    onAddPage = { viewModel.addPage() },
                                    onDeletePage = { pageIdx -> viewModel.deletePage(pageIdx) },
                                    onMovePageUp = { pageIdx -> viewModel.movePageUp(pageIdx) },
                                    onMovePageDown = { pageIdx -> viewModel.movePageDown(pageIdx) },
                                    onRenamePage = { pageIdx, newName -> viewModel.renamePage(pageIdx, newName) }
                                )
                            }
                        }

                        // Bottom Sheet Action Editor Dialog
                        editingSlotIndex?.let { slotIdx ->
                            val currentPage = activeProfile?.pages?.getOrNull(activeProfile?.activePageIndex ?: 0)
                            val currentAction = currentPage?.slots?.getOrNull(slotIdx) ?: DeckAction()
                            ActionEditorBottomSheet(
                                slotIndex = slotIdx,
                                currentAction = currentAction,
                                installedApps = installedApps,
                                pageCount = activeProfile?.pages?.size ?: 1,
                                onDismiss = { editingSlotIndex = null },
                                onSaveAction = { updatedAction ->
                                    viewModel.updateAction(slotIdx, updatedAction)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
