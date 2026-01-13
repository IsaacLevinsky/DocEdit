package com.mcmlv1.docedit.presentation.common

import com.mcmlv1.docedit.domain.model.Document
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.FontSize
import com.mcmlv1.docedit.domain.model.RecentDocument
import com.mcmlv1.docedit.domain.model.TextFormatting

/**
 * UI state for the document editor
 */
data class EditorUiState(
    val document: Document = Document.empty(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val formatting: TextFormatting = TextFormatting(),
    val showFormatMenu: Boolean = false,
    val showFontSizeMenu: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showSaveAsDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showConvertDialog: Boolean = false
) {
    val hasUnsavedChanges: Boolean
        get() = document.isModified
    
    val isEditable: Boolean
        get() = document.format.isEditable && !document.isReadOnly
    
    val canSave: Boolean
        get() = document.uri != null && isEditable && hasUnsavedChanges
    
    val canRename: Boolean
        get() = document.uri != null
    
    val wordCount: Int
        get() = document.wordCount
    
    val charCount: Int
        get() = document.charCount
}

/**
 * UI state for the document browser
 */
data class BrowserUiState(
    val recentDocuments: List<RecentDocument> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val selectedDocument: RecentDocument? = null,
    val showDeleteDialog: Boolean = false,
    val showSettingsDialog: Boolean = false
)

/**
 * App-wide settings state
 */
data class SettingsState(
    val isDarkMode: Boolean = false,
    val useSystemTheme: Boolean = true,
    val useDynamicColors: Boolean = true
)

/**
 * Events from editor to ViewModel
 */
sealed class EditorEvent {
    data class ContentChanged(val content: String) : EditorEvent()
    data class FontSizeChanged(val fontSize: FontSize) : EditorEvent()
    data object ToggleBold : EditorEvent()
    data object ToggleItalic : EditorEvent()
    data object ToggleUnderline : EditorEvent()
    data object ShowFormatMenu : EditorEvent()
    data object HideFormatMenu : EditorEvent()
    data object ShowFontSizeMenu : EditorEvent()
    data object HideFontSizeMenu : EditorEvent()
    data object Save : EditorEvent()
    data object SaveAs : EditorEvent()
    data object ExportPdf : EditorEvent()
    data object Share : EditorEvent()
    data object Rename : EditorEvent()
    data object Delete : EditorEvent()
    data object DiscardChanges : EditorEvent()
    data object ConfirmDiscard : EditorEvent()
    data object CancelDiscard : EditorEvent()
    data object DismissError : EditorEvent()
    data object DismissMessage : EditorEvent()
    data class ShowRenameDialog(val show: Boolean) : EditorEvent()
    data class ShowDeleteDialog(val show: Boolean) : EditorEvent()
    data class ShowConvertDialog(val show: Boolean) : EditorEvent()
    data class ConfirmRename(val newName: String) : EditorEvent()
    data object ConfirmDelete : EditorEvent()
}

/**
 * Events from browser to ViewModel
 */
sealed class BrowserEvent {
    data object LoadRecent : BrowserEvent()
    data class SelectDocument(val document: RecentDocument) : BrowserEvent()
    data class DeleteDocument(val document: RecentDocument) : BrowserEvent()
    data object ConfirmDelete : BrowserEvent()
    data object CancelDelete : BrowserEvent()
    data object ClearRecent : BrowserEvent()
    data object DismissError : BrowserEvent()
    data object DismissMessage : BrowserEvent()
    data object ShowSettings : BrowserEvent()
    data object HideSettings : BrowserEvent()
}

/**
 * Settings events
 */
sealed class SettingsEvent {
    data class SetDarkMode(val enabled: Boolean) : SettingsEvent()
    data class SetUseSystemTheme(val enabled: Boolean) : SettingsEvent()
    data class SetUseDynamicColors(val enabled: Boolean) : SettingsEvent()
}

/**
 * Navigation events
 */
sealed class NavEvent {
    data object NavigateToEditor : NavEvent()
    data object NavigateToBrowser : NavEvent()
    data object NavigateBack : NavEvent()
}

/**
 * Format option for Save As / Convert dialogs
 */
data class FormatOption(
    val format: DocumentFormat,
    val displayName: String,
    val description: String,
    val icon: String
) {
    companion object {
        val saveAsOptions = listOf(
            FormatOption(
                DocumentFormat.TXT,
                "Text File (.txt)",
                "Plain text, universal compatibility",
                "📄"
            ),
            FormatOption(
                DocumentFormat.DOCX,
                "Word Document (.docx)",
                "Microsoft Word format",
                "📘"
            ),
            FormatOption(
                DocumentFormat.ODT,
                "OpenDocument (.odt)",
                "Open standard format",
                "📗"
            )
        )
        
        val exportOptions = saveAsOptions + FormatOption(
            DocumentFormat.PDF,
            "PDF Document (.pdf)",
            "Portable Document Format",
            "📕"
        )
    }
}
