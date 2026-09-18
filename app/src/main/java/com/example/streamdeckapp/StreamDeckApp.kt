package com.example.streamdeckapp

import android.app.Application
import android.util.Log
import java.io.File

class StreamDeckApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set global exception handler to catch and log any unexpected crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("StreamDeckApp", "FATAL CRASH on thread ${thread.name}", throwable)
            try {
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText("Crash on ${System.currentTimeMillis()}:\n${throwable.stackTraceToString()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
