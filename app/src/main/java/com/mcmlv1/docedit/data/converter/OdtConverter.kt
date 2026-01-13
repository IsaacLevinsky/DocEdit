package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converter for OpenDocument Text (.odt) files.
 * 
 * ODT files are ZIP archives containing XML files following the ODF standard.
 * This implementation uses only Android's built-in XML parser.
 * No external libraries required.
 * 
 * Structure:
 * - mimetype - MIME type identifier (must be first, uncompressed)
 * - META-INF/manifest.xml - Manifest
 * - content.xml - Document content
 * - styles.xml - Document styles
 * - meta.xml - Metadata
 */
object OdtConverter {
    
    private const val TEXT_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
    private const val MIMETYPE = "application/vnd.oasis.opendocument.text"
    
    /**
     * Read ODT and extract text content
     */
    fun read(inputStream: InputStream, fileName: String): DocumentResult<Document> {
        return try {
            val content = extractTextFromOdt(inputStream)
            DocumentResult.Success(
                Document(
                    name = fileName,
                    content = content,
                    format = DocumentFormat.ODT,
                    wordCount = Document.calculateWordCount(content),
                    charCount = Document.calculateCharCount(content)
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to read OpenDocument file: ${e.message}", e)
        }
    }
    
    /**
     * Write content to ODT format
     */
    fun write(document: Document, outputStream: OutputStream): DocumentResult<Unit> {
        return try {
            createOdtFile(document.content, outputStream)
            DocumentResult.Success(Unit)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to save OpenDocument file: ${e.message}", e)
        }
    }
    
    /**
     * Extract text from ODT ZIP archive
     */
    private fun extractTextFromOdt(inputStream: InputStream): String {
        var contentXml: String? = null
        
        ZipInputStream(inputStream).use { zipInput ->
            var entry: ZipEntry? = zipInput.nextEntry
            while (entry != null) {
                if (entry.name == "content.xml") {
                    val baos = ByteArrayOutputStream()
                    zipInput.copyTo(baos)
                    contentXml = baos.toString(Charsets.UTF_8.name())
                    break
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        
        return contentXml?.let { parseContentXml(it) } ?: ""
    }
    
    /**
     * Parse content.xml and extract text
     */
    private fun parseContentXml(xml: String): String {
        val sb = StringBuilder()
        
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            
            // Find all text:p (paragraph) and text:h (heading) elements
            val paragraphs = doc.getElementsByTagNameNS(TEXT_NAMESPACE, "p")
            val headings = doc.getElementsByTagNameNS(TEXT_NAMESPACE, "h")
            
            // Process headings first
            for (i in 0 until headings.length) {
                val heading = headings.item(i) as Element
                val text = heading.textContent?.trim() ?: ""
                if (text.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text)
                }
            }
            
            // Process paragraphs
            for (i in 0 until paragraphs.length) {
                val para = paragraphs.item(i) as Element
                val text = para.textContent?.trim() ?: ""
                if (text.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text)
                }
            }
        } catch (e: Exception) {
            // Fallback: regex extraction
            val textPattern = Regex("<text:[ph][^>]*>([^<]*)</text:[ph]>")
            textPattern.findAll(xml).forEach { match ->
                val text = match.groupValues[1]
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                if (text.isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text.trim())
                }
            }
        }
        
        return sb.toString().trim()
    }
    
    /**
     * Create a valid ODT file with the given content
     */
    private fun createOdtFile(content: String, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zipOut ->
            // mimetype - MUST be first entry, STORED (not compressed)
            val mimetypeBytes = MIMETYPE.toByteArray(Charsets.UTF_8)
            val mimetypeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimetypeBytes.size.toLong()
                compressedSize = mimetypeBytes.size.toLong()
                crc = CRC32().apply { update(mimetypeBytes) }.value
            }
            zipOut.putNextEntry(mimetypeEntry)
            zipOut.write(mimetypeBytes)
            zipOut.closeEntry()
            
            // META-INF/manifest.xml
            zipOut.putNextEntry(ZipEntry("META-INF/manifest.xml"))
            zipOut.write(createManifestXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // content.xml
            zipOut.putNextEntry(ZipEntry("content.xml"))
            zipOut.write(createContentXml(content).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // styles.xml
            zipOut.putNextEntry(ZipEntry("styles.xml"))
            zipOut.write(createStylesXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // meta.xml
            zipOut.putNextEntry(ZipEntry("meta.xml"))
            zipOut.write(createMetaXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
    }
    
    private fun createManifestXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">
    <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/>
    <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
    <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
    <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
</manifest:manifest>"""
    
    private fun createContentXml(content: String): String {
        val paragraphs = content.split("\n").joinToString("") { line ->
            "<text:p text:style-name=\"Standard\">${escapeXml(line)}</text:p>"
        }
        
        return """<?xml version="1.0" encoding="UTF-8"?>
<office:document-content
    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
    office:version="1.3">
    <office:automatic-styles>
        <style:style style:name="Standard" style:family="paragraph"/>
    </office:automatic-styles>
    <office:body>
        <office:text>
            $paragraphs
        </office:text>
    </office:body>
</office:document-content>"""
    }
    
    private fun createStylesXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles
    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
    xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
    office:version="1.3">
    <office:styles>
        <style:style style:name="Standard" style:family="paragraph">
            <style:paragraph-properties fo:margin-top="0cm" fo:margin-bottom="0.212cm"/>
            <style:text-properties fo:font-size="12pt" fo:font-family="Liberation Sans"/>
        </style:style>
    </office:styles>
</office:document-styles>"""
    
    private fun createMetaXml(): String = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta
    xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    office:version="1.3">
    <office:meta>
        <meta:generator>DocEdit by MCMLV1, LLC</meta:generator>
        <meta:creation-date>${java.time.LocalDateTime.now()}</meta:creation-date>
    </office:meta>
</office:document-meta>"""
    
    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
