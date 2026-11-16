package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiModelOption
import com.example.data.model.AiModels
import com.example.data.model.AiProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.EmailAnalysisResult
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AiService"

enum class StudioAspectRatio(val label: String) {
    SQUARE("1:1"),
    LANDSCAPE("16:9"),
    PORTRAIT("9:16"),
    CLASSIC("4:3"),
    TALL("3:4")
}

class AiService {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val modelCache = mutableMapOf<String, GenerativeModel>()

    private fun getGeminiModel(modelId: String): GenerativeModel {
        val safeId = if (modelId in listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon")) {
            modelId
        } else {
            "gemini-3.8-flash"
        }
        return modelCache.getOrPut(safeId) {
            Firebase.ai.generativeModel(modelName = safeId)
        }
    }

    private val emailAnalysisModel by lazy {
        val emailSchema = Schema.obj(
            properties = mapOf(
                "priority" to Schema.enumeration(
                    values = listOf("high", "medium", "low"),
                    description = "Email urgency level"
                ),
                "summary" to Schema.string(description = "Concise summary of the email"),
                "suggestedReplySubject" to Schema.string(description = "Suggested reply email subject"),
                "suggestedReplyBody" to Schema.string(description = "Suggested professional reply body"),
                "extractedTasks" to Schema.array(
                    items = Schema.string(),
                    description = "Actionable tasks, deadlines, or meetings extracted from the email"
                )
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = emailSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    fun isOpenAiConfigured(): Boolean {
        val key = BuildConfig.OPENAI_API_KEY
        return key.isNotBlank() && key != "MY_OPENAI_API_KEY"
    }

    fun findModelOption(modelId: String): AiModelOption {
        return AiModels.chatModels.firstOrNull { it.id == modelId } ?: AiModels.chatModels.first()
    }

    fun streamPrompt(
        prompt: String,
        modelId: String,
        systemInstruction: String? = null
    ): Flow<String> = flow {
        val option = findModelOption(modelId)
        val fullPrompt = if (!systemInstruction.isNullOrBlank()) {
            "$systemInstruction\n\n$prompt"
        } else {
            prompt
        }

        if (option.provider == AiProviderType.OPENAI) {
            val responseText = callOpenAiChat(
                modelId = option.id,
                messages = listOf("user" to fullPrompt)
            )
            emit(responseText)
            return@flow
        }

        val model = getGeminiModel(option.id)
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Model returned an empty response.")
        }
    }.flowOn(Dispatchers.IO)

    fun streamChatMessage(
        history: List<ChatMessage>,
        userPrompt: String,
        modelId: String,
        attachedBitmap: Bitmap? = null,
        attachedPdfBytes: ByteArray? = null,
        attachedTextContent: String? = null
    ): Flow<String> = flow {
        val option = findModelOption(modelId)

        if (option.provider == AiProviderType.OPENAI) {
            val openAiMsgs = history.takeLast(12).map { msg ->
                (if (msg.role == "model") "assistant" else "user") to msg.content
            } + ("user" to buildString {
                append(userPrompt)
                if (!attachedTextContent.isNullOrBlank()) {
                    append("\n\n[Attached File Content]:\n")
                    append(attachedTextContent.take(12000))
                }
            })
            val result = callOpenAiChat(option.id, openAiMsgs)
            emit(result)
            return@flow
        }

        val model = getGeminiModel(option.id)
        val geminiHistory = history.takeLast(16).map { msg ->
            content(role = if (msg.role == "model") "model" else "user") {
                text(msg.content)
            }
        }
        val chat = model.startChat(history = geminiHistory)

        val requestContent = content("user") {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            if (attachedPdfBytes != null) {
                inlineData(attachedPdfBytes, "application/pdf")
            }
            val finalPrompt = buildString {
                append(userPrompt)
                if (!attachedTextContent.isNullOrBlank()) {
                    append("\n\n[Attached Document Text]:\n")
                    append(attachedTextContent.take(14000))
                }
            }
            text(finalPrompt)
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(requestContent).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Model returned an empty response.")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun analyzeDocument(
        fileName: String,
        mimeType: String,
        extractedText: String,
        pdfBytes: ByteArray?,
        bitmap: Bitmap?,
        question: String?,
        isArabic: Boolean
    ): String = withContext(Dispatchers.IO) {
        val model = getGeminiModel("gemini-3.8-flash")
        val langNote = if (isArabic) "Respond clearly in Arabic." else "Respond clearly in English."
        val instruction = if (question.isNullOrBlank()) {
            "Analyze the attached document ($fileName). Provide: 1) Concise summary, 2) Key points, 3) Action items if any. $langNote"
        } else {
            "Answer this question based strictly on the document ($fileName): $question\n$langNote"
        }

        val request = content {
            if (bitmap != null) {
                image(bitmap)
            } else if (pdfBytes != null && mimeType.contains("pdf", ignoreCase = true)) {
                inlineData(pdfBytes, "application/pdf")
            }
            val body = if (extractedText.isNotBlank()) {
                "$instruction\n\nDocument Content:\n${extractedText.take(15000)}"
            } else {
                instruction
            }
            text(body)
        }

        val response = withTimeout(60_000L) {
            model.generateContent(request)
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Document analysis returned empty text.")
    }

    suspend fun analyzeEmailStructured(
        emailContent: String,
        isArabic: Boolean
    ): EmailAnalysisResult = withContext(Dispatchers.IO) {
        val langHint = if (isArabic) "Write summary, suggestedReplySubject, suggestedReplyBody, and extractedTasks in Arabic." else "Write summary, suggestedReplySubject, suggestedReplyBody, and extractedTasks in English."
        val prompt = "Analyze this email thread, classify priority (high/medium/low), summarize key points, extract actionable tasks/deadlines, and draft a polite reply. $langHint\n\nEmail:\n${emailContent.take(12000)}"

        val response = withTimeout(60_000L) {
            emailAnalysisModel.generateContent(prompt)
        }
        val jsonString = response.text
            ?: throw IllegalStateException("Email analyzer returned empty JSON.")
        val json = JSONObject(jsonString)
        val tasksArr = json.optJSONArray("extractedTasks")
        val tasks = buildList {
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val item = tasksArr.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }
        EmailAnalysisResult(
            priority = json.optString("priority", "medium").lowercase().let {
                if (it in listOf("high", "medium", "low")) it else "medium"
            },
            summary = json.optString("summary", ""),
            suggestedReplySubject = json.optString("suggestedReplySubject", "Re: Follow-up"),
            suggestedReplyBody = json.optString("suggestedReplyBody", ""),
            extractedTasks = tasks
        )
    }

    suspend fun generateOrEditImage(
        prompt: String,
        modelId: String = "gemini-nano-banana-2.1",
        aspectRatio: StudioAspectRatio = StudioAspectRatio.SQUARE,
        inputBitmap: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val safeModelId = if (modelId in AiModels.imageModels) modelId else "gemini-nano-banana-2.1"
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            if (aspectRatio != StudioAspectRatio.SQUARE) {
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(
                        when (aspectRatio) {
                            StudioAspectRatio.SQUARE -> AspectRatio.SQUARE_1x1
                            StudioAspectRatio.LANDSCAPE -> AspectRatio.LANDSCAPE_16x9
                            StudioAspectRatio.PORTRAIT -> AspectRatio.PORTRAIT_9x16
                            StudioAspectRatio.CLASSIC -> AspectRatio.LANDSCAPE_4x3
                            StudioAspectRatio.TALL -> AspectRatio.PORTRAIT_3x4
                        }
                    )
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = safeModelId,
            generationConfig = config
        )

        val response = withTimeout(60_000L) {
            if (inputBitmap != null) {
                imageModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(prompt)
                    }
                )
            } else {
                imageModel.generateContent(prompt)
            }
        }

        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()

        imagePart?.image ?: throw IllegalStateException("No image was returned by the model.")
    }

    private suspend fun callOpenAiChat(
        modelId: String,
        messages: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        if (!isOpenAiConfigured()) {
            throw IllegalStateException(
                "OpenAI API key is not configured. Add OPENAI_API_KEY in AI Studio Secrets or switch to a Gemini model."
            )
        }
        val msgArray = JSONArray().apply {
            messages.forEach { (role, text) ->
                put(JSONObject().apply {
                    put("role", role)
                    put("content", text)
                })
            }
        }
        val payload = JSONObject().apply {
            put("model", modelId)
            put("messages", msgArray)
        }
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        withTimeout(60_000L) {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenAI HTTP ${response.code}: $bodyStr")
                    throw IllegalStateException("OpenAI error (${response.code}): ${bodyStr.take(200)}")
                }
                val json = JSONObject(bodyStr)
                val choices = json.getJSONArray("choices")
                if (choices.length() == 0) {
                    throw IllegalStateException("OpenAI returned no choices.")
                }
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
        }
    }
}
