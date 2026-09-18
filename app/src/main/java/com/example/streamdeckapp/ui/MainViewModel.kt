package com.example.streamdeckapp.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.model.DeckPage
import com.example.streamdeckapp.model.DeckProfile
import com.example.streamdeckapp.service.StreamDeckService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var service: StreamDeckService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound

    val activeProfile = MutableStateFlow<DeckProfile?>(null)
    val pressedKeyIndex = MutableStateFlow<Int?>(null)
    val isConnected = MutableStateFlow(false)
    val connectedDeviceName = MutableStateFlow("Disconnected")
    val serverUrl = MutableStateFlow("")

    val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? StreamDeckService.LocalBinder ?: return
            val s = localBinder.getService()
            service = s
            _isServiceBound.value = true

            viewModelScope.launch {
                s.activeProfile.collect { activeProfile.value = it }
            }
            viewModelScope.launch {
                s.pressedKeyIndex.collect { pressedKeyIndex.value = it }
            }
            viewModelScope.launch {
                s.streamDeckManager.isConnected.collect { isConnected.value = it }
            }
            viewModelScope.launch {
                s.streamDeckManager.connectedDeviceName.collect { connectedDeviceName.value = it }
            }
            viewModelScope.launch {
                s.serverUrl.collect { serverUrl.value = it }
            }

            s.refreshDisplays()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _isServiceBound.value = false
        }
    }

    init {
        loadInstalledApps()
        startAndBindService()
    }

    private fun startAndBindService() {
        try {
            val app = getApplication<Application>()
            val intent = Intent(app, StreamDeckService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    app.startForegroundService(intent)
                } else {
                    app.startService(intent)
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Service start fallback", e)
            }
            app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to bind StreamDeckService", e)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = getApplication<Application>().packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val list = resolveInfos.mapNotNull { info ->
                    try {
                        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                        val name = try {
                            info.loadLabel(pm).toString()
                        } catch (e: Throwable) {
                            pkg
                        }
                        InstalledAppInfo(appName = name, packageName = pkg)
                    } catch (e: Throwable) {
                        null
                    }
                }.sortedBy { it.appName.lowercase() }
                installedApps.value = list
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Failed to load installed apps", e)
            }
        }
    }

    fun onVirtualKeyClicked(slotIndex: Int) {
        val profile = activeProfile.value ?: return
        val currentPage = profile.pages.getOrNull(profile.activePageIndex) ?: return
        val action = currentPage.slots.getOrNull(slotIndex) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            service?.actionHandler?.executeAction(action)
            service?.refreshDisplays()
        }
    }

    fun updateAction(slotIndex: Int, updatedAction: DeckAction) {
        val profile = activeProfile.value ?: return
        val pages = profile.pages.toMutableList()
        val currentPageIndex = profile.activePageIndex.coerceIn(0, pages.size - 1)
        val currentPage = pages[currentPageIndex]

        val newSlots = currentPage.slots.toMutableList()
        if (slotIndex in 0 until newSlots.size) {
            newSlots[slotIndex] = updatedAction
        }

        pages[currentPageIndex] = currentPage.copy(slots = newSlots)
        val updatedProfile = profile.copy(pages = pages)

        service?.updateProfile(updatedProfile)
    }

    fun switchPage(pageIndex: Int) {
        service?.switchPage(pageIndex)
    }

    fun addPage(pageName: String = "New Page") {
        val profile = activeProfile.value ?: return
        val newPage = DeckPage(name = pageName, slots = MutableList(15) { DeckAction() })
        val pages = profile.pages.toMutableList()
        pages.add(newPage)

        val updatedProfile = profile.copy(pages = pages, activePageIndex = pages.size - 1)
        service?.updateProfile(updatedProfile)
    }

    fun deletePage(pageIndex: Int) {
        val profile = activeProfile.value ?: return
        if (profile.pages.size <= 1) return // Keep at least 1 page

        val pages = profile.pages.toMutableList()
        pages.removeAt(pageIndex)

        val newActiveIdx = profile.activePageIndex.coerceIn(0, pages.size - 1)
        val updatedProfile = profile.copy(pages = pages, activePageIndex = newActiveIdx)
        service?.updateProfile(updatedProfile)
    }

    fun movePageUp(index: Int) {
        val profile = activeProfile.value ?: return
        if (index <= 0 || index >= profile.pages.size) return
        val pages = profile.pages.toMutableList()
        val temp = pages[index]
        pages[index] = pages[index - 1]
        pages[index - 1] = temp
        val newActive = if (profile.activePageIndex == index) index - 1 else if (profile.activePageIndex == index - 1) index else profile.activePageIndex
        val updated = profile.copy(pages = pages, activePageIndex = newActive)
        service?.updateProfile(updated)
    }

    fun movePageDown(index: Int) {
        val profile = activeProfile.value ?: return
        if (index < 0 || index >= profile.pages.size - 1) return
        val pages = profile.pages.toMutableList()
        val temp = pages[index]
        pages[index] = pages[index + 1]
        pages[index + 1] = temp
        val newActive = if (profile.activePageIndex == index) index + 1 else if (profile.activePageIndex == index + 1) index else profile.activePageIndex
        val updated = profile.copy(pages = pages, activePageIndex = newActive)
        service?.updateProfile(updated)
    }

    fun renamePage(index: Int, newName: String) {
        val profile = activeProfile.value ?: return
        if (index !in 0 until profile.pages.size) return
        val pages = profile.pages.toMutableList()
        pages[index] = pages[index].copy(name = newName)
        val updated = profile.copy(pages = pages)
        service?.updateProfile(updated)
    }

    fun rotateDeck() {
        val profile = activeProfile.value ?: return
        val nextRotation = (profile.rotationDegrees + 90) % 360
        service?.setRotation(nextRotation)
    }

    fun connectUsbDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            service?.streamDeckManager?.connectDevice()
            service?.refreshDisplays()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isServiceBound.value) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
