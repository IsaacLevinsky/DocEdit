package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for DocxConverter.
 * Tests DOCX creation and parsing using pure XML/ZIP approach.
 */
class DocxConverterTest {

    @Test
    fun `write creates valid DOCX structure`() {
        // Given
        val document = Document(
            name = "test.docx",
            content = "Hello World",
            format = DocumentFormat.DOCX
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = DocxConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertTrue(outputStream.size() > 0)
        
        // Verify it's a valid ZIP (DOCX is a ZIP)
        val bytes = outputStream.toByteArray()
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])
    }

    @Test
    fun `write and read round trip preserves text content`() {
        // Given
        val originalContent = "Test paragraph one\nTest paragraph two\nTest paragraph three"
        val document = Document(
            name = "roundtrip.docx",
            content = originalContent,
            format = DocumentFormat.DOCX
        )
        
        // When - Write
        val outputStream = ByteArrayOutputStream()
        val writeResult = DocxConverter.write(document, outputStream)
        assertTrue(writeResult is DocumentResult.Success)
        
        // When - Read back
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = DocxConverter.read(inputStream, "roundtrip.docx")
        
        // Then
        assertTrue(readResult is DocumentResult.Success)
        val readDocument = (readResult as DocumentResult.Success).data
        assertEquals(originalContent, readDocument.content)
    }

    @Test
    fun `write handles empty content`() {
        // Given
        val document = Document(
            name = "empty.docx",
            content = "",
            format = DocumentFormat.DOCX
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = DocxConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertTrue(outputStream.size() > 0)
    }

    @Test
    fun `write handles special XML characters`() {
        // Given
        val content = "Test with <special> & \"characters\" 'here'"
        val document = Document(
            name = "special.docx",
            content = content,
            format = DocumentFormat.DOCX
        )
        
        // When - Write
        val outputStream = ByteArrayOutputStream()
        val writeResult = DocxConverter.write(document, outputStream)
        assertTrue(writeResult is DocumentResult.Success)
        
        // When - Read back
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = DocxConverter.read(inputStream, "special.docx")
        
        // Then
        assertTrue(readResult is DocumentResult.Success)
        val readDocument = (readResult as DocumentResult.Success).data
        assertEquals(content, readDocument.content)
    }

    @Test
    fun `read returns correct document format`() {
        // Given
        val document = Document(
            name = "format.docx",
            content = "Test",
            format = DocumentFormat.DOCX
        )
        val outputStream = ByteArrayOutputStream()
        DocxConverter.write(document, outputStream)
        
        // When
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = DocxConverter.read(inputStream, "format.docx")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val readDocument = (result as DocumentResult.Success).data
        assertEquals(DocumentFormat.DOCX, readDocument.format)
        assertEquals("format.docx", readDocument.name)
    }

    @Test
    fun `read calculates word count correctly`() {
        // Given
        val content = "One two three four five six"
        val document = Document(
            name = "count.docx",
            content = content,
            format = DocumentFormat.DOCX
        )
        val outputStream = ByteArrayOutputStream()
        DocxConverter.write(document, outputStream)
        
        // When
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = DocxConverter.read(inputStream, "count.docx")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val readDocument = (result as DocumentResult.Success).data
        assertEquals(6, readDocument.wordCount)
    }

    @Test
    fun `write handles multiline content with proper paragraphs`() {
        // Given
        val content = "Paragraph 1\nParagraph 2\nParagraph 3"
        val document = Document(
            name = "multiline.docx",
            content = content,
            format = DocumentFormat.DOCX
        )
        
        // When
        val outputStream = ByteArrayOutputStream()
        val writeResult = DocxConverter.write(document, outputStream)
        
        // Then
        assertTrue(writeResult is DocumentResult.Success)
        
        // Verify round trip
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = DocxConverter.read(inputStream, "multiline.docx")
        assertTrue(readResult is DocumentResult.Success)
        
        val readDocument = (readResult as DocumentResult.Success).data
        assertTrue(readDocument.content.contains("Paragraph 1"))
        assertTrue(readDocument.content.contains("Paragraph 2"))
        assertTrue(readDocument.content.contains("Paragraph 3"))
    }
}
