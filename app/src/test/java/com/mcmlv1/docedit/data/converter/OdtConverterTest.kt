package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * Unit tests for OdtConverter.
 * Tests ODT (OpenDocument Text) creation and parsing.
 */
class OdtConverterTest {

    @Test
    fun `write creates valid ODT structure`() {
        // Given
        val document = Document(
            name = "test.odt",
            content = "Hello World",
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = OdtConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertTrue(outputStream.size() > 0)
        
        // Verify it's a valid ZIP (ODT is a ZIP)
        val bytes = outputStream.toByteArray()
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])
    }

    @Test
    fun `write includes required ODT files`() {
        // Given
        val document = Document(
            name = "test.odt",
            content = "Test content",
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        OdtConverter.write(document, outputStream)
        
        // Then - Check ZIP contains required files
        val zipInput = ZipInputStream(ByteArrayInputStream(outputStream.toByteArray()))
        val entryNames = mutableListOf<String>()
        var entry = zipInput.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }
        zipInput.close()
        
        assertTrue("Missing mimetype", entryNames.contains("mimetype"))
        assertTrue("Missing content.xml", entryNames.contains("content.xml"))
        assertTrue("Missing META-INF/manifest.xml", entryNames.contains("META-INF/manifest.xml"))
    }

    @Test
    fun `write and read round trip preserves text content`() {
        // Given
        val originalContent = "First paragraph\nSecond paragraph\nThird paragraph"
        val document = Document(
            name = "roundtrip.odt",
            content = originalContent,
            format = DocumentFormat.ODT
        )
        
        // When - Write
        val outputStream = ByteArrayOutputStream()
        val writeResult = OdtConverter.write(document, outputStream)
        assertTrue(writeResult is DocumentResult.Success)
        
        // When - Read back
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = OdtConverter.read(inputStream, "roundtrip.odt")
        
        // Then
        assertTrue(readResult is DocumentResult.Success)
        val readDocument = (readResult as DocumentResult.Success).data
        assertEquals(originalContent, readDocument.content)
    }

    @Test
    fun `write handles empty content`() {
        // Given
        val document = Document(
            name = "empty.odt",
            content = "",
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = OdtConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertTrue(outputStream.size() > 0)
    }

    @Test
    fun `write handles special XML characters`() {
        // Given
        val content = "Test with <special> & \"characters\" 'here'"
        val document = Document(
            name = "special.odt",
            content = content,
            format = DocumentFormat.ODT
        )
        
        // When - Write
        val outputStream = ByteArrayOutputStream()
        val writeResult = OdtConverter.write(document, outputStream)
        assertTrue(writeResult is DocumentResult.Success)
        
        // When - Read back
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val readResult = OdtConverter.read(inputStream, "special.odt")
        
        // Then
        assertTrue(readResult is DocumentResult.Success)
        val readDocument = (readResult as DocumentResult.Success).data
        assertEquals(content, readDocument.content)
    }

    @Test
    fun `read returns correct document format`() {
        // Given
        val document = Document(
            name = "format.odt",
            content = "Test",
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        OdtConverter.write(document, outputStream)
        
        // When
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = OdtConverter.read(inputStream, "format.odt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val readDocument = (result as DocumentResult.Success).data
        assertEquals(DocumentFormat.ODT, readDocument.format)
        assertEquals("format.odt", readDocument.name)
    }

    @Test
    fun `read calculates word count correctly`() {
        // Given
        val content = "Word1 Word2 Word3 Word4"
        val document = Document(
            name = "count.odt",
            content = content,
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        OdtConverter.write(document, outputStream)
        
        // When
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val result = OdtConverter.read(inputStream, "count.odt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val readDocument = (result as DocumentResult.Success).data
        assertEquals(4, readDocument.wordCount)
    }

    @Test
    fun `mimetype is first entry in ODT archive`() {
        // Given - ODT spec requires mimetype to be first and uncompressed
        val document = Document(
            name = "test.odt",
            content = "Test",
            format = DocumentFormat.ODT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        OdtConverter.write(document, outputStream)
        
        // Then
        val zipInput = ZipInputStream(ByteArrayInputStream(outputStream.toByteArray()))
        val firstEntry = zipInput.nextEntry
        assertNotNull(firstEntry)
        assertEquals("mimetype", firstEntry?.name)
        zipInput.close()
    }
}
