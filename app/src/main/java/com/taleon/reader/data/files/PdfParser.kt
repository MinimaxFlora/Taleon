package com.taleon.reader.data.files

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.taleon.reader.domain.model.ParsedBook
import java.io.ByteArrayOutputStream
import java.io.File

class PdfParser {

    fun parse(file: File, fallbackTitle: String): ParsedBook {
        if (!file.exists()) throw BookParseException("PDF 文件不存在。")
        val descriptor = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (error: Exception) {
            throw BookParseException("无法打开 PDF 文件。", error)
        }
        descriptor.use { pfd ->
            val renderer = try {
                PdfRenderer(pfd)
            } catch (error: Exception) {
                throw BookParseException("PDF 已加密、损坏或格式不受支持。", error)
            }
            renderer.use {
                if (renderer.pageCount <= 0) throw BookParseException("PDF 没有可阅读的页面。")
                val cover = renderCover(renderer)
                return ParsedBook(
                    title = fallbackTitle.substringBeforeLast('.', fallbackTitle).trim().ifBlank { "未命名 PDF" },
                    author = null,
                    chapters = emptyList(),
                    coverBytes = cover,
                    pdfPageCount = renderer.pageCount,
                )
            }
        }
    }

    private fun renderCover(renderer: PdfRenderer): ByteArray? {
        return runCatching {
            renderer.openPage(0).use { page ->
                val width = 420
                val height = (width * page.height.toFloat() / page.width.toFloat()).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    ByteArrayOutputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
                        output.toByteArray()
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }.getOrNull()
    }
}
