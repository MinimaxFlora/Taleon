package com.taleon.reader.ui.reader

import android.graphics.Bitmap
import android.graphics.Typeface
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taleon.reader.data.files.BookParseException
import com.taleon.reader.data.files.PdfDocumentSession
import com.taleon.reader.data.repository.BookAccessException
import com.taleon.reader.data.repository.BookRepository
import com.taleon.reader.data.settings.SettingsRepository
import com.taleon.reader.domain.model.AppSettings
import com.taleon.reader.domain.model.Book
import com.taleon.reader.domain.model.BookFormat
import com.taleon.reader.domain.model.Bookmark
import com.taleon.reader.domain.model.Chapter
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.PageTurnAnimation
import com.taleon.reader.domain.model.ParsedBook
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ReadingFont
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ReaderViewport(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val scaledDensity: Float,
)

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val pagination: ChapterPagination? = null,
    val currentChapter: Int = 0,
    val currentPage: Int = 0,
    val currentOffset: Int = 0,
    val pageEndOffset: Int = 0,
    val jumpToken: Long = 0,
    val settings: AppSettings = AppSettings(),
    val bookmarks: List<Bookmark> = emptyList(),
    val loading: Boolean = true,
    val paginating: Boolean = false,
    val error: String? = null,
    val showChrome: Boolean = false,
    val tocOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val isPdf: Boolean = false,
    val pdfPageCount: Int = 0,
    val pdfCurrentPage: Int = 0,
    val message: String? = null,
) {
    val chapter: Chapter? get() = chapters.getOrNull(currentChapter)
    val page: ReaderPage? get() = pagination?.let { PagePaginator.readerPage(it, currentPage) }
    val pageMode: PageMode get() = settings.pageMode

    val fraction: Float
        get() {
            if (isPdf) {
                if (pdfPageCount <= 0) return 0f
                if (pdfCurrentPage >= pdfPageCount - 1) return 1f
                return (pdfCurrentPage.toFloat() / pdfPageCount).coerceIn(0f, 1f)
            }
            if (chapters.isEmpty()) return 0f
            val length = chapter?.length ?: 0
            val atEndOfLastChapter = if (pageMode == PageMode.SCROLL) {
                currentOffset >= (length - 1).coerceAtLeast(0)
            } else {
                pagination?.pageCount?.let { currentPage >= it - 1 } == true
            }
            if (currentChapter == chapters.lastIndex && atEndOfLastChapter) return 1f
            val within = if (length <= 0) 0f else currentOffset.toFloat().coerceIn(0f, length.toFloat()) / length
            return ((currentChapter + within) / chapters.size).coerceIn(0f, 1f)
        }

    val isCurrentPositionBookmarked: Boolean
        get() = bookmarks.any {
            it.chapterIndex == currentChapter && abs(it.charOffset - currentOffset) < 100
        }
}

class ReaderViewModel(
    private val bookId: Long,
    private val initialChapter: Int?,
    private val initialOffset: Int?,
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var viewport: ReaderViewport? = null
    private var paginationJob: Job? = null
    private var saveJob: Job? = null
    private var settingsLoaded = false
    private var sessionStartedAt: Long? = null
    private var pdfSession: PdfDocumentSession? = null
    private val pdfMutex = Mutex()
    private val pdfCache = object : LruCache<PdfCacheKey, Bitmap>(4) {}

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val previous = _state.value.settings
                val first = !settingsLoaded
                settingsLoaded = true
                _state.update { it.copy(settings = settings) }
                val layoutChanged = LayoutSignature.from(previous) != LayoutSignature.from(settings)
                val switchedToPaged = previous.pageMode != settings.pageMode &&
                    settings.pageMode == PageMode.PAGED
                if (!first && _state.value.book != null && !_state.value.isPdf &&
                    ((layoutChanged && settings.pageMode == PageMode.PAGED) || switchedToPaged)
                ) {
                    repaginatePreservingPosition(delayMs = 60)
                }
            }
        }
        viewModelScope.launch {
            repository.observeBookmarks(bookId).collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks) }
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val book = repository.getBook(bookId) ?: throw BookAccessException("书籍不存在。")
                _state.update { it.copy(book = book) }
                repository.touchLastOpened(bookId)
                val progress = repository.getProgress(bookId)
                if (book.format == BookFormat.PDF) {
                    openPdf(book, progress?.pageIndex ?: 0)
                } else {
                    val parsed = repository.openBook(bookId)
                    if (parsed.chapters.isEmpty()) throw BookAccessException("这本书没有可阅读的章节。")
                    val requestedChapter = (initialChapter ?: progress?.chapterIndex ?: 0)
                        .coerceIn(0, parsed.chapters.lastIndex)
                    val requestedOffset = (initialOffset ?: progress?.charOffset ?: 0)
                        .coerceAtLeast(0)
                    applyParsedBook(parsed, requestedChapter, requestedOffset)
                }
                sessionStartedAt = System.currentTimeMillis()
            } catch (error: BookAccessException) {
                _state.update { it.copy(loading = false, error = error.message ?: "无法打开书籍。") }
            } catch (error: BookParseException) {
                _state.update { it.copy(loading = false, error = error.message ?: "无法解析书籍。") }
            } catch (error: OutOfMemoryError) {
                _state.update { it.copy(loading = false, error = "书籍过大，设备内存不足。") }
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = "打开书籍时发生错误。") }
            }
        }
    }

    private suspend fun openPdf(book: Book, requestedPage: Int) {
        val session = withContext(Dispatchers.IO) { PdfDocumentSession(File(book.localPath)) }
        pdfSession = session
        val page = requestedPage.coerceIn(0, (session.pageCount - 1).coerceAtLeast(0))
        _state.update {
            it.copy(
                loading = false,
                isPdf = true,
                pdfPageCount = session.pageCount,
                pdfCurrentPage = page,
                currentChapter = 0,
                currentPage = page,
                currentOffset = 0,
                pageEndOffset = 0,
                error = null,
            )
        }
        scheduleProgressSave(delayMs = 0)
    }

    private fun applyParsedBook(parsed: ParsedBook, chapterIndex: Int, offset: Int) {
        _state.update {
            it.copy(
                chapters = parsed.chapters,
                loading = false,
                error = null,
                currentChapter = chapterIndex,
            )
        }
        schedulePagination(chapterIndex, offset, delayMs = 0)
    }

    fun onViewportChanged(widthPx: Int, heightPx: Int, density: Float, scaledDensity: Float) {
        if (widthPx <= 0 || heightPx <= 0) return
        val next = ReaderViewport(widthPx, heightPx, density, scaledDensity)
        if (viewport == next) return
        val hadViewport = viewport != null
        viewport = next
        if (hadViewport && !_state.value.isPdf && !_state.value.loading) {
            repaginatePreservingPosition(delayMs = 40)
        } else if (!_state.value.loading && !_state.value.isPdf && _state.value.pagination == null) {
            schedulePagination(_state.value.currentChapter, _state.value.currentOffset, delayMs = 0)
        }
    }

    private fun repaginatePreservingPosition(delayMs: Long) {
        val snapshot = _state.value
        if (snapshot.isPdf || snapshot.chapters.isEmpty()) return
        schedulePagination(
            chapterIndex = snapshot.currentChapter,
            targetOffset = snapshot.currentOffset,
            delayMs = delayMs,
        )
    }

    private fun schedulePagination(
        chapterIndex: Int,
        targetOffset: Int,
        pageFromEnd: Boolean = false,
        delayMs: Long = 0,
    ) {
        val viewport = viewport ?: return
        val chapter = _state.value.chapters.getOrNull(chapterIndex) ?: return
        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            _state.update { it.copy(paginating = true) }
            val spec = paginationSpec(viewport, _state.value.settings)
            val pagination = try {
                withContext(Dispatchers.Default) { PagePaginator.paginate(chapter, spec) }
            } catch (error: OutOfMemoryError) {
                _state.update { it.copy(paginating = false, message = "页面排版所需内存不足。") }
                return@launch
            } catch (error: Exception) {
                _state.update { it.copy(paginating = false, message = "重新排版失败。") }
                return@launch
            }
            val pageIndex = if (pageFromEnd) {
                (pagination.pageCount - 1).coerceAtLeast(0)
            } else {
                pagination.pageForOffset(targetOffset)
            }
            val slice = pagination.page(pageIndex)
            _state.update {
                it.copy(
                    pagination = pagination,
                    currentChapter = chapterIndex,
                    currentPage = pageIndex,
                    currentOffset = slice?.bodyStart ?: 0,
                    pageEndOffset = slice?.bodyEnd ?: 0,
                    paginating = false,
                )
            }
            scheduleProgressSave(delayMs = 180)
        }
    }

    fun nextPage() = turnPage(1)

    fun previousPage() = turnPage(-1)

    private fun turnPage(delta: Int) {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.paginating) return
        if (snapshot.isPdf) {
            setPdfPage(snapshot.pdfCurrentPage + delta)
            return
        }
        val pagination = snapshot.pagination ?: return
        val target = snapshot.currentPage + delta
        if (target in 0 until pagination.pageCount) {
            val slice = pagination.page(target) ?: return
            _state.update {
                it.copy(
                    currentPage = target,
                    currentOffset = slice.bodyStart,
                    pageEndOffset = slice.bodyEnd,
                )
            }
            scheduleProgressSave(delayMs = 320)
            return
        }
        if (delta > 0) {
            val nextChapter = snapshot.currentChapter + 1
            if (nextChapter < snapshot.chapters.size) {
                schedulePagination(nextChapter, 0, delayMs = 0)
            } else {
                _state.update { it.copy(message = "已经读到全书最后一页。") }
            }
        } else {
            val previousChapter = snapshot.currentChapter - 1
            if (previousChapter >= 0) {
                schedulePagination(previousChapter, Int.MAX_VALUE, pageFromEnd = true, delayMs = 0)
            } else {
                _state.update { it.copy(message = "已经是全书第一页。") }
            }
        }
    }

    fun goToChapter(chapterIndex: Int, charOffset: Int = 0) {
        val snapshot = _state.value
        if (snapshot.isPdf) {
            setPdfPage(charOffset)
            return
        }
        val chapter = snapshot.chapters.firstOrNull { it.index == chapterIndex } ?: return
        if (snapshot.pageMode == PageMode.SCROLL) {
            val start = charOffset.coerceIn(0, chapter.length)
            _state.update {
                it.copy(
                    currentChapter = chapterIndex,
                    currentPage = 0,
                    currentOffset = start,
                    pageEndOffset = (start + 800).coerceAtMost(chapter.length),
                    tocOpen = false,
                    showChrome = false,
                    jumpToken = it.jumpToken + 1,
                )
            }
            scheduleProgressSave(delayMs = 180)
            return
        }
        _state.update { it.copy(tocOpen = false, showChrome = false) }
        val pagination = snapshot.pagination
        if (chapterIndex == snapshot.currentChapter && pagination != null) {
            val pageIndex = pagination.pageForOffset(charOffset)
            val slice = pagination.page(pageIndex) ?: return
            _state.update {
                it.copy(
                    currentPage = pageIndex,
                    currentOffset = slice.bodyStart,
                    pageEndOffset = slice.bodyEnd,
                )
            }
            scheduleProgressSave(delayMs = 180)
        } else {
            schedulePagination(chapterIndex, charOffset, delayMs = 0)
        }
    }

    fun seekToFraction(fraction: Float) {
        val snapshot = _state.value
        if (snapshot.isPdf) {
            if (snapshot.pdfPageCount > 0) {
                setPdfPage(((snapshot.pdfPageCount - 1) * fraction.coerceIn(0f, 1f)).toInt())
            }
            return
        }
        if (snapshot.chapters.isEmpty()) return
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped >= 0.999f) {
            goToChapter(snapshot.chapters.lastIndex, Int.MAX_VALUE)
            return
        }
        val exact = clamped * snapshot.chapters.size
        val chapterIndex = exact.toInt().coerceIn(0, snapshot.chapters.lastIndex)
        val within = exact - chapterIndex
        val length = snapshot.chapters[chapterIndex].length
        goToChapter(chapterIndex, (within * length).toInt().coerceIn(0, length))
    }

    fun setPdfPage(pageIndex: Int) {
        val snapshot = _state.value
        if (!snapshot.isPdf) return
        val page = pageIndex.coerceIn(0, (snapshot.pdfPageCount - 1).coerceAtLeast(0))
        if (page == snapshot.pdfCurrentPage && snapshot.pdfCurrentPage != 0) return
        _state.update { it.copy(pdfCurrentPage = page, currentPage = page) }
        scheduleProgressSave(delayMs = 120)
    }

    suspend fun renderPdfPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        pdfMutex.withLock {
            val key = PdfCacheKey(pageIndex, targetWidthPx)
            pdfCache.get(key)?.let { return@withLock it }
            val session = pdfSession ?: return@withLock null
            try {
                session.renderPage(pageIndex, targetWidthPx).also { pdfCache.put(key, it) }
            } catch (error: Exception) {
                null
            }
        }
    }

    fun onScrollPositionChanged(charOffset: Int, charOffsetEnd: Int) {
        val snapshot = _state.value
        if (snapshot.pageMode != PageMode.SCROLL || snapshot.isPdf) return
        val chapter = snapshot.chapter ?: return
        val start = charOffset.coerceIn(0, chapter.length)
        val end = charOffsetEnd.coerceIn(start, chapter.length)
        if (start == snapshot.currentOffset && end == snapshot.pageEndOffset) return
        _state.update { it.copy(currentOffset = start, pageEndOffset = end) }
        scheduleProgressSave(delayMs = 700)
    }

    fun scrollToNextChapter() {
        val snapshot = _state.value
        val next = snapshot.currentChapter + 1
        if (next < snapshot.chapters.size) goToChapter(next, 0)
        else _state.update { it.copy(message = "已经读到全书最后一页。") }
    }

    fun scrollToPreviousChapter() {
        val previous = _state.value.currentChapter - 1
        if (previous >= 0) goToChapter(previous, 0)
    }

    fun toggleChrome() {
        _state.update { it.copy(showChrome = !it.showChrome) }
    }

    fun hideChrome() {
        _state.update { it.copy(showChrome = false) }
    }

    fun openToc() {
        _state.update { it.copy(tocOpen = true, showChrome = true) }
    }

    fun closeToc() {
        _state.update { it.copy(tocOpen = false) }
    }

    fun openSettings() {
        _state.update { it.copy(settingsOpen = true, showChrome = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(settingsOpen = false) }
    }

    fun toggleBookmark() {
        val snapshot = _state.value
        val chapter = snapshot.chapter ?: return
        val current = snapshot.bookmarks.firstOrNull {
            it.chapterIndex == snapshot.currentChapter && abs(it.charOffset - snapshot.currentOffset) < 100
        }
        viewModelScope.launch {
            if (current != null) {
                repository.deleteBookmark(current.id)
                _state.update { it.copy(message = "书签已移除") }
            } else {
                val preview = chapter.text.substring(
                    snapshot.currentOffset.coerceIn(0, chapter.length),
                    (snapshot.currentOffset + 140).coerceAtMost(chapter.length),
                )
                val added = repository.addBookmark(
                    bookId = bookId,
                    chapterIndex = snapshot.currentChapter,
                    chapterTitle = chapter.title,
                    charOffset = snapshot.currentOffset,
                    preview = preview,
                )
                _state.update { it.copy(message = if (added) "已添加书签" else "此处已有书签") }
            }
        }
    }

    fun setFontSize(value: Float) = updateSetting { settingsRepository.setFontSize(value) }

    fun setReadingFont(value: ReadingFont) = updateSetting { settingsRepository.setReadingFont(value) }

    fun setLineHeight(value: Float) = updateSetting { settingsRepository.setLineHeight(value) }

    fun setParagraphSpacing(value: Float) = updateSetting { settingsRepository.setParagraphSpacing(value) }

    fun setHorizontalPadding(value: Float) = updateSetting { settingsRepository.setHorizontalPadding(value) }

    fun setReaderTheme(value: ReaderTheme) = updateSetting { settingsRepository.setReaderTheme(value) }

    fun setPageMode(value: PageMode) = updateSetting { settingsRepository.setPageMode(value) }

    fun setPageTurnAnimation(value: PageTurnAnimation) =
        updateSetting { settingsRepository.setPageTurnAnimation(value) }

    fun setVolumeKeys(value: Boolean) = updateSetting { settingsRepository.setVolumeKeys(value) }

    fun setKeepScreenOn(value: Boolean) = updateSetting { settingsRepository.setKeepScreenOn(value) }

    private fun updateSetting(action: suspend () -> Unit) {
        viewModelScope.launch { action() }
    }

    fun onReaderResumed() {
        if (sessionStartedAt == null) sessionStartedAt = System.currentTimeMillis()
    }

    fun onReaderPaused() {
        flushProgress()
        val start = sessionStartedAt ?: return
        sessionStartedAt = null
        val end = System.currentTimeMillis()
        viewModelScope.launch { repository.recordSession(bookId, start, end) }
    }

    fun flushProgress() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { saveProgressNow() }
    }

    private fun scheduleProgressSave(delayMs: Long) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            saveProgressNow()
        }
    }

    private suspend fun saveProgressNow() {
        val snapshot = _state.value
        if (snapshot.book == null) return
        if (snapshot.isPdf) {
            repository.saveProgress(
                bookId = bookId,
                chapterIndex = 0,
                charOffset = 0,
                pageIndex = snapshot.pdfCurrentPage,
                percent = snapshot.fraction,
            )
        } else {
            repository.saveProgress(
                bookId = bookId,
                chapterIndex = snapshot.currentChapter,
                charOffset = snapshot.currentOffset,
                pageIndex = snapshot.currentPage,
                percent = snapshot.fraction,
            )
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun paginationSpec(viewport: ReaderViewport, settings: AppSettings): PaginationSpec {
        val typeface = when (settings.readingFont) {
            ReadingFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            ReadingFont.SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            ReadingFont.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        return PaginationSpec(
            widthPx = viewport.widthPx,
            heightPx = viewport.heightPx,
            textSizePx = settings.fontSize * viewport.scaledDensity,
            lineSpacingMultiplier = settings.lineHeight,
            paragraphSpacingPx = (settings.paragraphSpacingDp * viewport.density).toInt(),
            typeface = typeface,
        )
    }

    override fun onCleared() {
        val snapshot = _state.value
        if (snapshot.book != null && !snapshot.loading && snapshot.error == null) {
            // The final progress write must outlive the ViewModel. Without this, rapidly leaving
            // the reader could cancel the debounced write and lose the last page position.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                if (snapshot.isPdf) {
                    repository.saveProgress(
                        bookId = bookId,
                        chapterIndex = 0,
                        charOffset = 0,
                        pageIndex = snapshot.pdfCurrentPage,
                        percent = snapshot.fraction,
                    )
                } else {
                    repository.saveProgress(
                        bookId = bookId,
                        chapterIndex = snapshot.currentChapter,
                        charOffset = snapshot.currentOffset,
                        pageIndex = snapshot.currentPage,
                        percent = snapshot.fraction,
                    )
                }
            }
        }
        paginationJob?.cancel()
        saveJob?.cancel()
        pdfSession?.close()
        pdfSession = null
        pdfCache.evictAll()
        super.onCleared()
    }

    private data class LayoutSignature(
        val fontSize: Float,
        val font: ReadingFont,
        val lineHeight: Float,
        val paragraphSpacing: Float,
        val horizontalPadding: Float,
    ) {
        companion object {
            fun from(settings: AppSettings): LayoutSignature = LayoutSignature(
                fontSize = settings.fontSize,
                font = settings.readingFont,
                lineHeight = settings.lineHeight,
                paragraphSpacing = settings.paragraphSpacingDp,
                horizontalPadding = settings.horizontalPaddingDp,
            )
        }
    }

    private data class PdfCacheKey(val page: Int, val width: Int)
}

class ReaderViewModelFactory(
    private val bookId: Long,
    private val initialChapter: Int?,
    private val initialOffset: Int?,
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(
                bookId = bookId,
                initialChapter = initialChapter,
                initialOffset = initialOffset,
                repository = repository,
                settingsRepository = settingsRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
