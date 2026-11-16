package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyAiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_createConversation_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MyAiRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Alice", ALICE_EMAIL, null, "en")
        }
        assertTrue(profileRes.isSuccess)

        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConversation("Architecture Chat", "gemini-3.8-flash").getOrThrow()
        }
        assertTrue(convId.isNotBlank())

        val msgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMessage(convId, "user", "Explain Kotlin Coroutines", "gemini-3.8-flash")
        }
        assertTrue(msgRes.isSuccess)

        val conversations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConversations(uid).first { it.any { c -> c.id == convId } }
        }
        assertTrue(conversations.any { it.id == convId })
    }

    @Test
    fun saveDocumentAndEmailDraft_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = MyAiRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Alice", ALICE_EMAIL, null, "ar").getOrThrow()
        }

        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveDocumentRecord(
                fileName = "specs.txt",
                mimeType = "text/plain",
                fileSizeBytes = 1024L,
                extractedTextPreview = "Project specification",
                aiSummary = "Summary of project specification"
            ).getOrThrow()
        }
        assertTrue(docId.isNotBlank())

        val draftId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveEmailDraft(
                recipient = "client@example.com",
                subject = "Update",
                body = "Here is the update",
                priority = "high",
                summary = "Weekly update",
                extractedTasks = listOf("Send deck"),
                status = "analyzed"
            ).getOrThrow()
        }
        assertTrue(draftId.isNotBlank())

        val docs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDocuments(uid).first { it.any { d -> d.id == docId } }
        }
        assertEquals(1, docs.count { it.id == docId })
    }

    @Test
    fun observeConversations_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MyAiRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("Alice", ALICE_EMAIL, null, "en").getOrThrow()
            aliceRepo.createConversation("Alice Secret", "gemini-3.8-flash").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MyAiRepository(firestore)
        var captured: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeConversations(aliceUid).first()
            }
        } catch (e: Exception) {
            captured = e
        }
        val firestoreEx = checkNotNull(captured?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for cross-user access, got $captured"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun observeConversations_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MyAiRepository(firestore)
        var captured: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeConversations("some_uid_123").first()
            }
        } catch (e: Exception) {
            captured = e
        }
        val firestoreEx = checkNotNull(captured?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for unauthenticated access, got $captured"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_myai@test.com"
        const val BOB_EMAIL = "bob_myai@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
