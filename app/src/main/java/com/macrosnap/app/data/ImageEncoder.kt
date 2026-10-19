package com.macrosnap.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ImageEncoder(private val io: CoroutineDispatcher) {

    /**
     * Shrinks the photo in place (upright, max [MAX_SIDE] px, JPEG) and returns it base64-encoded.
     * A full-resolution camera shot is 5-10 MB; the model gains nothing from it and the upload
     * would be slow on mobile data. The shrunken file doubles as the log thumbnail.
     */
    @Throws(IOException::class)
    suspend fun shrinkAndEncode(path: String): String = withContext(io) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unreadable image: $path")

        val rotation = ExifInterface(path).rotationDegrees
        // Already shrunk by an earlier attempt: send it as is rather than lose quality to another JPEG pass.
        if (max(bounds.outWidth, bounds.outHeight) <= MAX_SIDE && rotation == 0 && bounds.outMimeType == "image/jpeg") {
            return@withContext Base64.encodeToString(File(path).readBytes(), Base64.NO_WRAP)
        }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2

        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: throw IOException("Unreadable image: $path")

        val scale = min(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val bytes = ByteArrayOutputStream().use { stream ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
        // Swap the file in with a rename: the review screen may be decoding the original right now,
        // and an in-place overwrite would corrupt that read.
        val shrunk = File("$path.tmp")
        shrunk.writeBytes(bytes)
        if (!shrunk.renameTo(File(path))) {
            shrunk.delete()
            throw IOException("Could not replace $path")
        }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private companion object {
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
    }
}
