package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppStrings
import com.example.data.files.DocumentExtractor
import com.example.data.files.ExtractedDocument
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.ui.components.PremiumCard
import com.example.ui.components.copyTextToClipboard
import com.example.ui.components.sharePlainText
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletDeep
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    strings: AppStrings,
    conversationsState: UiState<List<Conversation>>,
    activeConversationId: String?,
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    errorMessage: String?,
    selectedModel: String,
    onSelectConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSendMessage: (
        prompt: String,
        attachedBitmap: Bitmap?,
        attachedPdfBytes: ByteArray?,
        attachedText: String?,
        attachmentName: String?
    ) -> Unit,
    onRegenerateLast: () -> Unit,
    onEditUserMessage: (ChatMessage, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedDoc by remember { mutableStateOf<ExtractedDocument?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editDraftText by remember { mutableStateOf("") }
    var localAttachError by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val target = (messages.size + if (streamingText.isNotEmpty()) 1 else 0).coerceAtLeast(1) - 1
            listState.animateScrollToItem(target)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = DocumentExtractor.decodeBitmapFromUri(context, uri)
                if (bmp != null) {
                    attachedBitmap = bmp
                    attachedDoc = null
                    localAttachError = null
                } else {
                    localAttachError = "Unable to decode selected image."
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                DocumentExtractor.extractFromUri(context, uri)
                    .onSuccess { extracted ->
                        if (extracted.bitmap != null) {
                            attachedBitmap = extracted.bitmap
                            attachedDoc = null
                        } else {
                            attachedDoc = extracted
                            attachedBitmap = null
                        }
                        localAttachError = null
                    }
                    .onFailure { err ->
                        localAttachError = err.localizedMessage ?: "Failed to attach file."
                    }
            }
        }
    }

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
        ) {
            // Conversation switcher strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = activeConversationId == null,
                        onClick = onNewChat,
                        label = { Text(strings.newChat) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.newChat,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("new_chat_button")
                    )
                }
                items(conversations, key = { it.id }) { conv ->
                    val selected = conv.id == activeConversationId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectConversation(conv.id) },
                        label = {
                            Text(
                                text = conv.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 150.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeleteConversation(conv.id) }
                            )
                        }
                    )
                }
            }

            // Messages area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && !isGenerating) {
                    item {
                        PremiumCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            accentBorder = true
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanSecondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.emptyChatTitle,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.emptyChatSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${strings.selectModel}: $selectedModel",
                                style = MaterialTheme.typography.labelMedium,
                                color = VioletPrimary
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    val isLastModel = !isUser && messages.lastOrNull { it.role == "model" }?.id == msg.id
                    MessageBubble(
                        message = msg,
                        strings = strings,
                        isLastModel = isLastModel,
                        onCopy = { copyTextToClipboard(context, msg.content, strings.copied) },
                        onShare = { sharePlainText(context, msg.content) },
                        onRegenerate = if (isLastModel && !isGenerating) onRegenerateLast else null,
                        onEdit = if (isUser && !isGenerating) {
                            {
                                editingMessage = msg
                                editDraftText = msg.content
                            }
                        } else null
                    )
                }

                if (isGenerating) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceCard,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .border(1.dp, VioletPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanSecondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.thinking,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyanSecondary
                                    )
                                }
                                if (streamingText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = streamingText,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error banner
            val displayError = errorMessage ?: localAttachError
            if (!displayError.isNullOrBlank()) {
                Surface(
                    color = RoseError.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayError,
                        color = RoseError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Attachment preview
            if (attachedBitmap != null || attachedDoc != null) {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            attachedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = strings.attachImage,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.attachImage,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            attachedDoc?.let { doc ->
                                Icon(
                                    imageVector = Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    tint = CyanSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = {
                            attachedBitmap = null
                            attachedDoc = null
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.cancel
                            )
                        }
                    }
                }
            }

            // Composer bar
            Surface(
                color = SurfaceElevated,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("chat_attach_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = strings.attachImage,
                            tint = CyanSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            filePicker.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/plain",
                                    "text/markdown",
                                    "text/csv",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        },
                        modifier = Modifier.testTag("chat_attach_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = strings.attachFile,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = strings.chatPlaceholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = if (isGenerating) SurfaceCard else VioletPrimary,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = !isGenerating) {
                                val prompt = inputText
                                val bmp = attachedBitmap
                                val doc = attachedDoc
                                inputText = ""
                                attachedBitmap = null
                                attachedDoc = null
                                onSendMessage(
                                    prompt,
                                    bmp,
                                    doc?.pdfBytes,
                                    doc?.extractedText,
                                    doc?.fileName ?: if (bmp != null) "image.jpg" else null
                                )
                            }
                            .testTag("chat_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.send,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit user message dialog
    editingMessage?.let { targetMsg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text(strings.edit) },
            text = {
                OutlinedTextField(
                    value = editDraftText,
                    onValueChange = { editDraftText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = editDraftText
                        editingMessage = null
                        onEditUserMessage(targetMsg, updated)
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    strings: AppStrings,
    isLastModel: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: (() -> Unit)?,
    onEdit: (() -> Unit)?
) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) VioletDeep.copy(alpha = 0.85f) else SurfaceCard,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    1.dp,
                    if (isUser) VioletPrimary.copy(alpha = 0.5f) else BorderSubtle,
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.attachmentName.isNullOrBlank()) {
                    Surface(
                        color = ObsidianBlackFallback,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "📎 ${message.attachmentName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.modelId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = strings.copy,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = strings.share,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onEdit != null) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = strings.edit,
                                    modifier = Modifier.size(16.dp),
                                    tint = CyanSecondary
                                )
                            }
                        }
                        if (isLastModel && onRegenerate != null) {
                            IconButton(onClick = onRegenerate, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.regenerate,
                                    modifier = Modifier.size(16.dp),
                                    tint = CyanSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ObsidianBlackFallback = Color(0xFF0D0D16)
