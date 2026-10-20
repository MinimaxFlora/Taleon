package com.taleon.reader.data.local

import com.taleon.reader.domain.model.Book
import com.taleon.reader.domain.model.BookFormat
import com.taleon.reader.domain.model.Bookmark
import com.taleon.reader.domain.model.ImportStatus
import com.taleon.reader.domain.model.LibraryItem
import com.taleon.reader.domain.model.ReadingProgress

fun BookEntity.toDomain(): Book = Book(
    id = id,
    title = title,
    author = author,
    format = BookFormat.entries.firstOrNull { it.name == format } ?: BookFormat.TXT,
    localPath = localPath,
    originalUri = originalUri,
    fileName = fileName,
    contentHash = contentHash,
    sizeBytes = sizeBytes,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    coverPath = coverPath,
    chapterCount = chapterCount,
    favorite = favorite,
    category = category,
    importStatus = ImportStatus.entries.firstOrNull { it.name == importStatus } ?: ImportStatus.READY,
    errorMessage = errorMessage,
    pdfPageCount = pdfPageCount,
)

fun BookWithProgress.toDomain(): LibraryItem = LibraryItem(
    book = book.toDomain(),
    progress = progress?.toDomain(),
)

fun ReadingProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    bookId = bookId,
    chapterIndex = chapterIndex,
    charOffset = charOffset,
    pageIndex = pageIndex,
    percent = percent,
    updatedAt = updatedAt,
)

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    chapterIndex = chapterIndex,
    charOffset = charOffset,
    chapterTitle = chapterTitle,
    preview = preview,
    createdAt = createdAt,
)

fun Book.toEntity(lastOpenedAtOverride: Long? = lastOpenedAt): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    format = format.name,
    localPath = localPath,
    originalUri = originalUri,
    fileName = fileName,
    contentHash = contentHash,
    sizeBytes = sizeBytes,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAtOverride,
    coverPath = coverPath,
    chapterCount = chapterCount,
    favorite = favorite,
    category = category,
    importStatus = importStatus.name,
    errorMessage = errorMessage,
    pdfPageCount = pdfPageCount,
)
