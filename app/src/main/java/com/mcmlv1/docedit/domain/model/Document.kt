package com.mcmlv1.docedit.domain.model

import android.net.Uri
import java.util.Date

/**
 * Represents a document in the application.
 * Supports TXT, DOCX, ODT, and PDF (view-only) formats.
 */
data class Document(
    val uri: Uri? = null,
    val name: String = "Untitled",
    val content: String = "",
    val format: DocumentFormat = DocumentFormat.TXT,
    val lastModified: Date = Date(),
    val isModified: Boolean = false,
    val isReadOnly: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0
) {
    companion object {
        fun calculateWordCount(text: String): Int {
            if (text.isBlank()) return 0
            return text.trim().split(Regex("\\s+")).size
        }
        
        fun calculateCharCount(text: String): Int {
            return text.length
        }
        
        fun empty(): Document = Document()
    }
}

/**
 * Supported document formats
 */
enum class DocumentFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val isEditable: Boolean = true
) {
    TXT("txt", "text/plain", "Text File", true),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word Document", true),
    ODT("odt", "application/vnd.oasis.opendocument.text", "OpenDocument", true),
    PDF("pdf", "application/pdf", "PDF Document", false);
    
    companion object {
        fun fromExtension(extension: String): DocumentFormat {
            return entries.find { 
                it.extension.equals(extension, ignoreCase = true) 
            } ?: TXT
        }
        
        fun fromMimeType(mimeType: String): DocumentFormat {
            return entries.find { 
                it.mimeType.equals(mimeType, ignoreCase = true) 
            } ?: TXT
        }
        
        fun fromFileName(fileName: String): DocumentFormat {
            val ext = fileName.substringAfterLast('.', "")
            return fromExtension(ext)
        }
    }
}

/**
 * Text formatting options
 */
data class TextFormatting(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: FontSize = FontSize.NORMAL
)

/**
 * Available font sizes
 */
enum class FontSize(val sp: Int, val displayName: String) {
    SMALL(12, "Small"),
    NORMAL(16, "Normal"),
    LARGE(20, "Large"),
    EXTRA_LARGE(24, "Extra Large");
    
    companion object {
        fun fromSp(sp: Int): FontSize {
            return entries.minByOrNull { kotlin.math.abs(it.sp - sp) } ?: NORMAL
        }
    }
}

/**
 * Result wrapper for document operations
 */
sealed class DocumentResult<out T> {
    data class Success<T>(val data: T) : DocumentResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : DocumentResult<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

/**
 * Recent document entry for browser
 */
data class RecentDocument(
    val uri: Uri,
    val name: String,
    val format: DocumentFormat,
    val lastAccessed: Date,
    val size: Long = 0
)
