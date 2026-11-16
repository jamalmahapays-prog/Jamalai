package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DocumentRecord
import com.example.data.model.EmailDraft
import com.example.data.model.ToolHistoryEntry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MyAiRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- User Profile & Settings ---
    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        photoUrl: String?,
        defaultLanguage: String = "ar"
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = userDoc(uid)
        try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName.take(120),
                    "email" to email.take(200),
                    "preferredLanguage" to if (defaultLanguage in listOf("ar", "en")) defaultLanguage else "ar",
                    "preferredModel" to "gemini-3.8-flash",
                    "totalRequests" to 0L,
                    "totalImagesGenerated" to 0L,
                    "totalDocumentsAnalyzed" to 0L,
                    "totalEmailsAssisted" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    payload["photoUrl"] = photoUrl.take(500)
                }
                ref.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val ref = userDoc(userId)
        emitAll(
            ref.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowException(error, OperationType.GET, ref.path, userId)
                    throw error
                }
        )
    }

    suspend fun updateLanguage(language: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeLang = if (language == "en") "en" else "ar"
        val ref = userDoc(uid)
        try {
            ref.update(
                mapOf(
                    "preferredLanguage" to safeLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun updatePreferredModel(modelId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = userDoc(uid)
        try {
            ref.update(
                mapOf(
                    "preferredModel" to modelId.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun incrementUsage(
        requests: Long = 1L,
        images: Long = 0L,
        documents: Long = 0L,
        emails: Long = 0L
    ) {
        val uid = auth.currentUser?.uid ?: return
        val ref = userDoc(uid)
        try {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (requests > 0) updates["totalRequests"] = FieldValue.increment(requests)
            if (images > 0) updates["totalImagesGenerated"] = FieldValue.increment(images)
            if (documents > 0) updates["totalDocumentsAnalyzed"] = FieldValue.increment(documents)
            if (emails > 0) updates["totalEmailsAssisted"] = FieldValue.increment(emails)
            ref.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
        }
    }

    // --- Conversations & Chat Messages ---
    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val path = "users/$userId/conversations"
        emitAll(
            userDoc(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun createConversation(title: String, modelId: String): Result<String> = runCatching {
        val uid = requireUserId()
        val col = userDoc(uid).collection("conversations")
        val docRef = col.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "New Chat" }.take(160),
            "modelId" to modelId.take(60),
            "lastMessagePreview" to "",
            "messageCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val convRef = userDoc(uid).collection("conversations").document(conversationId)
        try {
            val messages = convRef.collection("messages").get().await()
            if (!messages.isEmpty) {
                val batch = db.batch()
                messages.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            convRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path)
            throw e
        }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/conversations/$conversationId/messages"
        emitAll(
            userDoc(userId).collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        modelId: String,
        attachmentName: String? = null,
        updateTitleIfFirst: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convRef = userDoc(uid).collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()
        val safeContent = content.take(20000).ifBlank { "..." }
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "conversationId" to conversationId,
            "role" to if (role == "model") "model" else "user",
            "content" to safeContent,
            "modelId" to modelId.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!attachmentName.isNullOrBlank()) {
            payload["attachmentName"] = attachmentName.take(200)
        }
        try {
            msgRef.set(payload).await()
            val convUpdates = mutableMapOf<String, Any>(
                "lastMessagePreview" to safeContent.take(300),
                "messageCount" to FieldValue.increment(1L),
                "modelId" to modelId.take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!updateTitleIfFirst.isNullOrBlank()) {
                convUpdates["title"] = updateTitleIfFirst.take(160)
            }
            convRef.update(convUpdates).await()
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun updateMessageContent(
        conversationId: String,
        messageId: String,
        newContent: String,
        modelId: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val convRef = userDoc(uid).collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document(messageId)
        val safeContent = newContent.take(20000).ifBlank { "..." }
        try {
            msgRef.update(
                mapOf(
                    "content" to safeContent,
                    "modelId" to modelId.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.update(
                mapOf(
                    "lastMessagePreview" to safeContent.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    // --- Documents ---
    fun observeDocuments(userId: String): Flow<List<DocumentRecord>> = flow {
        val path = "users/$userId/documents"
        emitAll(
            userDoc(userId).collection("documents")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DocumentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveDocumentRecord(
        fileName: String,
        mimeType: String,
        fileSizeBytes: Long,
        extractedTextPreview: String,
        aiSummary: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("documents").document()
        val payload = mapOf(
            "userId" to uid,
            "fileName" to fileName.take(200),
            "mimeType" to mimeType.take(100),
            "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 20971520L),
            "extractedTextPreview" to extractedTextPreview.take(15000),
            "aiSummary" to aiSummary.take(15000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("documents").document(documentId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- Email Drafts ---
    fun observeEmailDrafts(userId: String): Flow<List<EmailDraft>> = flow {
        val path = "users/$userId/emailDrafts"
        emitAll(
            userDoc(userId).collection("emailDrafts")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        EmailDraft::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveEmailDraft(
        recipient: String,
        subject: String,
        body: String,
        priority: String,
        summary: String,
        extractedTasks: List<String>,
        status: String = "draft"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("emailDrafts").document()
        val payload = mapOf(
            "userId" to uid,
            "recipient" to recipient.take(200),
            "subject" to subject.take(300),
            "body" to body.take(15000),
            "priority" to if (priority in listOf("high", "medium", "low")) priority else "medium",
            "summary" to summary.take(5000),
            "extractedTasks" to extractedTasks.take(20).map { it.take(300) },
            "status" to if (status in listOf("draft", "analyzed", "dispatched")) status else "draft",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markEmailDispatched(draft: EmailDraft): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(draft.id.isNotBlank()) { "Draft ID must not be blank" }
        val ref = userDoc(uid).collection("emailDrafts").document(draft.id)
        try {
            ref.update(
                mapOf(
                    "status" to "dispatched",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun deleteEmailDraft(draftId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = userDoc(uid).collection("emailDrafts").document(draftId)
        try {
            ref.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            throw e
        }
    }

    // --- Tool History ---
    fun observeToolHistory(userId: String): Flow<List<ToolHistoryEntry>> = flow {
        val path = "users/$userId/toolHistory"
        emitAll(
            userDoc(userId).collection("toolHistory")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ToolHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowException(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveToolHistory(
        toolType: String,
        prompt: String,
        result: String,
        modelId: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("toolHistory").document()
        val safeType = if (toolType in listOf("writer", "translator", "summarizer", "coding", "search", "voice")) {
            toolType
        } else "writer"
        val payload = mapOf(
            "userId" to uid,
            "toolType" to safeType,
            "prompt" to prompt.take(8000).ifBlank { "Prompt" },
            "result" to result.take(20000),
            "modelId" to modelId.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    private fun handleFlowException(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
