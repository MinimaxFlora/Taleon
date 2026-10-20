package com.taleon.reader.data.files

import com.taleon.reader.domain.model.Chapter
import com.taleon.reader.domain.model.ParsedBook
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * A safe EPUB 2/3 text reflow parser. It reads OPF spine order and the EPUB navigation document
 * (or NCX fallback), extracts the cover, and turns XHTML blocks into readable paragraphs.
 *
 * Rich CSS layout, embedded fonts and inline images are intentionally not rendered in this first
 * version. Images that carry meaning only as images are skipped.
 */
class EpubParser {

    fun parse(file: File, fallbackTitle: String): ParsedBook {
        ZipFile(file).use { zip ->
            validateArchive(zip)
            val entries = zip.entries().asSequence().associateBy { it.name }
            val containerEntry = entries["META-INF/container.xml"]
                ?: throw BookParseException("EPUB 缺少 META-INF/container.xml。")
            val container = parseXml(readEntry(zip, containerEntry, 512 * 1024), "container.xml")
            val rootPath = container.getElementsByTag("rootfile").firstOrNull()
                ?.attr("full-path")
                ?.takeIf { it.isNotBlank() }
                ?: throw BookParseException("EPUB 没有声明内容清单（OPF）。")
            normalizePath(rootPath) ?: throw BookParseException("EPUB 内容清单路径无效。")

            val opfEntry = entries[rootPath] ?: entries[normalizePath(rootPath)]
                ?: throw BookParseException("找不到 EPUB 内容清单：$rootPath")
            val opf = parseXml(readEntry(zip, opfEntry, 8 * 1024 * 1024), rootPath)
            val opfDir = rootPath.substringBeforeLast('/', "")

            val metadata = opf.getElementsByTag("metadata").firstOrNull()
            val title = metadata?.getElementsByTag("dc:title")?.firstOrNull()?.text()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: fallbackTitle.substringBeforeLast('.', fallbackTitle).trim().ifBlank { "未命名书籍" }
            val author = metadata?.getElementsByTag("dc:creator")
                ?.mapNotNull { it.text().trim().takeIf(String::isNotEmpty) }
                ?.joinToString("、")
                ?.takeIf { it.isNotBlank() }

            val manifestItems = opf.getElementsByTag("item").mapNotNull { element ->
                val id = element.attr("id")
                val href = element.attr("href")
                if (id.isBlank() || href.isBlank()) return@mapNotNull null
                ManifestItem(
                    id = id,
                    href = href,
                    path = resolvePath(opfDir, href) ?: return@mapNotNull null,
                    mediaType = element.attr("media-type").lowercase(),
                    properties = element.attr("properties"),
                )
            }.associateBy { it.id }

            val navTitles = readNavigationTitles(zip, entries, manifestItems, opf)
            val coverBytes = readCover(zip, entries, manifestItems, opf)
            val spinePaths = opf.getElementsByTag("itemref").mapNotNull { itemref ->
                val idref = itemref.attr("idref")
                if (itemref.attr("linear").equals("no", ignoreCase = true)) return@mapNotNull null
                manifestItems[idref]
                    ?.takeIf { it.mediaType in HTML_TYPES }
                    ?.path
            }.ifEmpty {
                manifestItems.values
                    .filter { it.mediaType in HTML_TYPES && !it.properties.contains("nav") }
                    .map { it.path }
            }.distinct()

            if (spinePaths.isEmpty()) throw BookParseException("EPUB 没有可阅读的章节。")

            val chapters = ArrayList<Chapter>()
            var totalRead = 0L
            spinePaths.forEach { path ->
                val entry = entries[path] ?: return@forEach
                if (entry.size > MAX_HTML_BYTES) throw BookParseException("EPUB 章节文件过大：${entry.name}")
                val bytes = readEntry(zip, entry, MAX_HTML_BYTES)
                totalRead += bytes.size
                if (totalRead > MAX_TOTAL_BYTES) throw BookParseException("EPUB 解压后内容过大，已停止读取。")
                val parsed = htmlToText(bytes)
                val fallbackChapterTitle = "第 ${chapters.size + 1} 章"
                val chapterTitle = navTitles[path]
                    ?: parsed.heading
                    ?: parsed.documentTitle
                    ?: fallbackChapterTitle
                val cleanedTitle = chapterTitle.cleanChapterTitle()
                chapters += Chapter(
                    index = chapters.size,
                    title = cleanedTitle,
                    text = stripLeadingHeading(parsed.text, listOfNotNull(cleanedTitle, parsed.heading)),
                    startOffset = 0,
                )
            }

            if (chapters.all { it.text.isBlank() }) {
                throw BookParseException("EPUB 中未找到可显示的正文，可能仅包含图片或不支持的排版。")
            }

            return ParsedBook(
                title = title,
                author = author,
                chapters = chapters,
                coverBytes = coverBytes,
            )
        }
    }

    private fun readNavigationTitles(
        zip: ZipFile,
        entries: Map<String, ZipEntry>,
        manifest: Map<String, ManifestItem>,
        opf: Document,
    ): Map<String, String> {
        val titles = mutableMapOf<String, String>()
        val navItem = manifest.values.firstOrNull { it.properties.split(' ').contains("nav") }
        if (navItem != null) {
            entries[navItem.path]?.let { entry ->
                runCatching {
                    val doc = Jsoup.parse(
                        ByteArrayInputStream(readEntry(zip, entry, MAX_TOC_BYTES)),
                        null,
                        "",
                    )
                    val nav = doc.getElementsByTag("nav").firstOrNull {
                        it.attr("epub:type").contains("toc", ignoreCase = true) ||
                            it.attr("role").contains("doc-toc", ignoreCase = true)
                    } ?: doc.getElementsByTag("nav").firstOrNull()
                    nav?.getElementsByTag("a")?.forEach { link ->
                        val href = link.attr("href")
                        if (href.isNotBlank()) {
                            val path = resolvePath(navItem.path.substringBeforeLast('/', ""), href)
                            val label = link.text().trim()
                            if (path != null && label.isNotEmpty()) titles.putIfAbsent(path, label)
                        }
                    }
                }
            }
        }

        val ncxItem = manifest.values.firstOrNull {
            it.mediaType == "application/x-dtbncx+xml" || it.path.endsWith(".ncx", ignoreCase = true)
        }
        if (titles.isEmpty() && ncxItem != null) {
            entries[ncxItem.path]?.let { entry ->
                runCatching {
                    val doc = parseXml(readEntry(zip, entry, MAX_TOC_BYTES), ncxItem.path)
                    doc.getElementsByTag("navPoint").forEach { point ->
                        val label = point.getElementsByTag("text").firstOrNull()?.text()?.trim()
                        val src = point.getElementsByTag("content").firstOrNull()?.attr("src")
                        if (!label.isNullOrBlank() && !src.isNullOrBlank()) {
                            val path = resolvePath(ncxItem.path.substringBeforeLast('/', ""), src)
                            if (path != null) titles.putIfAbsent(path, label)
                        }
                    }
                }
            }
        }
        return titles
    }

    private fun readCover(
        zip: ZipFile,
        entries: Map<String, ZipEntry>,
        manifest: Map<String, ManifestItem>,
        opf: Document,
    ): ByteArray? {
        val coverId = opf.getElementsByTag("meta").firstOrNull {
            it.attr("name").equals("cover", ignoreCase = true)
        }?.attr("content")
        val coverItem = manifest[coverId]
            ?: manifest.values.firstOrNull { it.properties.split(' ').contains("cover-image") }
            ?: manifest.values.firstOrNull {
                it.mediaType.startsWith("image/") &&
                    (it.href.contains("cover", ignoreCase = true) || it.id.contains("cover", ignoreCase = true))
            }
            ?: return null
        val entry = entries[coverItem.path] ?: return null
        if (entry.size > MAX_COVER_BYTES) return null
        return runCatching { readEntry(zip, entry, MAX_COVER_BYTES) }.getOrNull()
    }

    private fun htmlToText(bytes: ByteArray): HtmlResult {
        val document = try {
            Jsoup.parse(ByteArrayInputStream(bytes), null, "")
        } catch (error: IOException) {
            throw BookParseException("无法读取 EPUB 章节内容。", error)
        }
        document.select("script, style, noscript, svg, nav, audio, video").remove()
        val body = document.body() ?: document
        val blocks = mutableListOf<String>()

        fun walk(element: Element) {
            for (child in element.children()) {
                val tag = child.tagName().lowercase()
                if (tag == "nav" || tag == "script" || tag == "style") continue
                if (tag in BLOCK_TAGS) {
                    val text = if (tag == "pre") {
                        child.wholeText().trim()
                    } else {
                        child.text().replace('\u00A0', ' ').trim()
                    }
                    if (text.isNotEmpty()) blocks += text
                    // Do not descend into a block: its nested paragraphs are already represented
                    // by the block's flattened text and would otherwise be duplicated.
                } else {
                    walk(child)
                }
            }
        }
        walk(body)

        if (blocks.isEmpty()) {
            body.text().replace('\u00A0', ' ').trim().takeIf { it.isNotEmpty() }?.let { blocks += it }
        }
        val compact = blocks.map { it.replace(Regex("""[ \t]{2,}"""), " ") }
            .fold(mutableListOf<String>()) { acc, value ->
                if (acc.lastOrNull() != value) acc += value
                acc
            }
        val heading = document.selectFirst("h1, h2, h3")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val documentTitle = document.title().trim().takeIf { it.isNotEmpty() }
        return HtmlResult(
            // The reader adds configurable paragraph spacing, so one newline separates blocks.
            text = compact.joinToString("\n"),
            heading = heading,
            documentTitle = documentTitle,
        )
    }

    private fun parseXml(bytes: ByteArray, sourceName: String): Document = try {
        Jsoup.parse(ByteArrayInputStream(bytes), null, "", Parser.xmlParser())
    } catch (error: Exception) {
        throw BookParseException("EPUB 的 $sourceName 不是有效的 XML。", error)
    }

    private fun readEntry(zip: ZipFile, entry: ZipEntry, maxBytes: Int): ByteArray {
        if (entry.isDirectory) return ByteArray(0)
        if (entry.size > maxBytes) throw BookParseException("EPUB 中的文件过大：${entry.name}")
        val output = java.io.ByteArrayOutputStream(minOf(maxBytes, entry.size.coerceAtLeast(0L).toInt().coerceAtLeast(64)))
        zip.getInputStream(entry).use { input ->
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw BookParseException("EPUB 文件解压后超过安全大小限制。")
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
    }

    private fun validateArchive(zip: ZipFile) {
        var count = 0
        var declaredSize = 0L
        zip.entries().asSequence().forEach { entry ->
            count++
            if (count > MAX_ENTRIES) throw BookParseException("EPUB 文件条目过多，已停止读取。")
            if (entry.name.contains("..") || entry.name.startsWith("/") || entry.name.contains('\\')) {
                throw BookParseException("EPUB 包含不安全的文件路径。")
            }
            if (entry.size > 0) declaredSize += entry.size
            if (declaredSize > MAX_TOTAL_BYTES) throw BookParseException("EPUB 解压后内容过大。")
        }
    }

    private fun normalizePath(path: String): String? {
        if (path.isBlank()) return null
        val normalized = path.replace('\\', '/').removePrefix("/")
        val segments = ArrayDeque<String>()
        for (segment in normalized.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return null else segments.removeLast()
                else -> segments.addLast(segment)
            }
        }
        return segments.joinToString("/").takeIf { it.isNotBlank() }
    }

    private fun resolvePath(baseDir: String, href: String): String? {
        if (href.contains("://") || href.startsWith("//")) return null
        val withoutFragment = href.substringBefore('#')
        val decoded = try {
            URLDecoder.decode(withoutFragment, "UTF-8")
        } catch (_: IllegalArgumentException) {
            withoutFragment
        }
        val combined = if (decoded.startsWith('/')) decoded else if (baseDir.isBlank()) decoded else "$baseDir/$decoded"
        return normalizePath(combined)
    }

    private fun stripLeadingHeading(text: String, candidates: List<String>): String {
        if (text.isBlank()) return text
        val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
        if (firstLine.isEmpty()) return text
        val normalizedFirst = firstLine.normalizeHeading()
        val duplicate = candidates.any { candidate ->
            normalizedFirst == candidate.normalizeHeading() ||
                (normalizedFirst.isNotEmpty() && candidate.normalizeHeading().endsWith(normalizedFirst))
        }
        if (!duplicate) return text
        return text.substringAfter('\n', "").trimStart('\n', ' ', '\u3000')
    }

    private fun String.normalizeHeading(): String =
        replace(Regex("""[\s\u3000：:·・\-—]+"""), "").lowercase()

    private fun String.cleanChapterTitle(): String {
        val cleaned = replace(Regex("""\s+"""), " ").trim()
        return if (cleaned.length > 100) cleaned.take(100) else cleaned.ifBlank { "未命名章节" }
    }

    private data class ManifestItem(
        val id: String,
        val href: String,
        val path: String,
        val mediaType: String,
        val properties: String,
    )

    private data class HtmlResult(
        val text: String,
        val heading: String?,
        val documentTitle: String?,
    )

    private companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_HTML_BYTES = 12 * 1024 * 1024
        const val MAX_TOC_BYTES = 4 * 1024 * 1024
        const val MAX_COVER_BYTES = 16 * 1024 * 1024
        const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
        val HTML_TYPES = setOf("application/xhtml+xml", "text/html", "application/xml")
        val BLOCK_TAGS = setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "blockquote", "td")
    }
}
