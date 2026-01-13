package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for TxtConverter.
 * Tests plain text reading and writing functionality.
 */
class TxtConverterTest {

    @Test
    fun `read returns Success with correct content`() {
        // Given
        val content = "Hello, World!\nThis is a test."
        val inputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        
        // When
        val result = TxtConverter.read(inputStream, "test.txt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val document = (result as DocumentResult.Success).data
        assertEquals(content, document.content)
        assertEquals("test.txt", document.name)
        assertEquals(DocumentFormat.TXT, document.format)
    }

    @Test
    fun `read calculates word count correctly`() {
        // Given
        val content = "One two three four five"
        val inputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        
        // When
        val result = TxtConverter.read(inputStream, "test.txt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val document = (result as DocumentResult.Success).data
        assertEquals(5, document.wordCount)
    }

    @Test
    fun `read calculates character count correctly`() {
        // Given
        val content = "Hello"
        val inputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        
        // When
        val result = TxtConverter.read(inputStream, "test.txt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val document = (result as DocumentResult.Success).data
        assertEquals(5, document.charCount)
    }

    @Test
    fun `read handles empty file`() {
        // Given
        val inputStream = ByteArrayInputStream(ByteArray(0))
        
        // When
        val result = TxtConverter.read(inputStream, "empty.txt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val document = (result as DocumentResult.Success).data
        assertEquals("", document.content)
        assertEquals(0, document.wordCount)
    }

    @Test
    fun `read handles UTF-8 special characters`() {
        // Given
        val content = "Héllo Wörld! 你好 🎉"
        val inputStream = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        
        // When
        val result = TxtConverter.read(inputStream, "unicode.txt")
        
        // Then
        assertTrue(result is DocumentResult.Success)
        val document = (result as DocumentResult.Success).data
        assertEquals(content, document.content)
    }

    @Test
    fun `write outputs correct content`() {
        // Given
        val content = "Test content\nWith multiple lines"
        val document = com.mcmlv1.docedit.domain.model.Document(
            name = "test.txt",
            content = content,
            format = DocumentFormat.TXT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = TxtConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertEquals(content, outputStream.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `write handles empty content`() {
        // Given
        val document = com.mcmlv1.docedit.domain.model.Document(
            name = "empty.txt",
            content = "",
            format = DocumentFormat.TXT
        )
        val outputStream = ByteArrayOutputStream()
        
        // When
        val result = TxtConverter.write(document, outputStream)
        
        // Then
        assertTrue(result is DocumentResult.Success)
        assertEquals("", outputStream.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `read and write round trip preserves content`() {
        // Given
        val originalContent = "Round trip test\nLine 2\nLine 3 with special chars: é ñ 中文"
        val inputStream = ByteArrayInputStream(originalContent.toByteArray(Charsets.UTF_8))
        
        // When - Read
        val readResult = TxtConverter.read(inputStream, "roundtrip.txt")
        assertTrue(readResult is DocumentResult.Success)
        val document = (readResult as DocumentResult.Success).data
        
        // When - Write
        val outputStream = ByteArrayOutputStream()
        val writeResult = TxtConverter.write(document, outputStream)
        
        // Then
        assertTrue(writeResult is DocumentResult.Success)
        assertEquals(originalContent, outputStream.toString(Charsets.UTF_8.name()))
    }
}
