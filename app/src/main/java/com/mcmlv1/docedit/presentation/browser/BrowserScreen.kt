package com.mcmlv1.docedit.presentation.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcmlv1.docedit.R
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.domain.model.RecentDocument
import com.mcmlv1.docedit.presentation.common.BrowserEvent
import com.mcmlv1.docedit.presentation.common.SettingsEvent
import com.mcmlv1.docedit.presentation.theme.DocxColor
import com.mcmlv1.docedit.presentation.theme.OdtColor
import com.mcmlv1.docedit.presentation.theme.PdfColor
import com.mcmlv1.docedit.presentation.theme.TxtColor
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNewDocument: (DocumentFormat) -> Unit,
    onOpenDocument: () -> Unit,
    onDocumentSelected: (RecentDocument) -> Unit,
    onRenamePdf: (sourceUri: Uri, newName: String, destinationUri: Uri) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewDocMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Rename PDF flow state
    var showRenamePdfDialog by remember { mutableStateOf(false) }
    var pdfToRename by remember { mutableStateOf<Uri?>(null) }
    var pdfOriginalName by remember { mutableStateOf("") }
    var pdfNewName by remember { mutableStateOf("") }

    // PDF picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pdfToRename = it
            // Extract filename from URI
            val name = it.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":") ?: "document"
            pdfOriginalName = name
            pdfNewName = name.substringBeforeLast(".")
            showRenamePdfDialog = true
        }
    }

    // Save renamed PDF launcher
    val saveRenamedPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->
        destinationUri?.let { dest ->
            pdfToRename?.let { source ->
                onRenamePdf(source, pdfNewName, dest)
                pdfToRename = null
                pdfNewName = ""
                pdfOriginalName = ""
            }
        }
    }

    // Show error/message snackbars
    LaunchedEffect(uiState.error, uiState.message) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(BrowserEvent.DismissError)
        }
        uiState.message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.onEvent(BrowserEvent.DismissMessage)
        }
    }

    // Reload on resume
    LaunchedEffect(Unit) {
        viewModel.onEvent(BrowserEvent.LoadRecent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DocEdit")
                        Text(
                            text = "Offline Document Editor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Theme toggle (sun/moon)
                    IconButton(onClick = {
                        if (settingsState.useSystemTheme) {
                            viewModel.onSettingsEvent(SettingsEvent.SetUseSystemTheme(false))
                            viewModel.onSettingsEvent(SettingsEvent.SetDarkMode(!settingsState.isDarkMode))
                        } else {
                            viewModel.onSettingsEvent(SettingsEvent.SetDarkMode(!settingsState.isDarkMode))
                        }
                    }) {
                        Icon(
                            if (settingsState.isDarkMode) Icons.Default.DarkMode
                            else Icons.Default.LightMode,
                            "Toggle theme"
                        )
                    }

                    // Open file
                    IconButton(onClick = onOpenDocument) {
                        Icon(Icons.Default.FolderOpen, "Open file")
                    }

                    // About (replaced Settings)
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showNewDocMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "New document")
                }

                DropdownMenu(
                    expanded = showNewDocMenu,
                    onDismissRequest = { showNewDocMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📄 Text File (.txt)") },
                        onClick = {
                            showNewDocMenu = false
                            onNewDocument(DocumentFormat.TXT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📘 Word Document (.docx)") },
                        onClick = {
                            showNewDocMenu = false
                            onNewDocument(DocumentFormat.DOCX)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📗 OpenDocument (.odt)") },
                        onClick = {
                            showNewDocMenu = false
                            onNewDocument(DocumentFormat.ODT)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("📕 Rename a PDF") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                null,
                                tint = PdfColor
                            )
                        },
                        onClick = {
                            showNewDocMenu = false
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
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
            } else if (uiState.recentDocuments.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No documents yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to create or open a document",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = onOpenDocument) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open a file")
                    }
                }
            } else {
                // Recent documents list
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.recentDocuments,
                            key = { it.uri.toString() }
                        ) { document ->
                            DocumentCard(
                                document = document,
                                onClick = { onDocumentSelected(document) },
                                onDelete = { viewModel.onEvent(BrowserEvent.DeleteDocument(document)) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BrowserEvent.CancelDelete) },
            title = { Text("Delete Document?") },
            text = {
                Text("Remove \"${uiState.selectedDocument?.name}\" from recent documents?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BrowserEvent.ConfirmDelete) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(BrowserEvent.CancelDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename PDF dialog
    if (showRenamePdfDialog && pdfToRename != null) {
        AlertDialog(
            onDismissRequest = {
                showRenamePdfDialog = false
                pdfToRename = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        null,
                        tint = PdfColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Rename PDF")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Original: $pdfOriginalName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pdfNewName,
                        onValueChange = { pdfNewName = it },
                        label = { Text("New name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = ".pdf",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenamePdfDialog = false
                        val finalName = if (pdfNewName.endsWith(".pdf")) pdfNewName else "$pdfNewName.pdf"
                        saveRenamedPdfLauncher.launch(finalName)
                    },
                    enabled = pdfNewName.isNotBlank()
                ) {
                    Text("Choose Save Location")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenamePdfDialog = false
                    pdfToRename = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo and brand header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // App icon placeholder - replace with your actual logo
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "DocEdit",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tagline
                Text(
                    text = "Professional offline document editing.\nNo cloud. No subscriptions. No compromises.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Features section
                Text(
                    text = "FEATURES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FeatureItem(
                    icon = Icons.Default.WifiOff,
                    title = "100% Offline",
                    description = "Works without internet. Your documents never leave your device."
                )

                FeatureItem(
                    icon = Icons.Default.Description,
                    title = "Multiple Formats",
                    description = "Create and edit TXT, DOCX, and ODT files natively."
                )

                FeatureItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF Tools",
                    description = "Rename PDFs quickly with the built-in PDF renamer."
                )

                FeatureItem(
                    icon = Icons.Default.Security,
                    title = "Private & Secure",
                    description = "No accounts, no tracking, no data collection. Ever."
                )

                FeatureItem(
                    icon = Icons.Default.Speed,
                    title = "Fast & Reliable",
                    description = "Lightweight app that opens instantly and never crashes."
                )

                Spacer(modifier = Modifier.height(24.dp))

                // How to use section
                Text(
                    text = "HOW TO USE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                HowToItem("Tap  +  to create a new document or rename a PDF")
                HowToItem("Tap  📂  to open an existing file from your device")
                HowToItem("Tap  ☀️/🌙  to switch between light and dark mode")
                HowToItem("Tap any recent document to continue editing")
                HowToItem("Use the 3-dot menu on documents for more options")

                Spacer(modifier = Modifier.height(24.dp))

                // Supported formats
                Text(
                    text = "SUPPORTED FORMATS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FormatBadge("TXT", TxtColor)
                    FormatBadge("DOCX", DocxColor)
                    FormatBadge("ODT", OdtColor)
                    FormatBadge("PDF", PdfColor)
                }

                Spacer(modifier = Modifier.height(32.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(20.dp))

                // Company branding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Crafted with care by",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "MCMLV1, LLC",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "mcmlv1.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://mcmlv1.com")
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "© 2026 MCMLV1, LLC. All rights reserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Built for reliability. Built to last.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun HowToItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FormatBadge(format: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = format,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DocumentCard(
    document: RecentDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon
            Icon(
                imageVector = if (document.format == DocumentFormat.PDF)
                    Icons.Default.PictureAsPdf
                else
                    Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFormatColor(document.format)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Document info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = document.format.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ${dateFormat.format(document.lastAccessed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from recent") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun getFormatColor(format: DocumentFormat): Color {
    return when (format) {
        DocumentFormat.TXT -> TxtColor
        DocumentFormat.DOCX -> DocxColor
        DocumentFormat.ODT -> OdtColor
        DocumentFormat.PDF -> PdfColor
    }
}