package com.mcmlv1.docedit.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mcmlv1.docedit.data.converter.DocumentConverter
import com.mcmlv1.docedit.data.storage.FileManager
import com.mcmlv1.docedit.data.storage.RecentDocumentsStore
import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import com.mcmlv1.docedit.domain.model.RecentDocument
import java.io.ByteArrayOutputStream
import java.util.Date

/**
 * Repository for all document operations.
 * Single source of truth for document management.
 * 
 * Supports:
 * - TXT: Read, Write, Convert to/from
 * - DOCX: Read, Write, Convert to/from
 * - ODT: Read, Write, Convert to/from
 * - PDF: Read (view only), Export to
 */
class DocumentRepository(private val context: Context) {
    
    private val converter = DocumentConverter(context)
    private val fileManager = FileManager(context)
    private val recentStore = RecentDocumentsStore(context)
    
    /**
     * Open and read a document from URI
     */
    fun openDocument(uri: Uri): DocumentResult<Document> {
        val inputResult = fileManager.openInputStream(uri)
        if (inputResult is DocumentResult.Error) {
            return DocumentResult.Error(inputResult.message, inputResult.exception)
        }
        
        val inputStream = (inputResult as DocumentResult.Success).data
        val fileName = fileManager.getFileName(uri)
        val format = DocumentFormat.fromFileName(fileName)
        
        return try {
            inputStream.use { stream ->
                val result = converter.read(stream, fileName, format)
                
                if (result is DocumentResult.Success) {
                    // Add to recent documents
                    recentStore.addRecent(uri, fileName, format)
                    
                    // Return document with URI attached
                    DocumentResult.Success(result.data.copy(uri = uri))
                } else {
                    result
                }
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to read document: ${e.message}", e)
        }
    }
    
    /**
     * Save document to its original URI
     */
    fun saveDocument(document: Document): DocumentResult<Document> {
        val uri = document.uri ?: return DocumentResult.Error("No file location specified")
        
        // PDFs are read-only
        if (document.format == DocumentFormat.PDF) {
            return DocumentResult.Error("PDF files cannot be edited. Use 'Save As' to create a copy.")
        }
        
        val outputResult = fileManager.openOutputStream(uri)
        if (outputResult is DocumentResult.Error) {
            return DocumentResult.Error(outputResult.message, outputResult.exception)
        }
        
        val outputStream = (outputResult as DocumentResult.Success).data
        
        return try {
            outputStream.use { stream ->
                val writeResult = converter.write(document, stream)
                
                if (writeResult is DocumentResult.Success) {
                    DocumentResult.Success(
                        document.copy(
                            isModified = false,
                            lastModified = Date()
                        )
                    )
                } else {
                    DocumentResult.Error((writeResult as DocumentResult.Error).message)
                }
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to save document: ${e.message}", e)
        }
    }
    
    /**
     * Save document to a new URI with specified format
     */
    fun saveDocumentAs(
        document: Document,
        uri: Uri,
        format: DocumentFormat
    ): DocumentResult<Document> {
        val outputResult = fileManager.openOutputStream(uri)
        if (outputResult is DocumentResult.Error) {
            return DocumentResult.Error(outputResult.message, outputResult.exception)
        }
        
        val outputStream = (outputResult as DocumentResult.Success).data
        val newFileName = fileManager.getFileName(uri)
        
        return try {
            outputStream.use { stream ->
                val writeResult = converter.convertTo(document, stream, format)
                
                if (writeResult is DocumentResult.Success) {
                    // Add to recent documents
                    recentStore.addRecent(uri, newFileName, format)
                    
                    DocumentResult.Success(
                        document.copy(
                            uri = uri,
                            name = newFileName,
                            format = format,
                            isModified = false,
                            lastModified = Date(),
                            isReadOnly = format == DocumentFormat.PDF
                        )
                    )
                } else {
                    DocumentResult.Error((writeResult as DocumentResult.Error).message)
                }
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to save document: ${e.message}", e)
        }
    }
    
    /**
     * Export document to PDF
     */
    fun exportToPdf(document: Document, uri: Uri): DocumentResult<Unit> {
        val outputResult = fileManager.openOutputStream(uri)
        if (outputResult is DocumentResult.Error) {
            return DocumentResult.Error(outputResult.message, outputResult.exception)
        }
        
        val outputStream = (outputResult as DocumentResult.Success).data
        
        return try {
            outputStream.use { stream ->
                converter.exportToPdf(document, stream)
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to export PDF: ${e.message}", e)
        }
    }
    
    /**
     * Create a shareable file and return share intent
     */
    fun createShareIntent(document: Document): DocumentResult<Intent> {
        return try {
            // Create file content
            val baos = ByteArrayOutputStream()
            
            val writeResult = if (document.format == DocumentFormat.PDF) {
                // For PDF, we need to copy the original file
                document.uri?.let { uri ->
                    fileManager.openInputStream(uri).let { inputResult ->
                        if (inputResult is DocumentResult.Success) {
                            inputResult.data.use { input ->
                                input.copyTo(baos)
                            }
                            DocumentResult.Success(Unit)
                        } else {
                            DocumentResult.Error("Cannot read PDF file")
                        }
                    }
                } ?: DocumentResult.Error("No PDF file to share")
            } else {
                converter.write(document, baos)
            }
            
            if (writeResult is DocumentResult.Error) {
                return DocumentResult.Error(writeResult.message)
            }
            
            // Create shareable file
            val fileName = document.name.let { name ->
                if (name.contains('.')) name
                else "${name}.${document.format.extension}"
            }
            
            val shareResult = fileManager.createShareableFile(baos.toByteArray(), fileName)
            if (shareResult is DocumentResult.Error) {
                return DocumentResult.Error(shareResult.message)
            }
            
            val fileUri = (shareResult as DocumentResult.Success).data
            
            // Create share intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = document.format.mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            DocumentResult.Success(Intent.createChooser(intent, "Share document"))
        } catch (e: Exception) {
            DocumentResult.Error("Failed to create share intent: ${e.message}", e)
        }
    }
    
    /**
     * Rename a document
     */
    fun renameDocument(uri: Uri, newName: String): DocumentResult<Uri> {
        val result = fileManager.renameDocument(uri, newName)
        
        if (result is DocumentResult.Success) {
            // Update recent documents
            val format = DocumentFormat.fromFileName(newName)
            recentStore.addRecent(result.data, newName, format)
        }
        
        return result
    }
    
    

/**
 * Copy a document to a destination Uri (SAF-compatible).
 * Used for "Save a Copy..." for PDFs and other binary formats.
 */
fun copyDocument(source: Uri, destination: Uri): DocumentResult<Unit> {
    return fileManager.copyDocument(source, destination)
}
/**
     * Delete a document
     */
    fun deleteDocument(uri: Uri): DocumentResult<Unit> {
        val result = fileManager.deleteDocument(uri)
        
        if (result is DocumentResult.Success) {
            recentStore.removeRecent(uri)
        }
        
        return result
    }
    
    /**
     * Get list of recent documents
     */
    fun getRecentDocuments(): List<RecentDocument> {
        return recentStore.getRecentList()
    }
    
    /**
     * Clear recent documents list
     */
    fun clearRecentDocuments() {
        recentStore.clearRecent()
    }
    
    /**
     * Create a new empty document
     */
    fun createNewDocument(format: DocumentFormat = DocumentFormat.TXT): Document {
        return Document(
            name = "Untitled.${format.extension}",
            content = "",
            format = format,
            isModified = false,
            isReadOnly = !format.isEditable
        )
    }
    
    /**
     * Update document content (for editing)
     */
    fun updateContent(document: Document, newContent: String): Document {
        return document.copy(
            content = newContent,
            isModified = true,
            wordCount = Document.calculateWordCount(newContent),
            charCount = Document.calculateCharCount(newContent)
        )
    }
    
    /**
     * Cleanup old cached files
     */
    fun cleanup() {
        fileManager.cleanupCache()
    }
    
    /**
     * Get all formats that can be used for "Save As"
     */
    fun getSaveAsFormats(): List<DocumentFormat> {
        return DocumentFormat.entries.filter { it.isEditable }
    }
    
    /**
     * Get all formats that can be used for export (includes PDF)
     */
    fun getExportFormats(): List<DocumentFormat> {
        return DocumentFormat.entries.toList()
    }
}
