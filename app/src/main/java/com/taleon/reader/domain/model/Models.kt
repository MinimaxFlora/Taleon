package com.taleon.reader.domain.model

import java.io.File

enum class BookFormat(val label: String, val extension: String) {
    TXT("TXT", "txt"),
    EPUB("EPUB", "epub"),
    PDF("PDF", "pdf");

    companion object {
        fun fromFileName(name: String): BookFormat? {
            val extension = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == extension }
        }

        fun fromMimeType(mimeType: String?): BookFormat? = when (mimeType?.lowercase()) {
            "text/plain" -> TXT
            "application/epub+zip" -> EPUB
            "application/pdf" -> PDF
            else -> null
        }
    }
}

enum class ImportStatus { READY, FAILED }

data class Book(
    val id: Long,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val localPath: String,
    val originalUri: String?,
    val fileName: String,
    val contentHash: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val lastOpenedAt: Long?,
    val coverPath: String?,
    val chapterCount: Int,
    val favorite: Boolean,
    val category: String?,
    val importStatus: ImportStatus,
    val errorMessage: String?,
    val pdfPageCount: Int?,
) {
    val file: File get() = File(localPath)
    val hasReadableFile: Boolean get() = file.exists() && file.canRead()
}

data class ReadingProgress(
    val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val pageIndex: Int,
    val percent: Float,
    val updatedAt: Long,
)

data class LibraryItem(
    val book: Book,
    val progress: ReadingProgress?,
) {
    val fraction: Float get() = progress?.percent?.coerceIn(0f, 1f) ?: 0f
}

data class Bookmark(
    val id: Long,
    val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val chapterTitle: String,
    val preview: String,
    val createdAt: Long,
)

data class ReadingSession(
    val id: Long,
    val bookId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
)

data class Chapter(
    val index: Int,
    val title: String,
    val text: String,
    val startOffset: Int,
) {
    val length: Int get() = text.length
}

data class ChapterSummary(
    val index: Int,
    val title: String,
    val length: Int,
)

data class ParsedBook(
    val title: String,
    val author: String?,
    val chapters: List<Chapter>,
    val coverBytes: ByteArray? = null,
    val pdfPageCount: Int? = null,
)

sealed interface ImportResult {
    val displayName: String

    data class Success(
        override val displayName: String,
        val bookId: Long,
        val title: String,
    ) : ImportResult

    data class Duplicate(
        override val displayName: String,
        val existingTitle: String,
    ) : ImportResult

    data class Failure(
        override val displayName: String,
        val reason: String,
    ) : ImportResult
}
