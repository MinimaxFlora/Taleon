package com.taleon.reader.data.files

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class DecodedText(
    val text: String,
    val charsetName: String,
)

class TextDecodingException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * TXT files have no reliable encoding declaration. Prefer byte-order marks, then strict UTF-8,
 * then GB18030 (a superset of GBK/GB2312 used by most Chinese ebooks).
 */
object TextDecoder {

    fun decode(bytes: ByteArray): DecodedText {
        if (bytes.isEmpty()) return DecodedText("", "UTF-8")

        val bom = detectBom(bytes)
        if (bom != null) {
            val offset = bom.second
            val decoded = decodeStrict(bytes, offset, bytes.size - offset, bom.first)
                ?: throw TextDecodingException("文件声明为 ${bom.first.name()}，但内容无法按该编码读取。")
            return DecodedText(decoded, bom.first.name())
        }

        detectUtf16WithoutBom(bytes)?.let { (charset, text) ->
            return DecodedText(text, charset.name())
        }

        decodeStrict(bytes, 0, bytes.size, Charsets.UTF_8)?.let {
            return DecodedText(it, "UTF-8")
        }

        val gb18030 = Charset.forName("GB18030")
        decodeStrict(bytes, 0, bytes.size, gb18030)?.let {
            return DecodedText(it, "GB18030")
        }

        // Last resort: keep the file readable with replacement characters instead of failing import.
        val fallback = String(bytes, Charsets.UTF_8)
        if (fallback.count { it == '\uFFFD' } > fallback.length / 4) {
            throw TextDecodingException("无法识别文本编码。请将文件转换为 UTF-8 后重试。")
        }
        return DecodedText(fallback, "UTF-8")
    }

    private fun detectBom(bytes: ByteArray): Pair<Charset, Int>? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8 to 3
        }
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) return Charsets.UTF_16LE to 2
            if (b0 == 0xFE && b1 == 0xFF) return Charsets.UTF_16BE to 2
        }
        return null
    }

    private fun detectUtf16WithoutBom(bytes: ByteArray): Pair<Charset, String>? {
        val sampleSize = minOf(bytes.size, 4096)
        if (sampleSize < 8 || bytes.size % 2 != 0) return null
        var evenZeros = 0
        var oddZeros = 0
        var evenCjkHighBytes = 0
        var oddCjkHighBytes = 0
        for (i in 0 until sampleSize) {
            val value = bytes[i].toInt() and 0xFF
            if (value == 0) {
                if (i % 2 == 0) evenZeros++ else oddZeros++
            }
            // In UTF-16LE, the most significant byte of common CJK characters sits at an
            // odd byte index (0x4E..0x9F); UTF-16BE puts it at an even index.
            if (value in 0x4E..0x9F) {
                if (i % 2 == 0) evenCjkHighBytes++ else oddCjkHighBytes++
            }
        }
        val likelyLittleEndian = oddCjkHighBytes >= 2 &&
            oddCjkHighBytes > evenCjkHighBytes * 1.5 &&
            (oddZeros > 0 || evenZeros > 0)
        val likelyBigEndian = evenCjkHighBytes >= 2 &&
            evenCjkHighBytes > oddCjkHighBytes * 1.5 &&
            (oddZeros > 0 || evenZeros > 0)
        if (!likelyLittleEndian && !likelyBigEndian) return null
        return chooseUtf16Candidate(bytes, preferLittleEndian = likelyLittleEndian) ?: return null
    }

    private fun chooseUtf16Candidate(
        bytes: ByteArray,
        preferLittleEndian: Boolean?,
    ): Pair<Charset, String>? {
        val candidates = buildList {
            decodeStrict(bytes, 0, bytes.size, Charsets.UTF_16LE)?.let { add(Charsets.UTF_16LE to it) }
            decodeStrict(bytes, 0, bytes.size, Charsets.UTF_16BE)?.let { add(Charsets.UTF_16BE to it) }
        }
        if (candidates.isEmpty()) return null
        val ranked = candidates.map { (charset, text) -> Triple(charset, text, textQuality(text)) }
            .sortedByDescending { it.third }
        val best = ranked.first()
        val minimum = if (preferLittleEndian == null) 0.9 else 0.7
        if (best.third < minimum) return null
        if (preferLittleEndian != null) {
            val preferred = ranked.firstOrNull {
                (it.first == Charsets.UTF_16LE) == preferLittleEndian
            }
            if (preferred != null && preferred.third >= best.third - 0.05) {
                return preferred.first to preferred.second
            }
        }
        return best.first to best.second
    }

    private fun textQuality(text: String): Double {
        if (text.isEmpty()) return 0.0
        var good = 0
        var bad = 0
        for (character in text) {
            when {
                character == '\uFFFD' -> bad += 4
                character == '\n' || character == '\r' || character == '\t' -> good++
                character.isISOControl() -> bad += 4
                character.isWhitespace() -> good++
                character in '\u4E00'..'\u9FFF' -> good += 2
                character in '\u3000'..'\u303F' -> good += 2
                character in '\uFF00'..'\uFFEF' -> good += 2
                character.isLetterOrDigit() -> good++
                Character.isSurrogate(character) -> bad += 4
                else -> good++
            }
        }
        return good.toDouble() / (good + bad).coerceAtLeast(1)
    }

    private fun decodeStrict(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String? {
        if (length <= 0) return ""
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, offset, length)).toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }
}
