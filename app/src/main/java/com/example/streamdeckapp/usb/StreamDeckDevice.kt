package com.example.streamdeckapp.usb

import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection

interface StreamDeckKeyListener {
    fun onKeyPressed(logicalKeyIndex: Int)
    fun onKeyReleased(logicalKeyIndex: Int)
}

abstract class StreamDeckDevice(
    val device: UsbDevice,
    val connection: UsbDeviceConnection
) {
    var keyListener: StreamDeckKeyListener? = null
    protected var isRunning = false
    var rotationDegrees: Int = 0

    abstract fun startListening()
    abstract fun stopListening()
    abstract fun setBrightness(percent: Int)
    abstract fun writeKeyImage(keyIndex: Int, bitmap: Bitmap)
    abstract fun clearAllKeys()
}
