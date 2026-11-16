package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppStrings
import com.example.data.model.Conversation
import com.example.data.model.DocumentRecord
import com.example.data.model.UserProfile
import com.example.ui.components.PremiumCard
import com.example.ui.components.StatusPill
import com.example.ui.navigation.AppRoutes
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoTertiary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.UiState

private data class QuickToolTile(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun HomeDashboardScreen(
    strings: AppStrings,
    userName: String,
    userProfile: UserProfile?,
    conversationsState: UiState<List<Conversation>>,
    documentsState: UiState<List<DocumentRecord>>,
    isOnline: Boolean,
    isOpenAiConfigured: Boolean,
    onNavigate: (String) -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val quickTools = listOf(
        QuickToolTile(AppRoutes.CHAT, strings.navChat, Icons.Rounded.ChatBubble, VioletPrimary),
        QuickToolTile(AppRoutes.EMAIL, strings.navEmail, Icons.Rounded.Email, CyanSecondary),
        QuickToolTile(AppRoutes.WRITER, strings.navWriter, Icons.Rounded.EditNote, IndigoTertiary),
        QuickToolTile(AppRoutes.TRANSLATOR, strings.navTranslator, Icons.Rounded.Translate, EmeraldSuccess),
        QuickToolTile(AppRoutes.SUMMARIZER, strings.navSummarizer, Icons.Rounded.Summarize, CyanSecondary),
        QuickToolTile(AppRoutes.CODING, strings.navCoding, Icons.Rounded.Code, VioletPrimary),
        QuickToolTile(AppRoutes.IMAGE_STUDIO, strings.navImageStudio, Icons.Rounded.Image, IndigoTertiary),
        QuickToolTile(AppRoutes.DOCUMENTS, strings.navDocuments, Icons.Rounded.Description, EmeraldSuccess),
        QuickToolTile(AppRoutes.WEB_SEARCH, strings.navWebSearch, Icons.Rounded.Public, CyanSecondary),
        QuickToolTile(AppRoutes.VOICE, strings.navVoice, Icons.Rounded.Mic, VioletPrimary)
    )

    val docsList = (documentsState as? UiState.Success)?.data.orEmpty()
    val totalDocBytes = docsList.sumOf { it.fileSizeBytes }
    val convList = (conversationsState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Welcome Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                        .clickable { onNavigate(AppRoutes.CHAT) }
                        .testTag("dashboard_hero_card")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = strings.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianBlack.copy(alpha = 0.9f),
                                        ObsidianBlack.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = if (isOnline) strings.onlineStatus else strings.offlineStatus,
                                isHealthy = isOnline
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = VioletPrimary.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyanSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = userProfile?.preferredModel ?: "gemini-3.8-flash",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "${strings.welcomePrefix} ${userName.ifBlank { "Creator" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.tagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyanSecondary
                            )
                        }
                    }
                }
            }

            // 2. Real Usage Metrics & Storage Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = strings.totalRequests,
                        value = (userProfile?.totalRequests ?: 0L).toString(),
                        accent = VioletPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = strings.imagesCreated,
                        value = (userProfile?.totalImagesGenerated ?: 0L).toString(),
                        accent = CyanSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = strings.docsAnalyzed,
                        value = "${docsList.size} (${formatBytes(totalDocBytes)})",
                        accent = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. AI Services Health & Account Quick Action
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.aiServicesHealth,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceElevated,
                            modifier = Modifier
                                .clickable { onNavigate(AppRoutes.SETTINGS) }
                                .testTag("dashboard_settings_shortcut")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = strings.navSettings,
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.navSettings,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusRowItem(text = strings.geminiReady, active = true)
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRowItem(
                        text = if (isOpenAiConfigured) strings.openAiReady else strings.openAiNeedsKey,
                        active = isOpenAiConfigured
                    )
                }
            }

            // 4. Quick AI Tools Grid
            item {
                Text(
                    text = strings.quickTools,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(quickTools.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { tile ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                                .clickable { onNavigate(tile.route) }
                                .testTag("tool_tile_${tile.route}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tile.tint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.label,
                                        tint = tile.tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tile.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 5. Recent Conversations
            item {
                Text(
                    text = strings.recentChats,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (convList.isEmpty()) {
                item {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.noRecentChats,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(convList.take(5), key = { it.id }) { conv ->
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (conv.lastMessagePreview.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = conv.lastMessagePreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = strings.openChat,
                                tint = CyanSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusRowItem(text: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L -> "${bytes}B"
        bytes < 1024L * 1024L -> "${bytes / 1024L}KB"
        else -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
    }
}
