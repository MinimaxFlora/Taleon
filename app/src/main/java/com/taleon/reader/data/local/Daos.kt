package com.taleon.reader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Transaction
    @Query(
        """
        SELECT * FROM books
        ORDER BY CASE WHEN lastOpenedAt IS NULL THEN 1 ELSE 0 END,
                 lastOpenedAt DESC,
                 addedAt DESC
        """,
    )
    fun observeLibrary(): Flow<List<BookWithProgress>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: Long): Flow<BookWithProgress?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE contentHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE books SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE books SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String?)

    @Query("UPDATE books SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: Long, timestamp: Long)

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, author: String?)

    @Query("SELECT COUNT(*) FROM books")
    fun observeBookCount(): Flow<Int>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun observe(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun get(bookId: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun delete(bookId: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, charOffset")
    fun observeForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeRecentForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, charOffset")
    suspend fun getForBook(bookId: Long): List<BookmarkEntity>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    fun observeCount(bookId: Long): Flow<Int>
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity): Long

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM reading_sessions")
    fun observeTotalSeconds(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT bookId) FROM reading_sessions")
    fun observeBooksRead(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM reading_sessions WHERE startedAt >= :since")
    fun observeSecondsSince(since: Long): Flow<Long>
}
