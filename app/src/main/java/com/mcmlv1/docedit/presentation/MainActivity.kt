package com.mcmlv1.docedit.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mcmlv1.docedit.domain.model.DocumentFormat
import com.mcmlv1.docedit.presentation.browser.BrowserScreen
import com.mcmlv1.docedit.presentation.browser.BrowserViewModel
import com.mcmlv1.docedit.presentation.editor.EditorScreen
import com.mcmlv1.docedit.presentation.editor.EditorViewModel
import com.mcmlv1.docedit.presentation.theme.DocEditTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels { EditorViewModel.Factory(this) }
    private val browserViewModel: BrowserViewModel by viewModels { BrowserViewModel.Factory(this) }

    private var navController: NavHostController? = null

    // File picker for opening documents
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Permission might not be grantable, continue anyway
            }

            // Route based on file type - skip PDFs since we don't view them
            val mimeType = contentResolver.getType(it)
            if (mimeType != "application/pdf") {
                editorViewModel.openDocument(it)
                navController?.navigate("editor")
            }
        }
    }

    // File creator for Save As (TXT)
    private val saveAsTxtLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { editorViewModel.saveAs(it, DocumentFormat.TXT) }
    }

    // File creator for Save As (DOCX)
    private val saveAsDocxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri: Uri? ->
        uri?.let { editorViewModel.saveAs(it, DocumentFormat.DOCX) }
    }

    // File creator for Save As (ODT)
    private val saveAsOdtLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.oasis.opendocument.text")
    ) { uri: Uri? ->
        uri?.let { editorViewModel.saveAs(it, DocumentFormat.ODT) }
    }

    // File creator for PDF export
    private val exportPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { editorViewModel.exportToPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intents (VIEW action)
        handleIntent(intent)

        // Collect share intents
        lifecycleScope.launch {
            editorViewModel.shareIntent.collectLatest { shareIntent ->
                startActivity(shareIntent)
            }
        }

        setContent {
            val settingsState by browserViewModel.settingsState.collectAsState()

            DocEditTheme(
                darkTheme = settingsState.isDarkMode,
                useSystemTheme = settingsState.useSystemTheme,
                dynamicColor = settingsState.useDynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navHostController = rememberNavController()
                    navController = navHostController

                    var startDestination by remember { mutableStateOf("browser") }

                    DocEditNavHost(
                        navController = navHostController,
                        startDestination = startDestination,
                        editorViewModel = editorViewModel,
                        browserViewModel = browserViewModel,
                        onOpenDocument = { openDocument() },
                        onNewDocument = { format -> newDocument(format) },
                        onSaveAs = { format -> saveAs(format) },
                        onExportPdf = { exportPdf() },
                        onRenamePdf = { sourceUri, newName, destinationUri ->
                            renamePdf(sourceUri, destinationUri)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType != "application/pdf") {
                        editorViewModel.openDocument(uri)
                        navController?.navigate("editor")
                    }
                }
            }
        }
    }

    private fun openDocument() {
        // Removed PDF from open document types since we don't view them
        // PDF rename is handled separately via the + menu
        openDocumentLauncher.launch(arrayOf(
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text"
        ))
    }

    private fun newDocument(format: DocumentFormat) {
        editorViewModel.newDocument(format)
        navController?.navigate("editor")
    }

    private fun saveAs(format: DocumentFormat) {
        val doc = editorViewModel.getCurrentDocument()
        val baseName = doc.name.substringBeforeLast('.')
        val fileName = "$baseName.${format.extension}"

        when (format) {
            DocumentFormat.TXT -> saveAsTxtLauncher.launch(fileName)
            DocumentFormat.DOCX -> saveAsDocxLauncher.launch(fileName)
            DocumentFormat.ODT -> saveAsOdtLauncher.launch(fileName)
            DocumentFormat.PDF -> exportPdfLauncher.launch(fileName)
        }
    }

    private fun exportPdf() {
        val doc = editorViewModel.getCurrentDocument()
        val pdfName = doc.name.substringBeforeLast('.') + ".pdf"
        exportPdfLauncher.launch(pdfName)
    }

    /**
     * Copy PDF from source to destination (rename operation)
     */
    private fun renamePdf(sourceUri: Uri, destinationUri: Uri) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        contentResolver.openOutputStream(destinationUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                Toast.makeText(this@MainActivity, "PDF saved with new name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun DocEditNavHost(
    navController: NavHostController,
    startDestination: String,
    editorViewModel: EditorViewModel,
    browserViewModel: BrowserViewModel,
    onOpenDocument: () -> Unit,
    onNewDocument: (DocumentFormat) -> Unit,
    onSaveAs: (DocumentFormat) -> Unit,
    onExportPdf: () -> Unit,
    onRenamePdf: (sourceUri: Uri, newName: String, destinationUri: Uri) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("browser") {
            BrowserScreen(
                viewModel = browserViewModel,
                onNewDocument = onNewDocument,
                onOpenDocument = onOpenDocument,
                onDocumentSelected = { document ->
                    // Only handle non-PDF documents
                    if (document.format != DocumentFormat.PDF) {
                        editorViewModel.openDocument(document.uri)
                        navController.navigate("editor")
                    }
                },
                onRenamePdf = onRenamePdf
            )
        }

        composable("editor") {
            EditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onSaveAs = onSaveAs,
                onExportPdf = onExportPdf
            )
        }
    }
}