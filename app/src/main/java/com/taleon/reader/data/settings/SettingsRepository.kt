package com.taleon.reader.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taleon.reader.domain.model.AppSettings
import com.taleon.reader.domain.model.LibrarySort
import com.taleon.reader.domain.model.LibraryViewMode
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.PageTurnAnimation
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ReadingFont
import com.taleon.reader.domain.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "taleon_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w(TAG, "Unable to read settings; falling back to defaults.", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::readSettings)

    private fun readSettings(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = preferences[Keys.themeMode].toEnum(defaults.themeMode),
            fontSizeSp = preferences[Keys.fontSize] ?: defaults.fontSizeSp,
            readingFont = preferences[Keys.readingFont].toEnum(defaults.readingFont),
            lineHeightMultiplier = preferences[Keys.lineHeight] ?: defaults.lineHeightMultiplier,
            paragraphSpacingDp = preferences[Keys.paragraphSpacing] ?: defaults.paragraphSpacingDp,
            horizontalPaddingDp = preferences[Keys.horizontalPadding] ?: defaults.horizontalPaddingDp,
            readerTheme = preferences[Keys.readerTheme].toEnum(defaults.readerTheme),
            pageMode = preferences[Keys.pageMode].toEnum(defaults.pageMode),
            pageTurnAnimation = preferences[Keys.pageTurnAnimation].toEnum(defaults.pageTurnAnimation),
            volumeKeysTurnPages = preferences[Keys.volumeKeys] ?: defaults.volumeKeysTurnPages,
            keepScreenOn = preferences[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            libraryViewMode = preferences[Keys.libraryView].toEnum(defaults.libraryViewMode),
            librarySort = preferences[Keys.librarySort].toEnum(defaults.librarySort),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: fallback

    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }

    suspend fun setFontSize(value: Float) = edit { it[Keys.fontSize] = value.coerceIn(12f, 36f) }

    suspend fun setReadingFont(value: ReadingFont) = edit { it[Keys.readingFont] = value.name }

    suspend fun setLineHeight(value: Float) = edit { it[Keys.lineHeight] = value.coerceIn(1.1f, 2.4f) }

    suspend fun setParagraphSpacing(value: Float) = edit { it[Keys.paragraphSpacing] = value.coerceIn(0f, 24f) }

    suspend fun setHorizontalPadding(value: Float) = edit { it[Keys.horizontalPadding] = value.coerceIn(12f, 40f) }

    suspend fun setReaderTheme(value: ReaderTheme) = edit { it[Keys.readerTheme] = value.name }

    suspend fun setPageMode(value: PageMode) = edit { it[Keys.pageMode] = value.name }

    suspend fun setPageTurnAnimation(value: PageTurnAnimation) = edit { it[Keys.pageTurnAnimation] = value.name }

    suspend fun setVolumeKeys(value: Boolean) = edit { it[Keys.volumeKeys] = value }

    suspend fun setKeepScreenOn(value: Boolean) = edit { it[Keys.keepScreenOn] = value }

    suspend fun setLibraryView(value: LibraryViewMode) = edit { it[Keys.libraryView] = value.name }

    suspend fun setLibrarySort(value: LibrarySort) = edit { it[Keys.librarySort] = value.name }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val fontSize = floatPreferencesKey("reader_font_size")
        val readingFont = stringPreferencesKey("reader_font")
        val lineHeight = floatPreferencesKey("reader_line_height")
        val paragraphSpacing = floatPreferencesKey("reader_paragraph_spacing")
        val horizontalPadding = floatPreferencesKey("reader_horizontal_padding")
        val readerTheme = stringPreferencesKey("reader_theme")
        val pageMode = stringPreferencesKey("reader_page_mode")
        val pageTurnAnimation = stringPreferencesKey("reader_page_animation")
        val volumeKeys = booleanPreferencesKey("reader_volume_keys")
        val keepScreenOn = booleanPreferencesKey("reader_keep_screen_on")
        val libraryView = stringPreferencesKey("library_view")
        val librarySort = stringPreferencesKey("library_sort")
    }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}
