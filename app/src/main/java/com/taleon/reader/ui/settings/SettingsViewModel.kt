package com.taleon.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taleon.reader.data.repository.BookRepository
import com.taleon.reader.data.settings.SettingsRepository
import com.taleon.reader.domain.model.AppSettings
import com.taleon.reader.domain.model.LibrarySort
import com.taleon.reader.domain.model.LibraryViewMode
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val storedBytes: Long = 0L,
    val totalReadingSeconds: Long = 0L,
    val booksRead: Int = 0,
)

class SettingsViewModel(
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                repository.totalReadingSeconds,
                repository.booksRead,
            ) { settings, seconds, books ->
                Triple(settings, seconds, books)
            }.collect { (settings, seconds, books) ->
                _state.update {
                    it.copy(settings = settings, totalReadingSeconds = seconds, booksRead = books)
                }
            }
        }
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { repository.storedBytes() }
            _state.update { it.copy(storedBytes = bytes) }
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(value) }
    }

    fun setLibraryView(value: LibraryViewMode) {
        viewModelScope.launch { settingsRepository.setLibraryView(value) }
    }

    fun setLibrarySort(value: LibrarySort) {
        viewModelScope.launch { settingsRepository.setLibrarySort(value) }
    }

    fun setReaderTheme(value: ReaderTheme) {
        viewModelScope.launch { settingsRepository.setReaderTheme(value) }
    }

    fun setPageMode(value: PageMode) {
        viewModelScope.launch { settingsRepository.setPageMode(value) }
    }

    fun setFontSize(value: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(value) }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(value) }
    }

    fun setVolumeKeys(value: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeKeys(value) }
    }
}

class SettingsViewModelFactory(
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
