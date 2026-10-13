package io.github.elli0t0.phone.data.contacts.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream

object ContactPhotoUtil {
    private const val TAG = "ContactPhotoUtil"
    private const val MAX_PHOTO_DIMENSION = 720

    /**
     * Reads the image from the given Uri, crops it to a square (centered),
     * resizes it to at most [MAX_PHOTO_DIMENSION]x[MAX_PHOTO_DIMENSION], and encodes it to JPEG bytes.
     */
    fun processPickedPhoto(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Center square crop
            val minDim = minOf(originalBitmap.width, originalBitmap.height)
            val startX = (originalBitmap.width - minDim) / 2
            val startY = (originalBitmap.height - minDim) / 2
            val squareBitmap = Bitmap.createBitmap(originalBitmap, startX, startY, minDim, minDim)

            val scaledBitmap = if (minDim > MAX_PHOTO_DIMENSION) {
                Bitmap.createScaledBitmap(squareBitmap, MAX_PHOTO_DIMENSION, MAX_PHOTO_DIMENSION, true)
            } else {
                squareBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing contact photo from $uri", e)
            null
        }
    }
}
