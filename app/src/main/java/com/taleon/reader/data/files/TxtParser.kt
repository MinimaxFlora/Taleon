package com.taleon.reader.data.files

import com.taleon.reader.domain.model.Chapter
import com.taleon.reader.domain.model.ParsedBook

/**
 * Splits TXT novels into chapters without assuming one specific naming style.
 * All parsing here is pure JVM code so it can be unit tested without Android.
 */
object TxtParser {

    private const val MAX_CHAPTER_CHARS = 40_000

    private val chapterPatterns = listOf(
        Regex("""^第[零〇一二三四五六七八九十百千万两0-9]{1,12}[章节回卷部篇集](?:\s*[：:、.．\-—]?\s*.*)?$"""),
        Regex("""^(?:序章|楔子|引子|前言|序言|后记|尾声|终章|大结局|番外(?:篇)?)(?:\s*[：:、.．\-—]?\s*.*)?$"""),
        Regex("""^(?:Chapter|CHAPTER)\s+(?:[0-9]{1,4}|[IVXLCDM]{1,8})(?:\s*[：:.\-—]\s*.*)?$"""),
        Regex("""^卷[零〇一二三四五六七八九十百千万0-9]{1,8}(?:\s+.*)?$"""),
    )

    private val bookNamePattern = Regex("""^(?:书名|作品名|标题)\s*[：:]\s*(.+)$""")
    private val authorPattern = Regex("""^(?:作者|著者)\s*[：:]\s*(.+)$""")

    fun parse(bytes: ByteArray, fallbackTitle: String): ParsedBook {
        val decoded = try {
            TextDecoder.decode(bytes)
        } catch (error: TextDecodingException) {
            throw BookParseException(error.message ?: "文本编码无法识别。", error)
        }
        return parseText(decoded.text, fallbackTitle)
    }

    fun parseText(rawText: String, fallbackTitle: String): ParsedBook {
        if (rawText.isBlank()) throw BookParseException("文件内容为空。")
        val text = normalizeNewlines(rawText)
        val lines = collectLines(text)
        if (lines.isEmpty()) throw BookParseException("文件内容为空。")

        val metadata = extractMetadata(lines.take(12))
        val headings = buildList {
            for (line in lines) {
                val candidate = line.text
                    .trim()
                    .removePrefix("\uFEFF")
                    .replace(Regex("""^#{1,6}\s*"""), "")
                    .trim()
                if (isChapterHeading(candidate)) {
                    add(Heading(line.start, line.end, candidate))
                }
            }
        }

        val chapters = if (headings.isEmpty()) {
            chunkChapter(
                baseTitle = "正文",
                body = collapseBlankLines(text).trim(),
                startOffset = 0,
            )
        } else {
            buildList {
                val prefaceEnd = headings.first().lineStart
                if (prefaceEnd > 0) {
                    val preface = collapseBlankLines(text.substring(0, prefaceEnd)).trim()
                    if (preface.isNotBlank() && !isOnlyMetadata(preface)) {
                        add(Chapter(index = 0, title = "前言", text = preface, startOffset = 0))
                    }
                }
                headings.forEachIndexed { index, heading ->
                    val contentStart = heading.lineEnd + 1
                    val contentEnd = headings.getOrNull(index + 1)?.lineStart ?: text.length
                    if (contentStart > text.length || contentEnd < contentStart) return@forEachIndexed
                    val body = collapseBlankLines(text.substring(contentStart, contentEnd)).trim('\n', ' ', '\u3000')
                    addAll(chunkChapter(heading.title, body, heading.lineStart))
                }
            }
        }

        if (chapters.isEmpty()) {
            val body = collapseBlankLines(text).trim()
            if (body.isBlank()) throw BookParseException("文件没有可阅读的正文。")
            return ParsedBook(
                title = metadata.title ?: fallbackTitle.cleanTitle(),
                author = metadata.author,
                chapters = listOf(Chapter(0, "正文", body, 0)),
            )
        }

        val reindexed = chapters.mapIndexed { index, chapter -> chapter.copy(index = index) }
        return ParsedBook(
            title = metadata.title ?: fallbackTitle.cleanTitle(),
            author = metadata.author,
            chapters = reindexed,
        )
    }

    private fun chunkChapter(baseTitle: String, body: String, startOffset: Int): List<Chapter> {
        if (body.length <= MAX_CHAPTER_CHARS) {
            return listOf(Chapter(0, baseTitle, body, startOffset))
        }
        val result = mutableListOf<Chapter>()
        var cursor = 0
        var part = 0
        while (cursor < body.length) {
            var end = minOf(body.length, cursor + MAX_CHAPTER_CHARS)
            if (end < body.length) {
                val newline = body.lastIndexOf('\n', end)
                if (newline > cursor + MAX_CHAPTER_CHARS / 2) end = newline
            }
            val slice = body.substring(cursor, end).trim('\n')
            if (slice.isNotBlank()) {
                val title = if (part == 0) baseTitle else "$baseTitle（续${part}）"
                result += Chapter(part, title, slice, startOffset + cursor)
                part++
            }
            cursor = end
        }
        return result
    }

    private fun extractMetadata(lines: List<Line>): Metadata {
        var title: String? = null
        var author: String? = null
        for (line in lines) {
            val trimmed = line.text.trim()
            if (title == null) {
                bookNamePattern.matchEntire(trimmed)?.groupValues?.getOrNull(1)?.trim()
                    ?.takeIf { it.isNotEmpty() }?.let { title = it }
            }
            if (author == null) {
                authorPattern.matchEntire(trimmed)?.groupValues?.getOrNull(1)?.trim()
                    ?.takeIf { it.isNotEmpty() }?.let { author = it }
            }
            if (title != null && author != null) break
        }
        return Metadata(title, author)
    }

    private fun isOnlyMetadata(preface: String): Boolean {
        val nonEmpty = preface.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return nonEmpty.isNotEmpty() && nonEmpty.all {
            bookNamePattern.matches(it) || authorPattern.matches(it) ||
                it.matches(Regex("""^(?:来源|简介|内容简介|下载|声明)\s*[：:].*$"""))
        }
    }

    fun isChapterHeading(raw: String): Boolean {
        val candidate = raw.trim()
        if (candidate.isEmpty() || candidate.length > 48) return false
        if (candidate.any { it == '。' || it == '！' || it == '？' || it == '!' || it == '?' }) return false
        if (candidate.endsWith('，') || candidate.endsWith(',') || candidate.endsWith('；') || candidate.endsWith(';')) {
            return false
        }
        return chapterPatterns.any { it.matches(candidate) }
    }

    private fun normalizeNewlines(raw: String): String =
        raw.replace("\r\n", "\n").replace('\r', '\n').removePrefix("\uFEFF")

    private fun collapseBlankLines(text: String): String =
        text.replace(Regex("""\n[ \t\u3000]*\n(?:[ \t\u3000]*\n)+"""), "\n\n")

    private data class Line(val text: String, val start: Int, val end: Int)
    private data class Heading(val lineStart: Int, val lineEnd: Int, val title: String)
    private data class Metadata(val title: String?, val author: String?)

    private fun collectLines(text: String): List<Line> {
        val lines = ArrayList<Line>(text.count { it == '\n' } + 1)
        var start = 0
        var index = 0
        while (index <= text.length) {
            if (index == text.length || text[index] == '\n') {
                lines += Line(text.substring(start, index), start, index)
                start = index + 1
            }
            index++
        }
        return lines
    }

    private fun String.cleanTitle(): String {
        val withoutExtension = substringBeforeLast('.', this).trim()
        return withoutExtension.ifBlank { "未命名书籍" }
    }
}

class BookParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
