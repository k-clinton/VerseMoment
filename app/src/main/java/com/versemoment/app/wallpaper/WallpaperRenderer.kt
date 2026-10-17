package com.versemoment.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.versemoment.app.domain.BibleVerse
import java.io.File
import java.io.IOException
import kotlin.math.min

class WallpaperBackgroundUnavailableException(cause: Exception? = null) :
    IOException("Android does not allow VerseMoment to read the current lock-screen wallpaper.", cause)

class WallpaperRenderer(
    private val context: Context,
) {
    private val originalBackgroundFile: File
        get() = File(context.filesDir, "original_lock_wallpaper.jpg")

    fun loadOriginalBackground(): Bitmap {
        if (originalBackgroundFile.exists()) {
            return BitmapFactory.decodeFile(originalBackgroundFile.absolutePath)
                ?: throw IOException("The saved wallpaper background could not be opened.")
        }

        val wallpaperManager = WallpaperManager.getInstance(context)
        var lastException: Exception? = null
        for (which in intArrayOf(WallpaperManager.FLAG_LOCK, WallpaperManager.FLAG_SYSTEM)) {
            try {
                val descriptor = wallpaperManager.getWallpaperFile(which) ?: continue
                descriptor.use { parcelFile ->
                    val bitmap = BitmapFactory.decodeFileDescriptor(parcelFile.fileDescriptor)
                    if (bitmap != null) {
                        saveOriginalBackground(bitmap)
                        return bitmap
                    }
                }
            } catch (exception: SecurityException) {
                lastException = exception
            } catch (exception: IOException) {
                lastException = exception
            }
        }
        throw WallpaperBackgroundUnavailableException(lastException)
    }

    fun saveBackgroundFromUri(uri: Uri): Bitmap {
        val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: throw IOException("The selected background image could not be opened.")
        saveOriginalBackground(bitmap)
        return bitmap
    }

    fun renderWallpaper(verse: BibleVerse, background: Bitmap): Bitmap {
        val maxWidth = 1440
        val maxHeight = 3200
        val resizeFactor = min(
            1f,
            min(maxWidth.toFloat() / background.width, maxHeight.toFloat() / background.height),
        )
        val renderBackground = if (resizeFactor < 1f) {
            Bitmap.createScaledBitmap(
                background,
                (background.width * resizeFactor).toInt(),
                (background.height * resizeFactor).toInt(),
                true,
            )
        } else {
            background
        }
        val output = renderBackground.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IOException("Could not prepare the wallpaper image.")
        val canvas = Canvas(output)
        val scale = min(output.width / 1080f, output.height / 1920f)
        val outerMargin = 64f * scale
        val innerPadding = 40f * scale
        val cardWidth = output.width - outerMargin * 2
        val textWidth = (cardWidth - innerPadding * 2).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = when {
                verse.text.length > 220 -> 48f * scale
                verse.text.length > 150 -> 54f * scale
                else -> 62f * scale
            }
        }
        val textLayout = StaticLayout.Builder
            .obtain(verse.text, 0, verse.text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f * scale, 1f)
            .setIncludePad(false)
            .build()
        val referencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1D18B")
            textSize = 34f * scale
            textAlign = Paint.Align.CENTER
        }
        val reference = "${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV"
        val referenceHeight = referencePaint.fontMetrics.descent - referencePaint.fontMetrics.ascent
        val gap = 26f * scale
        val cardHeight = innerPadding * 2 + textLayout.height + gap + referenceHeight
        val centerY = output.height * 0.70f
        val card = RectF(
            outerMargin,
            centerY - cardHeight / 2,
            output.width - outerMargin,
            centerY + cardHeight / 2,
        )

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(208, 16, 24, 39)
        }
        canvas.drawRoundRect(card, 34f * scale, 34f * scale, cardPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        canvas.drawRoundRect(card, 34f * scale, 34f * scale, borderPaint)

        val textLeft = card.left + innerPadding
        val textTop = card.top + innerPadding
        canvas.save()
        canvas.translate(textLeft, textTop)
        textLayout.draw(canvas)
        canvas.restore()
        val referenceBaseline = textTop + textLayout.height + gap - referencePaint.fontMetrics.ascent
        canvas.drawText(reference, output.width / 2f, referenceBaseline, referencePaint)

        if (renderBackground !== background) renderBackground.recycle()
        return output
    }

    fun applyWallpaper(bitmap: Bitmap) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        check(wallpaperManager.isWallpaperSupported) {
            "This device does not support app-set wallpapers."
        }
        check(wallpaperManager.isSetWallpaperAllowed) {
            "Android or your device administrator does not allow this app to set wallpapers."
        }
        wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
    }

    private fun saveOriginalBackground(bitmap: Bitmap) {
        originalBackgroundFile.outputStream().buffered().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                "The wallpaper background could not be saved."
            }
        }
    }
}
