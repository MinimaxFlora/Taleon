package com.taleon.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.taleon.reader.domain.model.AppSettings
import com.taleon.reader.domain.model.PageMode
import com.taleon.reader.domain.model.PageTurnAnimation
import com.taleon.reader.domain.model.ReaderTheme
import com.taleon.reader.domain.model.ReadingFont
import com.taleon.reader.ui.theme.ReaderPalette
import com.taleon.reader.ui.theme.readerPalette
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: AppSettings,
    palette: ReaderPalette,
    isPdf: Boolean,
    onDismiss: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontChange: (ReadingFont) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onParagraphSpacingChange: (Float) -> Unit,
    onHorizontalPaddingChange: (Float) -> Unit,
    onReaderThemeChange: (ReaderTheme) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onPageTurnAnimationChange: (PageTurnAnimation) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fontSize by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize) }
    var lineHeight by remember(settings.lineHeight) { mutableFloatStateOf(settings.lineHeight) }
    var paragraphSpacing by remember(settings.paragraphSpacingDp) {
        mutableFloatStateOf(settings.paragraphSpacingDp)
    }
    var horizontalPadding by remember(settings.horizontalPaddingDp) {
        mutableFloatStateOf(settings.horizontalPaddingDp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.text,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .background(palette.secondaryText.copy(alpha = 0.35f), RoundedCornerShape(99.dp)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = palette.secondaryText)
                }
            }
            Spacer(Modifier.height(8.dp))

            ReaderPreview(
                palette = readerPalette(settings.readerTheme),
                fontSize = fontSize,
                lineHeight = lineHeight,
                font = settings.readingFont,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(22.dp))

            SettingsSection(title = "阅读主题", palette = palette) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReaderTheme.entries.forEach { theme ->
                        val themePalette = readerPalette(theme)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(width = 58.dp, height = 42.dp)
                                    .background(themePalette.background, RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (settings.readerTheme == theme) 2.dp else 1.dp,
                                        color = if (settings.readerTheme == theme) {
                                            palette.accent
                                        } else {
                                            palette.secondaryText.copy(alpha = 0.25f)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable { onReaderThemeChange(theme) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Aa",
                                    color = themePalette.text,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 14.sp,
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = theme.label,
                                fontSize = 11.sp,
                                color = if (settings.readerTheme == theme) palette.text else palette.secondaryText,
                            )
                        }
                    }
                }
            }

            if (!isPdf) {
                SettingsDivider(palette)
                SettingsSection(title = "字号", palette = palette) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val value = (fontSize - 1f).coerceAtLeast(12f)
                                fontSize = value
                                onFontSizeChange(value)
                            },
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "减小字号", tint = palette.text)
                        }
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            onValueChangeFinished = { onFontSizeChange(fontSize) },
                            valueRange = 12f..30f,
                            modifier = Modifier.weight(1f),
                            colors = readerSliderColors(palette),
                        )
                        IconButton(
                            onClick = {
                                val value = (fontSize + 1f).coerceAtMost(30f)
                                fontSize = value
                                onFontSizeChange(value)
                            },
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "增大字号", tint = palette.text)
                        }
                        Text(
                            text = "${fontSize.toInt()}",
                            color = palette.secondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp),
                        )
                    }
                }

                SettingsSection(title = "字体", palette = palette) {
                    ChoiceChips(
                        options = ReadingFont.entries,
                        label = { it.label },
                        selected = settings.readingFont,
                        onSelect = onFontChange,
                        palette = palette,
                    )
                }

                SettingsDivider(palette)
                SettingSlider(
                    title = "行距",
                    valueLabel = String.format(Locale.CHINA, "%.2f×", lineHeight),
                    value = lineHeight,
                    range = 1.15f..2.3f,
                    palette = palette,
                    onValueChange = { lineHeight = it },
                    onValueChangeFinished = { onLineHeightChange(lineHeight) },
                )

                SettingSlider(
                    title = "段落间距",
                    valueLabel = "${paragraphSpacing.toInt()} dp",
                    value = paragraphSpacing,
                    range = 0f..20f,
                    palette = palette,
                    onValueChange = { paragraphSpacing = it },
                    onValueChangeFinished = { onParagraphSpacingChange(paragraphSpacing) },
                )

                SettingSlider(
                    title = "页面边距",
                    valueLabel = "${horizontalPadding.toInt()} dp",
                    value = horizontalPadding,
                    range = 12f..40f,
                    palette = palette,
                    onValueChange = { horizontalPadding = it },
                    onValueChangeFinished = { onHorizontalPaddingChange(horizontalPadding) },
                )

                SettingsDivider(palette)
                SettingsSection(title = "阅读方式", palette = palette) {
                    ChoiceChips(
                        options = PageMode.entries,
                        label = { it.label },
                        selected = settings.pageMode,
                        onSelect = onPageModeChange,
                        palette = palette,
                    )
                }

                SettingsSection(title = "翻页动画", palette = palette) {
                    ChoiceChips(
                        options = PageTurnAnimation.entries,
                        label = { it.label },
                        selected = settings.pageTurnAnimation,
                        onSelect = onPageTurnAnimationChange,
                        palette = palette,
                    )
                }

                SettingsDivider(palette)
            }

            SwitchRow(
                title = "音量键翻页",
                subtitle = if (isPdf) "使用音量键切换 PDF 页面" else "仅翻页模式生效",
                checked = settings.volumeKeysTurnPages,
                onCheckedChange = onVolumeKeysChange,
                palette = palette,
            )
            SwitchRow(
                title = "阅读时保持屏幕常亮",
                subtitle = "退出阅读后自动恢复系统设置",
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
                palette = palette,
            )

            if (isPdf) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "PDF 按原始页面显示，字号、行距与页面边距不会改变 PDF 内容。",
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderPreview(
    palette: ReaderPalette,
    fontSize: Float,
    lineHeight: Float,
    font: ReadingFont,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = palette.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.secondaryText.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = "夜色落在书页上",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = (fontSize * 0.78f).sp,
                color = palette.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "故事才刚刚开始。风从窗缝里穿过，翻动着一页又一页未读完的时光。",
                fontFamily = when (font) {
                    ReadingFont.SERIF -> FontFamily.Serif
                    ReadingFont.SANS -> FontFamily.SansSerif
                    ReadingFont.MONO -> FontFamily.Monospace
                },
                fontSize = fontSize.sp,
                lineHeight = (fontSize * lineHeight).sp,
                color = palette.text,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    palette: ReaderPalette,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.secondaryText,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SettingsDivider(palette: ReaderPalette) {
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(1.dp)
            .background(palette.secondaryText.copy(alpha = 0.12f)),
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    palette: ReaderPalette,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 13.sp, color = palette.text, modifier = Modifier.weight(1f))
            Text(valueLabel, fontSize = 12.sp, color = palette.secondaryText)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = readerSliderColors(palette),
        )
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    label: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    palette: ReaderPalette,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 12.sp) },
                shape = RoundedCornerShape(99.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = palette.secondaryText,
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = if (palette.isDark) Color(0xFF17171D) else Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == option,
                    borderColor = palette.secondaryText.copy(alpha = 0.28f),
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    palette: ReaderPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = palette.text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = palette.secondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.secondaryText,
                uncheckedTrackColor = palette.secondaryText.copy(alpha = 0.22f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun readerSliderColors(palette: ReaderPalette) = SliderDefaults.colors(
    thumbColor = palette.accent,
    activeTrackColor = palette.accent,
    inactiveTrackColor = palette.secondaryText.copy(alpha = 0.2f),
)
