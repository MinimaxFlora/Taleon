package com.taleon.reader.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleon.reader.AppContainer
import com.taleon.reader.domain.model.BookFormat
import com.taleon.reader.domain.model.Bookmark
import com.taleon.reader.domain.model.LibraryItem
import com.taleon.reader.ui.components.BookCover
import com.taleon.reader.ui.components.ConfirmDialog
import com.taleon.reader.ui.components.ErrorPane
import com.taleon.reader.ui.components.LoadingPane
import com.taleon.reader.ui.components.MetadataRow
import com.taleon.reader.ui.components.SectionHeader
import com.taleon.reader.ui.components.TaleonProgress
import com.taleon.reader.ui.components.formatFileSize
import com.taleon.reader.ui.components.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookDetailsScreen(
    bookId: Long,
    container: AppContainer,
    onBack: () -> Unit,
    onRead: (Long, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DetailsViewModel = viewModel(
        key = "details-$bookId",
        factory = remember(bookId, container) {
            DetailsViewModelFactory(bookId, container.bookRepository)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showCategory by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        imageVector = if (state.item?.book?.favorite == true) {
                            Icons.Outlined.Star
                        } else {
                            Icons.Outlined.StarBorder
                        },
                        contentDescription = if (state.item?.book?.favorite == true) "取消收藏" else "收藏",
                        tint = if (state.item?.book?.favorite == true) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("设置分类") },
                            leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showCategory = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除书籍", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                showDelete = true
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingPane(
                label = "正在打开书籍…",
                modifier = Modifier.padding(padding),
            )
            state.item == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ErrorPane(
                    message = state.error ?: "书籍不存在。",
                    actionLabel = "返回书架",
                    onAction = onBack,
                )
            }
            else -> {
                val item = state.item!!
                DetailsContent(
                    item = item,
                    bookmarks = state.bookmarks,
                    onRead = {
                        val progress = item.progress
                        onRead(bookId, progress?.chapterIndex ?: 0, progress?.charOffset ?: 0)
                    },
                    onOpenBookmark = { bookmark ->
                        onRead(bookId, bookmark.chapterIndex, bookmark.charOffset)
                    },
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onShowCategory = { showCategory = true },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除《${state.item?.book?.title ?: "这本书"}》？",
            message = "书籍文件、阅读进度和书签都会从此设备移除，此操作无法撤销。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                showDelete = false
                viewModel.deleteBook()
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showCategory) {
        CategoryDialog(
            current = state.item?.book?.category,
            onDismiss = { showCategory = false },
            onSave = {
                viewModel.setCategory(it)
                showCategory = false
            },
        )
    }
}

@Composable
private fun DetailsContent(
    item: LibraryItem,
    bookmarks: List<Bookmark>,
    onRead: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onShowCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val book = item.book
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
    ) {
        item {
            Hero(item = item)
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (book.category != null) {
                        Surface(
                            onClick = onShowCategory,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = book.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    } else {
                        TextButton(onClick = onShowCategory, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                            Text("添加分类", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (book.favorite) {
                        Text(
                            "已收藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = book.author?.takeIf { it.isNotBlank() } ?: "未知作者",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                ReadingCard(item = item, onRead = onRead)
                Spacer(Modifier.height(26.dp))
                SectionHeader(title = "书籍信息")
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        MetadataRow(
                            "大小",
                            formatFileSize(book.sizeBytes),
                        )
                        MetadataRow(
                            "加入时间",
                            SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(book.addedAt)),
                        )
                        MetadataRow(
                            if (book.format == BookFormat.PDF) "页数" else "章节",
                            if (book.format == BookFormat.PDF) {
                                "${book.pdfPageCount ?: book.chapterCount} 页"
                            } else {
                                "${book.chapterCount} 章"
                            },
                        )
                        MetadataRow("文件名", book.fileName)
                    }
                }
                Spacer(Modifier.height(26.dp))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(
                    title = "书签",
                    modifier = Modifier.weight(1f),
                )
                if (bookmarks.isNotEmpty()) {
                    Text(
                        text = "${bookmarks.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if (bookmarks.isEmpty()) {
            item {
                Text(
                    text = "阅读时点击书签按钮，重要段落会留在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        } else {
            items(bookmarks.size, key = { bookmarks[it].id }) { index ->
                BookmarkRow(
                    bookmark = bookmarks[index],
                    onClick = { onOpenBookmark(bookmarks[index]) },
                    onDelete = { onDeleteBookmark(bookmarks[index].id) },
                )
            }
        }
    }
}

@Composable
private fun Hero(item: LibraryItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(178.dp)
                .height(238.dp)
                .graphicsLayer {
                    rotationZ = -5f
                    alpha = 0.24f
                }
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(14.dp)),
        )
        Box(
            modifier = Modifier
                .width(170.dp)
                .height(230.dp)
                .graphicsLayer {
                    rotationZ = 3f
                    alpha = 0.18f
                }
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
        )
        BookCover(
            book = item.book,
            modifier = Modifier
                .width(142.dp)
                .aspectRatio(0.68f)
                .shadow(18.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.28f)),
            cornerRadius = 10.dp,
        )
    }
}

@Composable
private fun ReadingCard(item: LibraryItem, onRead: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("阅读进度", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (item.progress == null) "尚未开始" else "${(item.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            TaleonProgress(item.fraction)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (item.progress == null) {
                    "从第一页开始这段故事。"
                } else {
                    "上次阅读于 ${formatRelativeTime(item.progress.updatedAt)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRead,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (item.progress == null) "开始阅读" else "继续阅读")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit, onDelete: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 24.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bookmark.chapterTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(bookmark.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bookmark.preview.ifBlank { "（此处没有文字）" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除书签",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CategoryDialog(current: String?, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var value by remember { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置分类", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "用分类整理你的书架，例如「科幻」「在读」「经典」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 24) value = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim().ifEmpty { null }) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (!current.isNullOrBlank()) {
                    TextButton(onClick = { onSave(null) }) { Text("移除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
