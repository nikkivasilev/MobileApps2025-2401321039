package com.macrosnap.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PhotoStore(
    private val context: Context,
    private val io: CoroutineDispatcher,
) {

    private val photoDir: File get() = File(context.filesDir, "photos")

    fun newPhotoFile(): File {
        val dir = photoDir.apply { mkdirs() }
        return File(dir, "meal_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Deletes photos no entry points to: reviews abandoned when the process died never got to
     * clean up after themselves. Recent files are spared, since a restored review may still use one.
     */
    suspend fun deleteUnreferenced(referenced: Set<String>, olderThanMillis: Long) = withContext(io) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        photoDir.listFiles().orEmpty()
            .filter { it.isFile && it.path !in referenced && it.lastModified() < cutoff }
            .forEach { it.delete() }
    }

    fun shareableUri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a gallery pick into app storage; returns null when the source can't be read. */
    suspend fun importFrom(uri: Uri): File? = withContext(io) {
        val target = newPhotoFile()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target
        } catch (ignored: IOException) {
            target.delete()
            null
        } catch (ignored: SecurityException) {
            target.delete()
            null
        }
    }

    suspend fun delete(path: String) {
        withContext(io) { File(path).delete() }
    }

    /** Blocking variant for ViewModel.onCleared, where no coroutine scope survives. */
    fun deleteNow(path: String) {
        File(path).delete()
    }
}
