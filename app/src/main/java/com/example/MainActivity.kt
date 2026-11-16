package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.core.AppStrings
import com.example.core.configureAppCheck
import com.example.core.stringsFor
import com.example.data.model.AiModels
import com.example.ui.components.StatusPill
import com.example.ui.components.isNetworkAvailable
import com.example.ui.navigation.AllDestinations
import com.example.ui.navigation.AppRoutes
import com.example.ui.screens.AiCodingScreen
import com.example.ui.screens.AiSummarizerScreen
import com.example.ui.screens.AiTranslatorScreen
import com.example.ui.screens.AiWriterScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.EmailAssistantScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.WebSearchScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.MyAiProTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.MyAiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "myai_prefs")
private val KEY_LANGUAGE = stringPreferencesKey("language")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before rendering UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val savedLang by remember(context) {
        context.dataStore.data.map { prefs -> prefs[KEY_LANGUAGE] ?: "ar" }
    }.collectAsStateWithLifecycle(initialValue = "ar")

    val currentUser by remember {
        Firebase.auth.authStateFlow()
    }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val persistLanguage: (String) -> Unit = { lang ->
        scope.launch {
            context.dataStore.edit { it[KEY_LANGUAGE] = lang }
        }
    }

    val user = currentUser
    if (user == null) {
        val isArabic = savedLang == "ar"
        val strings = stringsFor(savedLang)
        MyAiProTheme(isArabic = isArabic) {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                AuthScreen(
                    strings = strings,
                    onToggleLanguage = {
                        persistLanguage(if (savedLang == "ar") "en" else "ar")
                    },
                    onAuthSuccess = {}
                )
            }
        }
    } else {
        key(user.uid) {
            AuthenticatedMyAiApp(
                user = user,
                initialLanguage = savedLang,
                onPersistLanguage = persistLanguage,
                onSignOut = {
                    signOut(credentialManager, scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedMyAiApp(
    user: FirebaseUser,
    initialLanguage: String,
    onPersistLanguage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }

    val viewModel: MyAiViewModel = viewModel(
        key = user.uid,
        factory = MyAiViewModel.provideFactory(
            databaseId = databaseId,
            userId = user.uid,
            displayName = user.displayName ?: "MyAI User",
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString(),
            initialLanguage = initialLanguage
        )
    )

    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val chatStreamingText by viewModel.chatStreamingText.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val toolLoading by viewModel.toolLoading.collectAsStateWithLifecycle()
    val toolOutput by viewModel.toolOutput.collectAsStateWithLifecycle()
    val toolError by viewModel.toolError.collectAsStateWithLifecycle()
    val webSearchSources by viewModel.webSearchSources.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
    val docLoading by viewModel.docLoading.collectAsStateWithLifecycle()
    val docError by viewModel.docError.collectAsStateWithLifecycle()
    val docQaAnswer by viewModel.docQaAnswer.collectAsStateWithLifecycle()
    val emailDraftsState by viewModel.emailDraftsState.collectAsStateWithLifecycle()
    val emailLoading by viewModel.emailLoading.collectAsStateWithLifecycle()
    val emailError by viewModel.emailError.collectAsStateWithLifecycle()
    val latestEmailAnalysis by viewModel.latestEmailAnalysis.collectAsStateWithLifecycle()

    val isArabic = selectedLanguage == "ar"
    val strings = stringsFor(selectedLanguage)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.HOME
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showModelMenu by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val isOnline = remember(currentRoute) { isNetworkAvailable(context) }

    fun navigateTo(route: String) {
        viewModel.clearToolOutput()
        navController.navigate(route) {
            popUpTo(AppRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    MyAiProTheme(isArabic = isArabic) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 840.dp

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerSheetContent(
                            strings = strings,
                            currentRoute = currentRoute,
                            onSelectRoute = { route ->
                                scope.launch { drawerState.close() }
                                navigateTo(route)
                            }
                        )
                    }
                ) {
                    Scaffold(
                        containerColor = ObsidianBlack,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = DeepCharcoal,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_btn")
                                    ) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                    }
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(listOf(VioletPrimary, CyanSecondary))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = AllDestinations.firstOrNull { it.route == currentRoute }
                                                ?.title?.invoke(strings) ?: strings.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                actions = {
                                    // Model Selector Pill
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = SurfaceCard,
                                            modifier = Modifier
                                                .clickable { showModelMenu = true }
                                                .testTag("topbar_model_selector")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectedModel.removePrefix("gemini-"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CyanSecondary,
                                                    maxLines = 1
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.ExpandMore,
                                                    contentDescription = strings.selectModel,
                                                    tint = CyanSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = showModelMenu,
                                            onDismissRequest = { showModelMenu = false }
                                        ) {
                                            AiModels.chatModels.forEach { model ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(if (isArabic) model.displayNameAr else model.displayNameEn)
                                                    },
                                                    trailingIcon = {
                                                        if (model.id == selectedModel) {
                                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = CyanSecondary)
                                                        }
                                                    },
                                                    onClick = {
                                                        showModelMenu = false
                                                        viewModel.setModel(model.id)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Language Toggle
                                    IconButton(
                                        onClick = {
                                            val next = if (isArabic) "en" else "ar"
                                            onPersistLanguage(next)
                                            viewModel.setLanguage(next)
                                        },
                                        modifier = Modifier.testTag("topbar_language_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Language,
                                            contentDescription = strings.languageSection,
                                            tint = CyanSecondary
                                        )
                                    }

                                    // Notifications / Health Dialog
                                    IconButton(
                                        onClick = { showNotificationsDialog = true },
                                        modifier = Modifier.testTag("topbar_status_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Notifications,
                                            contentDescription = strings.notificationsTitle
                                        )
                                    }

                                    // Profile Avatar -> Settings
                                    IconButton(
                                        onClick = { navigateTo(AppRoutes.SETTINGS) },
                                        modifier = Modifier.testTag("topbar_profile_btn")
                                    ) {
                                        val photo = user.photoUrl?.toString()
                                        if (!photo.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photo,
                                                contentDescription = strings.accountTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Person, contentDescription = strings.accountTitle)
                                        }
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen) {
                                NavigationBar(containerColor = DeepCharcoal) {
                                    AllDestinations.filter { it.showInBottomBar }.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.title(strings)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = dest.title(strings),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWideScreen) {
                                NavigationRail(
                                    containerColor = DeepCharcoal,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        AllDestinations.filter { it.showInBottomBar || it.route == AppRoutes.SETTINGS }
                                            .forEach { dest ->
                                                val selected = currentRoute == dest.route
                                                NavigationRailItem(
                                                    selected = selected,
                                                    onClick = { navigateTo(dest.route) },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                            contentDescription = dest.title(strings)
                                                        )
                                                    },
                                                    label = { Text(dest.title(strings), maxLines = 1) }
                                                )
                                            }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = AppRoutes.HOME,
                                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                                    exitTransition = { fadeOut(animationSpec = tween(200)) }
                                ) {
                                    composable(AppRoutes.HOME) {
                                        HomeDashboardScreen(
                                            strings = strings,
                                            userName = user.displayName ?: "",
                                            userProfile = userProfile,
                                            conversationsState = conversationsState,
                                            documentsState = documentsState,
                                            isOnline = isOnline,
                                            isOpenAiConfigured = viewModel.isOpenAiConfigured,
                                            onNavigate = { navigateTo(it) },
                                            onOpenConversation = { convId ->
                                                viewModel.selectConversation(convId)
                                                navigateTo(AppRoutes.CHAT)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.CHAT) {
                                        ChatScreen(
                                            strings = strings,
                                            conversationsState = conversationsState,
                                            activeConversationId = activeConvId,
                                            messages = activeMessages,
                                            streamingText = chatStreamingText,
                                            isGenerating = isChatGenerating,
                                            errorMessage = chatError,
                                            selectedModel = selectedModel,
                                            onSelectConversation = { viewModel.selectConversation(it) },
                                            onNewChat = { viewModel.startNewConversation() },
                                            onDeleteConversation = { viewModel.deleteConversation(it) },
                                            onSendMessage = { prompt, bmp, pdfBytes, textContent, name ->
                                                viewModel.sendChatMessage(
                                                    prompt = prompt,
                                                    attachedBitmap = bmp,
                                                    attachedPdfBytes = pdfBytes,
                                                    attachedTextContent = textContent,
                                                    attachmentName = name
                                                )
                                            },
                                            onRegenerateLast = { viewModel.regenerateLastResponse() },
                                            onEditUserMessage = { msg, updated ->
                                                viewModel.editAndResendUserMessage(msg, updated)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.EMAIL) {
                                        EmailAssistantScreen(
                                            strings = strings,
                                            draftsState = emailDraftsState,
                                            isLoading = emailLoading,
                                            errorMessage = emailError,
                                            latestAnalysis = latestEmailAnalysis,
                                            onAnalyzeEmail = { viewModel.analyzeAndSaveEmail(it) },
                                            onDraftWithAi = { goal, recipient, onReady ->
                                                viewModel.draftEmailWithAi(goal, recipient, onReady)
                                            },
                                            onMarkDispatched = { viewModel.markEmailDispatched(it) },
                                            onDeleteDraft = { viewModel.deleteEmailDraft(it) }
                                        )
                                    }

                                    composable(AppRoutes.WRITER) {
                                        AiWriterScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            errorMessage = toolError,
                                            onRunTool = { type, prompt, sys ->
                                                viewModel.runToolStream(type, prompt, sys)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.TRANSLATOR) {
                                        AiTranslatorScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            errorMessage = toolError,
                                            onRunTool = { type, prompt, sys ->
                                                viewModel.runToolStream(type, prompt, sys)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.SUMMARIZER) {
                                        AiSummarizerScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            errorMessage = toolError,
                                            onRunTool = { type, prompt, sys ->
                                                viewModel.runToolStream(type, prompt, sys)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.CODING) {
                                        AiCodingScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            errorMessage = toolError,
                                            onRunTool = { type, prompt, sys ->
                                                viewModel.runToolStream(type, prompt, sys)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.IMAGE_STUDIO) {
                                        ImageStudioScreen(
                                            strings = strings,
                                            uiState = imageGenState,
                                            onGenerateOrEdit = { prompt, modelId, aspect, bmp ->
                                                viewModel.generateOrEditImage(prompt, modelId, aspect, bmp)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.DOCUMENTS) {
                                        DocumentsScreen(
                                            strings = strings,
                                            documentsState = documentsState,
                                            isLoading = docLoading,
                                            errorMessage = docError,
                                            qaAnswer = docQaAnswer,
                                            onUploadAndAnalyze = { viewModel.analyzeAndSaveDocument(it) },
                                            onAskQuestion = { record, q -> viewModel.askDocumentQuestion(record, q) },
                                            onDeleteDocument = { viewModel.deleteDocument(it) }
                                        )
                                    }

                                    composable(AppRoutes.WEB_SEARCH) {
                                        WebSearchScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            sources = webSearchSources,
                                            errorMessage = toolError,
                                            onSearch = { viewModel.runWebSearch(it) }
                                        )
                                    }

                                    composable(AppRoutes.VOICE) {
                                        VoiceAssistantScreen(
                                            strings = strings,
                                            isLoading = toolLoading,
                                            output = toolOutput,
                                            errorMessage = toolError,
                                            onRunVoicePrompt = { type, prompt, sys ->
                                                viewModel.runToolStream(type, prompt, sys)
                                            }
                                        )
                                    }

                                    composable(AppRoutes.SETTINGS) {
                                        SettingsScreen(
                                            strings = strings,
                                            userName = user.displayName ?: "",
                                            userEmail = user.email ?: "",
                                            userPhotoUrl = user.photoUrl?.toString(),
                                            selectedLanguage = selectedLanguage,
                                            selectedModel = selectedModel,
                                            isOpenAiConfigured = viewModel.isOpenAiConfigured,
                                            onSelectLanguage = { lang ->
                                                onPersistLanguage(lang)
                                                viewModel.setLanguage(lang)
                                            },
                                            onSelectModel = { viewModel.setModel(it) },
                                            onSignOut = onSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = { Text(strings.notificationsTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                label = "${strings.serviceStatusLabel}: ${if (isOnline) strings.onlineStatus else strings.offlineStatus}",
                                isHealthy = isOnline
                            )
                            StatusPill(label = strings.geminiReady, isHealthy = true)
                            StatusPill(
                                label = if (viewModel.isOpenAiConfigured) strings.openAiReady else strings.openAiNeedsKey,
                                isHealthy = viewModel.isOpenAiConfigured
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNotificationsDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DrawerSheetContent(
    strings: AppStrings,
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DeepCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VioletPrimary, CyanSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSecondary
                    )
                }
            }
            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(bottom = 8.dp))

            AllDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationDrawerItem(
                    label = { Text(dest.title(strings)) },
                    selected = selected,
                    onClick = { onSelectRoute(dest.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.title(strings)
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = VioletPrimary.copy(alpha = 0.22f),
                        selectedIconColor = CyanSecondary,
                        selectedTextColor = Color.White,
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_item_${dest.route}")
                )
            }
        }
    }
}
