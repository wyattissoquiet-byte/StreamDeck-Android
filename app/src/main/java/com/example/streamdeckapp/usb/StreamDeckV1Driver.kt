package com.example.streamdeckapp.usb

import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.usb.*
import com.example.streamdeckapp.model.DeckProfile
import kotlin.concurrent.thread

class StreamDeckV1Driver(
    device: UsbDevice,
    connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : StreamDeckDevice(device, connection) {

    private val endpointIn: UsbEndpoint? = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .find { it.direction == UsbConstants.USB_DIR_IN }

    private var pollThread: Thread? = null
    private val previousKeyState = BooleanArray(15)

    override fun startListening() {
        if (isRunning) return
        isRunning = true

        pollThread = thread(start = true, name = "StreamDeckV1Poll") {
            val buffer = ByteArray(endpointIn?.maxPacketSize ?: 32)
            while (isRunning) {
                if (endpointIn != null) {
                    val bytesRead = connection.bulkTransfer(endpointIn, buffer, buffer.size, 100)
                    if (bytesRead >= 17) {
                        // Protocol V1: byte 0 is report ID (0x01), bytes 2..16 are key states
                        for (i in 0 until 15) {
                            val isPressed = buffer[i + 2] != 0.toByte()
                            if (isPressed != previousKeyState[i]) {
                                previousKeyState[i] = isPressed
                                val mappedIndex = DeckProfile.mapPhysicalToLogical(i, rotationDegrees)
                                if (isPressed) {
                                    keyListener?.onKeyPressed(mappedIndex)
                                } else {
                                    keyListener?.onKeyReleased(mappedIndex)
                                }
                            }
                        }
                    }
                } else {
                    Thread.sleep(50)
                }
            }
        }
    }

    override fun stopListening() {
        isRunning = false
        pollThread?.interrupt()
        pollThread = null
    }

    override fun setBrightness(percent: Int) {
        val payload = ByteArray(17)
        payload[0] = 0x05
        payload[1] = 0x55.toByte()
        payload[2] = 0xAA.toByte()
        payload[3] = 0xD1.toByte()
        payload[4] = 0x01
        payload[5] = percent.coerceIn(0, 100).toByte()
        connection.controlTransfer(0x21, 0x09, 0x0500, usbInterface.id, payload, payload.size, 1000)
    }

    override fun writeKeyImage(keyIndex: Int, bitmap: Bitmap) {
        if (keyIndex !in 0..14) return

        // V1 requires 72x72 BGR raw pixels, mirrored horizontally and rotated with user orientation
        val matrix = Matrix().apply {
            postRotate(90f + rotationDegrees)
            postScale(-1f, 1f, 36f, 36f)
        }
        val transformedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val bgrBytes = ByteArray(72 * 72 * 3)
        var offset = 0
        for (y in 0 until 72) {
            for (x in 0 until 72) {
                val pixel = transformedBmp.getPixel(x, y)
                bgrBytes[offset++] = (pixel and 0xFF).toByte()        // Blue
                bgrBytes[offset++] = (pixel shr 8 and 0xFF).toByte()  // Green
                bgrBytes[offset++] = (pixel shr 16 and 0xFF).toByte() // Red
            }
        }

        // Split into 2 packets (3888 bytes each)
        val packet1PayloadSize = 3888
        val packet2PayloadSize = bgrBytes.size - packet1PayloadSize

        // Packet 1
        val header1 = ByteArray(8191)
        header1[0] = 0x02
        header1[1] = 0x01
        header1[2] = 0x01 // page 1
        header1[3] = 0x00 // isLast = 0
        header1[4] = (keyIndex + 1).toByte()
        System.arraycopy(bgrBytes, 0, header1, 16, packet1PayloadSize)
        connection.controlTransfer(0x21, 0x09, 0x0200, usbInterface.id, header1, header1.size, 1000)

        // Packet 2
        val header2 = ByteArray(8191)
        header2[0] = 0x02
        header2[1] = 0x01
        header2[2] = 0x02 // page 2
        header2[3] = 0x01 // isLast = 1
        header2[4] = (keyIndex + 1).toByte()
        System.arraycopy(bgrBytes, packet1PayloadSize, header2, 16, packet2PayloadSize)
        connection.controlTransfer(0x21, 0x09, 0x0200, usbInterface.id, header2, header2.size, 1000)
    }

    override fun clearAllKeys() {
        val blankBmp = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        for (i in 0 until 15) {
            writeKeyImage(i, blankBmp)
        }
    }
}
