package com.taleon.reader.ui.reader

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.LineHeightSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.taleon.reader.domain.model.Chapter

data class PaginationSpec(
    val widthPx: Int,
    val heightPx: Int,
    val textSizePx: Float,
    val lineSpacingMultiplier: Float,
    val paragraphSpacingPx: Int,
    val typeface: Typeface,
)

data class PageSlice(
    val pageIndex: Int,
    val bodyStart: Int,
    val bodyEnd: Int,
    val displayStart: Int,
    val displayEnd: Int,
)

data class ChapterPagination(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pages: List<PageSlice>,
    val displayText: CharSequence,
    val bodyLength: Int,
) {
    val pageCount: Int get() = pages.size

    fun pageForOffset(offset: Int): Int {
        if (pages.isEmpty()) return 0
        if (offset <= 0) return 0
        val safe = offset.coerceAtMost(bodyLength)
        return pages.indexOfLast { it.bodyStart <= safe }.coerceAtLeast(0)
    }

    fun page(index: Int): PageSlice? = pages.getOrNull(index)

    fun offsetOfPage(index: Int): Int = pages.getOrNull(index)?.bodyStart ?: 0
}

data class ReaderPage(
    val chapterIndex: Int,
    val pageIndex: Int,
    val pageCount: Int,
    val bodyStart: Int,
    val bodyEnd: Int,
    val displayStart: Int,
    val displayEnd: Int,
    val displayText: CharSequence,
    val chapterTitle: String,
    val chapterLength: Int,
)

object PagePaginator {

    // StaticLayout.Builder accepts the historical Layout constants on API 26+. Lint checks them
    // against the newer LineBreaker constants, which have the same values but require API 29.
    @SuppressLint("WrongConstant")
    fun paginate(chapter: Chapter, spec: PaginationSpec): ChapterPagination {
        val width = spec.widthPx.coerceAtLeast(120)
        val height = spec.heightPx.coerceAtLeast(80)
        val title = chapter.title.trim()
        val prefix = if (title.isEmpty()) "" else "$title\n\n"
        val body = chapter.text
        val display = SpannableStringBuilder(prefix).append(body)
        if (title.isNotEmpty()) {
            display.setSpan(
                RelativeSizeSpan(1.32f),
                0,
                title.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            display.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                title.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (spec.paragraphSpacingPx > 0 && body.isNotEmpty()) {
            val spacing = ParagraphSpacingSpan(spec.paragraphSpacingPx)
            val prefixLength = prefix.length
            for (index in body.indices) {
                if (body[index].isWhitespace()) continue
                val isParagraphStart = index == 0 ||
                    (body[index - 1] == '\n' && (index < 2 || body[index - 2] != '\n'))
                if (isParagraphStart) {
                    display.setSpan(
                        spacing,
                        prefixLength + index,
                        prefixLength + index + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
        }

        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = spec.textSizePx
            typeface = spec.typeface
            letterSpacing = 0.008f
            isSubpixelText = true
        }
        val layout = StaticLayout.Builder.obtain(display, 0, display.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, spec.lineSpacingMultiplier)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            .build()

        if (layout.lineCount == 0) {
            return ChapterPagination(
                chapterIndex = chapter.index,
                chapterTitle = title,
                pages = listOf(PageSlice(0, 0, 0, 0, display.length)),
                displayText = display,
                bodyLength = body.length,
            )
        }

        val pages = ArrayList<PageSlice>()
        var line = 0
        while (line < layout.lineCount) {
            val pageTop = layout.getLineTop(line)
            var lastLine = line
            while (lastLine + 1 < layout.lineCount &&
                layout.getLineBottom(lastLine + 1) - pageTop <= height
            ) {
                lastLine++
            }
            val displayStart = layout.getLineStart(line)
            val displayEnd = layout.getLineEnd(lastLine).coerceAtLeast(displayStart)
            val bodyStart = (displayStart - prefix.length).coerceIn(0, body.length)
            val bodyEnd = (displayEnd - prefix.length).coerceIn(bodyStart, body.length)
            pages += PageSlice(
                pageIndex = pages.size,
                bodyStart = bodyStart,
                bodyEnd = bodyEnd,
                displayStart = displayStart,
                displayEnd = displayEnd,
            )
            line = lastLine + 1
        }

        if (pages.isEmpty()) {
            pages += PageSlice(0, 0, body.length, 0, display.length)
        }
        return ChapterPagination(
            chapterIndex = chapter.index,
            chapterTitle = title,
            pages = pages,
            displayText = display,
            bodyLength = body.length,
        )
    }

    fun readerPage(pagination: ChapterPagination, pageIndex: Int): ReaderPage? {
        val slice = pagination.pages.getOrNull(pageIndex) ?: return null
        return ReaderPage(
            chapterIndex = pagination.chapterIndex,
            pageIndex = slice.pageIndex,
            pageCount = pagination.pageCount,
            bodyStart = slice.bodyStart,
            bodyEnd = slice.bodyEnd,
            displayStart = slice.displayStart,
            displayEnd = slice.displayEnd,
            displayText = pagination.displayText,
            chapterTitle = pagination.chapterTitle,
            chapterLength = pagination.bodyLength,
        )
    }

    private class ParagraphSpacingSpan(private val extraSpacingPx: Int) : LineHeightSpan {
        override fun chooseHeight(
            text: CharSequence,
            start: Int,
            end: Int,
            spanstartv: Int,
            lineHeight: Int,
            fm: android.graphics.Paint.FontMetricsInt,
        ) {
            if (spanstartv == lineHeight) {
                fm.ascent -= extraSpacingPx
            }
        }
    }
}
