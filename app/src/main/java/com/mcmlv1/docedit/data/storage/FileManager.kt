package com.mcmlv1.docedit.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import com.mcmlv1.docedit.domain.model.RecentDocument
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

/**
 * Handles all file system operations.
 * Works with both SAF (Storage Access Framework) URIs and local files.
 * Completely offline - no network operations.
 */
class FileManager(private val context: Context) {
    
    private val cacheDir: File = context.cacheDir
    private val filesDir: File = context.filesDir
    private val sharedDir: File = File(cacheDir, "shared").also { it.mkdirs() }
    private val documentsDir: File = File(filesDir, "documents").also { it.mkdirs() }
    
    /**
     * Open an input stream for reading a document
     */
    fun openInputStream(uri: Uri): DocumentResult<InputStream> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return DocumentResult.Error("Could not open file")
            DocumentResult.Success(inputStream)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to open file: ${e.message}", e)
        }
    }
    
    /**
     * Open an output stream for writing a document
     */
    fun openOutputStream(uri: Uri): DocumentResult<OutputStream> {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return DocumentResult.Error("Could not open file for writing")
            DocumentResult.Success(outputStream)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to open file for writing: ${e.message}", e)
        }
    }
    
    /**
     * Get file name from URI
     */
    fun getFileName(uri: Uri): String {
        val docFile = DocumentFile.fromSingleUri(context, uri)
        return docFile?.name ?: uri.lastPathSegment ?: "document"
    }
    
    /**
     * Get file size from URI
     */
    fun getFileSize(uri: Uri): Long {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            docFile?.length() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get last modified date from URI
     */
    fun getLastModified(uri: Uri): Date {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            Date(docFile?.lastModified() ?: System.currentTimeMillis())
        } catch (e: Exception) {
            Date()
        }
    }
    
    /**
     * Create a temporary file for sharing
     */
    fun createShareableFile(
        content: ByteArray,
        fileName: String
    ): DocumentResult<Uri> {
        return try {
            val file = File(sharedDir, fileName)
            file.writeBytes(content)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            DocumentResult.Success(uri)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to create shareable file: ${e.message}", e)
        }
    }
    
    /**
     * Create a shareable URI from existing file
     */
    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Rename a document (for SAF URIs)
     */
    fun renameDocument(uri: Uri, newName: String): DocumentResult<Uri> {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
                ?: return DocumentResult.Error("Could not access file")
            
            val renamed = docFile.renameTo(newName)
            if (renamed) {
                DocumentResult.Success(docFile.uri)
            } else {
                DocumentResult.Error("Failed to rename file")
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to rename: ${e.message}", e)
        }
    }
    
    

/**
 * Copy a document's bytes to a destination Uri.
 *
 * Useful for "Save a Copy..." for PDF and other binary formats.
 * This uses ContentResolver streams, so it works with SAF Uris as well as internal files.
 */
fun copyDocument(source: Uri, destination: Uri): DocumentResult<Unit> {
    return try {
        context.contentResolver.openInputStream(source)?.use { input ->
            context.contentResolver.openOutputStream(destination)?.use { output ->
                input.copyTo(output)
            } ?: return DocumentResult.Error("Could not open destination for write")
        } ?: return DocumentResult.Error("Could not open source for read")

        DocumentResult.Success(Unit)
    } catch (e: Exception) {
        DocumentResult.Error("Failed to copy: ${e.message}", e)
    }
}
/**
     * Delete a document
     */
    fun deleteDocument(uri: Uri): DocumentResult<Unit> {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
                ?: return DocumentResult.Error("Could not access file")
            
            if (docFile.delete()) {
                DocumentResult.Success(Unit)
            } else {
                DocumentResult.Error("Failed to delete file")
            }
        } catch (e: Exception) {
            DocumentResult.Error("Failed to delete: ${e.message}", e)
        }
    }
    
    /**
     * Check if a file exists
     */
    fun exists(uri: Uri): Boolean {
        return try {
            val docFile = DocumentFile.fromSingleUri(context, uri)
            docFile?.exists() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get document format from URI
     */
    fun getFormat(uri: Uri): DocumentFormat {
        val fileName = getFileName(uri)
        return DocumentFormat.fromFileName(fileName)
    }
    
    /**
     * Convert URI to RecentDocument
     */
    fun toRecentDocument(uri: Uri): RecentDocument {
        val fileName = getFileName(uri)
        val format = DocumentFormat.fromFileName(fileName)
        val lastModified = getLastModified(uri)
        val size = getFileSize(uri)
        
        return RecentDocument(
            uri = uri,
            name = fileName,
            format = format,
            lastAccessed = lastModified,
            size = size
        )
    }
    
    /**
     * Clean up old cache files
     */
    fun cleanupCache() {
        try {
            val maxAge = 24 * 60 * 60 * 1000 // 24 hours
            val cutoff = System.currentTimeMillis() - maxAge
            
            sharedDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Create a new file in the internal documents directory
     */
    fun createInternalDocument(fileName: String): File {
        return File(documentsDir, fileName)
    }
    
    /**
     * Get internal documents directory
     */
    fun getDocumentsDirectory(): File = documentsDir
}
