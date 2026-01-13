package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converter for Microsoft Word (.docx) files.
 * 
 * DOCX files are ZIP archives containing XML files.
 * This implementation uses only Android's built-in XML parser.
 * No external libraries required.
 * 
 * Structure:
 * - [Content_Types].xml - Content type definitions
 * - _rels/.rels - Relationships
 * - word/document.xml - Main document content
 * - word/_rels/document.xml.rels - Document relationships
 */
object DocxConverter {
    
    private const val WORD_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    
    /**
     * Read DOCX and extract text content
     */
    fun read(inputStream: InputStream, fileName: String): DocumentResult<Document> {
        return try {
            val content = extractTextFromDocx(inputStream)
            DocumentResult.Success(
                Document(
                    name = fileName,
                    content = content,
                    format = DocumentFormat.DOCX,
                    wordCount = Document.calculateWordCount(content),
                    charCount = Document.calculateCharCount(content)
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to read Word document: ${e.message}", e)
        }
    }
    
    /**
     * Write content to DOCX format
     */
    fun write(document: Document, outputStream: OutputStream): DocumentResult<Unit> {
        return try {
            createDocxFile(document.content, outputStream)
            DocumentResult.Success(Unit)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to save Word document: ${e.message}", e)
        }
    }
    
    /**
     * Extract text from DOCX ZIP archive
     */
    private fun extractTextFromDocx(inputStream: InputStream): String {
        var documentXml: String? = null
        
        ZipInputStream(inputStream).use { zipInput ->
            var entry: ZipEntry? = zipInput.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val baos = ByteArrayOutputStream()
                    zipInput.copyTo(baos)
                    documentXml = baos.toString(Charsets.UTF_8.name())
                    break
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        
        return documentXml?.let { parseDocumentXml(it) } ?: ""
    }
    
    /**
     * Parse document.xml and extract text
     */
    private fun parseDocumentXml(xml: String): String {
        val sb = StringBuilder()
        
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            
            // Find all w:p (paragraph) elements
            val paragraphs = doc.getElementsByTagNameNS(WORD_NAMESPACE, "p")
            
            for (i in 0 until paragraphs.length) {
                val para = paragraphs.item(i) as Element
                val text = extractParagraphText(para)
                if (text.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text)
                }
            }
        } catch (e: Exception) {
            // Fallback: regex extraction
            val textPattern = Regex("<w:t[^>]*>([^<]*)</w:t>")
            val matches = textPattern.findAll(xml)
            var lastPara = ""
            matches.forEach { match ->
                val text = match.groupValues[1]
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                lastPara += text
            }
            sb.append(lastPara)
        }
        
        return sb.toString().trim()
    }
    
    /**
     * Extract text from a paragraph element
     */
    private fun extractParagraphText(paragraph: Element): String {
        val sb = StringBuilder()
        
        // Find all w:t (text) elements within this paragraph
        val textNodes = paragraph.getElementsByTagNameNS(WORD_NAMESPACE, "t")
        for (i in 0 until textNodes.length) {
            val textNode = textNodes.item(i)
            sb.append(textNode.textContent ?: "")
        }
        
        return sb.toString()
    }
    
    /**
     * Create a valid DOCX file with the given content
     */
    private fun createDocxFile(content: String, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zipOut ->
            // [Content_Types].xml
            zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
            zipOut.write(createContentTypesXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // _rels/.rels
            zipOut.putNextEntry(ZipEntry("_rels/.rels"))
            zipOut.write(createRelsXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // word/_rels/document.xml.rels
            zipOut.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zipOut.write(createDocumentRelsXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // word/document.xml
            zipOut.putNextEntry(ZipEntry("word/document.xml"))
            zipOut.write(createDocumentXml(content).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // word/styles.xml
            zipOut.putNextEntry(ZipEntry("word/styles.xml"))
            zipOut.write(createStylesXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
    }
    
    private fun createContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
    <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
    
    private fun createRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    
    private fun createDocumentRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    
    private fun createDocumentXml(content: String): String {
        val paragraphs = content.split("\n").joinToString("") { line ->
            "<w:p><w:r><w:t>${escapeXml(line)}</w:t></w:r></w:p>"
        }
        
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        $paragraphs
        <w:sectPr>
            <w:pgSz w:w="12240" w:h="15840"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>"""
    }
    
    private fun createStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:docDefaults>
        <w:rPrDefault>
            <w:rPr>
                <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                <w:sz w:val="24"/>
            </w:rPr>
        </w:rPrDefault>
    </w:docDefaults>
</w:styles>"""
    
    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
