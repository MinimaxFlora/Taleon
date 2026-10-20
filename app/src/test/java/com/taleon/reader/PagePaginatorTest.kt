package com.taleon.reader

import android.graphics.Typeface
import com.taleon.reader.domain.model.Chapter
import com.taleon.reader.ui.reader.PagePaginator
import com.taleon.reader.ui.reader.PaginationSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PagePaginatorTest {

    private val spec = PaginationSpec(
        widthPx = 640,
        heightPx = 820,
        textSizePx = 44f,
        lineSpacingMultiplier = 1.65f,
        paragraphSpacingPx = 16,
        typeface = Typeface.SERIF,
    )

    @Test
    fun splitsLongChapterIntoOrderedPages() {
        val body = (1..120).joinToString("\n") {
            "第${it}段：夜色落在书页上，风从窗缝里穿过，故事才刚刚开始。"
        }
        val chapter = Chapter(0, "第一章 长夜", body, 0)

        val pagination = PagePaginator.paginate(chapter, spec)

        assertTrue(pagination.pageCount > 1)
        assertTrue(pagination.pages.all { it.displayEnd > it.displayStart })
        pagination.pages.zipWithNext().forEach { (first, second) ->
            assertTrue(second.bodyStart >= first.bodyStart)
            assertTrue(second.bodyEnd >= first.bodyEnd)
            assertTrue(second.displayStart >= first.displayStart)
        }
        assertEquals(pagination.pageCount - 1, pagination.pageForOffset(Int.MAX_VALUE))
        assertEquals(0, pagination.pageForOffset(0))
    }

    @Test
    fun emptyChapterStillProducesReadableTitlePage() {
        val pagination = PagePaginator.paginate(Chapter(1, "第二章 空白", "", 0), spec)
        assertEquals(1, pagination.pageCount)
        assertEquals("第二章 空白", pagination.pages.single().let { pagination.displayText.subSequence(it.displayStart, it.displayEnd).toString().trim() })
    }

    @Test
    fun pageForOffsetFindsContainingPage() {
        val body = (1..200).joinToString("\n") { "第${it}行，这是一段用于分页测试的文字。" }
        val pagination = PagePaginator.paginate(Chapter(0, "测试", body, 0), spec)
        val middlePage = pagination.pageCount / 2
        val offset = pagination.pages[middlePage].bodyStart
        assertEquals(middlePage, pagination.pageForOffset(offset))
    }
}
