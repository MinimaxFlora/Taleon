package com.taleon.reader

import com.taleon.reader.data.files.EpubParser
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpubParserTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun parsesMetadataNavigationAndSpineOrder() {
        val epub = temporaryFolder.newFile("sample.epub")
        ZipOutputStream(epub.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>海边的信</dc:title>
                    <dc:creator>沈舟</dc:creator>
                  </metadata>
                  <manifest>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="c1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                    <itemref idref="c2"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body><nav epub:type="toc"><ol>
                  <li><a href="text/ch1.xhtml">第一章 潮声</a></li>
                  <li><a href="text/ch2.xhtml">第二章 来信</a></li>
                </ol></nav></body></html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/text/ch1.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml"><head><title>潮声</title></head>
                <body><h1>潮声</h1><p>海风从窗外吹进来。</p><p>她把信压在书页下面。</p></body></html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/text/ch2.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml"><body>
                <h1>来信</h1><p>信封上没有寄件人的名字。</p>
                </body></html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
        }

        val book = EpubParser().parse(epub, "fallback.epub")

        assertEquals("海边的信", book.title)
        assertEquals("沈舟", book.author)
        assertEquals(2, book.chapters.size)
        assertEquals("第一章 潮声", book.chapters[0].title)
        assertEquals("第二章 来信", book.chapters[1].title)
        assertTrue(book.chapters[0].text.contains("海风"))
        assertTrue(book.chapters[0].text.lines().size >= 2)
        assertTrue(!book.chapters[0].text.startsWith("潮声"))
        assertEquals(0, book.chapters[0].startOffset)
    }

    @Test(expected = Exception::class)
    fun rejectsMissingContainer() {
        val epub = File(temporaryFolder.root, "broken.epub")
        ZipOutputStream(epub.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("hello.txt"))
            zip.write("not an epub".toByteArray())
            zip.closeEntry()
        }
        EpubParser().parse(epub, "broken")
    }
}
