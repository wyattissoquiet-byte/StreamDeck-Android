package com.example.streamdeckapp.usb

import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.usb.*
import com.example.streamdeckapp.model.DeckProfile
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class StreamDeckV2Driver(
    device: UsbDevice,
    connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : StreamDeckDevice(device, connection) {

    private val endpointIn: UsbEndpoint? = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .find { it.direction == UsbConstants.USB_DIR_IN }

    private val endpointOut: UsbEndpoint? = (0 until usbInterface.endpointCount)
        .map { usbInterface.getEndpoint(it) }
        .find { it.direction == UsbConstants.USB_DIR_OUT }

    private var pollThread: Thread? = null
    private val previousKeyState = BooleanArray(15)

    override fun startListening() {
        if (isRunning) return
        isRunning = true

        pollThread = thread(start = true, name = "StreamDeckV2Poll") {
            val buffer = ByteArray(endpointIn?.maxPacketSize ?: 512)
            while (isRunning) {
                if (endpointIn != null) {
                    val bytesRead = connection.bulkTransfer(endpointIn, buffer, buffer.size, 100)
                    if (bytesRead >= 4) {
                        // V2/MK2 report header format: byte 0/1 header, byte 2/3 num_keys, byte 4.. keys
                        val keyOffset = if (buffer[0] == 0x01.toByte() || buffer[0] == 0x00.toByte()) 4 else 1
                        for (i in 0 until 15) {
                            if (keyOffset + i < bytesRead) {
                                val isPressed = buffer[keyOffset + i] != 0.toByte()
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
        val payload = ByteArray(32)
        payload[0] = 0x03
        payload[1] = 0x08
        payload[2] = percent.coerceIn(0, 100).toByte()
        connection.controlTransfer(0x21, 0x09, 0x0300, usbInterface.id, payload, payload.size, 1000)
    }

    override fun writeKeyImage(keyIndex: Int, bitmap: Bitmap) {
        if (keyIndex !in 0..14) return

        // Rotate by hardware baseline (180) + user orientation setting
        val finalRotation = (180f + rotationDegrees) % 360f
        val rotated = if (finalRotation != 0f) {
            val matrix = Matrix().apply { postRotate(finalRotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // Compress to JPEG
        val baos = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val jpegBytes = baos.toByteArray()

        val MAX_PAYLOAD_PER_PACKET = 1016
        val PACKET_HEADER_SIZE = 8
        val PACKET_TOTAL_SIZE = 1024

        var bytesRemaining = jpegBytes.size
        var packetIndex = 0

        while (bytesRemaining > 0) {
            val chunkSize = minOf(bytesRemaining, MAX_PAYLOAD_PER_PACKET)
            val isLast = if (bytesRemaining <= MAX_PAYLOAD_PER_PACKET) 1 else 0

            val packet = ByteArray(PACKET_TOTAL_SIZE)
            packet[0] = 0x02
            packet[1] = 0x07
            packet[2] = keyIndex.toByte()
            packet[3] = isLast.toByte()
            packet[4] = (chunkSize and 0xFF).toByte()
            packet[5] = (chunkSize shr 8 and 0xFF).toByte()
            packet[6] = (packetIndex and 0xFF).toByte()
            packet[7] = (packetIndex shr 8 and 0xFF).toByte()

            val offset = packetIndex * MAX_PAYLOAD_PER_PACKET
            System.arraycopy(jpegBytes, offset, packet, PACKET_HEADER_SIZE, chunkSize)

            if (endpointOut != null) {
                connection.bulkTransfer(endpointOut, packet, packet.size, 1000)
            } else {
                connection.controlTransfer(0x21, 0x09, 0x0200, usbInterface.id, packet, packet.size, 1000)
            }

            bytesRemaining -= chunkSize
            packetIndex++
        }
    }

    override fun clearAllKeys() {
        val blankBmp = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        for (i in 0 until 15) {
            writeKeyImage(i, blankBmp)
        }
    }
}
