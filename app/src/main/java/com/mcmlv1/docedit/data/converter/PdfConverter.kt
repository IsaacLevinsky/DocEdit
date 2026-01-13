package com.mcmlv1.docedit.data.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Converter for PDF files using Android's native PDF APIs.
 * 
 * Uses:
 * - android.graphics.pdf.PdfRenderer (API 21+) for reading
 * - android.graphics.pdf.PdfDocument (API 19+) for writing
 * 
 * No external libraries required.
 * 
 * Note: PDF is READ-ONLY for viewing. Text extraction is limited
 * as PDFs don't store text in a structured way. Users can view
 * PDF content and export other documents TO PDF.
 */
object PdfConverter {
    
    // US Letter size in points (72 points = 1 inch)
    private const val PAGE_WIDTH = 612  // 8.5 inches
    private const val PAGE_HEIGHT = 792 // 11 inches
    private const val MARGIN = 72       // 1 inch margins
    private const val FONT_SIZE = 12f
    private const val LINE_HEIGHT = 16f
    
    /**
     * Read PDF - extracts visible text using OCR-like approach
     * PDFs store text as positioned glyphs, not structured text
     */
    fun read(
        context: Context,
        inputStream: InputStream,
        fileName: String
    ): DocumentResult<Document> {
        return try {
            // Copy to temp file for PdfRenderer (requires seekable file)
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            val content = extractTextFromPdf(tempFile)
            
            // Clean up temp file
            tempFile.delete()
            
            DocumentResult.Success(
                Document(
                    name = fileName,
                    content = content,
                    format = DocumentFormat.PDF,
                    wordCount = Document.calculateWordCount(content),
                    charCount = Document.calculateCharCount(content),
                    isReadOnly = true
                )
            )
        } catch (e: Exception) {
            DocumentResult.Error("Failed to read PDF: ${e.message}", e)
        }
    }
    
    /**
     * Extract text from PDF using PdfRenderer
     * Note: This renders pages and cannot extract actual text content
     * For text extraction, we'd need a library like PdfBox-Android
     */
    private fun extractTextFromPdf(file: File): String {
        val sb = StringBuilder()
        
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            
            val pageCount = renderer.pageCount
            sb.append("[PDF Document]\n")
            sb.append("Pages: $pageCount\n\n")
            
            // Note: PdfRenderer can render pages to Bitmap but cannot extract text
            // For a full-featured app, we would need to use a library
            // This provides basic info about the PDF
            
            sb.append("This PDF has $pageCount page${if (pageCount != 1) "s" else ""}.\n\n")
            sb.append("PDF viewing shows page images.\n")
            sb.append("Text extraction requires OCR capabilities.\n\n")
            sb.append("You can:\n")
            sb.append("• View this PDF's pages\n")
            sb.append("• Share this PDF\n")
            sb.append("• Export other documents to PDF\n")
            
            renderer.close()
            fd.close()
            
        } catch (e: Exception) {
            sb.append("[Unable to read PDF: ${e.message}]")
        }
        
        return sb.toString()
    }
    
    /**
     * Get page count from PDF
     */
    fun getPageCount(context: Context, inputStream: InputStream): Int {
        return try {
            val tempFile = File(context.cacheDir, "temp_count_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            tempFile.delete()
            
            count
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Render a PDF page to Bitmap for viewing
     */
    fun renderPage(
        context: Context,
        file: File,
        pageIndex: Int,
        width: Int
    ): Bitmap? {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return null
            }
            
            val page = renderer.openPage(pageIndex)
            
            // Calculate height maintaining aspect ratio
            val scale = width.toFloat() / page.width
            val height = (page.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            renderer.close()
            fd.close()
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Export document content to PDF
     */
    fun export(document: Document, outputStream: OutputStream): DocumentResult<Unit> {
        return try {
            val pdfDocument = PdfDocument()
            
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = FONT_SIZE
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }
            
            val contentWidth = PAGE_WIDTH - (2 * MARGIN)
            val contentHeight = PAGE_HEIGHT - (2 * MARGIN)
            val linesPerPage = (contentHeight / LINE_HEIGHT).toInt()
            
            // Word-wrap text into lines
            val lines = wrapText(document.content, paint, contentWidth.toFloat())
            
            if (lines.isEmpty()) {
                // Create at least one empty page
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                pdfDocument.finishPage(page)
            } else {
                // Create pages with content
                var lineIndex = 0
                var pageNum = 1
                
                while (lineIndex < lines.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    // White background
                    canvas.drawColor(Color.WHITE)
                    
                    var yPos = MARGIN + LINE_HEIGHT
                    var linesOnPage = 0
                    
                    while (lineIndex < lines.size && linesOnPage < linesPerPage) {
                        canvas.drawText(lines[lineIndex], MARGIN.toFloat(), yPos, paint)
                        yPos += LINE_HEIGHT
                        lineIndex++
                        linesOnPage++
                    }
                    
                    pdfDocument.finishPage(page)
                    pageNum++
                }
            }
            
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            
            DocumentResult.Success(Unit)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to export PDF: ${e.message}", e)
        }
    }
    
    /**
     * Wrap text to fit within specified width
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        
        text.split("\n").forEach { paragraph ->
            if (paragraph.isEmpty()) {
                lines.add("")
                return@forEach
            }
            
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()
            
            words.forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = paint.measureText(testLine)
                
                if (width <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    // Handle very long words
                    if (paint.measureText(word) > maxWidth) {
                        // Break long word
                        var remaining = word
                        while (remaining.isNotEmpty()) {
                            var endIndex = remaining.length
                            while (endIndex > 1 && paint.measureText(remaining.substring(0, endIndex)) > maxWidth) {
                                endIndex--
                            }
                            lines.add(remaining.substring(0, endIndex))
                            remaining = remaining.substring(endIndex)
                        }
                        currentLine = StringBuilder()
                    } else {
                        currentLine = StringBuilder(word)
                    }
                }
            }
            
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        
        return lines
    }
    
    /**
     * Copy PDF file (for saving/renaming)
     */
    fun copyPdf(inputStream: InputStream, outputStream: OutputStream): DocumentResult<Unit> {
        return try {
            inputStream.copyTo(outputStream)
            DocumentResult.Success(Unit)
        } catch (e: Exception) {
            DocumentResult.Error("Failed to copy PDF: ${e.message}", e)
        }
    }
}
