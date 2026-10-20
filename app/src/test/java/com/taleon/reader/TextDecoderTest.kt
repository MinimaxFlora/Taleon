package com.taleon.reader

import com.taleon.reader.data.files.TextDecoder
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDecoderTest {

    private val chinese = "第一章 风起\n夜色落在书页上，故事才刚刚开始。"

    @Test
    fun decodesUtf8WithBom() {
        val body = chinese.toByteArray(Charsets.UTF_8)
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body
        val decoded = TextDecoder.decode(bytes)
        assertEquals(chinese, decoded.text)
        assertEquals("UTF-8", decoded.charsetName)
    }

    @Test
    fun decodesGb18030() {
        val charset = Charset.forName("GB18030")
        val decoded = TextDecoder.decode(chinese.toByteArray(charset))
        assertEquals(chinese, decoded.text)
        assertEquals("GB18030", decoded.charsetName)
    }

    @Test
    fun decodesUtf16LittleEndianWithBom() {
        val body = chinese.toByteArray(Charsets.UTF_16LE)
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + body
        val decoded = TextDecoder.decode(bytes)
        assertEquals(chinese, decoded.text)
        assertEquals("UTF-16LE", decoded.charsetName)
    }

    @Test
    fun decodesUtf16BigEndianWithBom() {
        val body = chinese.toByteArray(Charsets.UTF_16BE)
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + body
        val decoded = TextDecoder.decode(bytes)
        assertEquals(chinese, decoded.text)
        assertEquals("UTF-16BE", decoded.charsetName)
    }

    @Test
    fun detectsUtf16WithoutBom() {
        val bytes = chinese.toByteArray(Charsets.UTF_16LE)
        val decoded = TextDecoder.decode(bytes)
        assertEquals(chinese, decoded.text)
    }

    @Test
    fun preservesPlainAscii() {
        val decoded = TextDecoder.decode("Hello, Taleon.\nChapter 1".toByteArray())
        assertTrue(decoded.text.startsWith("Hello"))
        assertEquals("UTF-8", decoded.charsetName)
    }
}
