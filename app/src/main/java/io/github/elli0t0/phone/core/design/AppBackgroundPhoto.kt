package io.github.elli0t0.phone.core.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max

/**
 * Saves a user-chosen image as the app background, downsampled so
 * its longest side is at most 1600px and stored as JPEG in filesDir.
 * Returns false when the image cannot be read or decoded.
 */
fun saveAppBackgroundPhoto(context: Context, uri: Uri): Boolean = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }!!
    File(context.filesDir, "app_background.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    true
}.getOrDefault(false)
