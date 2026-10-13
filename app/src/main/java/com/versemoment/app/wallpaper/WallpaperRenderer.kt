package com.versemoment.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.toColor
import com.versemoment.app.domain.BibleVerse
import com.versemoment.app.domain.WallpaperMetrics
import java.io.File
import kotlin.math.max

class WallpaperRenderer(
    private val context: Context,
) {
    fun renderPreview(verse: BibleVerse, themeId: String): Bitmap {
        val metrics = WallpaperMetrics(width = 1080, height = 1920, density = 3)
        val bitmap = Bitmap.createBitmap(metrics.width, metrics.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#101827"))
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val referencePaint = Paint().apply {
            color = Color.parseColor("#D8B46A")
            textSize = 42f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val text = verse.text
        val lines = wrapText(text, 18)
        var y = 300f
        for (line in lines) {
            canvas.drawText(line, metrics.width / 2f, y, paint)
            y += 90f
        }
        canvas.drawText("${verse.bookName} ${verse.chapter}:${verse.verseNumber} • KJV", metrics.width / 2f, metrics.height - 160f, referencePaint)
        return bitmap
    }

    fun renderWallpaper(verse: BibleVerse, themeId: String): Bitmap = renderPreview(verse, themeId)

    fun applyWallpaper(bitmap: Bitmap): Boolean {
        return try {
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeToCache(bitmap: Bitmap, fileName: String): File {
        val outputFile = File(context.cacheDir, fileName)
        outputFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return outputFile
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            if ((current + " " + word).length <= maxCharsPerLine) {
                current = if (current.isBlank()) word else "$current $word"
            } else {
                lines.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) lines.add(current)
        return lines
    }
}
