package com.taleon.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleon.reader.AppContainer
import com.taleon.reader.BuildConfig
import com.taleon.reader.domain.model.LibrarySort
import com.taleon.reader.domain.model.LibraryViewMode
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ThemeMode
import com.taleon.reader.ui.components.formatFileSize
import com.taleon.reader.ui.theme.readerPalette
import java.util.Locale

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = remember(container) {
            SettingsViewModelFactory(container.bookRepository, container.settingsRepository)
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
                    Text(
                        text = "把阅读调成你喜欢的样子。",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "所有偏好只保存在这台设备上。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsCard(title = "外观", subtitle = "应用主题") {
                    ChoiceRow(
                        options = ThemeMode.entries,
                        label = { it.label },
                        selected = state.settings.themeMode,
                        onSelect = viewModel::setThemeMode,
                    )
                }
            }

            item {
                SettingsCard(title = "书架", subtitle = "默认排列方式") {
                    Text(
                        "视图",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(7.dp))
                    ChoiceRow(
                        options = LibraryViewMode.entries,
                        label = { if (it == LibraryViewMode.GRID) "网格" else "列表" },
                        selected = state.settings.libraryViewMode,
                        onSelect = viewModel::setLibraryView,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "排序",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(7.dp))
                    ChoiceRow(
                        options = LibrarySort.entries,
                        label = { it.label },
                        selected = state.settings.librarySort,
                        onSelect = viewModel::setLibrarySort,
                    )
                }
            }

            item {
                SettingsCard(title = "阅读", subtitle = "新打开的书籍会使用这些默认值") {
                    val readerPalette = readerPalette(state.settings.readerTheme)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReaderTheme.entries.forEach { theme ->
                            val palette = readerPalette(theme)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 58.dp, height = 42.dp)
                                        .background(palette.background, RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (state.settings.readerTheme == theme) 2.dp else 1.dp,
                                            color = if (state.settings.readerTheme == theme) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable { viewModel.setReaderTheme(theme) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Aa",
                                        color = palette.text,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 14.sp,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    theme.label,
                                    fontSize = 10.sp,
                                    color = if (state.settings.readerTheme == theme) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    var fontSize by remember(state.settings.fontSize) {
                        mutableFloatStateOf(state.settings.fontSize)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "正文字号",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(64.dp),
                        )
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            onValueChangeFinished = { viewModel.setFontSize(fontSize) },
                            valueRange = 12f..30f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${fontSize.toInt()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ChoiceRow(
                        options = PageMode.entries,
                        label = { it.label },
                        selected = state.settings.pageMode,
                        onSelect = viewModel::setPageMode,
                    )
                    Spacer(Modifier.height(14.dp))
                    SimpleSwitchRow(
                        title = "音量键翻页",
                        checked = state.settings.volumeKeysTurnPages,
                        onCheckedChange = viewModel::setVolumeKeys,
                    )
                    SimpleSwitchRow(
                        title = "保持屏幕常亮",
                        checked = state.settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "字号、行距、边距与动画可在阅读界面随时调整。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingsCard(title = "阅读统计", subtitle = "只记录本机上的阅读时间") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = formatDuration(state.totalReadingSeconds),
                            label = "累计阅读",
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = "${state.booksRead} 本",
                            label = "读过的书",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                SettingsCard(title = "隐私与存储", subtitle = "离线优先，不联网") {
                    InfoRow(
                        icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                        title = "没有网络权限",
                        subtitle = "Taleon 不会上传书籍、阅读进度或统计数据。",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    InfoRow(
                        icon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        title = "本地占用",
                        subtitle = "导入的书籍与封面存储在应用私有目录。",
                        trailing = formatFileSize(state.storedBytes),
                    )
                }
            }

            item {
                Surface(
                    onClick = onOpenAbout,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("关于 Taleon", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "版本 ${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Taleon · Enter Your Story.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    label: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 12.sp) },
                shape = RoundedCornerShape(99.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == option,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SimpleSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                value,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) { icon() }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0 分钟"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "$hours 小时 $minutes 分"
        hours > 0 -> "$hours 小时"
        minutes > 0 -> "$minutes 分钟"
        else -> String.format(Locale.CHINA, "%d 秒", seconds)
    }
}
