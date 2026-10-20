package com.taleon.reader.data.files

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File

class PdfDocumentSession(file: File) : Closeable {

    private val descriptor: ParcelFileDescriptor = ParcelFileDescriptor.open(
        file,
        ParcelFileDescriptor.MODE_READ_ONLY,
    )
    private val renderer: PdfRenderer = try {
        PdfRenderer(descriptor)
    } catch (error: Exception) {
        descriptor.close()
        throw BookParseException("PDF 已加密、损坏或格式不受支持。", error)
    }

    val pageCount: Int = renderer.pageCount

    fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap {
        if (pageIndex !in 0 until pageCount) throw BookParseException("页码超出范围。")
        val width = targetWidthPx.coerceIn(320, 1600)
        renderer.openPage(pageIndex).use { page ->
            val height = (width * page.height.toFloat() / page.width.toFloat()).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}
