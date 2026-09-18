package com.example.streamdeckapp.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.streamdeckapp.R
import com.example.streamdeckapp.engine.ActionHandler
import com.example.streamdeckapp.engine.KeyRenderer
import com.example.streamdeckapp.model.DeckProfile
import com.example.streamdeckapp.storage.ProfileStorageManager
import com.example.streamdeckapp.usb.StreamDeckManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class StreamDeckService : Service() {

    private val binder = LocalBinder()
    private val CHANNEL_ID = "stream_deck_service_channel"
    private val NOTIF_ID = 1001

    private lateinit var profileStorage: ProfileStorageManager
    private lateinit var keyRenderer: KeyRenderer
    lateinit var streamDeckManager: StreamDeckManager
        private set
    lateinit var actionHandler: ActionHandler
        private set

    val activeProfile = MutableStateFlow<DeckProfile?>(null)
    val pressedKeyIndex = MutableStateFlow<Int?>(null)
    val serverUrl = MutableStateFlow<String>("")

    private var desktopConfigServer: com.example.streamdeckapp.server.DesktopConfigServer? = null
    val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateCheckJob: Job? = null
    private var scanJob: Job? = null
    private var usbReceiver: BroadcastReceiver? = null
    private var isForegroundActive = false

    inner class LocalBinder : Binder() {
        fun getService(): StreamDeckService = this@StreamDeckService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundServiceSafe()

        val action = intent?.action
        if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = getUsbDeviceFromIntent(intent)
            if (device != null && device.vendorId == StreamDeckManager.ELGATO_VENDOR_ID) {
                serviceScope.launch(Dispatchers.IO) {
                    val claimed = streamDeckManager.claimAndInitialize(device)
                    if (claimed) {
                        promoteToForegroundServiceSafe()
                        refreshDisplays()
                    }
                }
            } else {
                autoScanAndConnectWithRetry()
            }
        } else {
            // Auto scan whenever started/restarted
            autoScanAndConnectWithRetry()
        }

        return START_STICKY
    }

    fun autoScanAndConnectWithRetry() {
        scanJob?.cancel()
        scanJob = serviceScope.launch(Dispatchers.IO) {
            // If already connected, just refresh displays
            if (streamDeckManager.isConnected.value) {
                refreshDisplays()
                return@launch
            }

            // Retry sequence to catch delayed USB host enumeration (e.g. on car stereo boots)
            val delays = listOf(0L, 800L, 1500L, 3000L, 5000L)
            for (d in delays) {
                if (d > 0) delay(d)
                if (streamDeckManager.isConnected.value) {
                    refreshDisplays()
                    break
                }
                val connected = streamDeckManager.connectDevice()
                if (connected) {
                    promoteToForegroundServiceSafe()
                    refreshDisplays()
                    Log.d("StreamDeckService", "Auto-scan successfully connected to Stream Deck!")
                    break
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        profileStorage = ProfileStorageManager(this)
        keyRenderer = KeyRenderer(this)
        streamDeckManager = StreamDeckManager(this, keyRenderer)

        val loadedProfile = profileStorage.loadProfile()
        activeProfile.value = loadedProfile
        streamDeckManager.currentRotationDegrees = loadedProfile.rotationDegrees

        actionHandler = ActionHandler(this) { targetIndex, isNext, isPrev ->
            val profile = activeProfile.value ?: return@ActionHandler
            if (isNext == true) {
                val nextIdx = (profile.activePageIndex + 1) % profile.pages.size
                switchPage(nextIdx)
            } else if (isPrev == true) {
                val prevIdx = if (profile.activePageIndex - 1 < 0) profile.pages.size - 1 else profile.activePageIndex - 1
                switchPage(prevIdx)
            } else if (targetIndex != null && targetIndex in 0 until profile.pages.size) {
                switchPage(targetIndex)
            }
        }

        streamDeckManager.onKeyPressedCallback = { keyIndex ->
            serviceScope.launch {
                pressedKeyIndex.value = keyIndex
                val profile = activeProfile.value
                if (profile != null && profile.activePageIndex in 0 until profile.pages.size) {
                    val currentPage = profile.pages[profile.activePageIndex]
                    if (keyIndex in 0 until currentPage.slots.size) {
                        val action = currentPage.slots[keyIndex]
                        withContext(Dispatchers.IO) {
                            actionHandler.executeAction(action)
                        }
                        refreshDisplays()
                    }
                }
            }
        }

        streamDeckManager.onKeyReleasedCallback = { _ ->
            serviceScope.launch {
                pressedKeyIndex.value = null
            }
        }

        registerUsbReceiver()

        autoScanAndConnectWithRetry()

        startStateMonitoring()

        try {
            desktopConfigServer = com.example.streamdeckapp.server.DesktopConfigServer(
                port = 8080,
                getProfile = { activeProfile.value },
                updateProfile = { newProf -> updateProfile(newProf) },
                triggerAction = { slotIdx ->
                    serviceScope.launch {
                        val profile = activeProfile.value ?: return@launch
                        val currentPage = profile.pages.getOrNull(profile.activePageIndex) ?: return@launch
                        val action = currentPage.slots.getOrNull(slotIdx) ?: return@launch
                        withContext(Dispatchers.IO) {
                            actionHandler.executeAction(action)
                        }
                        refreshDisplays()
                    }
                }
            )
            desktopConfigServer?.start()
            serverUrl.value = desktopConfigServer?.getServerUrl() ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(StreamDeckManager.ACTION_USB_PERMISSION)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device?.vendorId == StreamDeckManager.ELGATO_VENDOR_ID) {
                            serviceScope.launch(Dispatchers.IO) {
                                val claimed = streamDeckManager.claimAndInitialize(device)
                                if (claimed) {
                                    promoteToForegroundServiceSafe()
                                    refreshDisplays()
                                }
                            }
                        }
                    }
                    StreamDeckManager.ACTION_USB_PERMISSION -> {
                        val device = getUsbDeviceFromIntent(intent)
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (granted && device?.vendorId == StreamDeckManager.ELGATO_VENDOR_ID) {
                            serviceScope.launch(Dispatchers.IO) {
                                val claimed = streamDeckManager.claimAndInitialize(device)
                                if (claimed) {
                                    promoteToForegroundServiceSafe()
                                    refreshDisplays()
                                }
                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device?.vendorId == StreamDeckManager.ELGATO_VENDOR_ID) {
                            serviceScope.launch(Dispatchers.IO) {
                                streamDeckManager.disconnect()
                            }
                        }
                    }
                }
            }
        }

        usbReceiver = receiver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Log.e("StreamDeckService", "Failed to register USB receiver", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun promoteToForegroundServiceSafe() {
        if (isForegroundActive) return
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stream Deck Active")
                .setContentText("Connected and communicating over USB")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
            isForegroundActive = true
        } catch (e: Throwable) {
            Log.w("StreamDeckService", "Could not start foreground notification (safe to continue)", e)
        }
    }

    fun switchPage(pageIndex: Int) {
        val profile = activeProfile.value ?: return
        if (pageIndex in 0 until profile.pages.size) {
            profile.activePageIndex = pageIndex
            activeProfile.value = profile.copy(activePageIndex = pageIndex)
            serviceScope.launch(Dispatchers.IO) {
                profileStorage.saveProfile(profile)
            }
            refreshDisplays()
        }
    }

    fun updateProfile(newProfile: DeckProfile) {
        activeProfile.value = newProfile
        serviceScope.launch(Dispatchers.IO) {
            profileStorage.saveProfile(newProfile)
        }
        refreshDisplays()
    }

    fun setRotation(degrees: Int) {
        val profile = activeProfile.value ?: return
        profile.rotationDegrees = degrees
        streamDeckManager.setRotation(degrees)
        activeProfile.value = profile.copy(rotationDegrees = degrees)
        serviceScope.launch(Dispatchers.IO) {
            profileStorage.saveProfile(profile)
        }
        refreshDisplays()
    }

    fun refreshDisplays() {
        serviceScope.launch(Dispatchers.IO) {
            val profile = activeProfile.value ?: return@launch
            if (profile.pages.isEmpty()) return@launch

            val pageIndex = profile.activePageIndex.coerceIn(0, profile.pages.size - 1)
            val currentPage = profile.pages[pageIndex]

            val isPlaying = actionHandler.isAudioPlaying()
            val isMuted = actionHandler.isAudioMuted()

            streamDeckManager.updatePageDisplays(
                page = currentPage,
                isPlaying = isPlaying,
                isMuted = isMuted,
                totalPages = profile.pages.size,
                pageIndex = pageIndex,
                rotationDegrees = profile.rotationDegrees
            )
        }
    }

    private fun startStateMonitoring() {
        stateCheckJob = serviceScope.launch {
            var lastPlaying = false
            var lastMuted = false
            while (isActive) {
                delay(1000)
                val currentPlaying = actionHandler.isAudioPlaying()
                val currentMuted = actionHandler.isAudioMuted()
                if (currentPlaying != lastPlaying || currentMuted != lastMuted) {
                    lastPlaying = currentPlaying
                    lastMuted = currentMuted
                    refreshDisplays()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stream Deck USB Connection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateCheckJob?.cancel()
        usbReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        desktopConfigServer?.stop()
        desktopConfigServer = null
        streamDeckManager.disconnect()
        serviceScope.cancel()
    }
}
