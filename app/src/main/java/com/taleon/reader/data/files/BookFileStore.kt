package com.taleon.reader.data.files

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class StagedFile(
    val tempFile: File,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val contentHash: String,
)

class BookFileStore(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    private val stagingDir = File(context.cacheDir, "import-staging").apply { mkdirs() }

    fun stage(uri: Uri): StagedFile {
        val metadata = queryMetadata(uri)
        val temp = File(stagingDir, "stage-${UUID.randomUUID()}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        try {
            val input = resolver.openInputStream(uri)
                ?: throw BookParseException("无法读取所选文件。")
            input.use { source ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(1024 * 128)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (read > 0) {
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            size += read
                        }
                    }
                    output.fd.sync()
                }
            }
        } catch (error: Exception) {
            temp.delete()
            if (error is BookParseException) throw error
            throw BookParseException("读取文件失败，请检查文件是否仍然可访问。", error)
        }
        if (size <= 0L) {
            temp.delete()
            throw BookParseException("文件为空。")
        }
        val name = metadata.name ?: "未命名书籍"
        return StagedFile(
            tempFile = temp,
            displayName = name,
            mimeType = metadata.mimeType,
            sizeBytes = size,
            contentHash = digest.digest().toHex(),
        )
    }

    fun promote(staged: StagedFile, extension: String): File {
        val destination = File(booksDir, "${staged.contentHash}.$extension")
        if (destination.exists()) {
            staged.tempFile.delete()
            return destination
        }
        if (!staged.tempFile.renameTo(destination)) {
            FileInputStream(staged.tempFile).use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            staged.tempFile.delete()
        }
        return destination
    }

    fun discard(staged: StagedFile) {
        staged.tempFile.delete()
    }

    fun saveCover(bytes: ByteArray?, contentHash: String): String? {
        if (bytes == null || bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 720 && bounds.outHeight / (sample * 2) >= 720) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val targetWidth = 600
        val targetHeight = (targetWidth * decoded.height.toFloat() / decoded.width.toFloat()).toInt().coerceAtLeast(1)
        val scaled = if (decoded.width > targetWidth) {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also { decoded.recycle() }
        } else {
            decoded
        }
        val outputFile = File(coversDir, "$contentHash.jpg")
        return try {
            FileOutputStream(outputFile).use { output ->
                val canvasBitmap = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(canvasBitmap)
                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    canvasBitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                } finally {
                    canvasBitmap.recycle()
                }
            }
            outputFile.absolutePath
        } catch (error: Exception) {
            outputFile.delete()
            null
        } finally {
            if (!scaled.isRecycled) scaled.recycle()
        }
    }

    fun coverFor(contentHash: String): File = File(coversDir, "$contentHash.jpg")

    fun deleteBookFiles(localPath: String, coverPath: String?) {
        deleteInside(File(localPath), booksDir)
        coverPath?.let { deleteInside(File(it), coversDir) }
    }

    fun totalStoredBytes(): Long {
        return booksDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun deleteInside(file: File, root: File) {
        val canonicalRoot = root.canonicalFile
        val canonicalFile = file.canonicalFile
        if (canonicalFile.path.startsWith(canonicalRoot.path + File.separator) && canonicalFile.exists()) {
            canonicalFile.delete()
        }
    }

    private fun queryMetadata(uri: Uri): Metadata {
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    }
                }
        }
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        return Metadata(
            name = name?.takeIf { it.isNotBlank() },
            size = size,
            mimeType = mime,
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private data class Metadata(val name: String?, val size: Long?, val mimeType: String?)
}
