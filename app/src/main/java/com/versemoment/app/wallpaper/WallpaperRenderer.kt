package com.versemoment.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.versemoment.app.domain.BibleVerse
import java.io.FileInputStream
import java.io.File
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

class WallpaperBackgroundUnavailableException(cause: Exception? = null) :
    IOException("Android does not allow VerseMoment to read the current lock-screen wallpaper.", cause)

class WallpaperRenderer(
    private val context: Context,
) {
    private val originalBackgroundFile: File
        get() = File(context.filesDir, "original_lock_wallpaper.jpg")

    fun loadOriginalBackground(): Bitmap {
        if (originalBackgroundFile.exists()) {
            return decodeSampledFile(originalBackgroundFile)
                ?: throw IOException("The saved wallpaper background could not be opened.")
        }

        val wallpaperManager = WallpaperManager.getInstance(context)
        var lastException: Exception? = null
        for (which in intArrayOf(WallpaperManager.FLAG_LOCK, WallpaperManager.FLAG_SYSTEM)) {
            try {
                val descriptor = wallpaperManager.getWallpaperFile(which) ?: continue
                descriptor.use { parcelFile ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFileDescriptor(parcelFile.fileDescriptor, null, bounds)
                    Os.lseek(parcelFile.fileDescriptor, 0, OsConstants.SEEK_SET)
                    val options = sampledOptions(bounds.outWidth, bounds.outHeight)
                    val bitmap = BitmapFactory.decodeFileDescriptor(parcelFile.fileDescriptor, null, options)
                    if (bitmap != null) {
                        saveOriginalBackground(bitmap)
                        return bitmap
                    }
                }
            } catch (exception: SecurityException) {
                lastException = exception
            } catch (exception: IOException) {
                lastException = exception
            } catch (exception: ErrnoException) {
                lastException = exception
            }
        }
        throw WallpaperBackgroundUnavailableException(lastException)
    }

    fun saveBackgroundFromUri(uri: Uri): Bitmap {
        var decoderFailure: Exception? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri),
                ) { decoder, imageInfo, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val scale = min(
                        1f,
                        min(
                            1440f / imageInfo.size.width,
                            3200f / imageInfo.size.height,
                        ),
                    )
                    decoder.setTargetSize(
                        (imageInfo.size.width * scale).roundToInt().coerceAtLeast(1),
                        (imageInfo.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
                saveOriginalBackground(bitmap)
                return bitmap
            } catch (exception: IOException) {
                decoderFailure = exception
            } catch (exception: SecurityException) {
                decoderFailure = exception
            } catch (exception: IllegalArgumentException) {
                decoderFailure = exception
            }
        }

        val selectedImage = File.createTempFile("selected-background-", ".image", context.cacheDir)
        try {
            copySelectedImage(uri, selectedImage)
            val bitmap = decodeSampledFile(selectedImage)
                ?: throw IOException("The selected file does not contain a supported image.")
            saveOriginalBackground(bitmap)
            return bitmap
        } catch (exception: IOException) {
            decoderFailure?.let(exception::addSuppressed)
            throw IOException("Could not read the selected image. Try saving it on this device and selecting it again.", exception)
        } catch (exception: SecurityException) {
            decoderFailure?.let(exception::addSuppressed)
            throw IOException("Android did not grant access to the selected image. Select it again and allow access.", exception)
        } finally {
            selectedImage.delete()
        }
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
            color = Color.parseColor("#E7B3FF")
            typeface = Typeface.create("cursive", Typeface.NORMAL)
            setShadowLayer(14f * scale, 0f, 0f, Color.parseColor("#FFB54BFF"))
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

    private fun decodeSampledFile(file: File): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, imageInfo, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val scale = min(
                        1f,
                        min(
                            1440f / imageInfo.size.width,
                            3200f / imageInfo.size.height,
                        ),
                    )
                    decoder.setTargetSize(
                        (imageInfo.size.width * scale).roundToInt().coerceAtLeast(1),
                        (imageInfo.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            } catch (_: IOException) {
                // Fall back to BitmapFactory for formats supported there but not by ImageDecoder.
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return BitmapFactory.decodeFile(
            file.absolutePath,
            sampledOptions(bounds.outWidth, bounds.outHeight),
        )
    }

    private fun copySelectedImage(uri: Uri, destination: File) {
        var streamFailure: Exception? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("The selected image provider returned no data.")
            input.use { imageStream ->
                destination.outputStream().buffered().use(imageStream::copyTo)
            }
            if (destination.length() > 0) return
            streamFailure = IOException("The selected image provider returned an empty file.")
        } catch (exception: IOException) {
            streamFailure = exception
        } catch (exception: SecurityException) {
            streamFailure = exception
        }

        try {
            val descriptor = context.contentResolver.openTypedAssetFileDescriptor(uri, "image/*", null)
                ?: throw IOException("The selected document provider did not return image data.")
            descriptor.use { asset ->
                asset.createInputStream().use { imageStream ->
                    destination.outputStream().buffered().use(imageStream::copyTo)
                }
            }
            if (destination.length() > 0) return
            throw IOException("The selected image provider returned an empty file.")
        } catch (exception: IOException) {
            streamFailure?.let(exception::addSuppressed)
            throw exception
        } catch (exception: SecurityException) {
            streamFailure?.let(exception::addSuppressed)
            throw IOException("Android denied access to the selected image.", exception)
        }
    }

    private fun sampledOptions(width: Int, height: Int) = BitmapFactory.Options().apply {
        var sample = 1
        while (width / sample > 1440 || height / sample > 3200) {
            sample *= 2
        }
        inSampleSize = sample
    }
}
