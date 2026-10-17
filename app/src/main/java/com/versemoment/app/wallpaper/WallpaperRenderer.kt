package com.versemoment.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.WallpaperMetrics
import java.io.File

class WallpaperRenderer(
    private val context: Context,
) {
    fun renderPreview(verse: BibleVerse, themeId: String): Bitmap {
        val metrics = WallpaperMetrics(width = 1080, height = 1920, density = 3)
        val bitmap = Bitmap.createBitmap(metrics.width, metrics.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#101827"))
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = when {
                verse.text.length > 220 -> 50f
                verse.text.length > 150 -> 56f
                else -> 64f
            }
        }
        val maxTextWidth = metrics.width - 160
        val textLayout = StaticLayout.Builder
            .obtain(verse.text, 0, verse.text.length, paint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(12f, 1f)
            .setIncludePad(false)
            .build()
        val textTop = metrics.height * 0.68f - textLayout.height / 2f
        canvas.save()
        canvas.translate((metrics.width - maxTextWidth) / 2f, textTop)
        textLayout.draw(canvas)
        canvas.restore()

        val referencePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8B46A")
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV",
            metrics.width / 2f,
            metrics.height * 0.88f,
            referencePaint,
        )
        return bitmap
    }

    fun renderWallpaper(verse: BibleVerse, themeId: String): Bitmap = renderPreview(verse, themeId)

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

    fun writeToCache(bitmap: Bitmap, fileName: String): File {
        val outputFile = File(context.cacheDir, fileName)
        outputFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return outputFile
    }
}
