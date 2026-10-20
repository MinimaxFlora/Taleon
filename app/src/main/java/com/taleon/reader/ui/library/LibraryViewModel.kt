package com.taleon.reader.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taleon.reader.data.repository.BookRepository
import com.taleon.reader.data.settings.SettingsRepository
import com.taleon.reader.domain.model.ImportResult
import com.taleon.reader.domain.model.ImportStatus
import com.taleon.reader.domain.model.LibraryItem
import com.taleon.reader.domain.model.LibrarySort
import com.taleon.reader.domain.model.LibraryViewMode
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ShelfFilter {
    val label: String

    data object All : ShelfFilter { override val label = "全部" }
    data object Reading : ShelfFilter { override val label = "在读" }
    data object Favorites : ShelfFilter { override val label = "收藏" }
    data class Category(val category: String) : ShelfFilter { override val label = category }
}

data class ImportSummary(
    val imported: Int,
    val duplicates: List<Duplicate> = emptyList(),
    val failures: List<Failure> = emptyList(),
) {
    data class Duplicate(val name: String, val existingTitle: String)
    data class Failure(val name: String, val reason: String)
}

data class LibraryUiState(
    val allItems: List<LibraryItem> = emptyList(),
    val visibleItems: List<LibraryItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val continueReading: LibraryItem? = null,
    val query: String = "",
    val filter: ShelfFilter = ShelfFilter.All,
    val sort: LibrarySort = LibrarySort.RECENT,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val isLoading: Boolean = true,
    val importTotal: Int = 0,
    val importCompleted: Int = 0,
    val importingName: String? = null,
    val importSummary: ImportSummary? = null,
    val message: String? = null,
) {
    val isImporting: Boolean get() = importingName != null
}

class LibraryViewModel(
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.library.collect { items ->
                _state.update { current ->
                    recompute(
                        current.copy(
                            allItems = items,
                            isLoading = false,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    recompute(
                        current.copy(
                            viewMode = settings.libraryViewMode,
                            sort = settings.librarySort,
                        ),
                    )
                }
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { recompute(it.copy(query = query)) }
    }

    fun setFilter(filter: ShelfFilter) {
        _state.update { recompute(it.copy(filter = filter)) }
    }

    fun setSort(sort: LibrarySort) {
        _state.update { recompute(it.copy(sort = sort)) }
        viewModelScope.launch { settingsRepository.setLibrarySort(sort) }
    }

    fun toggleViewMode() {
        val next = if (_state.value.viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
        _state.update { it.copy(viewMode = next) }
        viewModelScope.launch { settingsRepository.setLibraryView(next) }
    }

    fun importBooks(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.isImporting) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    importTotal = uris.size,
                    importCompleted = 0,
                    importingName = "准备导入…",
                    importSummary = null,
                )
            }
            val results = ArrayList<ImportResult>(uris.size)
            uris.forEachIndexed { index, uri ->
                _state.update {
                    it.copy(
                        importCompleted = index,
                        importingName = "正在导入第 ${index + 1} / ${uris.size} 本",
                    )
                }
                results += repository.importBooks(listOf(uri))
            }
            _state.update {
                it.copy(
                    importCompleted = uris.size,
                    importingName = null,
                    importSummary = summarize(results),
                    message = null,
                )
            }
        }
    }

    fun dismissImportSummary() {
        _state.update { it.copy(importSummary = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun summarize(results: List<ImportResult>): ImportSummary {
        val imported = results.filterIsInstance<ImportResult.Success>()
        val duplicates = results.filterIsInstance<ImportResult.Duplicate>()
        val failures = results.filterIsInstance<ImportResult.Failure>()
        return ImportSummary(
            imported = imported.size,
            duplicates = duplicates.map { ImportSummary.Duplicate(it.displayName, it.existingTitle) },
            failures = failures.map { ImportSummary.Failure(it.displayName, it.reason) },
        )
    }

    private fun recompute(state: LibraryUiState): LibraryUiState {
        val query = state.query.trim()
        val filtered = state.allItems.filter { item ->
            val matchesFilter = when (val filter = state.filter) {
                ShelfFilter.All -> true
                ShelfFilter.Reading -> item.book.lastOpenedAt != null && item.fraction < 0.999f
                ShelfFilter.Favorites -> item.book.favorite
                is ShelfFilter.Category -> item.book.category == filter.category
            }
            if (!matchesFilter) return@filter false
            if (query.isEmpty()) return@filter true
            item.book.title.contains(query, ignoreCase = true) ||
                item.book.author?.contains(query, ignoreCase = true) == true ||
                item.book.fileName.contains(query, ignoreCase = true)
        }
        val collator = Collator.getInstance(Locale.CHINA)
        val sorted = when (state.sort) {
            LibrarySort.RECENT -> filtered.sortedWith(
                compareByDescending<LibraryItem> { it.book.lastOpenedAt ?: 0L }
                    .thenByDescending { it.book.addedAt },
            )
            LibrarySort.ADDED -> filtered.sortedByDescending { it.book.addedAt }
            LibrarySort.TITLE -> filtered.sortedWith { a, b -> collator.compare(a.book.title, b.book.title) }
            LibrarySort.AUTHOR -> filtered.sortedWith(
                compareBy<LibraryItem> { it.book.author == null }
                    .thenComparator { a, b -> collator.compare(a.book.author ?: "", b.book.author ?: "") }
                    .thenBy { it.book.title },
            )
        }
        val continueReading = state.allItems
            .filter { it.book.lastOpenedAt != null && it.fraction < 0.999f && it.book.importStatus == ImportStatus.READY }
            .maxByOrNull { it.book.lastOpenedAt ?: 0L }
        val categories = state.allItems.mapNotNull { it.book.category?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
        return state.copy(
            visibleItems = sorted,
            continueReading = continueReading,
            categories = categories,
        )
    }
}

class LibraryViewModelFactory(
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
