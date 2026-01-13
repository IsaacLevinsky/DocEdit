package com.mcmlv1.docedit.presentation.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mcmlv1.docedit.data.repository.DocumentRepository
import com.mcmlv1.docedit.data.storage.SettingsRepository
import com.mcmlv1.docedit.domain.model.RecentDocument
import com.mcmlv1.docedit.presentation.common.BrowserEvent
import com.mcmlv1.docedit.presentation.common.BrowserUiState
import com.mcmlv1.docedit.presentation.common.SettingsEvent
import com.mcmlv1.docedit.presentation.common.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for document browser/home screen.
 * Also manages app-wide settings including theme.
 */
class BrowserViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()
    
    // Settings state combining dark mode and system theme preferences
    val settingsState: StateFlow<SettingsState> = combine(
        settingsRepository.isDarkMode,
        settingsRepository.useSystemTheme
    ) { isDarkMode, useSystemTheme ->
        SettingsState(
            isDarkMode = isDarkMode,
            useSystemTheme = useSystemTheme,
            useDynamicColors = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )
    
    init {
        loadRecentDocuments()
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: BrowserEvent) {
        when (event) {
            BrowserEvent.LoadRecent -> loadRecentDocuments()
            is BrowserEvent.SelectDocument -> selectDocument(event.document)
            is BrowserEvent.DeleteDocument -> showDeleteConfirmation(event.document)
            BrowserEvent.ConfirmDelete -> confirmDelete()
            BrowserEvent.CancelDelete -> cancelDelete()
            BrowserEvent.ClearRecent -> clearRecent()
            BrowserEvent.DismissError -> dismissError()
            BrowserEvent.DismissMessage -> dismissMessage()
            BrowserEvent.ShowSettings -> showSettings(true)
            BrowserEvent.HideSettings -> showSettings(false)
        }
    }
    
    /**
     * Handle settings events
     */
    fun onSettingsEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetDarkMode -> {
                    settingsRepository.setDarkMode(event.enabled)
                }
                is SettingsEvent.SetUseSystemTheme -> {
                    settingsRepository.setUseSystemTheme(event.enabled)
                }
                is SettingsEvent.SetUseDynamicColors -> {
                    // Dynamic colors are always enabled for now
                }
            }
        }
    }
    
    /**
     * Load recent documents
     */
    private fun loadRecentDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val documents = repository.getRecentDocuments()
                _uiState.update {
                    it.copy(
                        recentDocuments = documents,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load recent documents"
                    )
                }
            }
        }
    }
    
    /**
     * Select a document
     */
    private fun selectDocument(document: RecentDocument) {
        _uiState.update { it.copy(selectedDocument = document) }
    }
    
    /**
     * Show delete confirmation
     */
    private fun showDeleteConfirmation(document: RecentDocument) {
        _uiState.update {
            it.copy(
                selectedDocument = document,
                showDeleteDialog = true
            )
        }
    }
    
    /**
     * Confirm delete
     */
    private fun confirmDelete() {
        val document = _uiState.value.selectedDocument ?: return
        
        viewModelScope.launch {
            val result = repository.deleteDocument(document.uri)
            
            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    selectedDocument = null,
                    message = if (result.isSuccess()) "Document deleted" else "Failed to delete"
                )
            }
            
            // Reload list
            loadRecentDocuments()
        }
    }
    
    /**
     * Cancel delete
     */
    private fun cancelDelete() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                selectedDocument = null
            )
        }
    }
    
    /**
     * Clear all recent documents
     */
    private fun clearRecent() {
        repository.clearRecentDocuments()
        _uiState.update {
            it.copy(
                recentDocuments = emptyList(),
                message = "Recent documents cleared"
            )
        }
    }
    
    /**
     * Show/hide settings dialog
     */
    private fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }
    
    /**
     * Dismiss error
     */
    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Dismiss message
     */
    private fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    /**
     * Factory for creating BrowserViewModel
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                return BrowserViewModel(
                    DocumentRepository(context),
                    SettingsRepository(context)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
