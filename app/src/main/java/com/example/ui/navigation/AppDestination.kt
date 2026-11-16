package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.AppStrings

object AppRoutes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val EMAIL = "email"
    const val WRITER = "writer"
    const val TRANSLATOR = "translator"
    const val SUMMARIZER = "summarizer"
    const val CODING = "coding"
    const val IMAGE_STUDIO = "image_studio"
    const val DOCUMENTS = "documents"
    const val WEB_SEARCH = "web_search"
    const val VOICE = "voice"
    const val SETTINGS = "settings"
}

data class NavDestination(
    val route: String,
    val title: (AppStrings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showInBottomBar: Boolean = false
)

val AllDestinations = listOf(
    NavDestination(AppRoutes.HOME, { it.navHome }, Icons.Filled.Home, Icons.Outlined.Home, showInBottomBar = true),
    NavDestination(AppRoutes.CHAT, { it.navChat }, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, showInBottomBar = true),
    NavDestination(AppRoutes.EMAIL, { it.navEmail }, Icons.Filled.Email, Icons.Outlined.Email, showInBottomBar = true),
    NavDestination(AppRoutes.WRITER, { it.navWriter }, Icons.Filled.EditNote, Icons.Outlined.EditNote),
    NavDestination(AppRoutes.TRANSLATOR, { it.navTranslator }, Icons.Filled.Translate, Icons.Outlined.Translate),
    NavDestination(AppRoutes.SUMMARIZER, { it.navSummarizer }, Icons.Filled.Summarize, Icons.Outlined.Summarize),
    NavDestination(AppRoutes.CODING, { it.navCoding }, Icons.Filled.Code, Icons.Outlined.Code),
    NavDestination(AppRoutes.IMAGE_STUDIO, { it.navImageStudio }, Icons.Filled.Image, Icons.Outlined.Image, showInBottomBar = true),
    NavDestination(AppRoutes.DOCUMENTS, { it.navDocuments }, Icons.Filled.Description, Icons.Outlined.Description, showInBottomBar = true),
    NavDestination(AppRoutes.WEB_SEARCH, { it.navWebSearch }, Icons.Filled.Public, Icons.Outlined.Public),
    NavDestination(AppRoutes.VOICE, { it.navVoice }, Icons.Filled.Mic, Icons.Outlined.MicNone),
    NavDestination(AppRoutes.SETTINGS, { it.navSettings }, Icons.Filled.Settings, Icons.Outlined.Settings)
)
