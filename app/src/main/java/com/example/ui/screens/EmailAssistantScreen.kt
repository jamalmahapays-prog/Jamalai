package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.EmailAnalysisResult
import com.example.data.model.EmailDraft
import com.example.ui.components.PremiumCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun EmailAssistantScreen(
    strings: AppStrings,
    draftsState: UiState<List<EmailDraft>>,
    isLoading: Boolean,
    errorMessage: String?,
    latestAnalysis: EmailAnalysisResult?,
    onAnalyzeEmail: (String) -> Unit,
    onDraftWithAi: (goal: String, recipient: String, onReady: (String, String) -> Unit) -> Unit,
    onMarkDispatched: (EmailDraft) -> Unit,
    onDeleteDraft: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var modeAnalyze by remember { mutableStateOf(true) }
    var rawEmailText by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var pendingSendDraft by remember { mutableStateOf<EmailDraft?>(null) }
    var showConfirmSendDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                DocumentExtractor.extractFromUri(context, uri)
                    .onSuccess { doc ->
                        if (doc.extractedText.isNotBlank()) {
                            rawEmailText = doc.extractedText
                        }
                    }
            }
        }
    }

    val drafts = (draftsState as? UiState.Success)?.data.orEmpty()

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
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = CyanSecondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = strings.emailTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.emailSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = modeAnalyze,
                            onClick = { modeAnalyze = true },
                            label = { Text(strings.emailModeAnalyze) },
                            modifier = Modifier.testTag("email_mode_analyze")
                        )
                        FilterChip(
                            selected = !modeAnalyze,
                            onClick = { modeAnalyze = false },
                            label = { Text(strings.emailModeCompose) },
                            modifier = Modifier.testTag("email_mode_compose")
                        )
                    }
                }
            }

            if (modeAnalyze) {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rawEmailText,
                            onValueChange = { rawEmailText = it },
                            label = { Text(strings.emailPasteLabel) },
                            minLines = 5,
                            maxLines = 10,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_paste_field")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { filePicker.launch(arrayOf("text/plain", "message/rfc822", "*/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.emailImportFile, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { onAnalyzeEmail(rawEmailText) },
                                enabled = !isLoading && rawEmailText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("email_analyze_btn")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.emailAnalyzeBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                latestAnalysis?.let { result ->
                    item {
                        PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.summaryLabel, style = MaterialTheme.typography.titleMedium)
                                PriorityBadge(priority = result.priority, strings = strings)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.summary, style = MaterialTheme.typography.bodyMedium)

                            if (result.extractedTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = strings.extractedTasksLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                result.extractedTasks.forEach { task ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(task, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = SurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = result.suggestedReplySubject,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VioletPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(result.suggestedReplyBody, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    subject = result.suggestedReplySubject
                                    body = result.suggestedReplyBody
                                    modeAnalyze = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.emailModeCompose)
                            }
                        }
                    }
                }
            } else {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = goalText,
                            onValueChange = { goalText = it },
                            label = { Text(strings.emailGoalLabel) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { recipient = it },
                            label = { Text(strings.emailRecipientLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onDraftWithAi(goalText, recipient) { genSubject, genBody ->
                                    subject = genSubject
                                    body = genBody
                                }
                            },
                            enabled = !isLoading && goalText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_generate_btn")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.emailGenerateBtn)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(strings.emailSubjectLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = { Text(strings.emailBodyLabel) },
                            minLines = 5,
                            maxLines = 12,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                pendingSendDraft = null
                                showConfirmSendDialog = true
                            },
                            enabled = body.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_send_btn")
                        ) {
                            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.emailSendViaGmail)
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Surface(
                        color = RoseError.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(errorMessage, color = RoseError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item {
                Text(strings.emailSavedDrafts, style = MaterialTheme.typography.titleMedium)
            }

            items(drafts, key = { it.id }) { draft ->
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            recipient = draft.recipient
                            subject = draft.subject
                            body = draft.body
                            modeAnalyze = false
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = draft.subject.ifBlank { "(No Subject)" },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = draft.summary.ifBlank { draft.body },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PriorityBadge(priority = draft.priority, strings = strings)
                        IconButton(onClick = { onDeleteDraft(draft.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmSendDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSendDialog = false },
            title = { Text(strings.emailConfirmSendTitle) },
            text = { Text(strings.emailConfirmSendBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmSendDialog = false
                        pendingSendDraft?.let { onMarkDispatched(it) }
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${Uri.encode(recipient)}")
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, strings.emailSendViaGmail))
                        }.onFailure {
                            Toast.makeText(context, "No email app installed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(strings.emailConfirmSendBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSendDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun PriorityBadge(priority: String, strings: AppStrings) {
    val (label, color) = when (priority) {
        "high" -> strings.priorityHigh to RoseError
        "low" -> strings.priorityLow to EmeraldSuccess
        else -> strings.priorityMedium to AmberWarning
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
