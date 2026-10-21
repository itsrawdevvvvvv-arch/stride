package com.epicmems.stride.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps the chosen avatar in app-private storage.
 *
 * The photo picker only grants temporary read access to the picked file, so the image is copied
 * in straight away. It is also downscaled — an avatar never needs a 12 MP original, and this
 * keeps the app's storage small.
 */
object ProfileImageStore {

    private const val DIR = "profile"
    private const val NAME = "avatar.png"
    private const val MAX_PX = 512

    fun file(context: Context): File = File(File(context.filesDir, DIR), NAME)

    /** Copies and downscales the picked image. Returns the saved path, or null on failure. */
    fun save(context: Context, uri: Uri): String? = runCatching {
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val scaled = downscale(decoded)
        val target = file(context)
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaled !== decoded) decoded.recycle()
        target.absolutePath
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun downscale(source: Bitmap): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= MAX_PX) return source
        val ratio = MAX_PX.toFloat() / longest
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
