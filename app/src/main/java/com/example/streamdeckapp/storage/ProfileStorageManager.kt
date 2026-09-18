package com.example.streamdeckapp.storage

import android.content.Context
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction
import com.example.streamdeckapp.model.DeckPage
import com.example.streamdeckapp.model.DeckProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProfileStorageManager(private val context: Context) {

    private val configFile = File(context.filesDir, "streamdeck_profile.json")

    fun loadProfile(): DeckProfile {
        if (!configFile.exists()) {
            val defaultProfile = createDefaultProfile()
            saveProfile(defaultProfile)
            return defaultProfile
        }

        return try {
            val jsonStr = configFile.readText()
            parseProfileJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = createDefaultProfile()
            saveProfile(fallback)
            fallback
        }
    }

    fun saveProfile(profile: DeckProfile) {
        try {
            val jsonStr = serializeProfileJson(profile)
            configFile.writeText(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDefaultProfile(): DeckProfile {
        val page1Slots = MutableList(15) { DeckAction() }

        // Slot 0: Prev Track (Electric Blue)
        page1Slots[0] = DeckAction(
            type = ActionType.MEDIA_PREV,
            label = "PREV",
            backgroundColor = 0xFF003366
        )
        // Slot 1: Play/Pause (Vivid Emerald Green)
        page1Slots[1] = DeckAction(
            type = ActionType.MEDIA_PLAY_PAUSE,
            label = "PLAY/PAUSE",
            backgroundColor = 0xFF004D20
        )
        // Slot 2: Next Track (Electric Blue)
        page1Slots[2] = DeckAction(
            type = ActionType.MEDIA_NEXT,
            label = "NEXT",
            backgroundColor = 0xFF003366
        )
        // Slot 3: Volume Down (Vibrant Sunset Orange)
        page1Slots[3] = DeckAction(
            type = ActionType.VOLUME_DOWN,
            label = "VOL -",
            backgroundColor = 0xFF8A3B00
        )
        // Slot 4: Volume Up (Solar Gold)
        page1Slots[4] = DeckAction(
            type = ActionType.VOLUME_UP,
            label = "VOL +",
            backgroundColor = 0xFF8A3B00
        )
        // Slot 5: Mute Toggle (Vivid Crimson)
        page1Slots[5] = DeckAction(
            type = ActionType.VOLUME_MUTE_TOGGLE,
            label = "MUTE",
            backgroundColor = 0xFF7A0019
        )
        // Slot 6: Prev Page (Electric Violet)
        page1Slots[6] = DeckAction(
            type = ActionType.PREV_PAGE,
            label = "PREV PAGE",
            backgroundColor = 0xFF4A0072
        )
        // Slot 7: Next Page (Electric Violet)
        page1Slots[7] = DeckAction(
            type = ActionType.NEXT_PAGE,
            label = "NEXT PAGE",
            backgroundColor = 0xFF4A0072
        )

        val page1 = DeckPage(name = "Page 1 - Media", slots = page1Slots)
        val page2 = DeckPage(name = "Page 2 - Custom", slots = MutableList(15) { DeckAction() })

        return DeckProfile(
            id = "default_profile",
            name = "Car Stereo Profile",
            pages = mutableListOf(page1, page2),
            activePageIndex = 0,
            rotationDegrees = 0
        )
    }

    private fun parseProfileJson(jsonStr: String): DeckProfile {
        val root = JSONObject(jsonStr)
        val profileId = root.optString("id", "default_profile")
        val profileName = root.optString("name", "Default Profile")
        val activePageIndex = root.optInt("activePageIndex", 0)
        val rotationDegrees = root.optInt("rotationDegrees", 0)

        val pagesArray = root.optJSONArray("pages") ?: JSONArray()
        val pages = mutableListOf<DeckPage>()

        for (i in 0 until pagesArray.length()) {
            val pageObj = pagesArray.getJSONObject(i)
            val pageId = pageObj.optString("id")
            val pageName = pageObj.optString("name", "Page ${i + 1}")

            val slotsArray = pageObj.optJSONArray("slots") ?: JSONArray()
            val slots = MutableList(15) { DeckAction() }

            for (j in 0 until minOf(15, slotsArray.length())) {
                val actionObj = slotsArray.getJSONObject(j)
                val typeStr = actionObj.optString("type", "NONE")
                val actionType = try {
                    ActionType.valueOf(typeStr)
                } catch (e: Exception) {
                    ActionType.NONE
                }

                slots[j] = DeckAction(
                    id = actionObj.optString("id"),
                    type = actionType,
                    label = actionObj.optString("label", ""),
                    labelColor = actionObj.optLong("labelColor", 0xFFFFFFFF),
                    backgroundColor = actionObj.optLong("backgroundColor", 0xFF1C1C24),
                    iconName = actionObj.optString("iconName", ""),
                    targetPackageName = actionObj.optString("targetPackageName", ""),
                    targetAppName = actionObj.optString("targetAppName", ""),
                    shellCommand = actionObj.optString("shellCommand", ""),
                    targetPageIndex = actionObj.optInt("targetPageIndex", 0),
                    customImagePath = actionObj.optString("customImagePath", "")
                )
            }
            pages.add(DeckPage(id = pageId, name = pageName, slots = slots))
        }

        if (pages.isEmpty()) {
            return createDefaultProfile()
        }

        return DeckProfile(
            id = profileId,
            name = profileName,
            pages = pages,
            activePageIndex = activePageIndex.coerceIn(0, pages.size - 1),
            rotationDegrees = rotationDegrees
        )
    }

    private fun serializeProfileJson(profile: DeckProfile): String {
        val root = JSONObject()
        root.put("id", profile.id)
        root.put("name", profile.name)
        root.put("activePageIndex", profile.activePageIndex)
        root.put("rotationDegrees", profile.rotationDegrees)

        val pagesArray = JSONArray()
        for (page in profile.pages) {
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            pageObj.put("name", page.name)

            val slotsArray = JSONArray()
            for (action in page.slots) {
                val actionObj = JSONObject()
                actionObj.put("id", action.id)
                actionObj.put("type", action.type.name)
                actionObj.put("label", action.label)
                actionObj.put("labelColor", action.labelColor)
                actionObj.put("backgroundColor", action.backgroundColor)
                actionObj.put("iconName", action.iconName)
                actionObj.put("targetPackageName", action.targetPackageName)
                actionObj.put("targetAppName", action.targetAppName)
                actionObj.put("shellCommand", action.shellCommand)
                actionObj.put("targetPageIndex", action.targetPageIndex)
                actionObj.put("customImagePath", action.customImagePath)
                slotsArray.put(actionObj)
            }
            pageObj.put("slots", slotsArray)
            pagesArray.put(pageObj)
        }
        root.put("pages", pagesArray)

        return root.toString(2)
    }
}
