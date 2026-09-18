package com.example.streamdeckapp.engine

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import com.example.streamdeckapp.model.ActionType
import com.example.streamdeckapp.model.DeckAction

class KeyRenderer(private val context: Context) {

    private val KEY_SIZE = 72

    fun renderKey(
        action: DeckAction,
        isPlaying: Boolean = false,
        isMuted: Boolean = false,
        pageIndexInfo: String? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(KEY_SIZE, KEY_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Determine primary signature theme color for this action
        val primaryColor = getActionPrimaryColor(action, isPlaying, isMuted)

        // 1. Draw Rich Gradient Background
        drawVibrantBackground(canvas, action, primaryColor)

        // 2. Draw Colorful Central Icon & Graphics
        drawColorfulIcon(canvas, action, primaryColor, isPlaying, isMuted)

        // 3. Draw Crisp High-Contrast Label
        drawCrispLabel(canvas, action, isPlaying, isMuted, pageIndexInfo)

        return bitmap
    }

    private fun getActionPrimaryColor(action: DeckAction, isPlaying: Boolean, isMuted: Boolean): Int {
        return when (action.type) {
            // System & Navigation
            ActionType.SYSTEM_HOME -> Color.parseColor("#29B6F6")         // Bright Sky Blue
            ActionType.SYSTEM_BACK -> Color.parseColor("#AB47BC")         // Purple
            ActionType.SYSTEM_RECENTS -> Color.parseColor("#26A69A")      // Teal
            ActionType.SYSTEM_NOTIFICATIONS -> Color.parseColor("#FFA726")// Orange
            ActionType.SYSTEM_QUICK_SETTINGS -> Color.parseColor("#7E57C2") // Deep Indigo
            ActionType.SYSTEM_LOCK_SCREEN -> Color.parseColor("#EF5350")  // Red
            ActionType.SYSTEM_POWER_DIALOG -> Color.parseColor("#D32F2F") // Crimson
            ActionType.SYSTEM_SPLIT_SCREEN -> Color.parseColor("#5C6BC0") // Indigo
            ActionType.SYSTEM_SCREENSHOT -> Color.parseColor("#EC407A")   // Rose Pink

            // Screen Touch & Gesture Simulation
            ActionType.SIMULATED_TAP -> Color.parseColor("#00E5FF")       // Neon Cyan
            ActionType.SIMULATED_SWIPE,
            ActionType.SIMULATED_SWIPE_UP,
            ActionType.SIMULATED_SWIPE_DOWN,
            ActionType.SIMULATED_SWIPE_LEFT,
            ActionType.SIMULATED_SWIPE_RIGHT -> Color.parseColor("#69F0AE") // Mint Spring Green

            // Media & Audio
            ActionType.MEDIA_PLAY_PAUSE -> {
                if (isPlaying) Color.parseColor("#FF9100")
                else Color.parseColor("#00E676")
            }
            ActionType.MEDIA_PLAY -> Color.parseColor("#00E676")
            ActionType.MEDIA_PAUSE -> Color.parseColor("#FF9100")
            ActionType.MEDIA_STOP -> Color.parseColor("#FF1744")
            ActionType.MEDIA_NEXT, ActionType.MEDIA_PREV -> Color.parseColor("#00E5FF")
            ActionType.MEDIA_FAST_FORWARD, ActionType.MEDIA_REWIND -> Color.parseColor("#1DE9B6")
            ActionType.VOLUME_UP -> Color.parseColor("#FFD600")
            ActionType.VOLUME_DOWN -> Color.parseColor("#FF6D00")
            ActionType.VOLUME_MUTE_TOGGLE -> {
                if (isMuted) Color.parseColor("#FF1744")
                else Color.parseColor("#00B0FF")
            }

            // Brightness & Display
            ActionType.BRIGHTNESS_UP, ActionType.BRIGHTNESS_DOWN -> Color.parseColor("#FFD700") // Gold
            ActionType.SCREEN_OFF -> Color.parseColor("#78909C")          // Cool Slate

            // Settings Shortcuts
            ActionType.SETTINGS_BLUETOOTH -> Color.parseColor("#2979FF") // Electric Blue
            ActionType.SETTINGS_WIFI -> Color.parseColor("#00E676")      // Cyber Green
            ActionType.SETTINGS_SOUND -> Color.parseColor("#FF9100")     // Amber
            ActionType.SETTINGS_DISPLAY -> Color.parseColor("#FFEA00")   // Yellow
            ActionType.SETTINGS_DATE_TIME -> Color.parseColor("#AB47BC") // Purple
            ActionType.SETTINGS_LOCATION -> Color.parseColor("#FF5252")  // Coral
            ActionType.SETTINGS_APPS -> Color.parseColor("#E040FB")      // Magenta
            ActionType.SETTINGS_MAIN -> Color.parseColor("#90A4AE")      // Silver Grey

            // Tools & Vehicle
            ActionType.TORCH_TOGGLE -> Color.parseColor("#FFF176")       // Bright Flash Yellow
            ActionType.OPEN_URL -> Color.parseColor("#40C4FF")           // Web Blue
            ActionType.VOICE_ASSISTANT -> Color.parseColor("#EA4335")    // Google Red
            ActionType.DIAL_PHONE -> Color.parseColor("#34A853")         // Call Green
            ActionType.LAUNCH_APP -> Color.parseColor("#FF4081")         // Neon Pink
            ActionType.SHELL_COMMAND -> Color.parseColor("#00E676")      // Matrix Green

            // Page Navigation
            ActionType.NEXT_PAGE, ActionType.PREV_PAGE -> Color.parseColor("#E040FB")
            ActionType.GOTO_PAGE -> Color.parseColor("#7C4DFF")
            ActionType.NONE -> Color.parseColor("#455A64")
        }
    }

    private fun drawVibrantBackground(canvas: Canvas, action: DeckAction, primaryColor: Int) {
        val baseColorInt = action.backgroundColor.toInt()

        // Create top-to-bottom subtle gradient
        val topColor = blendColors(baseColorInt, primaryColor, 0.25f)
        val bottomColor = blendColors(baseColorInt, Color.BLACK, 0.40f)

        val gradient = LinearGradient(
            0f, 0f, 0f, KEY_SIZE.toFloat(),
            topColor, bottomColor,
            Shader.TileMode.CLAMP
        )

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        val bgRect = RectF(0f, 0f, KEY_SIZE.toFloat(), KEY_SIZE.toFloat())
        canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)

        // Outer colorful glow / rim border
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(140, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        }
        canvas.drawRoundRect(RectF(1.5f, 1.5f, KEY_SIZE - 1.5f, KEY_SIZE - 1.5f), 9f, 9f, glowPaint)

        // Subtle glossy top specular highlight
        val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, KEY_SIZE * 0.45f,
                Color.argb(55, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(2f, 2f, KEY_SIZE - 2f, KEY_SIZE * 0.45f), 8f, 8f, glossPaint)
    }

    private fun drawColorfulIcon(
        canvas: Canvas,
        action: DeckAction,
        primaryColor: Int,
        isPlaying: Boolean,
        isMuted: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = primaryColor
        }

        val centerX = KEY_SIZE / 2f
        val centerY = KEY_SIZE / 2f - 4f

        // Draw custom image asset if set by user
        if (action.customImagePath.isNotBlank()) {
            try {
                val file = java.io.File(action.customImagePath)
                if (file.exists()) {
                    val customBmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (customBmp != null) {
                        val scaled = Bitmap.createScaledBitmap(customBmp, 36, 36, true)
                        canvas.drawBitmap(scaled, (KEY_SIZE - 36) / 2f, (KEY_SIZE - 36) / 2f - 4f, null)
                        return
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        when (action.type) {
            ActionType.MEDIA_PLAY_PAUSE -> {
                if (isPlaying) {
                    // Vivid Pause Bars with shadow
                    paint.color = primaryColor
                    val barWidth = 6.5f
                    val barHeight = 22f
                    canvas.drawRoundRect(RectF(centerX - 8.5f, centerY - barHeight / 2, centerX - 8.5f + barWidth, centerY + barHeight / 2), 3f, 3f, paint)
                    canvas.drawRoundRect(RectF(centerX + 2f, centerY - barHeight / 2, centerX + 2f + barWidth, centerY + barHeight / 2), 3f, 3f, paint)
                } else {
                    // Vivid Play Triangle
                    val path = Path().apply {
                        moveTo(centerX - 7f, centerY - 13f)
                        lineTo(centerX + 13f, centerY)
                        lineTo(centerX - 7f, centerY + 13f)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }

            ActionType.MEDIA_NEXT -> {
                paint.color = primaryColor
                // Two glowing right arrows
                val path1 = Path().apply {
                    moveTo(centerX - 10f, centerY - 11f)
                    lineTo(centerX - 1f, centerY)
                    lineTo(centerX - 10f, centerY + 11f)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(centerX - 1f, centerY - 11f)
                    lineTo(centerX + 8f, centerY)
                    lineTo(centerX - 1f, centerY + 11f)
                    close()
                }
                canvas.drawPath(path1, paint)
                canvas.drawPath(path2, paint)
                // Next bar
                canvas.drawRoundRect(RectF(centerX + 9f, centerY - 11f, centerX + 12f, centerY + 11f), 1.5f, 1.5f, paint)
            }

            ActionType.MEDIA_PREV -> {
                paint.color = primaryColor
                // Prev bar
                canvas.drawRoundRect(RectF(centerX - 12f, centerY - 11f, centerX - 9f, centerY + 11f), 1.5f, 1.5f, paint)
                // Two glowing left arrows
                val path1 = Path().apply {
                    moveTo(centerX + 1f, centerY - 11f)
                    lineTo(centerX - 8f, centerY)
                    lineTo(centerX + 1f, centerY + 11f)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(centerX + 10f, centerY - 11f)
                    lineTo(centerX + 1f, centerY)
                    lineTo(centerX + 10f, centerY + 11f)
                    close()
                }
                canvas.drawPath(path1, paint)
                canvas.drawPath(path2, paint)
            }

            ActionType.VOLUME_UP -> {
                paint.color = primaryColor
                // Speaker body
                val speakerPath = Path().apply {
                    moveTo(centerX - 14f, centerY - 5f)
                    lineTo(centerX - 7f, centerY - 5f)
                    lineTo(centerX - 1f, centerY - 11f)
                    lineTo(centerX - 1f, centerY + 11f)
                    lineTo(centerX - 7f, centerY + 5f)
                    lineTo(centerX - 14f, centerY + 5f)
                    close()
                }
                canvas.drawPath(speakerPath, paint)

                // Plus badge in radiant yellow
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(centerX + 6f, centerY, centerX + 15f, centerY, paint)
                canvas.drawLine(centerX + 10.5f, centerY - 4.5f, centerX + 10.5f, centerY + 4.5f, paint)
            }

            ActionType.VOLUME_DOWN -> {
                paint.color = primaryColor
                val speakerPath = Path().apply {
                    moveTo(centerX - 14f, centerY - 5f)
                    lineTo(centerX - 7f, centerY - 5f)
                    lineTo(centerX - 1f, centerY - 11f)
                    lineTo(centerX - 1f, centerY + 11f)
                    lineTo(centerX - 7f, centerY + 5f)
                    lineTo(centerX - 14f, centerY + 5f)
                    close()
                }
                canvas.drawPath(speakerPath, paint)

                // Minus sign in warm orange
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.5f
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(centerX + 6f, centerY, centerX + 15f, centerY, paint)
            }

            ActionType.VOLUME_MUTE_TOGGLE -> {
                paint.color = primaryColor
                val speakerPath = Path().apply {
                    moveTo(centerX - 13f, centerY - 5f)
                    lineTo(centerX - 6f, centerY - 5f)
                    lineTo(centerX, centerY - 11f)
                    lineTo(centerX, centerY + 11f)
                    lineTo(centerX - 6f, centerY + 5f)
                    lineTo(centerX - 13f, centerY + 5f)
                    close()
                }
                canvas.drawPath(speakerPath, paint)

                if (isMuted) {
                    // Bright red diagonal slash
                    val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FF1744")
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(centerX - 14f, centerY + 13f, centerX + 14f, centerY - 13f, slashPaint)
                } else {
                    // Radiating soundwave arcs
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2.5f
                    val arc1 = RectF(centerX + 2f, centerY - 6f, centerX + 8f, centerY + 6f)
                    canvas.drawArc(arc1, -55f, 110f, false, paint)
                    val arc2 = RectF(centerX + 4f, centerY - 10f, centerX + 14f, centerY + 10f)
                    canvas.drawArc(arc2, -55f, 110f, false, paint)
                }
            }

            ActionType.SYSTEM_HOME -> {
                paint.color = primaryColor
                val roof = Path().apply {
                    moveTo(centerX, centerY - 13f)
                    lineTo(centerX + 12f, centerY - 2f)
                    lineTo(centerX - 12f, centerY - 2f)
                    close()
                }
                canvas.drawPath(roof, paint)
                canvas.drawRect(RectF(centerX - 8f, centerY - 2f, centerX + 8f, centerY + 10f), paint)
            }

            ActionType.SYSTEM_BACK -> {
                paint.color = primaryColor
                val arrow = Path().apply {
                    moveTo(centerX - 9f, centerY)
                    lineTo(centerX + 2f, centerY - 10f)
                    lineTo(centerX + 2f, centerY - 4f)
                    lineTo(centerX + 10f, centerY - 4f)
                    lineTo(centerX + 10f, centerY + 4f)
                    lineTo(centerX + 2f, centerY + 4f)
                    lineTo(centerX + 2f, centerY + 10f)
                    close()
                }
                canvas.drawPath(arrow, paint)
            }

            ActionType.SYSTEM_RECENTS -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(centerX - 10f, centerY - 6f, centerX + 3f, centerY + 8f), 2f, 2f, paint)
                canvas.drawRoundRect(RectF(centerX - 4f, centerY - 11f, centerX + 9f, centerY + 3f), 2f, 2f, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SYSTEM_NOTIFICATIONS -> {
                paint.color = primaryColor
                val bell = Path().apply {
                    moveTo(centerX, centerY - 12f)
                    quadTo(centerX + 9f, centerY - 4f, centerX + 9f, centerY + 5f)
                    lineTo(centerX - 9f, centerY + 5f)
                    quadTo(centerX - 9f, centerY - 4f, centerX, centerY - 12f)
                    close()
                }
                canvas.drawPath(bell, paint)
                canvas.drawCircle(centerX, centerY + 8f, 2.5f, paint)
            }

            ActionType.SYSTEM_QUICK_SETTINGS, ActionType.SETTINGS_MAIN -> {
                paint.color = primaryColor
                canvas.drawCircle(centerX, centerY, 10f, paint)
                val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawCircle(centerX, centerY, 4f, holePaint)
            }

            ActionType.SYSTEM_LOCK_SCREEN, ActionType.SCREEN_OFF -> {
                paint.color = primaryColor
                canvas.drawRoundRect(RectF(centerX - 8f, centerY - 2f, centerX + 8f, centerY + 10f), 3f, 3f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawArc(RectF(centerX - 5f, centerY - 11f, centerX + 5f, centerY - 1f), 180f, 180f, false, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SYSTEM_POWER_DIALOG -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawArc(RectF(centerX - 9f, centerY - 7f, centerX + 9f, centerY + 11f), 140f, 260f, false, paint)
                canvas.drawLine(centerX, centerY - 12f, centerX, centerY - 1f, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SYSTEM_SPLIT_SCREEN -> {
                paint.color = primaryColor
                canvas.drawRoundRect(RectF(centerX - 10f, centerY - 9f, centerX - 1f, centerY + 9f), 2f, 2f, paint)
                canvas.drawRoundRect(RectF(centerX + 1f, centerY - 9f, centerX + 10f, centerY + 9f), 2f, 2f, paint)
            }

            ActionType.SYSTEM_SCREENSHOT -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawRoundRect(RectF(centerX - 10f, centerY - 8f, centerX + 10f, centerY + 8f), 3f, 3f, paint)
                canvas.drawCircle(centerX, centerY, 4f, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SIMULATED_TAP -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawCircle(centerX, centerY, 9f, paint)
                canvas.drawLine(centerX - 13f, centerY, centerX + 13f, centerY, paint)
                canvas.drawLine(centerX, centerY - 13f, centerX, centerY + 13f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, 3f, paint)
            }

            ActionType.SIMULATED_SWIPE, ActionType.SIMULATED_SWIPE_UP, ActionType.SIMULATED_SWIPE_DOWN,
            ActionType.SIMULATED_SWIPE_LEFT, ActionType.SIMULATED_SWIPE_RIGHT -> {
                paint.color = primaryColor
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val arrowSymbol = when (action.type) {
                    ActionType.SIMULATED_SWIPE_UP -> "↑"
                    ActionType.SIMULATED_SWIPE_DOWN -> "↓"
                    ActionType.SIMULATED_SWIPE_LEFT -> "←"
                    ActionType.SIMULATED_SWIPE_RIGHT -> "→"
                    else -> "⇄"
                }
                canvas.drawText(arrowSymbol, centerX, centerY + 8f, textPaint)
            }

            ActionType.MEDIA_PLAY -> {
                paint.color = primaryColor
                val path = Path().apply {
                    moveTo(centerX - 7f, centerY - 13f)
                    lineTo(centerX + 13f, centerY)
                    lineTo(centerX - 7f, centerY + 13f)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            ActionType.MEDIA_PAUSE -> {
                paint.color = primaryColor
                val barWidth = 6.5f
                val barHeight = 22f
                canvas.drawRoundRect(RectF(centerX - 8.5f, centerY - barHeight / 2, centerX - 8.5f + barWidth, centerY + barHeight / 2), 3f, 3f, paint)
                canvas.drawRoundRect(RectF(centerX + 2f, centerY - barHeight / 2, centerX + 2f + barWidth, centerY + barHeight / 2), 3f, 3f, paint)
            }

            ActionType.MEDIA_STOP -> {
                paint.color = primaryColor
                canvas.drawRoundRect(RectF(centerX - 10f, centerY - 10f, centerX + 10f, centerY + 10f), 3f, 3f, paint)
            }

            ActionType.MEDIA_FAST_FORWARD -> {
                paint.color = primaryColor
                val p1 = Path().apply {
                    moveTo(centerX - 9f, centerY - 10f)
                    lineTo(centerX + 1f, centerY)
                    lineTo(centerX - 9f, centerY + 10f)
                    close()
                }
                val p2 = Path().apply {
                    moveTo(centerX + 1f, centerY - 10f)
                    lineTo(centerX + 11f, centerY)
                    lineTo(centerX + 1f, centerY + 10f)
                    close()
                }
                canvas.drawPath(p1, paint)
                canvas.drawPath(p2, paint)
            }

            ActionType.MEDIA_REWIND -> {
                paint.color = primaryColor
                val p1 = Path().apply {
                    moveTo(centerX - 1f, centerY - 10f)
                    lineTo(centerX - 11f, centerY)
                    lineTo(centerX - 1f, centerY + 10f)
                    close()
                }
                val p2 = Path().apply {
                    moveTo(centerX + 9f, centerY - 10f)
                    lineTo(centerX - 1f, centerY)
                    lineTo(centerX + 9f, centerY + 10f)
                    close()
                }
                canvas.drawPath(p1, paint)
                canvas.drawPath(p2, paint)
            }

            ActionType.BRIGHTNESS_UP, ActionType.BRIGHTNESS_DOWN -> {
                paint.color = primaryColor
                canvas.drawCircle(centerX, centerY, 7f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians(angle.toDouble())
                    val x1 = centerX + (10f * Math.cos(rad)).toFloat()
                    val y1 = centerY + (10f * Math.sin(rad)).toFloat()
                    val x2 = centerX + (14f * Math.cos(rad)).toFloat()
                    val y2 = centerY + (14f * Math.sin(rad)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
                paint.style = Paint.Style.FILL
            }

            ActionType.SETTINGS_BLUETOOTH -> {
                paint.color = primaryColor
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("ᛒ", centerX, centerY + 7f, textPaint)
            }

            ActionType.SETTINGS_WIFI -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawArc(RectF(centerX - 12f, centerY - 8f, centerX + 12f, centerY + 12f), 200f, 140f, false, paint)
                canvas.drawArc(RectF(centerX - 7f, centerY - 3f, centerX + 7f, centerY + 11f), 205f, 130f, false, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY + 9f, 2.5f, paint)
            }

            ActionType.SETTINGS_SOUND -> {
                paint.color = primaryColor
                val speakerPath = Path().apply {
                    moveTo(centerX - 11f, centerY - 4f)
                    lineTo(centerX - 5f, centerY - 4f)
                    lineTo(centerX - 1f, centerY - 9f)
                    lineTo(centerX - 1f, centerY + 9f)
                    lineTo(centerX - 5f, centerY + 4f)
                    lineTo(centerX - 11f, centerY + 4f)
                    close()
                }
                canvas.drawPath(speakerPath, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawArc(RectF(centerX + 1f, centerY - 5f, centerX + 7f, centerY + 5f), -50f, 100f, false, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SETTINGS_DISPLAY -> {
                paint.color = primaryColor
                canvas.drawRoundRect(RectF(centerX - 11f, centerY - 9f, centerX + 11f, centerY + 5f), 2f, 2f, paint)
                canvas.drawRect(RectF(centerX - 2f, centerY + 5f, centerX + 2f, centerY + 9f), paint)
                canvas.drawRect(RectF(centerX - 6f, centerY + 9f, centerX + 6f, centerY + 11f), paint)
            }

            ActionType.SETTINGS_DATE_TIME -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawCircle(centerX, centerY, 10f, paint)
                canvas.drawLine(centerX, centerY, centerX, centerY - 6f, paint)
                canvas.drawLine(centerX, centerY, centerX + 5f, centerY, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.SETTINGS_LOCATION -> {
                paint.color = primaryColor
                val pin = Path().apply {
                    moveTo(centerX, centerY + 11f)
                    lineTo(centerX - 7f, centerY - 1f)
                    quadTo(centerX - 7f, centerY - 10f, centerX, centerY - 10f)
                    quadTo(centerX + 7f, centerY - 10f, centerX + 7f, centerY - 1f)
                    close()
                }
                canvas.drawPath(pin, paint)
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawCircle(centerX, centerY - 2f, 2.5f, dotPaint)
            }

            ActionType.SETTINGS_APPS -> {
                paint.color = primaryColor
                for (r in -1..1) {
                    for (c in -1..1) {
                        canvas.drawCircle(centerX + (c * 7f), centerY + (r * 7f), 2f, paint)
                    }
                }
            }

            ActionType.TORCH_TOGGLE -> {
                paint.color = primaryColor
                val torch = Path().apply {
                    moveTo(centerX - 4f, centerY - 9f)
                    lineTo(centerX + 4f, centerY - 9f)
                    lineTo(centerX + 3f, centerY + 10f)
                    lineTo(centerX - 3f, centerY + 10f)
                    close()
                }
                canvas.drawPath(torch, paint)
                canvas.drawRoundRect(RectF(centerX - 6f, centerY - 12f, centerX + 6f, centerY - 9f), 1.5f, 1.5f, paint)
            }

            ActionType.OPEN_URL -> {
                paint.color = primaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawCircle(centerX, centerY, 10f, paint)
                canvas.drawLine(centerX - 10f, centerY, centerX + 10f, centerY, paint)
                canvas.drawOval(RectF(centerX - 5f, centerY - 10f, centerX + 5f, centerY + 10f), paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.VOICE_ASSISTANT -> {
                paint.color = primaryColor
                canvas.drawRoundRect(RectF(centerX - 4f, centerY - 11f, centerX + 4f, centerY + 1f), 4f, 4f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                canvas.drawArc(RectF(centerX - 7f, centerY - 6f, centerX + 7f, centerY + 4f), 0f, 180f, false, paint)
                canvas.drawLine(centerX, centerY + 4f, centerX, centerY + 9f, paint)
                paint.style = Paint.Style.FILL
            }

            ActionType.DIAL_PHONE -> {
                paint.color = primaryColor
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("📞", centerX, centerY + 7f, textPaint)
            }

            ActionType.NEXT_PAGE -> {
                paint.color = primaryColor
                val path = Path().apply {
                    moveTo(centerX - 8f, centerY - 13f)
                    lineTo(centerX + 8f, centerY)
                    lineTo(centerX - 8f, centerY + 13f)
                    lineTo(centerX - 3f, centerY)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            ActionType.PREV_PAGE -> {
                paint.color = primaryColor
                val path = Path().apply {
                    moveTo(centerX + 8f, centerY - 13f)
                    lineTo(centerX - 8f, centerY)
                    lineTo(centerX + 8f, centerY + 13f)
                    lineTo(centerX + 3f, centerY)
                    close()
                }
                canvas.drawPath(path, paint)
            }

            ActionType.GOTO_PAGE -> {
                paint.color = primaryColor
                val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(80, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(centerX, centerY, 15f, circlePaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 19f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${action.targetPageIndex + 1}", centerX, centerY + 7f, textPaint)
            }

            ActionType.LAUNCH_APP -> {
                if (action.targetPackageName.isNotBlank()) {
                    try {
                        val iconDrawable = context.packageManager.getApplicationIcon(action.targetPackageName)
                        val iconBitmap = if (iconDrawable is BitmapDrawable) {
                            iconDrawable.bitmap
                        } else {
                            val bmp = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
                            val c = Canvas(bmp)
                            iconDrawable.setBounds(0, 0, c.width, c.height)
                            iconDrawable.draw(c)
                            bmp
                        }
                        val scaled = Bitmap.createScaledBitmap(iconBitmap, 30, 30, true)
                        canvas.drawBitmap(scaled, centerX - 15f, centerY - 15f, null)
                        return
                    } catch (e: Throwable) {
                        // fallback
                    }
                }
                // Colorful Rocket Icon
                paint.color = primaryColor
                val rocket = Path().apply {
                    moveTo(centerX, centerY - 14f)
                    lineTo(centerX + 9f, centerY + 4f)
                    lineTo(centerX + 4f, centerY + 3f)
                    lineTo(centerX + 2f, centerY + 9f)
                    lineTo(centerX - 2f, centerY + 9f)
                    lineTo(centerX - 4f, centerY + 3f)
                    lineTo(centerX - 9f, centerY + 4f)
                    close()
                }
                canvas.drawPath(rocket, paint)
            }

            ActionType.SHELL_COMMAND -> {
                paint.color = Color.parseColor("#00E676") // Neon Matrix Green
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00E676")
                    textSize = 18f
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(">_", centerX, centerY + 7f, textPaint)
            }

            ActionType.NONE -> {
                paint.color = Color.argb(40, 255, 255, 255)
                canvas.drawCircle(centerX, centerY, 5f, paint)
            }
        }
    }

    private fun drawCrispLabel(
        canvas: Canvas,
        action: DeckAction,
        isPlaying: Boolean,
        isMuted: Boolean,
        pageIndexInfo: String?
    ) {
        val labelText = when {
            action.label.isNotBlank() -> action.label
            action.type == ActionType.SYSTEM_HOME -> "HOME"
            action.type == ActionType.SYSTEM_BACK -> "BACK"
            action.type == ActionType.SYSTEM_RECENTS -> "RECENTS"
            action.type == ActionType.SYSTEM_NOTIFICATIONS -> "NOTIF"
            action.type == ActionType.SYSTEM_QUICK_SETTINGS -> "QUICK"
            action.type == ActionType.SYSTEM_LOCK_SCREEN -> "LOCK"
            action.type == ActionType.SYSTEM_POWER_DIALOG -> "POWER"
            action.type == ActionType.SYSTEM_SPLIT_SCREEN -> "SPLIT"
            action.type == ActionType.SYSTEM_SCREENSHOT -> "SHOT"
            action.type == ActionType.SIMULATED_TAP -> "TAP"
            action.type == ActionType.SIMULATED_SWIPE -> "SWIPE"
            action.type == ActionType.SIMULATED_SWIPE_UP -> "SWIPE UP"
            action.type == ActionType.SIMULATED_SWIPE_DOWN -> "SWIPE DN"
            action.type == ActionType.SIMULATED_SWIPE_LEFT -> "SWIPE LT"
            action.type == ActionType.SIMULATED_SWIPE_RIGHT -> "SWIPE RT"
            action.type == ActionType.MEDIA_PLAY_PAUSE -> if (isPlaying) "PAUSE" else "PLAY"
            action.type == ActionType.MEDIA_PLAY -> "PLAY"
            action.type == ActionType.MEDIA_PAUSE -> "PAUSE"
            action.type == ActionType.MEDIA_STOP -> "STOP"
            action.type == ActionType.MEDIA_NEXT -> "NEXT"
            action.type == ActionType.MEDIA_PREV -> "PREV"
            action.type == ActionType.MEDIA_FAST_FORWARD -> "FWD"
            action.type == ActionType.MEDIA_REWIND -> "RWD"
            action.type == ActionType.VOLUME_MUTE_TOGGLE -> if (isMuted) "MUTED" else "MUTE"
            action.type == ActionType.VOLUME_UP -> "VOL +"
            action.type == ActionType.VOLUME_DOWN -> "VOL -"
            action.type == ActionType.BRIGHTNESS_UP -> "BRT +"
            action.type == ActionType.BRIGHTNESS_DOWN -> "BRT -"
            action.type == ActionType.SCREEN_OFF -> "SLEEP"
            action.type == ActionType.SETTINGS_BLUETOOTH -> "BT"
            action.type == ActionType.SETTINGS_WIFI -> "WIFI"
            action.type == ActionType.SETTINGS_SOUND -> "SOUND"
            action.type == ActionType.SETTINGS_DISPLAY -> "DISP"
            action.type == ActionType.SETTINGS_DATE_TIME -> "TIME"
            action.type == ActionType.SETTINGS_LOCATION -> "GPS"
            action.type == ActionType.SETTINGS_APPS -> "APPS"
            action.type == ActionType.SETTINGS_MAIN -> "SETTINGS"
            action.type == ActionType.TORCH_TOGGLE -> "TORCH"
            action.type == ActionType.OPEN_URL -> "WEB"
            action.type == ActionType.VOICE_ASSISTANT -> "VOICE"
            action.type == ActionType.DIAL_PHONE -> "PHONE"
            action.type == ActionType.NEXT_PAGE -> "NEXT"
            action.type == ActionType.PREV_PAGE -> "PREV"
            action.type == ActionType.GOTO_PAGE -> pageIndexInfo ?: "PAGE ${action.targetPageIndex + 1}"
            action.type == ActionType.LAUNCH_APP -> action.targetAppName.takeIf { it.isNotBlank() } ?: "APP"
            action.type == ActionType.SHELL_COMMAND -> "CMD"
            else -> ""
        }

        if (labelText.isBlank()) return

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = action.labelColor.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)

        val textY = KEY_SIZE - 5f
        val pillRect = RectF(
            (KEY_SIZE / 2f) - (textBounds.width() / 2f) - 4f,
            textY - textBounds.height() - 2f,
            (KEY_SIZE / 2f) + (textBounds.width() / 2f) + 4f,
            textY + 2f
        )

        // Dark contrasting pill behind text for readability
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 4f, 4f, pillPaint)

        // Draw label text
        canvas.drawText(labelText, KEY_SIZE / 2f, textY, textPaint)
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }
}
