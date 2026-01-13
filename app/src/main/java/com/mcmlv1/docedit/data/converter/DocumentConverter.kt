package com.mcmlv1.docedit.data.converter

import android.content.Context
import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import java.io.InputStream
import java.io.OutputStream

/**
 * Central document converter that dispatches to format-specific converters.
 * 
 * All conversions use Android's built-in APIs only:
 * - XML Parser (javax.xml) for DOCX/ODT
 * - ZIP streams (java.util.zip) for archive formats
 * - PdfDocument/PdfRenderer (android.graphics.pdf) for PDF
 * 
 * No external libraries required. MIT License compatible.
 */
class DocumentConverter(private val context: Context) {
    
    /**
     * Read a document from an input stream
     */
    fun read(
        inputStream: InputStream,
        fileName: String,
        format: DocumentFormat
    ): DocumentResult<Document> {
        return when (format) {
            DocumentFormat.TXT -> TxtConverter.read(inputStream, fileName)
            DocumentFormat.DOCX -> DocxConverter.read(inputStream, fileName)
            DocumentFormat.ODT -> OdtConverter.read(inputStream, fileName)
            DocumentFormat.PDF -> PdfConverter.read(context, inputStream, fileName)
        }
    }
    
    /**
     * Write a document to an output stream in its current format
     */
    fun write(
        document: Document,
        outputStream: OutputStream
    ): DocumentResult<Unit> {
        return when (document.format) {
            DocumentFormat.TXT -> TxtConverter.write(document, outputStream)
            DocumentFormat.DOCX -> DocxConverter.write(document, outputStream)
            DocumentFormat.ODT -> OdtConverter.write(document, outputStream)
            DocumentFormat.PDF -> DocumentResult.Error("Cannot write to PDF format directly. Use export instead.")
        }
    }
    
    /**
     * Convert document to a different format
     */
    fun convertTo(
        document: Document,
        outputStream: OutputStream,
        targetFormat: DocumentFormat
    ): DocumentResult<Unit> {
        // PDF can only be exported to, not converted from
        if (document.format == DocumentFormat.PDF && targetFormat != DocumentFormat.PDF) {
            return DocumentResult.Error("Cannot convert from PDF. PDF content is not editable text.")
        }
        
        return when (targetFormat) {
            DocumentFormat.TXT -> TxtConverter.write(document, outputStream)
            DocumentFormat.DOCX -> DocxConverter.write(document, outputStream)
            DocumentFormat.ODT -> OdtConverter.write(document, outputStream)
            DocumentFormat.PDF -> PdfConverter.export(document, outputStream)
        }
    }
    
    /**
     * Export any document to PDF
     */
    fun exportToPdf(
        document: Document,
        outputStream: OutputStream
    ): DocumentResult<Unit> {
        return PdfConverter.export(document, outputStream)
    }
    
    companion object {
        /**
         * Get file extension for format
         */
        fun getExtension(format: DocumentFormat): String = format.extension
        
        /**
         * Get MIME type for format
         */
        fun getMimeType(format: DocumentFormat): String = format.mimeType
        
        /**
         * Get all supported formats for reading
         */
        fun getSupportedReadFormats(): List<DocumentFormat> = DocumentFormat.entries.toList()
        
        /**
         * Get all supported formats for writing
         */
        fun getSupportedWriteFormats(): List<DocumentFormat> = 
            DocumentFormat.entries.filter { it.isEditable }
        
        /**
         * Get all formats available for export/conversion
         */
        fun getExportFormats(): List<DocumentFormat> = DocumentFormat.entries.toList()
    }
}
