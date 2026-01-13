package com.mcmlv1.docedit.presentation.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mcmlv1.docedit.data.repository.DocumentRepository
import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.DocumentResult
import com.mcmlv1.docedit.domain.model.FontSize
import com.mcmlv1.docedit.domain.model.TextFormatting
import com.mcmlv1.docedit.presentation.common.EditorEvent
import com.mcmlv1.docedit.presentation.common.EditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the document editor.
 * Handles all document operations and UI state.
 */
class EditorViewModel(
    private val repository: DocumentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private val _shareIntent = MutableSharedFlow<Intent>()
    val shareIntent = _shareIntent.asSharedFlow()
    
    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()
    
    private var pendingDiscardAction: (() -> Unit)? = null
    
    /**
     * Handle UI events
     */
    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.ContentChanged -> updateContent(event.content)
            is EditorEvent.FontSizeChanged -> updateFontSize(event.fontSize)
            EditorEvent.ToggleBold -> toggleBold()
            EditorEvent.ToggleItalic -> toggleItalic()
            EditorEvent.ToggleUnderline -> toggleUnderline()
            EditorEvent.ShowFormatMenu -> showFormatMenu(true)
            EditorEvent.HideFormatMenu -> showFormatMenu(false)
            EditorEvent.ShowFontSizeMenu -> showFontSizeMenu(true)
            EditorEvent.HideFontSizeMenu -> showFontSizeMenu(false)
            EditorEvent.Save -> save()
            EditorEvent.SaveAs -> { /* Handled by activity launcher */ }
            EditorEvent.ExportPdf -> { /* Handled by activity launcher */ }
            EditorEvent.Share -> share()
            EditorEvent.Rename -> showRenameDialog(true)
            EditorEvent.Delete -> showDeleteDialog(true)
            EditorEvent.DiscardChanges -> checkUnsavedChanges { navigateBack() }
            EditorEvent.ConfirmDiscard -> confirmDiscard()
            EditorEvent.CancelDiscard -> cancelDiscard()
            EditorEvent.DismissError -> dismissError()
            EditorEvent.DismissMessage -> dismissMessage()
            is EditorEvent.ShowRenameDialog -> showRenameDialog(event.show)
            is EditorEvent.ShowDeleteDialog -> showDeleteDialog(event.show)
            is EditorEvent.ConfirmRename -> confirmRename(event.newName)
            EditorEvent.ConfirmDelete -> confirmDelete()
            else -> {}
        }
    }
    
    /**
     * Create new document
     */
    fun newDocument(format: DocumentFormat = DocumentFormat.TXT) {
        _uiState.update {
            it.copy(
                document = repository.createNewDocument(format),
                isLoading = false,
                error = null
            )
        }
    }
    
    /**
     * Open document from URI
     */
    fun openDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = withContext(Dispatchers.IO) {
                repository.openDocument(uri)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            document = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Update document content
     */
    private fun updateContent(content: String) {
        _uiState.update { state ->
            val updatedDoc = repository.updateContent(state.document, content)
            state.copy(document = updatedDoc)
        }
    }
    
    /**
     * Save document
     */
    private fun save() {
        val currentState = _uiState.value
        if (currentState.document.uri == null) {
            // No URI yet, need Save As
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = withContext(Dispatchers.IO) {
                repository.saveDocument(currentState.document)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            document = result.data,
                            isLoading = false,
                            message = "Document saved"
                        )
                    }
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Save document to new location
     */
    fun saveAs(uri: Uri, format: DocumentFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = withContext(Dispatchers.IO) {
                repository.saveDocumentAs(_uiState.value.document, uri, format)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            document = result.data,
                            isLoading = false,
                            message = "Document saved"
                        )
                    }
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Export to PDF
     */
    fun exportToPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = withContext(Dispatchers.IO) {
                repository.exportToPdf(_uiState.value.document, uri)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Exported to PDF"
                        )
                    }
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Share document
     */
    private fun share() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.createShareIntent(_uiState.value.document)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _shareIntent.emit(result.data)
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(error = result.message)
                    }
                }
            }
        }
    }
    
    /**
     * Show/hide rename dialog
     */
    private fun showRenameDialog(show: Boolean) {
        _uiState.update { it.copy(showRenameDialog = show) }
    }
    
    /**
     * Confirm rename
     */
    private fun confirmRename(newName: String) {
        val uri = _uiState.value.document.uri ?: return
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.renameDocument(uri, newName)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            document = it.document.copy(
                                uri = result.data,
                                name = newName
                            ),
                            showRenameDialog = false,
                            message = "Document renamed"
                        )
                    }
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            showRenameDialog = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Show/hide delete dialog
     */
    private fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }
    
    /**
     * Confirm delete
     */
    private fun confirmDelete() {
        val uri = _uiState.value.document.uri ?: return
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.deleteDocument(uri)
            }
            
            when (result) {
                is DocumentResult.Success -> {
                    _uiState.update {
                        it.copy(
                            showDeleteDialog = false,
                            message = "Document deleted"
                        )
                    }
                    _navigateBack.emit(Unit)
                }
                is DocumentResult.Error -> {
                    _uiState.update {
                        it.copy(
                            showDeleteDialog = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Toggle bold formatting
     */
    private fun toggleBold() {
        _uiState.update { state ->
            state.copy(
                formatting = state.formatting.copy(
                    isBold = !state.formatting.isBold
                )
            )
        }
    }
    
    /**
     * Toggle italic formatting
     */
    private fun toggleItalic() {
        _uiState.update { state ->
            state.copy(
                formatting = state.formatting.copy(
                    isItalic = !state.formatting.isItalic
                )
            )
        }
    }
    
    /**
     * Toggle underline formatting
     */
    private fun toggleUnderline() {
        _uiState.update { state ->
            state.copy(
                formatting = state.formatting.copy(
                    isUnderline = !state.formatting.isUnderline
                )
            )
        }
    }
    
    /**
     * Update font size
     */
    private fun updateFontSize(fontSize: FontSize) {
        _uiState.update { state ->
            state.copy(
                formatting = state.formatting.copy(fontSize = fontSize),
                showFontSizeMenu = false
            )
        }
    }
    
    /**
     * Show/hide format menu
     */
    private fun showFormatMenu(show: Boolean) {
        _uiState.update { it.copy(showFormatMenu = show) }
    }
    
    /**
     * Show/hide font size menu
     */
    private fun showFontSizeMenu(show: Boolean) {
        _uiState.update { it.copy(showFontSizeMenu = show) }
    }
    
    /**
     * Check for unsaved changes before action
     */
    fun checkUnsavedChanges(action: () -> Unit) {
        if (_uiState.value.hasUnsavedChanges) {
            pendingDiscardAction = action
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            action()
        }
    }
    
    /**
     * Confirm discard changes
     */
    private fun confirmDiscard() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        pendingDiscardAction?.invoke()
        pendingDiscardAction = null
    }
    
    /**
     * Cancel discard
     */
    private fun cancelDiscard() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        pendingDiscardAction = null
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
     * Navigate back
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigateBack.emit(Unit)
        }
    }
    
    /**
     * Get current document
     */
    fun getCurrentDocument(): Document = _uiState.value.document
    
    /**
     * Factory for creating EditorViewModel
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
                return EditorViewModel(DocumentRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
