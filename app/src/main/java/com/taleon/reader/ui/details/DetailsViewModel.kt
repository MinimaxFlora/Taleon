package com.taleon.reader.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taleon.reader.data.repository.BookRepository
import com.taleon.reader.domain.model.Bookmark
import com.taleon.reader.domain.model.LibraryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val item: LibraryItem? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleted: Boolean = false,
    val message: String? = null,
)

class DetailsViewModel(
    private val bookId: Long,
    private val repository: BookRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBook(bookId).collect { item ->
                _state.update {
                    it.copy(
                        item = item,
                        isLoading = false,
                        error = if (item == null && !it.deleted) "这本书已从书架移除。" else null,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeBookmarks(bookId).collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    fun toggleFavorite() {
        val item = _state.value.item ?: return
        viewModelScope.launch {
            repository.toggleFavorite(bookId, !item.book.favorite)
        }
    }

    fun setCategory(category: String?) {
        viewModelScope.launch {
            repository.setCategory(bookId, category)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
            _state.update { it.copy(message = "书签已删除") }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            val deleted = repository.deleteBook(bookId)
            _state.update {
                it.copy(
                    deleted = deleted,
                    message = if (deleted) null else "删除失败，请稍后重试。",
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}

class DetailsViewModelFactory(
    private val bookId: Long,
    private val repository: BookRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            return DetailsViewModel(bookId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
