package com.taleon.reader.data.files

import com.taleon.reader.domain.model.BookFormat
import com.taleon.reader.domain.model.ParsedBook
import java.io.File

class BookParser(
    private val txtParser: TxtParser = TxtParser,
    private val epubParser: EpubParser = EpubParser(),
    private val pdfParser: PdfParser = PdfParser(),
) {
    fun parse(file: File, format: BookFormat, fallbackTitle: String): ParsedBook {
        if (!file.exists() || !file.canRead()) throw BookParseException("书籍文件不存在或无法读取。")
        return when (format) {
            BookFormat.TXT -> {
                if (file.length() > MAX_TXT_BYTES) {
                    throw BookParseException("TXT 文件超过 48 MB，当前版本无法安全载入。")
                }
                txtParser.parse(file.readBytes(), fallbackTitle)
            }
            BookFormat.EPUB -> epubParser.parse(file, fallbackTitle)
            BookFormat.PDF -> pdfParser.parse(file, fallbackTitle)
        }
    }

    private companion object {
        const val MAX_TXT_BYTES = 48L * 1024 * 1024
    }
}
