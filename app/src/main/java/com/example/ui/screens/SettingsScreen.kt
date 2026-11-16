package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.AppStrings
import com.example.data.model.AiModels
import com.example.ui.components.PremiumCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletPrimary

@Composable
fun SettingsScreen(
    strings: AppStrings,
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    selectedLanguage: String,
    selectedModel: String,
    isOpenAiConfigured: Boolean,
    onSelectLanguage: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onSignOut: () -> Unit
) {
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
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = userName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = VioletPrimary.copy(alpha = 0.25f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = CyanSecondary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName.ifBlank { strings.accountTitle }, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.clearHistoryNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.languageSection, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = selectedLanguage == "ar",
                            onClick = { onSelectLanguage("ar") },
                            label = { Text(strings.arabicLabel) },
                            modifier = Modifier.testTag("settings_lang_ar")
                        )
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = { onSelectLanguage("en") },
                            label = { Text(strings.englishLabel) },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                    }
                }
            }

            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyanSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.defaultModelSection, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AiModels.chatModels.forEach { model ->
                        val isSelected = selectedModel == model.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VioletPrimary.copy(alpha = 0.2f) else SurfaceElevated,
                            onClick = { onSelectModel(model.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (strings.isArabic) model.displayNameAr else model.displayNameEn,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${model.id} • ${model.badge}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.providerStatusSection, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusPill(label = strings.geminiReady, isHealthy = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusPill(
                        label = if (isOpenAiConfigured) strings.openAiReady else strings.openAiNeedsKey,
                        isHealthy = isOpenAiConfigured
                    )
                }
            }

            item {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("settings_sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, color = Color.White)
                }
            }
        }
    }
}
