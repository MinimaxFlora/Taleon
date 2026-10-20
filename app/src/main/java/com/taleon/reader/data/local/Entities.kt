package com.taleon.reader.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["contentHash"], unique = true),
        Index(value = ["lastOpenedAt"]),
        Index(value = ["addedAt"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val format: String,
    val localPath: String,
    val originalUri: String?,
    val fileName: String,
    val contentHash: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val lastOpenedAt: Long?,
    val coverPath: String?,
    val chapterCount: Int,
    val favorite: Boolean = false,
    val category: String? = null,
    val importStatus: String = "READY",
    val errorMessage: String? = null,
    val pdfPageCount: Int? = null,
)

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class ReadingProgressEntity(
    @PrimaryKey val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val pageIndex: Int,
    val percent: Float,
    val updatedAt: Long,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val charOffset: Int,
    val chapterTitle: String,
    val preview: String,
    val createdAt: Long,
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
)

data class BookWithProgress(
    @Embedded val book: BookEntity,
    @Relation(parentColumn = "id", entityColumn = "bookId")
    val progress: ReadingProgressEntity?,
)
