package com.taleon.reader.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Typeface
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleon.reader.AppContainer
import com.taleon.reader.ReaderHardwareKeys
import com.taleon.reader.domain.model.Book
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.PageTurnAnimation
import com.taleon.reader.domain.model.ReadingFont
import com.taleon.reader.ui.theme.ReaderPalette
import com.taleon.reader.ui.theme.readerPalette
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    bookId: Long,
    initialChapter: Int?,
    initialOffset: Int?,
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReaderViewModel = viewModel(
        key = "reader-$bookId",
        factory = remember(bookId, initialChapter, initialOffset, container) {
            ReaderViewModelFactory(
                bookId = bookId,
                initialChapter = initialChapter,
                initialOffset = initialOffset,
                repository = container.bookRepository,
                settingsRepository = container.settingsRepository,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = readerPalette(state.settings.readerTheme)
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    val latestState by rememberUpdatedState(state)

    DisposableEffect(Unit) {
        onDispose { ReaderHardwareKeys.isReaderActive = false }
    }
    SideEffect {
        ReaderHardwareKeys.isReaderActive = state.settings.volumeKeysTurnPages &&
            (state.isPdf || state.settings.pageMode == PageMode.PAGED) &&
            !state.settingsOpen && !state.tocOpen
    }
    LaunchedEffect(Unit) {
        ReaderHardwareKeys.volumeKeys.collect { keyCode ->
            if (!latestState.settings.volumeKeysTurnPages) return@collect
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> viewModel.previousPage()
                KeyEvent.KEYCODE_VOLUME_DOWN -> viewModel.nextPage()
            }
        }
    }

    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onReaderResumed()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.onReaderPaused()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onReaderPaused()
        }
    }

    DisposableEffect(state.settings.keepScreenOn) {
        view.keepScreenOn = state.settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    ReaderSystemBars(visible = state.error != null, darkIcons = palette.isDark)

    BackHandler(enabled = state.showChrome && !state.tocOpen && !state.settingsOpen) {
        viewModel.hideChrome()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val readerCanvas = if (state.isPdf || state.settings.pageMode == PageMode.PAGED) {
        palette.canvas
    } else {
        palette.background
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerCanvas)
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        when {
            state.error != null -> ReaderError(
                message = state.error.orEmpty(),
                palette = palette,
                onBack = onBack,
            )
            state.loading -> ReaderLoading(palette)
            state.isPdf -> PdfReaderContent(
                state = state,
                palette = palette,
                renderPage = viewModel::renderPdfPage,
                onToggleChrome = viewModel::toggleChrome,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onOpenPageSheet = viewModel::openToc,
            )
            else -> ReflowReaderContent(
                state = state,
                palette = palette,
                onViewportChanged = viewModel::onViewportChanged,
                onTapZone = { zone ->
                    when (zone) {
                        ReaderTapZone.PREVIOUS -> viewModel.previousPage()
                        ReaderTapZone.CENTER -> viewModel.toggleChrome()
                        ReaderTapZone.NEXT -> viewModel.nextPage()
                    }
                },
                onSwipe = { forward -> if (forward) viewModel.nextPage() else viewModel.previousPage() },
                onToggleChrome = viewModel::toggleChrome,
                onScrollPositionChanged = viewModel::onScrollPositionChanged,
                onPreviousChapter = viewModel::scrollToPreviousChapter,
                onNextChapter = viewModel::scrollToNextChapter,
            )
        }

        ReaderChrome(
            state = state,
            palette = palette,
            onBack = onBack,
            onToggleBookmark = viewModel::toggleBookmark,
            onOpenContents = viewModel::openToc,
            onOpenSettings = viewModel::openSettings,
            onPreviousPage = viewModel::previousPage,
            onNextPage = viewModel::nextPage,
            onSeek = viewModel::seekToFraction,
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = if (state.showChrome) 170.dp else 26.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = if (palette.isDark) Color(0xFF2B2B33) else Color(0xFF2E2C35),
                contentColor = Color(0xFFF7F3EA),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }

    if (state.tocOpen) {
        if (state.isPdf) {
            PdfPageSheet(
                currentPage = state.pdfCurrentPage,
                pageCount = state.pdfPageCount,
                palette = palette,
                onJump = {
                    viewModel.setPdfPage(it)
                    viewModel.closeToc()
                    viewModel.hideChrome()
                },
                onDismiss = viewModel::closeToc,
            )
        } else {
            ChapterListSheet(
                chapters = state.chapters,
                currentChapter = state.currentChapter,
                palette = palette,
                onSelect = { chapterIndex -> viewModel.goToChapter(chapterIndex, 0) },
                onDismiss = viewModel::closeToc,
            )
        }
    }

    if (state.settingsOpen) {
        ReaderSettingsSheet(
            settings = state.settings,
            palette = palette,
            isPdf = state.isPdf,
            onDismiss = viewModel::closeSettings,
            onFontSizeChange = viewModel::setFontSize,
            onFontChange = viewModel::setReadingFont,
            onLineHeightChange = viewModel::setLineHeight,
            onParagraphSpacingChange = viewModel::setParagraphSpacing,
            onHorizontalPaddingChange = viewModel::setHorizontalPadding,
            onReaderThemeChange = viewModel::setReaderTheme,
            onPageModeChange = viewModel::setPageMode,
            onPageTurnAnimationChange = viewModel::setPageTurnAnimation,
            onVolumeKeysChange = viewModel::setVolumeKeys,
            onKeepScreenOnChange = viewModel::setKeepScreenOn,
        )
    }
}

@Composable
private fun ReaderSystemBars(visible: Boolean, darkIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(visible, darkIcons) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            controller.isAppearanceLightStatusBars = !darkIcons
            controller.isAppearanceLightNavigationBars = !darkIcons
            if (visible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.isAppearanceLightStatusBars = !darkIcons
            controller?.isAppearanceLightNavigationBars = !darkIcons
        }
    }
}

@Composable
private fun ReaderLoading(palette: ReaderPalette) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "T",
                fontFamily = FontFamily.Serif,
                fontSize = 54.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = palette.accent,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(14.dp))
            Text("正在展开书页…", fontSize = 12.sp, color = palette.secondaryText, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun ReaderError(message: String, palette: ReaderPalette, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "T",
                fontFamily = FontFamily.Serif,
                fontSize = 54.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "这本书暂时无法打开",
                fontFamily = FontFamily.Serif,
                fontSize = 23.sp,
                color = palette.text,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = palette.secondaryText,
                lineHeight = 21.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            TextButton(onClick = onBack) {
                Text("返回书架", color = palette.accent)
            }
        }
    }
}

private enum class ReaderTapZone { PREVIOUS, CENTER, NEXT }

private class PageFrame(val key: String, val page: ReaderPage?) {
    override fun equals(other: Any?): Boolean = other is PageFrame && other.key == key
    override fun hashCode(): Int = key.hashCode()
}

@Composable
private fun ReflowReaderContent(
    state: ReaderUiState,
    palette: ReaderPalette,
    onViewportChanged: (Int, Int, Float, Float) -> Unit,
    onTapZone: (ReaderTapZone) -> Unit,
    onSwipe: (Boolean) -> Unit,
    onToggleChrome: () -> Unit,
    onScrollPositionChanged: (Int, Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
) {
    if (state.settings.pageMode == PageMode.SCROLL) {
        ScrollReaderContent(
            state = state,
            palette = palette,
            onToggleChrome = onToggleChrome,
            onScrollPositionChanged = onScrollPositionChanged,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter,
        )
    } else {
        PagedReaderContent(
            state = state,
            palette = palette,
            onViewportChanged = onViewportChanged,
            onTapZone = onTapZone,
            onSwipe = onSwipe,
        )
    }
}

@Composable
private fun PagedReaderContent(
    state: ReaderUiState,
    palette: ReaderPalette,
    onViewportChanged: (Int, Int, Float, Float) -> Unit,
    onTapZone: (ReaderTapZone) -> Unit,
    onSwipe: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val horizontalPadding = state.settings.horizontalPaddingDp.dp
    val pageMargin = 8.dp
    val topPadding = 14.dp
    val headerHeight = 18.dp
    val headerGap = 8.dp
    val footerHeight = 22.dp
    val bottomPadding = 8.dp
    val pageShape = RoundedCornerShape(8.dp)
    val chromePageShift by animateDpAsState(
        targetValue = if (state.showChrome) 64.dp else 0.dp,
        animationSpec = tween(220),
        label = "reader-page-shift",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        val contentWidth = with(density) {
            (maxWidth - pageMargin * 2 - horizontalPadding * 2).roundToPx().coerceAtLeast(120)
        }
        val contentHeight = with(density) {
            (
                maxHeight - pageMargin * 2 - topPadding - headerHeight -
                    headerGap - footerHeight - bottomPadding
                ).roundToPx().coerceAtLeast(80)
        }
        LaunchedEffect(contentWidth, contentHeight, density.density, density.fontScale) {
            onViewportChanged(
                contentWidth,
                contentHeight,
                density.density,
                density.density * density.fontScale,
            )
        }
        val pageStyle = remember(
            state.settings.fontSize,
            state.settings.readingFont,
            state.settings.lineHeight,
            palette.text,
        ) {
            PageRenderStyle(
                textSizePx = with(density) { state.settings.fontSize.sp.toPx() },
                lineSpacingMultiplier = state.settings.lineHeight,
                typeface = pageTypeface(state.settings.readingFont),
                textColor = palette.text.toArgb(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = chromePageShift)
                .padding(pageMargin)
                .shadow(
                    elevation = 7.dp,
                    shape = pageShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                )
                .clip(pageShape)
                .background(palette.background)
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = topPadding,
                    bottom = bottomPadding,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.chapter?.title.orEmpty(),
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp,
                    color = palette.secondaryText.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.book?.title.orEmpty(),
                    fontSize = 9.sp,
                    color = palette.secondaryText.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(headerGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val page = state.page
                val frame = remember(page) {
                    PageFrame(
                        key = "${page?.chapterIndex}:${page?.pageIndex}:${page?.displayStart}:${page?.displayEnd}",
                        page = page,
                    )
                }
                AnimatedContent(
                    targetState = frame,
                    transitionSpec = {
                        val before = initialState.page
                        val after = targetState.page
                        val forward = when {
                            before == null || after == null -> true
                            before.chapterIndex != after.chapterIndex ->
                                after.chapterIndex > before.chapterIndex
                            else -> after.pageIndex >= before.pageIndex
                        }
                        pageTransition(state.settings.pageTurnAnimation, forward)
                    },
                    contentKey = { it.key },
                    modifier = Modifier.fillMaxSize(),
                ) { target ->
                    val targetPage = target.page
                    if (targetPage == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = palette.accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { context -> ReaderPageView(context) },
                            update = { view ->
                                view.setPage(
                                    display = targetPage.displayText,
                                    start = targetPage.displayStart,
                                    end = targetPage.displayEnd,
                                    style = pageStyle,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (state.chapter?.text.isNullOrBlank()) {
                            Text(
                                text = "本章暂无正文",
                                color = palette.secondaryText,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }

                if (state.paginating) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(1.5.dp),
                        color = palette.accent.copy(alpha = 0.55f),
                        trackColor = Color.Transparent,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(footerHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (state.page != null) {
                        "${state.currentPage + 1} / ${state.pagination?.pageCount ?: 1}"
                    } else {
                        ""
                    },
                    fontSize = 9.sp,
                    color = palette.secondaryText.copy(alpha = 0.55f),
                )
                Spacer(Modifier.weight(1f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.pageMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var lastPosition = down.position
                        var event: PointerEvent
                        do {
                            event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null) {
                                totalX += change.position.x - lastPosition.x
                                totalY += change.position.y - lastPosition.y
                                lastPosition = change.position
                                if (abs(totalX) > viewConfiguration.touchSlop) change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        val threshold = size.width * 0.12f
                        when {
                            abs(totalX) > threshold && abs(totalX) > abs(totalY) * 1.2f -> {
                                onSwipe(totalX < 0)
                            }
                            abs(totalX) < viewConfiguration.touchSlop && abs(totalY) < viewConfiguration.touchSlop -> {
                                val x = down.position.x / size.width.coerceAtLeast(1)
                                when {
                                    x < 0.3f -> onTapZone(ReaderTapZone.PREVIOUS)
                                    x > 0.7f -> onTapZone(ReaderTapZone.NEXT)
                                    else -> onTapZone(ReaderTapZone.CENTER)
                                }
                            }
                        }
                    }
                },
        )
    }
}

@Composable
private fun ScrollReaderContent(
    state: ReaderUiState,
    palette: ReaderPalette,
    onToggleChrome: () -> Unit,
    onScrollPositionChanged: (Int, Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
) {
    val chapter = state.chapter ?: return
    val paragraphs = remember(chapter) { splitParagraphs(chapter.text) }
    val listState = rememberLazyListState()
    val horizontalPadding = state.settings.horizontalPaddingDp.dp
    val fontFamily = when (state.settings.readingFont) {
        ReadingFont.SERIF -> FontFamily.Serif
        ReadingFont.SANS -> FontFamily.SansSerif
        ReadingFont.MONO -> FontFamily.Monospace
    }

    LaunchedEffect(chapter.index, state.jumpToken) {
        val paragraphIndex = paragraphs.indexOfLast { it.start <= state.currentOffset }.coerceAtLeast(0)
        val target = if (state.currentOffset <= 0) 0 else paragraphIndex + 1
        listState.scrollToItem(target.coerceAtMost(paragraphs.size))
    }

    LaunchedEffect(listState, chapter.index, paragraphs) {
        snapshotFlow {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull { it.index in 1..paragraphs.size }
            val last = info.visibleItemsInfo.lastOrNull { it.index in 1..paragraphs.size }
            val start = first?.let { paragraphs.getOrNull(it.index - 1)?.start } ?: 0
            val end = last?.let { paragraphs.getOrNull(it.index - 1)?.end } ?: start
            start to end
        }.distinctUntilChanged().collect { (start, end) ->
            onScrollPositionChanged(start, end)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 54.dp,
                bottom = 110.dp,
            ),
        ) {
            item(key = "chapter-header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleChrome,
                        )
                        .padding(top = 12.dp, bottom = 28.dp),
                ) {
                    Text(
                        text = "第 ${chapter.index + 1} 章",
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = palette.accent.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = chapter.title.ifBlank { "未命名章节" },
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (state.settings.fontSize * 1.5f).sp,
                        lineHeight = (state.settings.fontSize * 2.0f).sp,
                        color = palette.text,
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(1.dp)
                            .background(palette.accent.copy(alpha = 0.5f)),
                    )
                }
            }
            items(paragraphs, key = { it.start }) { paragraph ->
                Text(
                    text = paragraph.text,
                    fontFamily = fontFamily,
                    fontSize = state.settings.fontSize.sp,
                    lineHeight = (state.settings.fontSize * state.settings.lineHeight).sp,
                    color = palette.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleChrome,
                        )
                        .padding(bottom = state.settings.paragraphSpacingDp.dp),
                )
            }
            item(key = "chapter-footer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = onPreviousChapter,
                        enabled = state.currentChapter > 0,
                    ) {
                        Text("上一章", color = if (state.currentChapter > 0) palette.accent else palette.secondaryText.copy(alpha = 0.4f))
                    }
                    TextButton(
                        onClick = onNextChapter,
                        enabled = state.currentChapter < state.chapters.lastIndex,
                    ) {
                        Text(
                            "下一章",
                            color = if (state.currentChapter < state.chapters.lastIndex) {
                                palette.accent
                            } else {
                                palette.secondaryText.copy(alpha = 0.4f)
                            },
                        )
                    }
                }
                Text(
                    text = "· 本章完 ·",
                    color = palette.secondaryText.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PdfReaderContent(
    state: ReaderUiState,
    palette: ReaderPalette,
    renderPage: suspend (Int, Int) -> Bitmap?,
    onToggleChrome: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenPageSheet: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        val density = LocalDensity.current
        val availableWidth = constraints.maxWidth
        val availableHeight = constraints.maxHeight
        val renderWidth = minOf(availableWidth, (availableHeight * 0.72f).toInt()).coerceAtLeast(320)
        val page = state.pdfCurrentPage
        val bitmap by produceState<Bitmap?>(initialValue = null, page, renderWidth) {
            value = null
            value = renderPage(page, renderWidth)
        }
        var scale by remember(page) { mutableFloatStateOf(1f) }
        var offsetX by remember(page) { mutableFloatStateOf(0f) }
        var offsetY by remember(page) { mutableFloatStateOf(0f) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
            scale = newScale
            if (newScale > 1f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (palette.isDark) 0.15f else 0.03f)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "PDF 第 ${page + 1} 页",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 26.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        )
                        .transformable(transformState),
                )
            } else {
                CircularProgressIndicator(
                    color = palette.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(page, scale) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalX = 0f
                            var totalY = 0f
                            var lastX = down.position.x
                            var lastY = down.position.y
                            var event: PointerEvent
                            do {
                                event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null) {
                                    totalX += change.position.x - lastX
                                    totalY += change.position.y - lastY
                                    lastX = change.position.x
                                    lastY = change.position.y
                                }
                            } while (event.changes.any { it.pressed })
                            if (abs(totalX) < viewConfiguration.touchSlop && abs(totalY) < viewConfiguration.touchSlop) {
                                if (scale > 1.05f) {
                                    onToggleChrome()
                                } else {
                                    val fraction = down.position.x / size.width.coerceAtLeast(1)
                                    when {
                                        fraction < 0.28f -> onPreviousPage()
                                        fraction > 0.72f -> onNextPage()
                                        else -> onToggleChrome()
                                    }
                                }
                            } else if (scale <= 1.05f && abs(totalX) > size.width * 0.14f) {
                                if (totalX < 0) onNextPage() else onPreviousPage()
                            }
                        }
                    },
            )
            if (scale > 1.05f) {
                Surface(
                    onClick = onOpenPageSheet,
                    shape = RoundedCornerShape(99.dp),
                    color = palette.background.copy(alpha = 0.86f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                ) {
                    Text(
                        text = "${page + 1} / ${state.pdfPageCount} · 缩放中",
                        color = palette.secondaryText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderChrome(
    state: ReaderUiState,
    palette: ReaderPalette,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenContents: () -> Unit,
    onOpenSettings: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.showChrome,
            enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.background,
                    contentColor = palette.text,
                    tonalElevation = 0.dp,
                    shadowElevation = 9.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回书架", tint = palette.text)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.book?.title.orEmpty(),
                                fontSize = 10.sp,
                                color = palette.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when {
                                    state.isPdf -> "第 ${state.pdfCurrentPage + 1} 页"
                                    !state.chapter?.title.isNullOrBlank() -> state.chapter?.title.orEmpty()
                                    else -> "正在阅读"
                                },
                                fontSize = 14.sp,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!state.isPdf) {
                            IconButton(onClick = onToggleBookmark) {
                                Icon(
                                    imageVector = if (state.isCurrentPositionBookmarked) {
                                        Icons.Outlined.Bookmark
                                    } else {
                                        Icons.Outlined.BookmarkBorder
                                    },
                                    contentDescription = if (state.isCurrentPositionBookmarked) "移除书签" else "添加书签",
                                    tint = if (state.isCurrentPositionBookmarked) palette.accent else palette.text,
                                )
                            }
                        }
                        IconButton(onClick = onOpenContents) {
                            Icon(
                                Icons.Outlined.List,
                                contentDescription = if (state.isPdf) "跳转页码" else "目录",
                                tint = palette.text,
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Tune, contentDescription = "阅读设置", tint = palette.text)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { state.fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp),
                        color = palette.accent,
                        trackColor = palette.secondaryText.copy(alpha = 0.14f),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                    }
                }
            }
        }

        var sliderDragging by remember { mutableStateOf(false) }
        var sliderValue by remember { mutableFloatStateOf(state.fraction) }
        LaunchedEffect(state.fraction, sliderDragging) {
            if (!sliderDragging) sliderValue = state.fraction
        }
        AnimatedVisibility(
            visible = state.showChrome,
            enter = fadeIn(tween(160)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.background,
                    contentColor = palette.text,
                    tonalElevation = 0.dp,
                    shadowElevation = 9.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPreviousPage) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一页", tint = palette.text)
                        }
                        Text(
                            text = when {
                                state.isPdf -> "第 ${state.pdfCurrentPage + 1} / ${state.pdfPageCount} 页"
                                else -> "${(state.fraction * 100).toInt()}%"
                            },
                            fontSize = 12.sp,
                            color = palette.text,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(onClick = onNextPage) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "下一页", tint = palette.text)
                        }
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderDragging = true
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            onSeek(sliderValue)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.secondaryText.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    }
                }
            }
        }
    }
}

private fun pageTransition(
    animation: PageTurnAnimation,
    forward: Boolean,
): androidx.compose.animation.ContentTransform {
    return when (animation) {
        PageTurnAnimation.NONE -> EnterTransition.None togetherWith ExitTransition.None
        PageTurnAnimation.FADE -> fadeIn(tween(180)) togetherWith fadeOut(tween(140))
        PageTurnAnimation.SLIDE -> {
            if (forward) {
                (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(160)))
            } else {
                (slideInHorizontally(tween(240)) { -it / 3 } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(160)))
            }
        }
    }
}

private fun pageTypeface(font: ReadingFont): Typeface = when (font) {
    ReadingFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    ReadingFont.SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    ReadingFont.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
}

private data class Paragraph(val start: Int, val end: Int, val text: String)

private fun splitParagraphs(text: String): List<Paragraph> {
    if (text.isBlank()) return emptyList()
    val result = ArrayList<Paragraph>()
    var start = 0
    var index = 0
    while (index <= text.length) {
        if (index == text.length || text[index] == '\n') {
            val raw = text.substring(start, index)
            val trimmed = raw.trim('\r', ' ', '\u3000', '\t')
            if (trimmed.isNotEmpty()) {
                val leading = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                result += Paragraph(
                    start = start + leading,
                    end = start + raw.trimEnd().length,
                    text = trimmed,
                )
            }
            start = index + 1
        }
        index++
    }
    return result
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
