package com.taleon.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taleon.reader.domain.model.Chapter
import com.taleon.reader.ui.theme.ReaderPalette
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    chapters: List<Chapter>,
    currentChapter: Int,
    palette: ReaderPalette,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentChapter.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.text,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = { SheetHandle(palette) },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "目录",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.text,
                    )
                    Text(
                        "共 ${chapters.size} 章",
                        fontSize = 11.sp,
                        color = palette.secondaryText,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭目录", tint = palette.secondaryText)
                }
            }
            HorizontalDivider(color = palette.secondaryText.copy(alpha = 0.12f))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp),
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.index }) { index, chapter ->
                    val selected = chapter.index == currentChapter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) palette.accent.copy(alpha = 0.1f) else Color.Transparent,
                            )
                            .clickable { onSelect(chapter.index) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            fontSize = 11.sp,
                            color = if (selected) palette.accent else palette.secondaryText,
                            modifier = Modifier.width(32.dp),
                        )
                        Text(
                            text = chapter.title.ifBlank { "第 ${index + 1} 章" },
                            fontSize = 14.sp,
                            color = if (selected) palette.text else palette.text.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (selected) "正在阅读" else wordCount(chapter.length),
                            fontSize = 10.sp,
                            color = if (selected) palette.accent else palette.secondaryText,
                        )
                    }
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageSheet(
    currentPage: Int,
    pageCount: Int,
    palette: ReaderPalette,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember(currentPage, pageCount) {
        mutableFloatStateOf(currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)).toFloat())
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.text,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = { SheetHandle(palette) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 36.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("跳转页码", style = MaterialTheme.typography.titleLarge, color = palette.text)
                    Text("共 $pageCount 页", fontSize = 11.sp, color = palette.secondaryText)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = palette.secondaryText)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = (selected.toInt() + 1).toString(),
                fontSize = 44.sp,
                color = palette.text,
            )
            Text("页", fontSize = 12.sp, color = palette.secondaryText)
            Spacer(Modifier.height(16.dp))
            Slider(
                value = selected,
                onValueChange = { selected = it },
                valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                enabled = pageCount > 1,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.secondaryText.copy(alpha = 0.2f),
                ),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onJump(selected.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = if (palette.isDark) Color(0xFF17171D) else Color.White,
                ),
            ) {
                Text("跳转到第 ${selected.toInt() + 1} 页")
            }
        }
    }
}

@Composable
private fun SheetHandle(palette: ReaderPalette) {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 4.dp)
            .size(width = 34.dp, height = 4.dp)
            .background(palette.secondaryText.copy(alpha = 0.35f), RoundedCornerShape(99.dp)),
    )
}

private fun wordCount(length: Int): String = when {
    length >= 10_000 -> String.format(Locale.CHINA, "%.1f 万字", length / 10_000f)
    length >= 1_000 -> String.format(Locale.CHINA, "%.1f 千字", length / 1_000f)
    else -> "$length 字"
}
