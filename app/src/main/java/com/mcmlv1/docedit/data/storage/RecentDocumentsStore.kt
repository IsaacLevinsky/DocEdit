package com.mcmlv1.docedit.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.RecentDocument
import java.util.Date

/**
 * Persists list of recently accessed documents.
 * Uses SharedPreferences for simple, reliable storage.
 */
class RecentDocumentsStore(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "docedit_recent_docs"
        private const val KEY_RECENT_URIS = "recent_uris"
        private const val KEY_RECENT_NAMES = "recent_names"
        private const val KEY_RECENT_FORMATS = "recent_formats"
        private const val KEY_RECENT_TIMES = "recent_times"
        private const val MAX_RECENT = 20
        private const val SEPARATOR = "|||"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val fileManager = FileManager(context)
    
    /**
     * Add a document to recent list
     */
    fun addRecent(uri: Uri, name: String, format: DocumentFormat) {
        val documents = getRecentList().toMutableList()
        
        // Remove if already exists
        documents.removeAll { it.uri == uri }
        
        // Add to beginning
        documents.add(0, RecentDocument(
            uri = uri,
            name = name,
            format = format,
            lastAccessed = Date()
        ))
        
        // Trim to max size
        while (documents.size > MAX_RECENT) {
            documents.removeAt(documents.lastIndex)
        }
        
        // Save
        saveList(documents)
    }
    
    /**
     * Get list of recent documents
     */
    fun getRecentList(): List<RecentDocument> {
        val uris = prefs.getString(KEY_RECENT_URIS, "") ?: ""
        val names = prefs.getString(KEY_RECENT_NAMES, "") ?: ""
        val formats = prefs.getString(KEY_RECENT_FORMATS, "") ?: ""
        val times = prefs.getString(KEY_RECENT_TIMES, "") ?: ""
        
        if (uris.isBlank()) return emptyList()
        
        val uriList = uris.split(SEPARATOR)
        val nameList = names.split(SEPARATOR)
        val formatList = formats.split(SEPARATOR)
        val timeList = times.split(SEPARATOR)
        
        return uriList.mapIndexedNotNull { index, uriString ->
            try {
                val uri = Uri.parse(uriString)
                
                // Verify file still exists
                if (!fileManager.exists(uri)) {
                    return@mapIndexedNotNull null
                }
                
                RecentDocument(
                    uri = uri,
                    name = nameList.getOrNull(index) ?: "Document",
                    format = DocumentFormat.fromExtension(formatList.getOrNull(index) ?: "txt"),
                    lastAccessed = Date(timeList.getOrNull(index)?.toLongOrNull() ?: System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Remove a document from recent list
     */
    fun removeRecent(uri: Uri) {
        val documents = getRecentList().toMutableList()
        documents.removeAll { it.uri == uri }
        saveList(documents)
    }
    
    /**
     * Clear all recent documents
     */
    fun clearRecent() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Save document list to preferences
     */
    private fun saveList(documents: List<RecentDocument>) {
        val uris = documents.joinToString(SEPARATOR) { it.uri.toString() }
        val names = documents.joinToString(SEPARATOR) { it.name }
        val formats = documents.joinToString(SEPARATOR) { it.format.extension }
        val times = documents.joinToString(SEPARATOR) { it.lastAccessed.time.toString() }
        
        prefs.edit()
            .putString(KEY_RECENT_URIS, uris)
            .putString(KEY_RECENT_NAMES, names)
            .putString(KEY_RECENT_FORMATS, formats)
            .putString(KEY_RECENT_TIMES, times)
            .apply()
    }
}
