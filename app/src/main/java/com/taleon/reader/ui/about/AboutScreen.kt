package com.taleon.reader.ui.about

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taleon.reader.BuildConfig

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            Text("关于", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 12.dp,
                bottom = 44.dp,
            ),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(82.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "T",
                                fontFamily = FontFamily.Serif,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Taleon", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "ENTER YOUR STORY.",
                        fontSize = 10.sp,
                        letterSpacing = 2.6.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "一款安静、离线、专注于阅读本身的 Android 小说与电子书阅读器。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            item {
                AboutSection(title = "支持的格式") {
                    FormatInfo(
                        name = "TXT",
                        description = "自动识别 UTF-8、UTF-16 与 GB18030 编码，识别常见中文章节标题，支持翻页与滚动阅读。",
                    )
                    FormatInfo(
                        name = "EPUB",
                        description = "读取 EPUB 2/3 元数据、目录、封面与阅读顺序，将 XHTML 正文重排为舒适文本。当前版本不渲染复杂 CSS、内嵌字体与正文图片。",
                    )
                    FormatInfo(
                        name = "PDF",
                        description = "使用系统 PDF 引擎逐页渲染，支持缩放、平移与页码跳转。暂不支持 PDF 文本选择与全文搜索。",
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            item {
                AboutSection(title = "数据与隐私") {
                    Text(
                        text = "Taleon 没有申请网络权限。书籍文件、封面、阅读进度、书签与统计均保存在本机应用私有目录，不会上传到任何服务器。",
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "导入时会复制一份文件到应用目录，因此原文件被移动或删除后仍可继续阅读；重复导入会依据文件内容哈希自动跳过。",
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            item {
                AboutSection(title = "开源组件") {
                    LicenseRow("AndroidX · Jetpack Compose · Room", "Apache License 2.0")
                    LicenseRow("Kotlin · Kotlin Coroutines", "Apache License 2.0")
                    LicenseRow("Jsoup", "MIT License")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = "感谢这些开源项目让独立阅读器成为可能。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            item {
                AboutSection(title = "设计参考") {
                    Text(
                        text = "书架的信息组织、书页式阅读区与悬浮工具栏参考了开源阅读器 Koodo Reader 的界面思路。Taleon 未复制其源代码或图像素材。",
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            item {
                Text(
                    text = "Made for readers, with care.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AboutSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun FormatInfo(name: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp)) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.width(52.dp),
        ) {
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LicenseRow(name: String, license: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(license, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
