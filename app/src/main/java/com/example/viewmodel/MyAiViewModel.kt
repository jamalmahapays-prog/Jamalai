package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiService
import com.example.data.ai.StudioAspectRatio
import com.example.data.files.ExtractedDocument
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DocumentRecord
import com.example.data.model.EmailAnalysisResult
import com.example.data.model.EmailDraft
import com.example.data.model.ToolHistoryEntry
import com.example.data.model.UserProfile
import com.example.data.model.WebSearchSource
import com.example.data.repository.MyAiRepository
import com.example.data.search.WebSearchClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "MyAiViewModel"
private const val STOP_TIMEOUT_MS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyAiViewModel(
    private val repository: MyAiRepository,
    private val aiService: AiService,
    private val webSearchClient: WebSearchClient,
    val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val userPhotoUrl: String?,
    initialLanguage: String
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(initialLanguage)
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3.8-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    val isOpenAiConfigured: Boolean = aiService.isOpenAiConfigured()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndLog("profile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId)
            .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapAndLog("conversations", error))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeMessages(currentUserId, convId)
                    .catch { error ->
                        unwrapAndLog("messages", error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val documentsState: StateFlow<UiState<List<DocumentRecord>>> =
        repository.observeDocuments(currentUserId)
            .map<List<DocumentRecord>, UiState<List<DocumentRecord>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapAndLog("documents", error))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val emailDraftsState: StateFlow<UiState<List<EmailDraft>>> =
        repository.observeEmailDrafts(currentUserId)
            .map<List<EmailDraft>, UiState<List<EmailDraft>>> { UiState.Success(it) }
            .catch { error -> emit(UiState.Error(unwrapAndLog("emailDrafts", error))) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val toolHistory: StateFlow<List<ToolHistoryEntry>> =
        repository.observeToolHistory(currentUserId)
            .catch { error ->
                unwrapAndLog("toolHistory", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Chat transient state
    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _chatStreamingText = MutableStateFlow("")
    val chatStreamingText: StateFlow<String> = _chatStreamingText.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    // Tool transient state (Writer, Translator, Summarizer, Coding, Web Search, Voice)
    private val _toolLoading = MutableStateFlow(false)
    val toolLoading: StateFlow<Boolean> = _toolLoading.asStateFlow()

    private val _toolOutput = MutableStateFlow("")
    val toolOutput: StateFlow<String> = _toolOutput.asStateFlow()

    private val _toolError = MutableStateFlow<String?>(null)
    val toolError: StateFlow<String?> = _toolError.asStateFlow()

    private val _webSearchSources = MutableStateFlow<List<WebSearchSource>>(emptyList())
    val webSearchSources: StateFlow<List<WebSearchSource>> = _webSearchSources.asStateFlow()

    // Image Studio state
    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    // Email Assistant transient state
    private val _emailLoading = MutableStateFlow(false)
    val emailLoading: StateFlow<Boolean> = _emailLoading.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _latestEmailAnalysis = MutableStateFlow<EmailAnalysisResult?>(null)
    val latestEmailAnalysis: StateFlow<EmailAnalysisResult?> = _latestEmailAnalysis.asStateFlow()

    // Document Assistant transient state
    private val _docLoading = MutableStateFlow(false)
    val docLoading: StateFlow<Boolean> = _docLoading.asStateFlow()

    private val _docError = MutableStateFlow<String?>(null)
    val docError: StateFlow<String?> = _docError.asStateFlow()

    private val _docQaAnswer = MutableStateFlow<String?>(null)
    val docQaAnswer: StateFlow<String?> = _docQaAnswer.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl,
                defaultLanguage = initialLanguage
            )
            repository.observeUserProfile(currentUserId).collect { profile ->
                if (profile != null) {
                    if (profile.preferredLanguage in listOf("ar", "en")) {
                        _selectedLanguage.value = profile.preferredLanguage
                    }
                    if (profile.preferredModel.isNotBlank()) {
                        _selectedModel.value = profile.preferredModel
                    }
                }
            }
        }
    }

    fun setLanguage(lang: String) {
        val safe = if (lang == "en") "en" else "ar"
        _selectedLanguage.value = safe
        viewModelScope.launch {
            repository.updateLanguage(safe)
        }
    }

    fun setModel(modelId: String) {
        _selectedModel.value = modelId
        viewModelScope.launch {
            repository.updatePreferredModel(modelId)
        }
    }

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        _chatError.value = null
    }

    fun startNewConversation() {
        _activeConversationId.value = null
        _chatStreamingText.value = ""
        _chatError.value = null
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
        }
    }

    fun sendChatMessage(
        prompt: String,
        attachedBitmap: Bitmap? = null,
        attachedPdfBytes: ByteArray? = null,
        attachedTextContent: String? = null,
        attachmentName: String? = null
    ) {
        if (prompt.isBlank() && attachedBitmap == null && attachedPdfBytes == null && attachedTextContent.isNullOrBlank()) {
            return
        }
        val cleanPrompt = prompt.trim().ifBlank { "Analyze the attached file." }
        val modelId = _selectedModel.value

        viewModelScope.launch {
            _isChatGenerating.value = true
            _chatError.value = null
            _chatStreamingText.value = ""

            try {
                val convId = _activeConversationId.value ?: run {
                    val createdId = repository.createConversation(
                        title = cleanPrompt.take(50),
                        modelId = modelId
                    ).getOrThrow()
                    _activeConversationId.value = createdId
                    createdId
                }

                val historyBeforeSend = activeMessages.value
                val isFirstMessage = historyBeforeSend.isEmpty()

                repository.addMessage(
                    conversationId = convId,
                    role = "user",
                    content = cleanPrompt,
                    modelId = modelId,
                    attachmentName = attachmentName,
                    updateTitleIfFirst = if (isFirstMessage) cleanPrompt.take(50) else null
                ).getOrThrow()

                var finalReply = ""
                aiService.streamChatMessage(
                    history = historyBeforeSend,
                    userPrompt = cleanPrompt,
                    modelId = modelId,
                    attachedBitmap = attachedBitmap,
                    attachedPdfBytes = attachedPdfBytes,
                    attachedTextContent = attachedTextContent
                ).collect { partial ->
                    finalReply = partial
                    _chatStreamingText.value = partial
                }

                if (finalReply.isNotBlank()) {
                    repository.addMessage(
                        conversationId = convId,
                        role = "model",
                        content = finalReply,
                        modelId = modelId
                    ).getOrThrow()
                    repository.incrementUsage(requests = 1L)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _chatError.value = "Request timed out after 60 seconds. Please try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                _chatError.value = e.localizedMessage ?: "Failed to generate response."
            } finally {
                _isChatGenerating.value = false
                _chatStreamingText.value = ""
            }
        }
    }

    fun regenerateLastResponse() {
        val convId = _activeConversationId.value ?: return
        val msgs = activeMessages.value
        val lastUser = msgs.lastOrNull { it.role == "user" } ?: return
        val lastModel = msgs.lastOrNull { it.role == "model" }
        val modelId = _selectedModel.value

        viewModelScope.launch {
            _isChatGenerating.value = true
            _chatError.value = null
            _chatStreamingText.value = ""

            try {
                val historyUpToUser = msgs.takeWhile { it.id != lastUser.id }
                var finalReply = ""
                aiService.streamChatMessage(
                    history = historyUpToUser,
                    userPrompt = lastUser.content,
                    modelId = modelId
                ).collect { partial ->
                    finalReply = partial
                    _chatStreamingText.value = partial
                }

                if (finalReply.isNotBlank()) {
                    if (lastModel != null && msgs.lastOrNull()?.id == lastModel.id) {
                        repository.updateMessageContent(convId, lastModel.id, finalReply, modelId)
                    } else {
                        repository.addMessage(convId, "model", finalReply, modelId)
                    }
                    repository.incrementUsage(requests = 1L)
                }
            } catch (e: TimeoutCancellationException) {
                _chatError.value = "Regeneration timed out after 60 seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Regenerate failed", e)
                _chatError.value = e.localizedMessage ?: "Failed to regenerate."
            } finally {
                _isChatGenerating.value = false
                _chatStreamingText.value = ""
            }
        }
    }

    fun editAndResendUserMessage(message: ChatMessage, newText: String) {
        val convId = _activeConversationId.value ?: return
        val cleanText = newText.trim()
        if (cleanText.isBlank()) return
        val modelId = _selectedModel.value

        viewModelScope.launch {
            _isChatGenerating.value = true
            _chatError.value = null
            _chatStreamingText.value = ""

            try {
                repository.updateMessageContent(convId, message.id, cleanText, modelId).getOrThrow()
                val historyUpToEdit = activeMessages.value.takeWhile { it.id != message.id }
                var finalReply = ""
                aiService.streamChatMessage(
                    history = historyUpToEdit,
                    userPrompt = cleanText,
                    modelId = modelId
                ).collect { partial ->
                    finalReply = partial
                    _chatStreamingText.value = partial
                }
                if (finalReply.isNotBlank()) {
                    repository.addMessage(convId, "model", finalReply, modelId)
                    repository.incrementUsage(requests = 1L)
                }
            } catch (e: TimeoutCancellationException) {
                _chatError.value = "Request timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Edit message failed", e)
                _chatError.value = e.localizedMessage ?: "Edit failed."
            } finally {
                _isChatGenerating.value = false
                _chatStreamingText.value = ""
            }
        }
    }

    // --- General AI Tools (Writer, Translator, Summarizer, Coding, Voice) ---
    fun runToolStream(
        toolType: String,
        prompt: String,
        systemInstruction: String,
        overrideModelId: String? = null
    ) {
        if (prompt.isBlank()) return
        val modelId = overrideModelId ?: _selectedModel.value
        viewModelScope.launch {
            _toolLoading.value = true
            _toolError.value = null
            _toolOutput.value = ""
            try {
                var result = ""
                aiService.streamPrompt(
                    prompt = prompt,
                    modelId = modelId,
                    systemInstruction = systemInstruction
                ).collect { partial ->
                    result = partial
                    _toolOutput.value = partial
                }
                if (result.isNotBlank()) {
                    repository.saveToolHistory(toolType, prompt, result, modelId)
                    repository.incrementUsage(requests = 1L)
                }
            } catch (e: TimeoutCancellationException) {
                _toolError.value = "Generation timed out after 60 seconds."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tool $toolType failed", e)
                _toolError.value = e.localizedMessage ?: "Tool execution failed."
            } finally {
                _toolLoading.value = false
            }
        }
    }

    fun clearToolOutput() {
        _toolOutput.value = ""
        _toolError.value = null
        _webSearchSources.value = emptyList()
    }

    // --- Web Search ---
    fun runWebSearch(query: String) {
        if (query.isBlank()) return
        val isArabic = _selectedLanguage.value == "ar"
        val modelId = _selectedModel.value
        viewModelScope.launch {
            _toolLoading.value = true
            _toolError.value = null
            _toolOutput.value = ""
            _webSearchSources.value = emptyList()
            try {
                val sources = withTimeout(30_000L) {
                    webSearchClient.searchLiveSources(query, preferArabic = isArabic)
                }
                _webSearchSources.value = sources

                val sourcesContext = if (sources.isEmpty()) {
                    "No external search snippets returned; answer carefully and note that live web snippets were limited."
                } else {
                    sources.mapIndexed { idx, src ->
                        "[${idx + 1}] ${src.title}\nURL: ${src.url}\nSnippet: ${src.snippet}"
                    }.joinToString("\n\n")
                }

                val langPrompt = if (isArabic) "Summarize the live sources and answer clearly in Arabic with numbered citations [1], [2]." else "Summarize the live sources and answer clearly in English with numbered citations [1], [2]."
                val prompt = "Query: $query\n\nLive Web Sources:\n$sourcesContext\n\n$langPrompt"

                var result = ""
                aiService.streamPrompt(prompt, modelId).collect { partial ->
                    result = partial
                    _toolOutput.value = partial
                }
                if (result.isNotBlank()) {
                    repository.saveToolHistory("search", query, result, modelId)
                    repository.incrementUsage(requests = 1L)
                }
            } catch (e: TimeoutCancellationException) {
                _toolError.value = "Web search timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Web search failed", e)
                _toolError.value = e.localizedMessage ?: "Web search failed."
            } finally {
                _toolLoading.value = false
            }
        }
    }

    // --- Image Studio ---
    fun generateOrEditImage(
        prompt: String,
        modelId: String,
        aspectRatio: StudioAspectRatio,
        inputBitmap: Bitmap?
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _imageGenState.value = ImageGenUiState.Loading
            try {
                val bitmap = aiService.generateOrEditImage(
                    prompt = prompt,
                    modelId = modelId,
                    aspectRatio = aspectRatio,
                    inputBitmap = inputBitmap
                )
                _imageGenState.value = ImageGenUiState.Success(bitmap)
                repository.incrementUsage(requests = 1L, images = 1L)
            } catch (e: TimeoutCancellationException) {
                _imageGenState.value = ImageGenUiState.Error("Image generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageGenState.value = ImageGenUiState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    // --- Smart Email Assistant ---
    fun analyzeAndSaveEmail(rawEmailText: String) {
        if (rawEmailText.isBlank()) return
        val isArabic = _selectedLanguage.value == "ar"
        viewModelScope.launch {
            _emailLoading.value = true
            _emailError.value = null
            try {
                val analysis = aiService.analyzeEmailStructured(rawEmailText, isArabic)
                _latestEmailAnalysis.value = analysis
                repository.saveEmailDraft(
                    recipient = "",
                    subject = analysis.suggestedReplySubject,
                    body = analysis.suggestedReplyBody,
                    priority = analysis.priority,
                    summary = analysis.summary,
                    extractedTasks = analysis.extractedTasks,
                    status = "analyzed"
                ).getOrThrow()
                repository.incrementUsage(requests = 1L, emails = 1L)
            } catch (e: TimeoutCancellationException) {
                _emailError.value = "Email analysis timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Email analysis failed", e)
                _emailError.value = e.localizedMessage ?: "Failed to analyze email."
            } finally {
                _emailLoading.value = false
            }
        }
    }

    fun draftEmailWithAi(
        goal: String,
        recipient: String,
        onDraftReady: (subject: String, body: String) -> Unit
    ) {
        if (goal.isBlank()) return
        val isArabic = _selectedLanguage.value == "ar"
        val modelId = _selectedModel.value
        viewModelScope.launch {
            _emailLoading.value = true
            _emailError.value = null
            try {
                val instruction = if (isArabic) {
                    "Write a clear, professional email in Arabic. Format the first line as 'Subject: <subject>' followed by a blank line and the body."
                } else {
                    "Write a clear, professional email in English. Format the first line as 'Subject: <subject>' followed by a blank line and the body."
                }
                var full = ""
                aiService.streamPrompt(
                    prompt = "Recipient: $recipient\nGoal: $goal",
                    modelId = modelId,
                    systemInstruction = instruction
                ).collect { full = it }

                val lines = full.lines()
                val firstSubjectLine = lines.firstOrNull { it.startsWith("Subject:", ignoreCase = true) }
                val subject = firstSubjectLine?.substringAfter(":")?.trim().orEmpty().ifBlank { goal.take(60) }
                val body = if (firstSubjectLine != null) {
                    lines.dropWhile { it != firstSubjectLine }.drop(1).joinToString("\n").trim()
                } else {
                    full.trim()
                }
                repository.saveEmailDraft(
                    recipient = recipient,
                    subject = subject,
                    body = body,
                    priority = "medium",
                    summary = goal.take(200),
                    extractedTasks = emptyList(),
                    status = "draft"
                )
                repository.incrementUsage(requests = 1L, emails = 1L)
                onDraftReady(subject, body)
            } catch (e: TimeoutCancellationException) {
                _emailError.value = "Email drafting timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Email draft failed", e)
                _emailError.value = e.localizedMessage ?: "Failed to draft email."
            } finally {
                _emailLoading.value = false
            }
        }
    }

    fun markEmailDispatched(draft: EmailDraft) {
        viewModelScope.launch {
            repository.markEmailDispatched(draft)
        }
    }

    fun deleteEmailDraft(draftId: String) {
        viewModelScope.launch {
            repository.deleteEmailDraft(draftId)
        }
    }

    // --- Documents ---
    fun analyzeAndSaveDocument(doc: ExtractedDocument) {
        val isArabic = _selectedLanguage.value == "ar"
        viewModelScope.launch {
            _docLoading.value = true
            _docError.value = null
            try {
                val summary = aiService.analyzeDocument(
                    fileName = doc.fileName,
                    mimeType = doc.mimeType,
                    extractedText = doc.extractedText,
                    pdfBytes = doc.pdfBytes,
                    bitmap = doc.bitmap,
                    question = null,
                    isArabic = isArabic
                )
                repository.saveDocumentRecord(
                    fileName = doc.fileName,
                    mimeType = doc.mimeType,
                    fileSizeBytes = doc.sizeBytes,
                    extractedTextPreview = doc.extractedText.ifBlank { "[Binary/Multimodal: ${doc.mimeType}]" },
                    aiSummary = summary
                ).getOrThrow()
                repository.incrementUsage(requests = 1L, documents = 1L)
            } catch (e: TimeoutCancellationException) {
                _docError.value = "Document analysis timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Document analysis failed", e)
                _docError.value = e.localizedMessage ?: "Failed to analyze document."
            } finally {
                _docLoading.value = false
            }
        }
    }

    fun askDocumentQuestion(record: DocumentRecord, question: String) {
        if (question.isBlank()) return
        val isArabic = _selectedLanguage.value == "ar"
        viewModelScope.launch {
            _docLoading.value = true
            _docError.value = null
            _docQaAnswer.value = null
            try {
                val contextText = buildString {
                    append("File: ${record.fileName}\n")
                    append("Summary:\n${record.aiSummary}\n\n")
                    if (record.extractedTextPreview.isNotBlank()) {
                        append("Content:\n${record.extractedTextPreview}")
                    }
                }
                val answer = aiService.analyzeDocument(
                    fileName = record.fileName,
                    mimeType = record.mimeType,
                    extractedText = contextText,
                    pdfBytes = null,
                    bitmap = null,
                    question = question,
                    isArabic = isArabic
                )
                _docQaAnswer.value = answer
                repository.incrementUsage(requests = 1L)
            } catch (e: TimeoutCancellationException) {
                _docError.value = "Question timed out."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Document Q&A failed", e)
                _docError.value = e.localizedMessage ?: "Failed to answer question."
            } finally {
                _docLoading.value = false
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
        }
    }

    private fun unwrapAndLog(streamName: String, error: Throwable): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        return rootError.localizedMessage ?: "Failed to load $streamName"
    }

    companion object {
        fun provideFactory(
            databaseId: String,
            userId: String,
            displayName: String,
            email: String,
            photoUrl: String?,
            initialLanguage: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = FirebaseFirestore.getInstance(databaseId)
                MyAiViewModel(
                    repository = MyAiRepository(db),
                    aiService = AiService(),
                    webSearchClient = WebSearchClient(),
                    currentUserId = userId,
                    userDisplayName = displayName,
                    userEmail = email,
                    userPhotoUrl = photoUrl,
                    initialLanguage = initialLanguage
                )
            }
        }
    }
}
