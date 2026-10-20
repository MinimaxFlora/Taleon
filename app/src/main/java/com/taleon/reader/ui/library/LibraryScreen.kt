package com.taleon.reader.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleon.reader.AppContainer
import com.taleon.reader.domain.model.LibraryItem
import com.taleon.reader.domain.model.LibrarySort
import com.taleon.reader.domain.model.LibraryViewMode
import com.taleon.reader.ui.components.BookCover
import com.taleon.reader.ui.components.EmptySearch
import com.taleon.reader.ui.components.EmptyShelf
import com.taleon.reader.ui.components.LibraryGridCard
import com.taleon.reader.ui.components.LibraryListCard
import com.taleon.reader.ui.components.TaleonProgress
import com.taleon.reader.ui.components.formatRelativeTime
import kotlinx.coroutines.launch

private val bookMimeTypes = arrayOf(
    "text/plain",
    "application/epub+zip",
    "application/pdf",
    "application/octet-stream",
)

@Composable
fun LibraryScreen(
    container: AppContainer,
    onOpenBook: (Long) -> Unit,
    onContinueReading: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = remember(container) {
            LibraryViewModelFactory(container.bookRepository, container.settingsRepository)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importBooks(uris)
    }
    val chooseShelf: (ShelfFilter) -> Unit = { filter ->
        viewModel.setFilter(filter)
        viewModel.setQuery("")
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.34f),
        drawerContent = {
            LibraryDrawer(
                state = state,
                onClose = { scope.launch { drawerState.close() } },
                onSelect = chooseShelf,
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val grid = state.viewMode == LibraryViewMode.GRID
            LazyVerticalGrid(
                columns = if (grid) GridCells.Adaptive(minSize = 138.dp) else GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Bottom,
                        ),
                    ),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(if (grid) 18.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(if (grid) 24.dp else 0.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryHeader(
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onImport = { importLauncher.launch(bookMimeTypes) },
                    )
                }

                if (state.query.isBlank() && state.filter == ShelfFilter.All) {
                    state.continueReading?.let { item ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueReadingCard(
                                item = item,
                                onClick = { onContinueReading(item.book.id) },
                            )
                        }
                    }
                }

                if (state.allItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ShelfToolbar(
                            title = activeShelfTitle(state),
                            count = state.visibleItems.size,
                            sort = state.sort,
                            viewMode = state.viewMode,
                            onSort = viewModel::setSort,
                            onToggleView = viewModel::toggleViewMode,
                        )
                    }
                }

                if (!state.isLoading && state.visibleItems.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        when {
                            state.allItems.isEmpty() -> EmptyShelf(
                                onImport = { importLauncher.launch(bookMimeTypes) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 44.dp),
                            )
                            else -> EmptySearch(
                                onClear = {
                                    viewModel.setQuery("")
                                    viewModel.setFilter(ShelfFilter.All)
                                },
                                modifier = Modifier.padding(top = 28.dp),
                            )
                        }
                    }
                }

                items(
                    items = state.visibleItems,
                    key = { it.book.id },
                ) { item ->
                    if (grid) {
                        LibraryGridCard(
                            item = item,
                            onClick = { onOpenBook(item.book.id) },
                        )
                    } else {
                        LibraryListCard(
                            item = item,
                            onClick = { onOpenBook(item.book.id) },
                        )
                    }
                }
            }

            if (state.isImporting) {
                ImportProgressDialog(state = state)
            }

            state.importSummary?.let { summary ->
                ImportSummaryDialog(
                    summary = summary,
                    onDismiss = viewModel::dismissImportSummary,
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = "打开书架导航",
                    modifier = Modifier.size(25.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Taleon",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 31.sp,
                    letterSpacing = (-0.7).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 7.dp)
                        .size(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
            val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Button(
                onClick = onImport,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) Color(0xFF4A4A4D) else Color(0xFF3B3B3E),
                    contentColor = Color(0xFFF8F6F1),
                ),
            ) {
                Text("导入", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(10.dp))
        LibrarySearchField(
            query = query,
            onQueryChange = onQueryChange,
        )
    }
}

@Composable
private fun LibrarySearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索我的书架",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "清除搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .clickable { onQueryChange("") }
                                .padding(4.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ShelfToolbar(
    title: String,
    count: Int,
    sort: LibrarySort,
    viewMode: LibraryViewMode,
    onSort: (LibrarySort) -> Unit,
    onToggleView: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { sortMenuOpen = true }) {
                Icon(Icons.Outlined.Sort, contentDescription = "排序：${sort.label}")
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSort(option)
                            sortMenuOpen = false
                        },
                        leadingIcon = {
                            if (sort == option) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
        IconButton(onClick = onToggleView) {
            Icon(
                imageVector = if (viewMode == LibraryViewMode.GRID) {
                    Icons.Outlined.ViewList
                } else {
                    Icons.Outlined.GridView
                },
                contentDescription = if (viewMode == LibraryViewMode.GRID) "切换为列表" else "切换为网格",
            )
        }
    }
}

@Composable
private fun ContinueReadingCard(item: LibraryItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                book = item.book,
                modifier = Modifier
                    .width(62.dp)
                    .height(91.dp),
                cornerRadius = 5.dp,
            )
            Spacer(Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "继续阅读",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.book.author?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(12.dp))
                TaleonProgress(
                    fraction = item.fraction,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(item.fraction * 100).toInt()}% · ${formatRelativeTime(item.progress?.updatedAt ?: 0)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "继续",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDrawer(
    state: LibraryUiState,
    onClose: () -> Unit,
    onSelect: (ShelfFilter) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val selectedBackground = if (dark) Color(0xFF4A4A4D) else Color(0xFF3B3B3E)
    val selectedContent = Color(0xFFF8F6F1)
    val readingCount = state.allItems.count {
        it.book.lastOpenedAt != null && it.fraction < 0.999f
    }
    val favoriteCount = state.allItems.count { it.book.favorite }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 10.dp, top = 22.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "Taleon",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                letterSpacing = (-0.7).sp,
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp, bottom = 7.dp)
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaterialTheme.colorScheme.tertiary),
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "ENTER YOUR STORY.",
                            fontSize = 8.sp,
                            letterSpacing = 2.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭导航")
                    }
                }

                DrawerItem(
                    icon = Icons.Outlined.AutoStories,
                    label = "全部书籍",
                    count = state.allItems.size,
                    selected = state.filter == ShelfFilter.All,
                    selectedBackground = selectedBackground,
                    selectedContent = selectedContent,
                    onClick = { onSelect(ShelfFilter.All) },
                )
                DrawerItem(
                    icon = Icons.Outlined.StarBorder,
                    label = "收藏",
                    count = favoriteCount,
                    selected = state.filter == ShelfFilter.Favorites,
                    selectedBackground = selectedBackground,
                    selectedContent = selectedContent,
                    onClick = { onSelect(ShelfFilter.Favorites) },
                )
                DrawerItem(
                    icon = Icons.Outlined.History,
                    label = "在读",
                    count = readingCount,
                    selected = state.filter == ShelfFilter.Reading,
                    selectedBackground = selectedBackground,
                    selectedContent = selectedContent,
                    onClick = { onSelect(ShelfFilter.Reading) },
                )

                DrawerSectionTitle("分类")
                if (state.categories.isEmpty()) {
                    Text(
                        text = "在书籍详情中添加分类",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 22.dp, top = 3.dp, bottom = 8.dp),
                    )
                } else {
                    state.categories.take(6).forEach { category ->
                        DrawerItem(
                            icon = null,
                            label = category,
                            count = state.allItems.count { it.book.category == category },
                            selected = state.filter == ShelfFilter.Category(category),
                            selectedBackground = selectedBackground,
                            selectedContent = selectedContent,
                            indent = true,
                            onClick = { onSelect(ShelfFilter.Category(category)) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            DrawerItem(
                icon = Icons.Outlined.Settings,
                label = "设置",
                count = null,
                selected = false,
                selectedBackground = selectedBackground,
                selectedContent = selectedContent,
                onClick = onOpenSettings,
            )
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(12.dp),
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector?,
    label: String,
    count: Int?,
    selected: Boolean,
    selectedBackground: Color,
    selectedContent: Color,
    onClick: () -> Unit,
    indent: Boolean = false,
) {
    val contentColor = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .background(if (selected) selectedBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = if (indent) 46.dp else 22.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(15.dp))
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                color = if (selected) contentColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 6.dp),
    )
}

private fun activeShelfTitle(state: LibraryUiState): String {
    if (state.query.isNotBlank()) return "搜索结果"
    return when (val filter = state.filter) {
        ShelfFilter.All -> "全部书籍"
        ShelfFilter.Reading -> "在读"
        ShelfFilter.Favorites -> "收藏"
        is ShelfFilter.Category -> filter.category
    }
}

@Composable
private fun ImportProgressDialog(state: LibraryUiState) {
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("正在导入", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.importingName ?: "请稍候…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = {
                        if (state.importTotal == 0) 0f
                        else state.importCompleted.toFloat() / state.importTotal
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp)),
                )
            }
        }
    }
}

@Composable
private fun ImportSummaryDialog(summary: ImportSummary, onDismiss: () -> Unit) {
    val title = when {
        summary.imported > 0 -> "导入完成"
        summary.failures.isNotEmpty() -> "未能导入"
        else -> "没有新增书籍"
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (summary.failures.isEmpty()) Icons.Outlined.Check else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (summary.failures.isEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "成功导入 ${summary.imported} 本${if (summary.duplicates.isNotEmpty()) "，跳过 ${summary.duplicates.size} 本重复书籍" else ""}。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                summary.duplicates.take(4).forEach {
                    Text(
                        text = "重复：${it.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                summary.failures.take(6).forEach {
                    Text(
                        text = "${it.name}\n${it.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (summary.failures.size > 6) {
                    Text(
                        text = "另有 ${summary.failures.size - 6} 个文件失败",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("好") } },
    )
}
