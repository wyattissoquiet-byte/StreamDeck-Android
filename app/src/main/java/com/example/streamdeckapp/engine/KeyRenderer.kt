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
            ActionType.MEDIA_PLAY_PAUSE -> {
                if (isPlaying) Color.parseColor("#FF9100") // Vivid Amber-Orange (Pause state)
                else Color.parseColor("#00E676")           // Bright Neon Green (Play state)
            }
            ActionType.MEDIA_NEXT, ActionType.MEDIA_PREV -> Color.parseColor("#00E5FF") // Electric Cyan
            ActionType.VOLUME_UP -> Color.parseColor("#FFD600")                          // Radiant Sun Gold
            ActionType.VOLUME_DOWN -> Color.parseColor("#FF6D00")                        // Deep Sunset Coral
            ActionType.VOLUME_MUTE_TOGGLE -> {
                if (isMuted) Color.parseColor("#FF1744")   // Fiery Scarlet Crimson
                else Color.parseColor("#00B0FF")           // Bright Sky Azure
            }
            ActionType.NEXT_PAGE, ActionType.PREV_PAGE -> Color.parseColor("#E040FB")    // Electric Magenta / Violet
            ActionType.GOTO_PAGE -> Color.parseColor("#7C4DFF")                          // Royal Purple
            ActionType.LAUNCH_APP -> Color.parseColor("#FF4081")                         // Neon Pink / Rose
            ActionType.SHELL_COMMAND -> Color.parseColor("#00E676")                      // Matrix Cyber Green
            ActionType.NONE -> Color.parseColor("#455A64")                               // Cool Slate
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
            action.type == ActionType.NEXT_PAGE -> "NEXT"
            action.type == ActionType.PREV_PAGE -> "PREV"
            action.type == ActionType.MEDIA_PLAY_PAUSE -> if (isPlaying) "PAUSE" else "PLAY"
            action.type == ActionType.VOLUME_MUTE_TOGGLE -> if (isMuted) "MUTED" else "MUTE"
            action.type == ActionType.VOLUME_UP -> "VOL +"
            action.type == ActionType.VOLUME_DOWN -> "VOL -"
            action.type == ActionType.MEDIA_NEXT -> "NEXT"
            action.type == ActionType.MEDIA_PREV -> "PREV"
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
