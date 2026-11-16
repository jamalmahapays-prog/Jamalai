package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppStrings
import com.example.data.files.DocumentExtractor
import com.example.data.files.ExtractedDocument
import com.example.data.model.DocumentRecord
import com.example.ui.components.AiOutputCard
import com.example.ui.components.PremiumCard
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun DocumentsScreen(
    strings: AppStrings,
    documentsState: UiState<List<DocumentRecord>>,
    isLoading: Boolean,
    errorMessage: String?,
    qaAnswer: String?,
    onUploadAndAnalyze: (ExtractedDocument) -> Unit,
    onAskQuestion: (DocumentRecord, String) -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedDocId by remember { mutableStateOf<String?>(null) }
    var questionText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                DocumentExtractor.extractFromUri(context, uri)
                    .onSuccess { doc ->
                        localError = null
                        onUploadAndAnalyze(doc)
                    }
                    .onFailure { err ->
                        localError = err.localizedMessage ?: "Failed to read file."
                    }
            }
        }
    }

    val docs = (documentsState as? UiState.Success)?.data.orEmpty()
    val selectedDoc = docs.firstOrNull { it.id == selectedDocId } ?: docs.firstOrNull()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.documentsTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.documentsSizeLimit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            filePicker.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                    "text/markdown",
                                    "text/csv",
                                    "image/*"
                                )
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("documents_upload_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.documentsUploadBtn)
                        }
                    }
                }
            }

            val err = errorMessage ?: localError
            if (!err.isNullOrBlank()) {
                item {
                    Surface(
                        color = RoseError.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(err, color = RoseError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (docs.isEmpty()) {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.documentsEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(docs, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDocId = doc.id },
                        accentBorder = isSelected
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${doc.mimeType} • ${(doc.fileSizeBytes / 1024L).coerceAtLeast(1L)} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanSecondary
                                )
                            }
                            IconButton(onClick = { onDeleteDocument(doc.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = strings.delete,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = doc.aiSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                selectedDoc?.let { activeDoc ->
                    item {
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                label = { Text("${strings.documentsAskPlaceholder} (${activeDoc.fileName})") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onAskQuestion(activeDoc, questionText)
                                },
                                enabled = !isLoading && questionText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.documentsAskBtn)
                            }
                        }
                    }
                }

                if (!qaAnswer.isNullOrBlank()) {
                    item {
                        AiOutputCard(
                            title = selectedDoc?.fileName ?: strings.documentsTitle,
                            text = qaAnswer,
                            strings = strings,
                            context = context
                        )
                    }
                }
            }
        }
    }
}
