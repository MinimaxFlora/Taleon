package com.taleon.reader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        ReadingSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TaleonDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): TaleonDatabase =
            Room.databaseBuilder(context.applicationContext, TaleonDatabase::class.java, "taleon.db").build()
    }
}
