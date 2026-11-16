package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.ui.unit.dp
import com.example.core.AppStrings
import com.example.data.files.DocumentExtractor
import com.example.ui.components.AiOutputCard
import com.example.ui.components.PremiumCard
import com.example.ui.components.copyTextToClipboard
import com.example.ui.components.sharePlainText
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiWriterScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    errorMessage: String?,
    onRunTool: (toolType: String, prompt: String, systemInstruction: String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val tones = if (strings.isArabic) {
        listOf("احترافي", "إبداعي", "تسويقي", "أكاديمي", "مختصر")
    } else {
        listOf("Professional", "Creative", "Marketing", "Academic", "Concise")
    }
    var selectedTone by remember(strings.isArabic) { mutableStateOf(tones.first()) }

    ToolScaffoldColumn {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = CyanSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.writerTitle, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.writerToneLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tones.forEach { tone ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = { selectedTone = tone },
                            label = { Text(tone) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(strings.writerPromptLabel) },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("writer_prompt_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val langHint = if (strings.isArabic) "Write in rich, well-structured Arabic." else "Write in clear, well-structured English."
                        onRunTool(
                            "writer",
                            prompt,
                            "You are an expert content writer. Tone: $selectedTone. $langHint"
                        )
                    },
                    enabled = !isLoading && prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("writer_generate_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.generateBtn)
                    }
                }
            }
        }
        errorBannerItem(errorMessage)
        if (output.isNotBlank()) {
            item {
                AiOutputCard(
                    title = strings.writerTitle,
                    text = output,
                    strings = strings,
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiTranslatorScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    errorMessage: String?,
    onRunTool: (toolType: String, prompt: String, systemInstruction: String) -> Unit
) {
    val context = LocalContext.current
    var sourceText by remember { mutableStateOf("") }
    val langs = listOf("Arabic", "English", "French", "Spanish", "German", "Turkish")
    var targetLang by remember { mutableStateOf(if (strings.isArabic) "English" else "Arabic") }

    ToolScaffoldColumn {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Translate, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.translatorTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    if (output.isNotBlank()) {
                        IconButton(onClick = { sourceText = output }) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = strings.swapLanguages, tint = CyanSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.translatorTargetLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    langs.forEach { lang ->
                        FilterChip(
                            selected = targetLang == lang,
                            onClick = { targetLang = lang },
                            label = { Text(lang) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    label = { Text(strings.translatorSourceLabel) },
                    minLines = 4,
                    maxLines = 10,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translator_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onRunTool(
                            "translator",
                            sourceText,
                            "Translate the following text accurately and naturally into $targetLang. Preserve formatting and nuance. Return the translation directly."
                        )
                    },
                    enabled = !isLoading && sourceText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translator_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.translateBtn)
                    }
                }
            }
        }
        errorBannerItem(errorMessage)
        if (output.isNotBlank()) {
            item {
                AiOutputCard(
                    title = targetLang,
                    text = output,
                    strings = strings,
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSummarizerScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    errorMessage: String?,
    onRunTool: (toolType: String, prompt: String, systemInstruction: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val styles = if (strings.isArabic) {
        listOf("نقاط رئيسية", "ملخص تنفيذي", "أهم الاستنتاجات")
    } else {
        listOf("Bullet Points", "Executive Summary", "Key Takeaways")
    }
    var selectedStyle by remember(strings.isArabic) { mutableStateOf(styles.first()) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                DocumentExtractor.extractFromUri(context, uri)
                    .onSuccess { doc ->
                        if (doc.extractedText.isNotBlank()) {
                            inputText = doc.extractedText
                        }
                    }
            }
        }
    }

    ToolScaffoldColumn {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Summarize, contentDescription = null, tint = CyanSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.summarizerTitle, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.summarizerStyleLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    styles.forEach { st ->
                        FilterChip(
                            selected = selectedStyle == st,
                            onClick = { selectedStyle = st },
                            label = { Text(st) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(strings.summarizerInputLabel) },
                    minLines = 5,
                    maxLines = 12,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summarizer_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            filePicker.launch(
                                arrayOf(
                                    "text/plain",
                                    "text/markdown",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.attachFile)
                    }
                    Button(
                        onClick = {
                            val langHint = if (strings.isArabic) "Summarize in clear Arabic." else "Summarize in clear English."
                            onRunTool(
                                "summarizer",
                                inputText,
                                "Provide a $selectedStyle summary. $langHint"
                            )
                        },
                        enabled = !isLoading && inputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("summarizer_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(strings.summarizeBtn)
                        }
                    }
                }
            }
        }
        errorBannerItem(errorMessage)
        if (output.isNotBlank()) {
            item {
                AiOutputCard(
                    title = strings.summarizerTitle,
                    text = output,
                    strings = strings,
                    context = context
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCodingScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    errorMessage: String?,
    onRunTool: (toolType: String, prompt: String, systemInstruction: String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val languages = listOf("Kotlin", "Python", "TypeScript", "Java", "SQL", "HTML/CSS")
    var selectedLang by remember { mutableStateOf("Kotlin") }
    val tasks = if (strings.isArabic) {
        listOf("كتابة كود", "تصحيح الأخطاء", "شرح الكود", "تحسين الأداء")
    } else {
        listOf("Generate", "Debug & Fix", "Explain", "Refactor")
    }
    var selectedTask by remember(strings.isArabic) { mutableStateOf(tasks.first()) }

    ToolScaffoldColumn {
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = CyanSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.codingTitle, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.codingLanguageLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.codingTaskLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        FilterChip(
                            selected = selectedTask == task,
                            onClick = { selectedTask = task },
                            label = { Text(task) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(strings.codingPromptLabel) },
                    minLines = 5,
                    maxLines = 12,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coding_prompt_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val langNote = if (strings.isArabic) "Explain briefly in Arabic and output clean production code." else "Explain concisely in English and output clean production code."
                        onRunTool(
                            "coding",
                            prompt,
                            "You are a senior $selectedLang engineer. Task: $selectedTask. $langNote"
                        )
                    },
                    enabled = !isLoading && prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coding_run_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.runCodingBtn)
                    }
                }
            }
        }
        errorBannerItem(errorMessage)
        if (output.isNotBlank()) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth(), accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedLang, style = MaterialTheme.typography.titleSmall, color = CyanSecondary)
                        Row {
                            IconButton(onClick = { copyTextToClipboard(context, output, strings.copied) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = strings.copy)
                            }
                            IconButton(onClick = { sharePlainText(context, output, selectedLang) }) {
                                Icon(Icons.Rounded.Share, contentDescription = strings.share)
                            }
                        }
                    }
                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = output,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolScaffoldColumn(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.errorBannerItem(errorMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
        item {
            Surface(
                color = RoseError.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = RoseError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
