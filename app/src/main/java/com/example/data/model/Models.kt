package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredLanguage: String = "ar",
    val preferredModel: String = "gemini-3.8-flash",
    val totalRequests: Long = 0L,
    val totalImagesGenerated: Long = 0L,
    val totalDocumentsAnalyzed: Long = 0L,
    val totalEmailsAssisted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val modelId: String = "gemini-3.8-flash",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val content: String = "",
    val modelId: String = "gemini-3.8-flash",
    val attachmentName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DocumentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val extractedTextPreview: String = "",
    val aiSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EmailDraft(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recipient: String = "",
    val subject: String = "",
    val body: String = "",
    val priority: String = "medium",
    val summary: String = "",
    val extractedTasks: List<String> = emptyList(),
    val status: String = "draft",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ToolHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val toolType: String = "writer",
    val prompt: String = "",
    val result: String = "",
    val modelId: String = "gemini-3.8-flash",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AiProviderType {
    GEMINI,
    OPENAI
}

data class AiModelOption(
    val id: String,
    val displayNameEn: String,
    val displayNameAr: String,
    val provider: AiProviderType,
    val badge: String
)

object AiModels {
    val chatModels = listOf(
        AiModelOption(
            id = "gemini-3.8-flash",
            displayNameEn = "Gemini 3.8 Flash",
            displayNameAr = "جيميني 3.8 فلاش",
            provider = AiProviderType.GEMINI,
            badge = "Default"
        ),
        AiModelOption(
            id = "gemini-3.5-flash-lite",
            displayNameEn = "Gemini 3.5 Flash-Lite",
            displayNameAr = "جيميني 3.5 لايت",
            provider = AiProviderType.GEMINI,
            badge = "Fast"
        ),
        AiModelOption(
            id = "gemini-4-argon",
            displayNameEn = "Gemini 4 Argon",
            displayNameAr = "جيميني 4 أرجون",
            provider = AiProviderType.GEMINI,
            badge = "Pro"
        ),
        AiModelOption(
            id = "gpt-4o-mini",
            displayNameEn = "OpenAI GPT-4o Mini",
            displayNameAr = "أوبن إيه آي GPT-4o Mini",
            provider = AiProviderType.OPENAI,
            badge = "OpenAI"
        ),
        AiModelOption(
            id = "gpt-4o",
            displayNameEn = "OpenAI GPT-4o",
            displayNameAr = "أوبن إيه آي GPT-4o",
            provider = AiProviderType.OPENAI,
            badge = "OpenAI Pro"
        )
    )

    val imageModels = listOf(
        "gemini-nano-banana-2.1",
        "gemini-3-pro-image"
    )
}

data class EmailAnalysisResult(
    val priority: String,
    val summary: String,
    val suggestedReplySubject: String,
    val suggestedReplyBody: String,
    val extractedTasks: List<String>
)

data class WebSearchSource(
    val title: String,
    val snippet: String,
    val url: String
)
