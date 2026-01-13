package com.mcmlv1.docedit.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Document model and utility functions.
 */
class DocumentTest {

    @Test
    fun `calculateWordCount returns correct count for simple text`() {
        assertEquals(5, Document.calculateWordCount("one two three four five"))
    }

    @Test
    fun `calculateWordCount returns zero for empty string`() {
        assertEquals(0, Document.calculateWordCount(""))
    }

    @Test
    fun `calculateWordCount returns zero for whitespace only`() {
        assertEquals(0, Document.calculateWordCount("   \n\t  "))
    }

    @Test
    fun `calculateWordCount handles multiple spaces between words`() {
        assertEquals(3, Document.calculateWordCount("one    two     three"))
    }

    @Test
    fun `calculateWordCount handles newlines`() {
        assertEquals(4, Document.calculateWordCount("one\ntwo\nthree\nfour"))
    }

    @Test
    fun `calculateCharCount returns correct count`() {
        assertEquals(5, Document.calculateCharCount("hello"))
    }

    @Test
    fun `calculateCharCount includes spaces`() {
        assertEquals(11, Document.calculateCharCount("hello world"))
    }

    @Test
    fun `calculateCharCount returns zero for empty string`() {
        assertEquals(0, Document.calculateCharCount(""))
    }

    @Test
    fun `empty document has correct defaults`() {
        val doc = Document.empty()
        assertEquals("", doc.content)
        assertEquals("Untitled", doc.name)
        assertEquals(DocumentFormat.TXT, doc.format)
        assertEquals(0, doc.wordCount)
        assertEquals(0, doc.charCount)
        assertFalse(doc.isModified)
        assertFalse(doc.isReadOnly)
        assertNull(doc.uri)
    }

    @Test
    fun `DocumentFormat fromFileName detects txt`() {
        assertEquals(DocumentFormat.TXT, DocumentFormat.fromFileName("test.txt"))
        assertEquals(DocumentFormat.TXT, DocumentFormat.fromFileName("TEST.TXT"))
        assertEquals(DocumentFormat.TXT, DocumentFormat.fromFileName("my.file.txt"))
    }

    @Test
    fun `DocumentFormat fromFileName detects docx`() {
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromFileName("test.docx"))
        assertEquals(DocumentFormat.DOCX, DocumentFormat.fromFileName("TEST.DOCX"))
    }

    @Test
    fun `DocumentFormat fromFileName detects odt`() {
        assertEquals(DocumentFormat.ODT, DocumentFormat.fromFileName("test.odt"))
        assertEquals(DocumentFormat.ODT, DocumentFormat.fromFileName("TEST.ODT"))
    }

    @Test
    fun `DocumentFormat fromFileName detects pdf`() {
        assertEquals(DocumentFormat.PDF, DocumentFormat.fromFileName("test.pdf"))
        assertEquals(DocumentFormat.PDF, DocumentFormat.fromFileName("TEST.PDF"))
    }

    @Test
    fun `DocumentFormat fromFileName defaults to TXT for unknown`() {
        assertEquals(DocumentFormat.TXT, DocumentFormat.fromFileName("test.xyz"))
        assertEquals(DocumentFormat.TXT, DocumentFormat.fromFileName("noextension"))
    }

    @Test
    fun `DocumentFormat has correct extensions`() {
        assertEquals("txt", DocumentFormat.TXT.extension)
        assertEquals("docx", DocumentFormat.DOCX.extension)
        assertEquals("odt", DocumentFormat.ODT.extension)
        assertEquals("pdf", DocumentFormat.PDF.extension)
    }

    @Test
    fun `DocumentFormat has correct MIME types`() {
        assertEquals("text/plain", DocumentFormat.TXT.mimeType)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentFormat.DOCX.mimeType)
        assertEquals("application/vnd.oasis.opendocument.text", DocumentFormat.ODT.mimeType)
        assertEquals("application/pdf", DocumentFormat.PDF.mimeType)
    }

    @Test
    fun `DocumentFormat isEditable correct for each format`() {
        assertTrue(DocumentFormat.TXT.isEditable)
        assertTrue(DocumentFormat.DOCX.isEditable)
        assertTrue(DocumentFormat.ODT.isEditable)
        assertFalse(DocumentFormat.PDF.isEditable)
    }

    @Test
    fun `DocumentResult Success contains data`() {
        val result = DocumentResult.Success("test data")
        assertTrue(result.isSuccess())
        assertEquals("test data", result.data)
    }

    @Test
    fun `DocumentResult Error contains message`() {
        val result = DocumentResult.Error<String>("Something went wrong")
        assertFalse(result.isSuccess())
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `DocumentResult Error can contain exception`() {
        val exception = RuntimeException("Test exception")
        val result = DocumentResult.Error<String>("Error", exception)
        assertEquals(exception, result.exception)
    }
}
