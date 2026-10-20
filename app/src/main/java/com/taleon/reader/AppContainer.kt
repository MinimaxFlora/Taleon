package com.taleon.reader

import android.content.Context
import com.taleon.reader.data.files.BookFileStore
import com.taleon.reader.data.files.BookParser
import com.taleon.reader.data.local.TaleonDatabase
import com.taleon.reader.data.repository.BookRepository
import com.taleon.reader.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: TaleonDatabase by lazy { TaleonDatabase.create(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val bookRepository: BookRepository by lazy {
        BookRepository(
            context = appContext,
            bookDao = database.bookDao(),
            progressDao = database.progressDao(),
            bookmarkDao = database.bookmarkDao(),
            sessionDao = database.sessionDao(),
            fileStore = BookFileStore(appContext),
            parser = BookParser(),
        )
    }
}
