package com.taleon.reader

import com.taleon.reader.data.files.TxtParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtParserTest {

    @Test
    fun readsMetadataAndSplitsChineseChapters() {
        val raw = """
            书名：星河入梦
            作者：林川

            第一章 风起
            夜色落在窗沿上。
            他翻开了第一页。

            第二章 夜深
            远处传来钟声。
        """.trimIndent()

        val book = TxtParser.parseText(raw, "fallback")

        assertEquals("星河入梦", book.title)
        assertEquals("林川", book.author)
        assertEquals(2, book.chapters.size)
        assertEquals("第一章 风起", book.chapters[0].title)
        assertTrue(book.chapters[0].text.contains("第一页"))
        assertEquals("第二章 夜深", book.chapters[1].title)
        assertEquals("远处传来钟声。", book.chapters[1].text.trim())
        assertTrue(book.chapters[0].startOffset >= 0)
        assertTrue(book.chapters[1].startOffset > book.chapters[0].startOffset)
    }

    @Test
    fun keepsPrefaceBeforeFirstHeading() {
        val raw = """
            这个故事发生在一个没有地图的国度。

            第一章 出发
            清晨，雨停了。
        """.trimIndent()

        val book = TxtParser.parseText(raw, "游记")

        assertEquals(2, book.chapters.size)
        assertEquals("前言", book.chapters[0].title)
        assertTrue(book.chapters[0].text.contains("没有地图"))
        assertEquals("第一章 出发", book.chapters[1].title)
    }

    @Test
    fun createsSingleChapterWhenNoHeadingsExist() {
        val raw = "只是一段很短的故事。\n没有章节标题。"
        val book = TxtParser.parseText(raw, "短篇.txt")
        assertEquals(1, book.chapters.size)
        assertEquals("正文", book.chapters[0].title)
        assertEquals("短篇", book.title)
    }

    @Test
    fun handlesWindowsLineEndingsAndBlankLines() {
        val raw = "第一章 起点\r\n第一段\r\n\r\n\r\n第二段\r\n"
        val book = TxtParser.parseText(raw, "test")
        assertEquals("第一章 起点", book.chapters[0].title)
        assertTrue(book.chapters[0].text.contains("第二段"))
        assertFalse(book.chapters[0].text.contains("\r"))
    }

    @Test
    fun recognizesCommonChapterForms() {
        assertTrue(TxtParser.isChapterHeading("第十二章 归来"))
        assertTrue(TxtParser.isChapterHeading("序章"))
        assertTrue(TxtParser.isChapterHeading("Chapter 3: The Road"))
        assertTrue(TxtParser.isChapterHeading("番外篇 一"))
        assertFalse(TxtParser.isChapterHeading("他翻开了第二章，却什么也没看见。"))
        assertFalse(TxtParser.isChapterHeading("这是普通的一行文字"))
    }

    @Test
    fun splitsExtremelyLongChapterAtParagraphBoundary() {
        val paragraph = "这是一个很长的段落，用来验证超长章节会被拆分。".repeat(30)
        val raw = "第一章 长夜\n" + (0 until 80).joinToString("\n\n") { paragraph }
        val book = TxtParser.parseText(raw, "long")
        assertTrue(book.chapters.size > 1)
        assertTrue(book.chapters.all { it.text.length <= 40_100 })
        assertTrue(book.chapters.first().title.startsWith("第一章 长夜"))
    }
}
