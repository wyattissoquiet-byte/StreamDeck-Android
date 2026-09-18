package com.example.streamdeckapp.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.streamdeckapp.engine.KeyRenderer
import com.example.streamdeckapp.model.DeckPage
import com.example.streamdeckapp.model.DeckProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StreamDeckManager(
    private val context: Context,
    private val keyRenderer: KeyRenderer
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private var currentDevice: StreamDeckDevice? = null
    var currentRotationDegrees: Int = 0

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectedDeviceName = MutableStateFlow("Disconnected")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName

    var onKeyPressedCallback: ((keyIndex: Int) -> Unit)? = null
    var onKeyReleasedCallback: ((keyIndex: Int) -> Unit)? = null

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.streamdeckapp.USB_PERMISSION"
        const val ELGATO_VENDOR_ID = 4057 // 0x0FD9
        const val PID_ORIGINAL_V1 = 96     // 0x0060
        const val PID_ORIGINAL_V2 = 109    // 0x006D
        const val PID_MK2 = 128            // 0x0080
    }

    fun setRotation(degrees: Int) {
        currentRotationDegrees = degrees
        currentDevice?.rotationDegrees = degrees
    }

    fun connectDevice(): Boolean {
        if (currentDevice != null) return true
        val manager = usbManager ?: return false

        val deviceList = try {
            manager.deviceList
        } catch (e: Exception) {
            Log.e("StreamDeckManager", "Error reading device list", e)
            emptyMap()
        }

        val streamDeckUsb = deviceList.values.find { it.vendorId == ELGATO_VENDOR_ID }
        if (streamDeckUsb == null) {
            _isConnected.value = false
            _connectedDeviceName.value = "Disconnected"
            return false
        }

        return claimAndInitialize(streamDeckUsb)
    }

    fun requestUsbPermission(usbDevice: UsbDevice) {
        val manager = usbManager ?: return
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
                flags
            )
            manager.requestPermission(usbDevice, permissionIntent)
        } catch (e: Exception) {
            Log.e("StreamDeckManager", "Error requesting USB permission", e)
        }
    }

    fun claimAndInitialize(usbDevice: UsbDevice): Boolean {
        val manager = usbManager ?: return false

        if (!manager.hasPermission(usbDevice)) {
            _isConnected.value = false
            _connectedDeviceName.value = "Requesting USB Permission..."
            requestUsbPermission(usbDevice)
            return false
        }

        return try {
            val usbInterface = (0 until usbDevice.interfaceCount)
                .map { usbDevice.getInterface(it) }
                .find { it.interfaceClass == UsbConstants.USB_CLASS_HID || it.interfaceClass == UsbConstants.USB_CLASS_PER_INTERFACE }
                ?: usbDevice.getInterface(0)

            val connection = manager.openDevice(usbDevice) ?: run {
                _isConnected.value = false
                _connectedDeviceName.value = "Could not open USB device"
                return false
            }

            if (!connection.claimInterface(usbInterface, true)) {
                connection.close()
                _isConnected.value = false
                _connectedDeviceName.value = "Failed to claim USB interface"
                return false
            }

            val driver: StreamDeckDevice = when (usbDevice.productId) {
                PID_ORIGINAL_V1 -> StreamDeckV1Driver(usbDevice, connection, usbInterface)
                else -> StreamDeckV2Driver(usbDevice, connection, usbInterface)
            }

            driver.rotationDegrees = currentRotationDegrees

            driver.keyListener = object : StreamDeckKeyListener {
                override fun onKeyPressed(logicalKeyIndex: Int) {
                    onKeyPressedCallback?.invoke(logicalKeyIndex)
                }

                override fun onKeyReleased(logicalKeyIndex: Int) {
                    onKeyReleasedCallback?.invoke(logicalKeyIndex)
                }
            }

            try {
                driver.setBrightness(80)
                driver.startListening()
            } catch (e: Exception) {
                Log.e("StreamDeckManager", "Error starting driver", e)
            }

            currentDevice = driver
            _isConnected.value = true
            _connectedDeviceName.value = "Elgato Stream Deck (PID: ${usbDevice.productId})"
            true
        } catch (e: Exception) {
            Log.e("StreamDeckManager", "claimAndInitialize error", e)
            _isConnected.value = false
            _connectedDeviceName.value = "Connection error: ${e.message}"
            false
        }
    }

    fun updatePageDisplays(
        page: DeckPage,
        isPlaying: Boolean,
        isMuted: Boolean,
        totalPages: Int,
        pageIndex: Int,
        rotationDegrees: Int = currentRotationDegrees
    ) {
        val dev = currentDevice ?: return
        dev.rotationDegrees = rotationDegrees

        for (logicalSlot in 0 until minOf(15, page.slots.size)) {
            val action = page.slots[logicalSlot]
            val bitmap = keyRenderer.renderKey(
                action = action,
                isPlaying = isPlaying,
                isMuted = isMuted,
                pageIndexInfo = "${pageIndex + 1} / $totalPages"
            )
            val physicalKeyIndex = DeckProfile.mapLogicalToPhysical(logicalSlot, rotationDegrees)
            try {
                dev.writeKeyImage(physicalKeyIndex, bitmap)
            } catch (e: Exception) {
                Log.e("StreamDeckManager", "Error writing key image to physical slot $physicalKeyIndex", e)
            }
        }
    }

    fun updateSingleKeyDisplay(keyIndex: Int, bitmap: Bitmap) {
        try {
            currentDevice?.writeKeyImage(keyIndex, bitmap)
        } catch (e: Exception) {
            Log.e("StreamDeckManager", "Error writing single key $keyIndex", e)
        }
    }

    fun disconnect() {
        try {
            currentDevice?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentDevice = null
        _isConnected.value = false
        _connectedDeviceName.value = "Disconnected"
    }
}
