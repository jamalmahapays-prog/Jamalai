package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppStrings
import com.example.data.model.WebSearchSource
import com.example.ui.components.AiOutputCard
import com.example.ui.components.PremiumCard
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletPrimary

@Composable
fun WebSearchScreen(
    strings: AppStrings,
    isLoading: Boolean,
    output: String,
    sources: List<WebSearchSource>,
    errorMessage: String?,
    onSearch: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

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
                        Icon(Icons.Rounded.Public, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.webSearchTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(strings.webSearchHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_search_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onSearch(query) },
                        enabled = !isLoading && query.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("web_search_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.webSearchBtn)
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

            if (output.isBlank() && sources.isEmpty() && !isLoading) {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.webSearchEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (output.isNotBlank()) {
                item {
                    AiOutputCard(
                        title = strings.webSearchTitle,
                        text = output,
                        strings = strings,
                        context = context
                    )
                }
            }

            if (sources.isNotEmpty()) {
                item {
                    Text(strings.webSearchSources, style = MaterialTheme.typography.titleMedium)
                }
                items(sources) { src ->
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src.url)))
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = src.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CyanSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (src.snippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = src.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = src.title,
                                tint = CyanSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
