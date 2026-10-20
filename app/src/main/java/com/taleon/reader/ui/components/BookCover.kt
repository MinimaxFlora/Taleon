package com.taleon.reader.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taleon.reader.domain.model.Book
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val coverCache = object : LruCache<String, ImageBitmap>(24) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = 1
}

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val coverPath = book.coverPath
    val bitmap by produceState<ImageBitmap?>(initialValue = coverPath?.let(coverCache::get), key1 = coverPath) {
        if (coverPath == null) {
            value = null
            return@produceState
        }
        coverCache.get(coverPath)?.let {
            value = it
            return@produceState
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                if (!File(coverPath).exists()) return@runCatching null
                BitmapFactory.decodeFile(coverPath)?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) coverCache.put(coverPath, loaded)
        value = loaded
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), shape),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "${book.title}封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            GeneratedCover(book = book, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GeneratedCover(book: Book, modifier: Modifier = Modifier) {
    val palette = coverPalette(book.title + book.id)
    BoxWithConstraints(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(palette.first, palette.second),
            ),
        ),
    ) {
        val coverWidth = maxWidth
        val compact = coverWidth < 96.dp
        val medium = coverWidth < 132.dp
        val outerPadding = when {
            compact -> 4.dp
            medium -> 7.dp
            else -> 10.dp
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(if (compact) 3.dp else 6.dp),
                ),
        )
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = book.title.take(1).ifBlank { "T" },
                    color = Color(0xFFFBF7EF),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (coverWidth.value * 0.38f).coerceIn(18f, 34f).sp,
                    maxLines = 1,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (medium) 9.dp else 14.dp,
                        vertical = if (medium) 9.dp else 13.dp,
                    ),
            ) {
                Text(
                    text = "TALEON",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = if (medium) 6.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (medium) 1.2.sp else 1.8.sp,
                )
                Spacer(Modifier.weight(0.7f))
                Text(
                    text = book.title,
                    color = Color(0xFFFBF7EF),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (medium) 11.sp else 16.sp,
                    lineHeight = if (medium) 14.sp else 20.sp,
                    maxLines = if (medium) 3 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = book.author,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = if (medium) 7.sp else 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun coverPalette(seedText: String): Pair<Color, Color> {
    val palettes = listOf(
        Color(0xFF27375B) to Color(0xFF6C80BE),
        Color(0xFF443329) to Color(0xFFAA8967),
        Color(0xFF233832) to Color(0xFF699483),
        Color(0xFF3B2B39) to Color(0xFF9A6E8C),
        Color(0xFF1D2F42) to Color(0xFF678FB2),
        Color(0xFF4A3B22) to Color(0xFFC09A59),
        Color(0xFF332F45) to Color(0xFF8A7CAC),
    )
    val index = (seedText.hashCode() and Int.MAX_VALUE) % palettes.size
    return palettes[index]
}

@Composable
fun CoverStackPlaceholder(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.78f)
                .padding(start = 18.dp, top = 16.dp)
                .background(Color(0xFF9A7B5B).copy(alpha = 0.35f), shape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 8.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF243355), Color(0xFF697DBB))),
                    shape,
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "T",
                color = Color(0xFFF7F3EA),
                fontFamily = FontFamily.Serif,
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
