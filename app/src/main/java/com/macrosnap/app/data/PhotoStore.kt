package com.macrosnap.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class PhotoStore(
    private val context: Context,
    private val io: CoroutineDispatcher,
) {

    fun newPhotoFile(): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "meal_${System.currentTimeMillis()}.jpg")
    }

    fun shareableUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a gallery pick into app storage; returns null when the source can't be read. */
    suspend fun importFrom(uri: Uri): File? = withContext(io) {
        val target = newPhotoFile()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target
        } catch (e: IOException) {
            target.delete()
            null
        } catch (e: SecurityException) {
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
