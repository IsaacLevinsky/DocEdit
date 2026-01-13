package com.mcmlv1.docedit.data.converter

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import java.io.InputStream
import java.io.OutputStream

/**
 * Converter for plain text (.txt) files.
 * Uses standard Java I/O - no external dependencies.
 */
object TxtConverter {
    
    fun read(inputStream: InputStream, fileName: String): DocumentResult<Document> {
        return try {
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            DocumentResult.Success(
                Document(
                    name = fileName,
                    content = content,
                    format = DocumentFormat.TXT,
                    wordCount = Document.calculateWordCount(content),
                    charCount = Document.calculateCharCount(content)
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to read text file: ${e.message}", e)
        }
    }
    
    fun write(document: Document, outputStream: OutputStream): DocumentResult<Unit> {
        return try {
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(document.content)
            }
            DocumentResult.Success(Unit)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to save text file: ${e.message}", e)
        }
    }
}
