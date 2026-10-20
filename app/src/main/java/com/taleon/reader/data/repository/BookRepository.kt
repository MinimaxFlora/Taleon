package com.taleon.reader.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.taleon.reader.data.files.BookFileStore
import com.taleon.reader.data.files.BookParseException
import com.taleon.reader.data.files.BookParser
import com.taleon.reader.data.local.BookDao
import com.taleon.reader.data.local.BookEntity
import com.taleon.reader.data.local.BookmarkDao
import com.taleon.reader.data.local.BookmarkEntity
import com.taleon.reader.data.local.ProgressDao
import com.taleon.reader.data.local.ReadingProgressEntity
import com.taleon.reader.data.local.ReadingSessionEntity
import com.taleon.reader.data.local.SessionDao
import com.taleon.reader.data.local.toDomain
import com.taleon.reader.domain.model.Book
import com.taleon.reader.domain.model.BookFormat
import com.taleon.reader.domain.model.Bookmark
import com.taleon.reader.domain.model.ImportResult
import com.taleon.reader.domain.model.ImportStatus
import com.taleon.reader.domain.model.LibraryItem
import com.taleon.reader.domain.model.ParsedBook
import com.taleon.reader.domain.model.ReadingProgress
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val progressDao: ProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val sessionDao: SessionDao,
    private val fileStore: BookFileStore,
    private val parser: BookParser,
) {
    private val parsedCache = object : LinkedHashMap<Long, ParsedBook>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ParsedBook>?): Boolean =
            size > MAX_CACHED_BOOKS
    }

    val library: Flow<List<LibraryItem>> = bookDao.observeLibrary().map { books ->
        books.map { it.toDomain() }
    }

    val totalReadingSeconds: Flow<Long> = sessionDao.observeTotalSeconds()
    val booksRead: Flow<Int> = sessionDao.observeBooksRead()

    fun observeBook(id: Long): Flow<LibraryItem?> =
        bookDao.observeBook(id).map { it?.toDomain() }

    fun observeProgress(id: Long): Flow<ReadingProgress?> =
        progressDao.observe(id).map { it?.toDomain() }

    fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { list -> list.map { it.toDomain() } }

    fun observeBookmarkCount(bookId: Long): Flow<Int> = bookmarkDao.observeCount(bookId)

    fun storedBytes(): Long = fileStore.totalStoredBytes()

    suspend fun getBook(id: Long): Book? = withContext(Dispatchers.IO) {
        bookDao.getBook(id)?.toDomain()
    }

    suspend fun getProgress(id: Long): ReadingProgress? = withContext(Dispatchers.IO) {
        progressDao.get(id)?.toDomain()
    }

    suspend fun importBooks(uris: List<Uri>): List<ImportResult> = withContext(Dispatchers.IO) {
        uris.map { uri -> importOne(uri) }
    }

    private suspend fun importOne(uri: Uri): ImportResult {
        var displayName = uri.lastPathSegment ?: "未命名文件"
        val persistable = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        if (!persistable) {
            Log.d(TAG, "Provider does not offer persistable read permission for $uri")
        }

        val staged = try {
            fileStore.stage(uri)
        } catch (error: BookParseException) {
            return ImportResult.Failure(displayName, error.message ?: "无法读取文件。")
        } catch (error: SecurityException) {
            return ImportResult.Failure(displayName, "没有读取该文件的权限。")
        } catch (error: Exception) {
            Log.w(TAG, "Copy failed for $uri", error)
            return ImportResult.Failure(displayName, "复制文件失败。")
        }
        displayName = staged.displayName

        val format = BookFormat.fromFileName(staged.displayName)
            ?: BookFormat.fromMimeType(staged.mimeType)
        if (format == null) {
            fileStore.discard(staged)
            return ImportResult.Failure(displayName, "仅支持 TXT、EPUB 和 PDF 文件。")
        }

        bookDao.findByHash(staged.contentHash)?.let { existing ->
            fileStore.discard(staged)
            return ImportResult.Duplicate(displayName, existing.title)
        }

        val localFile = fileStore.promote(staged, format.extension)
        val parsed = try {
            parser.parse(localFile, format, staged.displayName)
        } catch (error: BookParseException) {
            localFile.delete()
            return ImportResult.Failure(displayName, error.message ?: "解析书籍失败。")
        } catch (error: OutOfMemoryError) {
            localFile.delete()
            return ImportResult.Failure(displayName, "书籍过大，设备内存不足。")
        } catch (error: Exception) {
            Log.w(TAG, "Parse failed for $displayName", error)
            localFile.delete()
            return ImportResult.Failure(displayName, "无法解析该书籍文件。")
        }

        val coverPath = fileStore.saveCover(parsed.coverBytes, staged.contentHash)
        val chapterCount = if (format == BookFormat.PDF) parsed.pdfPageCount ?: 0 else parsed.chapters.size
        val entity = BookEntity(
            title = parsed.title.trim().ifBlank { displayName },
            author = parsed.author?.trim()?.takeIf { it.isNotEmpty() },
            format = format.name,
            localPath = localFile.absolutePath,
            originalUri = uri.toString(),
            fileName = staged.displayName,
            contentHash = staged.contentHash,
            sizeBytes = staged.sizeBytes,
            addedAt = System.currentTimeMillis(),
            lastOpenedAt = null,
            coverPath = coverPath,
            chapterCount = chapterCount,
            favorite = false,
            category = null,
            importStatus = ImportStatus.READY.name,
            errorMessage = null,
            pdfPageCount = parsed.pdfPageCount,
        )

        return try {
            val id = bookDao.insert(entity)
            synchronized(parsedCache) { parsedCache[id] = parsed }
            ImportResult.Success(displayName, id, entity.title)
        } catch (error: Exception) {
            Log.e(TAG, "Could not persist imported book $displayName", error)
            fileStore.deleteBookFiles(localFile.absolutePath, coverPath)
            bookDao.findByHash(staged.contentHash)?.let {
                ImportResult.Duplicate(displayName, it.title)
            } ?: ImportResult.Failure(displayName, "无法保存书籍信息。")
        }
    }

    suspend fun openBook(id: Long): ParsedBook = withContext(Dispatchers.IO) {
        synchronized(parsedCache) { parsedCache[id] }?.let { return@withContext it }
        val entity = bookDao.getBook(id) ?: throw BookAccessException("书籍不存在。")
        val file = File(entity.localPath)
        if (!file.exists() || !file.canRead()) {
            throw BookAccessException("书籍文件已被移动或删除，请重新导入。")
        }
        val format = BookFormat.entries.firstOrNull { it.name == entity.format }
            ?: throw BookAccessException("不支持的书籍格式。")
        val parsed = parser.parse(file, format, entity.fileName)
        synchronized(parsedCache) { parsedCache[id] = parsed }
        parsed
    }

    suspend fun touchLastOpened(id: Long, timestamp: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) { bookDao.touchLastOpened(id, timestamp) }

    suspend fun saveProgress(
        bookId: Long,
        chapterIndex: Int,
        charOffset: Int,
        pageIndex: Int,
        percent: Float,
    ) = withContext(Dispatchers.IO) {
        progressDao.upsert(
            ReadingProgressEntity(
                bookId = bookId,
                chapterIndex = chapterIndex.coerceAtLeast(0),
                charOffset = charOffset.coerceAtLeast(0),
                pageIndex = pageIndex.coerceAtLeast(0),
                percent = percent.coerceIn(0f, 1f),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun toggleFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        bookDao.setFavorite(id, favorite)
    }

    suspend fun setCategory(id: Long, category: String?) = withContext(Dispatchers.IO) {
        bookDao.setCategory(id, category?.trim()?.takeIf { it.isNotEmpty() })
    }

    suspend fun addBookmark(
        bookId: Long,
        chapterIndex: Int,
        chapterTitle: String,
        charOffset: Int,
        preview: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getForBook(bookId)
        if (existing.any { it.chapterIndex == chapterIndex && kotlin.math.abs(it.charOffset - charOffset) < 80 }) {
            return@withContext false
        }
        bookmarkDao.insert(
            BookmarkEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                charOffset = charOffset,
                chapterTitle = chapterTitle,
                preview = preview.replace(Regex("""\s+"""), " ").trim().take(160),
                createdAt = System.currentTimeMillis(),
            ),
        )
        true
    }

    suspend fun deleteBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.delete(id)
    }

    suspend fun deleteBook(id: Long): Boolean = withContext(Dispatchers.IO) {
        val entity = bookDao.getBook(id) ?: return@withContext false
        bookDao.delete(id)
        synchronized(parsedCache) { parsedCache.remove(id) }
        fileStore.deleteBookFiles(entity.localPath, entity.coverPath)
        true
    }

    suspend fun recordSession(bookId: Long, startedAt: Long, endedAt: Long) = withContext(Dispatchers.IO) {
        val duration = ((endedAt - startedAt) / 1000L).coerceAtLeast(0L)
        if (duration < MIN_SESSION_SECONDS) return@withContext
        sessionDao.insert(
            ReadingSessionEntity(
                bookId = bookId,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = duration,
            ),
        )
    }

    suspend fun findBookTitle(id: Long): String? = withContext(Dispatchers.IO) {
        bookDao.getBook(id)?.title
    }

    private companion object {
        const val TAG = "BookRepository"
        const val MAX_CACHED_BOOKS = 2
        const val MIN_SESSION_SECONDS = 5L
    }
}

class BookAccessException(message: String, cause: Throwable? = null) : Exception(message, cause)
