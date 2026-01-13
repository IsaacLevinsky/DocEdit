package com.mcmlv1.docedit.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.FontSize
import com.mcmlv1.docedit.domain.model.TextFormatting
import com.mcmlv1.docedit.presentation.common.EditorEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onSaveAs: (DocumentFormat) -> Unit,
    onExportPdf: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showSaveAsMenu by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            onNavigateBack()
        }
    }

    // Show error/message snackbars
    LaunchedEffect(uiState.error, uiState.message) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(EditorEvent.DismissError)
        }
        uiState.message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(EditorEvent.DismissMessage)
        }
    }

    // Update rename text when dialog opens
    LaunchedEffect(uiState.showRenameDialog) {
        if (uiState.showRenameDialog) {
            renameText = uiState.document.name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.document.name.ifEmpty { "Untitled" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                uiState.hasUnsavedChanges -> "Edited • ${uiState.document.format.displayName}"
                                uiState.document.isReadOnly -> "Read-only • ${uiState.document.format.displayName}"
                                else -> uiState.document.format.displayName
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.hasUnsavedChanges)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)  // FIXED: was onPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.checkUnsavedChanges { onNavigateBack() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Save button
                    if (uiState.isEditable && uiState.document.uri != null) {
                        IconButton(
                            onClick = { viewModel.onEvent(EditorEvent.Save) },
                            enabled = uiState.hasUnsavedChanges
                        ) {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }

                    // Share button
                    IconButton(onClick = { viewModel.onEvent(EditorEvent.Share) }) {
                        Icon(Icons.Default.Share, "Share")
                    }

                    // More menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Save As submenu
                            Box {
                                DropdownMenuItem(
                                    text = { Text("Save As...") },
                                    leadingIcon = { Icon(Icons.Default.SaveAs, null) },
                                    onClick = {
                                        showMenu = false
                                        showSaveAsMenu = true
                                    }
                                )
                            }

                            // Export to PDF
                            DropdownMenuItem(
                                text = { Text("Export to PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                onClick = {
                                    showMenu = false
                                    onExportPdf()
                                }
                            )

                            HorizontalDivider()

                            if (uiState.document.uri != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.onEvent(EditorEvent.Rename)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.onEvent(EditorEvent.Delete)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (uiState.isEditable) {
                FormattingToolbar(
                    formatting = uiState.formatting,
                    onBoldClick = { viewModel.onEvent(EditorEvent.ToggleBold) },
                    onItalicClick = { viewModel.onEvent(EditorEvent.ToggleItalic) },
                    onUnderlineClick = { viewModel.onEvent(EditorEvent.ToggleUnderline) },
                    showFontSizeMenu = uiState.showFontSizeMenu,
                    onFontSizeMenuToggle = {
                        if (uiState.showFontSizeMenu) {
                            viewModel.onEvent(EditorEvent.HideFontSizeMenu)
                        } else {
                            viewModel.onEvent(EditorEvent.ShowFontSizeMenu)
                        }
                    },
                    onFontSizeSelect = { viewModel.onEvent(EditorEvent.FontSizeChanged(it)) },
                    wordCount = uiState.wordCount,
                    charCount = uiState.charCount
                )
            } else {
                // Read-only status bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "View only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.wordCount} words · ${uiState.charCount} characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Text editor
                val scrollState = rememberScrollState()

                BasicTextField(
                    value = uiState.document.content,
                    onValueChange = { viewModel.onEvent(EditorEvent.ContentChanged(it)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                        .imePadding(),
                    textStyle = TextStyle(
                        fontSize = uiState.formatting.fontSize.sp.sp,
                        fontWeight = if (uiState.formatting.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (uiState.formatting.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (uiState.formatting.isUnderline) TextDecoration.Underline else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    readOnly = !uiState.isEditable,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.document.content.isEmpty()) {
                                Text(
                                    text = if (uiState.isEditable) "Start typing..." else "",
                                    style = TextStyle(
                                        fontSize = uiState.formatting.fontSize.sp.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    // Save As format selection dialog
    if (showSaveAsMenu) {
        AlertDialog(
            onDismissRequest = { showSaveAsMenu = false },
            title = { Text("Save As") },
            text = {
                Column {
                    Text(
                        text = "Choose format:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // TXT option
                    TextButton(
                        onClick = {
                            showSaveAsMenu = false
                            onSaveAs(DocumentFormat.TXT)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📄 Text File (.txt)", modifier = Modifier.fillMaxWidth())
                    }

                    // DOCX option
                    TextButton(
                        onClick = {
                            showSaveAsMenu = false
                            onSaveAs(DocumentFormat.DOCX)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📘 Word Document (.docx)", modifier = Modifier.fillMaxWidth())
                    }

                    // ODT option
                    TextButton(
                        onClick = {
                            showSaveAsMenu = false
                            onSaveAs(DocumentFormat.ODT)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📗 OpenDocument (.odt)", modifier = Modifier.fillMaxWidth())
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // PDF export
                    TextButton(
                        onClick = {
                            showSaveAsMenu = false
                            onExportPdf()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📕 Export to PDF (.pdf)", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSaveAsMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename dialog
    if (uiState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.ShowRenameDialog(false)) },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(EditorEvent.ConfirmRename(renameText)) },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.ShowRenameDialog(false)) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.ShowDeleteDialog(false)) },
            title = { Text("Delete Document?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(EditorEvent.ConfirmDelete) }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.ShowDeleteDialog(false)) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Discard changes dialog
    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.CancelDiscard) },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Discard them?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.ConfirmDiscard) }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.CancelDiscard) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FormattingToolbar(
    formatting: TextFormatting,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    showFontSizeMenu: Boolean,
    onFontSizeMenuToggle: () -> Unit,
    onFontSizeSelect: (FontSize) -> Unit,
    wordCount: Int,
    charCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formatting buttons
                Row {
                    IconButton(
                        onClick = onBoldClick,
                        colors = if (formatting.isBold)
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        else IconButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            Icons.Default.FormatBold,
                            "Bold",
                            tint = if (formatting.isBold)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onItalicClick,
                        colors = if (formatting.isItalic)
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        else IconButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            Icons.Default.FormatItalic,
                            "Italic",
                            tint = if (formatting.isItalic)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onUnderlineClick,
                        colors = if (formatting.isUnderline)
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        else IconButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            Icons.Default.FormatUnderlined,
                            "Underline",
                            tint = if (formatting.isUnderline)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Font size button with dropdown
                    Box {
                        IconButton(onClick = onFontSizeMenuToggle) {
                            Icon(
                                Icons.Default.FormatSize,
                                "Font Size",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showFontSizeMenu,
                            onDismissRequest = onFontSizeMenuToggle
                        ) {
                            FontSize.entries.forEach { size ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = size.displayName,
                                            fontWeight = if (formatting.fontSize == size)
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { onFontSizeSelect(size) }
                                )
                            }
                        }
                    }
                }

                // Word/character count
                Text(
                    text = "$wordCount words · $charCount chars",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}