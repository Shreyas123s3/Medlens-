package com.medlens.app.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.medlens.app.BuildConfig
import com.medlens.app.data.ChatMessage
import com.medlens.app.data.ChatSession
import com.medlens.app.data.Medicine
import com.medlens.app.data.RecentSearch
import com.medlens.app.data.RecentlyViewed
import com.medlens.app.data.SavedMedicine
import com.medlens.app.data.SymptomInfo
import com.medlens.app.ui.components.CategoryChip
import com.medlens.app.ui.components.ChatBubble
import com.medlens.app.ui.components.EmergencyCard
import com.medlens.app.ui.components.EmptyState
import com.medlens.app.ui.components.ErrorState
import com.medlens.app.ui.components.FeatureCard
import com.medlens.app.ui.components.Glyph
import com.medlens.app.ui.components.InfoSection
import com.medlens.app.ui.components.MedLensMascot
import com.medlens.app.ui.components.MedLensTopBar
import com.medlens.app.ui.components.MedicineCard
import com.medlens.app.ui.components.MascotExpression
import com.medlens.app.ui.components.SearchBar
import com.medlens.app.ui.components.SectionHeader
import com.medlens.app.ui.components.ShimmerPlaceholder
import com.medlens.app.ui.components.WarningBanner
import com.medlens.app.ui.components.TypingIndicator
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_EXPLORE = "explore"
private const val ROUTE_ASSISTANT = "assistant"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_MEDICINE = "medicine"
private const val ROUTE_SYMPTOMS = "symptoms"
private const val ROUTE_SYMPTOM = "symptom-detail"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_PREVIEW = "design-preview"

@Composable
fun MedLensApp(viewModel: MedLensViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(950); splashFinished = true }

    when {
        !state.isInitialized || !splashFinished -> SplashScreen()
        !state.onboardingComplete -> OnboardingScreen(onComplete = viewModel::completeOnboarding)
        state.accountId == null -> AccountScreen(state, viewModel::createAccount, viewModel::signIn, viewModel::clearError)
        else -> AuthenticatedApp(state, viewModel)
    }

    if (state.showDisclaimer && state.accountId != null) {
        AlertDialog(
            onDismissRequest = viewModel::hideDisclaimer,
            title = { Text("Medical disclaimer") },
            text = { Text(DISCLAIMER_TEXT) },
            confirmButton = { TextButton(onClick = viewModel::hideDisclaimer) { Text("Close") } },
        )
    }
    state.emergencyMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEmergency,
            title = { Text("Emergency help needed", color = MaterialTheme.colorScheme.error) },
            text = { EmergencyCard(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissEmergency) { Text("Close") } },
        )
    }
}

private const val DISCLAIMER_TEXT = "MedLens provides educational information only. It does not diagnose, prescribe, recommend prescription-only medicines, or provide doses or treatment durations. Check the product label and ask a pharmacist or doctor. Take extra care for children, pregnancy, breastfeeding, older adults, and chronic conditions. If this may be an emergency in India, call 112."

@Composable
private fun SplashScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MedLensMascot(MascotExpression.WAVING, Modifier.size(140.dp))
            Text("MedLens", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("Everyday health clarity", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OnboardingScreen(onComplete: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var understood by rememberSaveable { mutableStateOf(false) }
    val titles = listOf("Health information, made clearer", "Start with trusted sources", "Your information stays on this device", "Use MedLens safely")
    val descriptions = listOf(
        "Explore educational information and keep your medicine questions organized.",
        "Medicine and symptom information should show where it came from. No source means no claim.",
        "Your local profile, searches, bookmarks, and assistant conversations are kept on this device.",
        DISCLAIMER_TEXT,
    )
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MedLensMascot(if (page == 3) MascotExpression.CONCERNED else MascotExpression.HAPPY, Modifier.size(140.dp))
            Spacer(Modifier.height(16.dp))
            Text("${page + 1} of 4", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(titles[page], style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(descriptions[page], modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (page == 3) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = understood, onCheckedChange = { understood = it })
                    Text("I understand", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (page > 0) page -= 1 }, enabled = page > 0) { Text("Back") }
                Button(onClick = { if (page < 3) page += 1 else onComplete() }, enabled = page < 3 || understood) {
                Text(if (page == 3) "I understand" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(state: MedLensUiState, onCreate: (String, String, String) -> Unit, onSignIn: (String, String) -> Unit, onClearError: () -> Unit) {
    var isSignUp by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MedLensMascot(MascotExpression.WAVING, Modifier.size(112.dp))
            Text(if (isSignUp) "Create your local account" else "Welcome back", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Your account and saved information stay on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isSignUp) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text("Name") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
            OutlinedTextField(email, { email = it; onClearError() }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            OutlinedTextField(password, { password = it; onClearError() }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Password · at least 8 characters") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (isSignUp) onCreate(name, email, password) else onSignIn(email, password) }))
            state.authError?.let { Text(it, Modifier.fillMaxWidth().padding(top = 10.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = { if (isSignUp) onCreate(name, email, password) else onSignIn(email, password) }, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = !state.isSubmitting) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (isSignUp) "Create account" else "Sign in")
            }
            TextButton(onClick = { isSignUp = !isSignUp; onClearError() }) { Text(if (isSignUp) "Already have an account? Sign in" else "New to MedLens? Create an account") }
            Text("Passwords are salted and hashed on this device. There is no account recovery service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthenticatedApp(state: MedLensUiState, viewModel: MedLensViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: ROUTE_HOME
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MedLensTopBar(
                onLogoClick = { if (BuildConfig.DEBUG) navController.navigate(ROUTE_PREVIEW) },
                onSettingsClick = { navController.navigate(ROUTE_PROFILE) },
                profileInitials = state.accountName.firstOrNull()?.toString() ?: "P",
            )
        },
        bottomBar = {
            FloatingPillNavigation(route) { destination ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                when (destination) { ROUTE_LIBRARY -> viewModel.loadLibrary(); ROUTE_EXPLORE -> viewModel.search(state.query) }
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        NavHost(navController, startDestination = ROUTE_HOME, modifier = Modifier.padding(innerPadding)) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    state = state,
                    onQueryChange = viewModel::search,
                    onSubmit = { query -> viewModel.routeHomeQuery(query, { q -> viewModel.openAssistant(q); navController.navigate(ROUTE_ASSISTANT) }, { navController.navigate(ROUTE_EXPLORE) }, { navController.navigate(ROUTE_MEDICINE) }) },
                    onExplore = { viewModel.search(""); navController.navigate(ROUTE_EXPLORE) },
                    onAssistant = { viewModel.openAssistant(); navController.navigate(ROUTE_ASSISTANT) },
                    onSymptoms = { navController.navigate(ROUTE_SYMPTOMS) },
                    onLibrary = { viewModel.loadLibrary(); navController.navigate(ROUTE_LIBRARY) },
                    onMedicine = { viewModel.openMedicine(it); navController.navigate(ROUTE_MEDICINE) },
                    onRecentlyViewed = { viewModel.openRecentlyViewed(it); navController.navigate(ROUTE_MEDICINE) },
                    onRecentSearch = { query -> viewModel.routeHomeQuery(query, { q -> viewModel.openAssistant(q); navController.navigate(ROUTE_ASSISTANT) }, { navController.navigate(ROUTE_EXPLORE) }, { navController.navigate(ROUTE_MEDICINE) }) },
                )
            }
            composable(ROUTE_EXPLORE) {
                ExploreScreen(state, viewModel::search, { viewModel.search(it, true) }, viewModel::selectCategory, viewModel::toggleSort, { viewModel.openMedicine(it); navController.navigate(ROUTE_MEDICINE) }, { query -> viewModel.lookupMedicineOnline(query) { medicine -> medicine?.let { viewModel.openMedicine(it); navController.navigate(ROUTE_MEDICINE) } } })
            }
            composable(ROUTE_MEDICINE) {
                MedicineDetailScreen(
                    state = state,
                    onBack = { navController.navigateUp() },
                    onSave = viewModel::toggleSelectedMedicineSaved,
                    onAsk = { query -> viewModel.openAssistant(query); navController.navigate(ROUTE_ASSISTANT) },
                )
            }
            composable(ROUTE_SYMPTOMS) {
                SymptomLibraryScreen(state.symptomLibrary, onSelect = { viewModel.selectSymptom(it); navController.navigate(ROUTE_SYMPTOM) })
            }
            composable(ROUTE_SYMPTOM) {
                state.selectedSymptom?.let { SymptomDetailScreen(it, onBack = { navController.navigateUp() }) }
                    ?: EmptyState("Information unavailable", "Return to the symptom library and choose an available topic.", actionLabel = "Back", onAction = { navController.navigateUp() })
            }
            composable(ROUTE_LIBRARY) {
                LibraryScreen(
                    state = state,
                    onOpenSaved = { viewModel.openSavedMedicine(it); navController.navigate(ROUTE_MEDICINE) },
                    onOpenViewed = { viewModel.openRecentlyViewed(it); navController.navigate(ROUTE_MEDICINE) },
                    onRemove = { item ->
                        viewModel.removeSaved(item)
                        scope.launch {
                            if (snackbar.showSnackbar("Removed ${item.genericName}", "UNDO") == androidx.compose.material3.SnackbarResult.ActionPerformed) viewModel.undoRemoveSaved(item)
                        }
                    },
                    onSearchAgain = { query -> viewModel.routeHomeQuery(query, { q -> viewModel.openAssistant(q); navController.navigate(ROUTE_ASSISTANT) }, { viewModel.search(query); navController.navigate(ROUTE_EXPLORE) }, { navController.navigate(ROUTE_MEDICINE) }) },
                )
            }
            composable(ROUTE_ASSISTANT) {
                AssistantScreen(
                    state = state,
                    onSend = viewModel::sendMessage,
                    onNew = viewModel::newChat,
                    onClear = viewModel::clearCurrentChat,
                    onClearAll = viewModel::clearAllChats,
                    onSelectConversation = viewModel::selectChat,
                    onSaveMedicine = { text ->
                        viewModel.saveMentionedMedicine(text) { saved ->
                            Toast.makeText(context, if (saved) "Medicine saved to your library." else "No catalog medicine was found in this message.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenMedicine = { name -> viewModel.openMedicineByName(name); navController.navigate(ROUTE_MEDICINE) },
                    onConsent = viewModel::answerAiConsent,
                )
            }
            composable(ROUTE_PROFILE) {
                ProfileScreen(state, onSave = viewModel::updateProfile, onDarkMode = viewModel::setDarkMode, onClearChats = viewModel::clearAllChats, onClearSearches = viewModel::clearSearchHistory, onDisclaimer = viewModel::showDisclaimerAgain, onLogout = viewModel::logout, onSetAiConsent = viewModel::setAiConsentPreference, onClearAllData = viewModel::clearAllMyData)
            }
            composable(ROUTE_PREVIEW) {
                if (BuildConfig.DEBUG) DesignPreviewScreen(onBack = { navController.navigateUp() }) else navController.navigateUp()
            }
        }
    }
}

@Composable
private fun FloatingPillNavigation(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(Triple(ROUTE_HOME, "Home", "home"), Triple(ROUTE_EXPLORE, "Explore", "search"), Triple(ROUTE_ASSISTANT, "Assistant", "chat"), Triple(ROUTE_LIBRARY, "Library", "bookmark"))
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp, shadowElevation = 7.dp) {
            Row(Modifier.padding(horizontal = 3.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { (destination, label, icon) ->
                    NavigationBarItem(selected = currentRoute == destination || (destination == ROUTE_EXPLORE && currentRoute == ROUTE_MEDICINE), onClick = { onNavigate(destination) }, icon = { Glyph(icon, label) }, label = { Text(label) }, alwaysShowLabel = false)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: MedLensUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onExplore: () -> Unit,
    onAssistant: () -> Unit,
    onSymptoms: () -> Unit,
    onLibrary: () -> Unit,
    onMedicine: (Medicine) -> Unit,
    onRecentlyViewed: (RecentlyViewed) -> Unit,
    onRecentSearch: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hello, ${state.accountName.ifBlank { "there" }}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("What would you like to understand today?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MedLensMascot(MascotExpression.HAPPY, Modifier.size(84.dp))
        }
        SearchBar(value = state.query, onValueChange = onQueryChange, onSearch = { onSubmit(state.query) }, placeholder = "Describe your symptoms or search for a medicine…")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("I have a headache", "What is a common cold?", "Medicine information").forEach { example ->
                CategoryChip(example, false, onClick = { onQueryChange(example); onSubmit(example) })
            }
        }
        if (state.recentSearches.isNotEmpty()) {
            SectionHeader("Recent searches")
            state.recentSearches.take(4).forEach { item -> TextButton(onClick = { onRecentSearch(item.query) }) { Text(item.query) } }
        }
        SectionHeader("Explore MedLens")
        FeatureCard("AI Assistant", "Organize a question with the offline assistant.", "Start a conversation", onAssistant)
        FeatureCard("Explore medicines", "Search the verified local catalog.", "Browse catalog", onExplore)
        FeatureCard("Symptom information", "Read sourced, general educational information.", "Open symptom library", onSymptoms)
        FeatureCard("Recently viewed", "Return to entries you opened on this device.", "View history", onLibrary)
        FeatureCard("Saved", "Open medicines saved to this local account.", "Open your library", onLibrary)
        FeatureCard("Health library", "Browse the symptom information topics.", "Browse topics", onSymptoms)
        if (state.recentlyViewed.isNotEmpty()) {
            SectionHeader("Recently viewed")
            state.recentlyViewed.take(3).forEach { item ->
                FeatureCard(item.genericName, "Medicine information", "Open again", { onRecentlyViewed(item) })
            }
        }
        WarningBanner("Education only. Check the product label and ask a pharmacist or doctor. No diagnosis, prescriptions, doses, or treatment durations.")
    }
}

@Composable
private fun ExploreScreen(state: MedLensUiState, onQuery: (String) -> Unit, onSubmitSearch: (String) -> Unit, onCategory: (String?) -> Unit, onSort: () -> Unit, onSelect: (Medicine) -> Unit, onLookupOnline: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Explore medicines", supportingText = "Search by generic or brand name. Catalog entries are shown only when locally available.")
        SearchBar(state.query, onQuery, onSearch = { onSubmitSearch(state.query) }, placeholder = "Generic or brand name")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip("All", state.selectedCategory == null, onClick = { onCategory(null) })
            state.categories.forEach { category -> CategoryChip(category, state.selectedCategory == category, onClick = { onCategory(category) }) }
        }
        TextButton(onClick = onSort) { Text(if (state.ascending) "Sort: A–Z" else "Sort: Z–A") }
        when {
            state.errorMessage != null -> ErrorState(state.errorMessage, { onQuery(state.query) })
            state.isLoading -> repeat(3) { ShimmerPlaceholder() }
            state.medicines.isEmpty() && state.query.isBlank() -> EmptyState("Catalog not available yet", "No reviewed medicine entries are installed. This list stays empty until verified data with a source is added.")
            state.medicines.isEmpty() -> {
                EmptyState("No matching medicines", "Try another spelling or search by a generic name. You can also check the FDA label source while online.")
                OutlinedButton(onClick = { onLookupOnline(state.query) }, enabled = !state.onlineLookupInProgress && state.query.isNotBlank()) {
                    if (state.onlineLookupInProgress) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Look up on OpenFDA")
                }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            else -> state.medicines.forEach { medicine -> MedicineCard(medicine, state.savedMedicines.any { it.genericName == medicine.genericName }) { onSelect(medicine) } }
        }
        WarningBanner("Medicine information is educational. Check the label and ask a pharmacist or doctor.")
    }
}

@Composable
private fun MedicineDetailScreen(state: MedLensUiState, onBack: () -> Unit, onSave: () -> Unit, onAsk: (String) -> Unit) {
    val medicine = state.selectedMedicine
    val context = LocalContext.current
    if (medicine == null) {
        EmptyState("Medicine unavailable", "Return to the catalog and open an available item.", actionLabel = "Back", onAction = onBack)
        return
    }
    val unavailable = "Information unavailable"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(medicine.genericName, style = MaterialTheme.typography.headlineLarge)
        Text(medicine.category.ifBlank { unavailable }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        Text(if (medicine.otcStatus == "OTC") "OTC · FDA label" else if (medicine.otcStatus == "RX") "Prescription information · not a recommendation" else "Regulatory status unavailable", style = MaterialTheme.typography.labelLarge)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Overview", style = MaterialTheme.typography.titleMedium)
                Text(medicine.uses.ifBlank { unavailable }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Button(onClick = onSave, Modifier.fillMaxWidth()) { Text(if (state.selectedMedicineSaved) "Remove from Saved" else "Save medicine") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${medicine.genericName}\nSource: ${medicine.source.ifBlank { unavailable }}\nEducational information only. Check the label and ask a pharmacist or doctor.") }
                runCatching { context.startActivity(Intent.createChooser(share, "Share medicine information")) }
            }) { Text("Share") }
            TextButton(onClick = { onAsk("Please explain the information available about ${medicine.genericName} in the local catalog.") }) { Text("Ask MedLens AI about this medicine") }
        }
        WarningBanner("Educational information only. No diagnosis, prescription, dose, or treatment duration. Check the product label and ask a pharmacist or doctor.")
        listOf(
            "Generic name" to medicine.genericName,
            "Brands" to medicine.brands,
            "Brand availability" to medicine.brandAvailability,
            "Category" to medicine.category,
            "Uses" to medicine.uses,
            "How supplied" to medicine.howSupplied,
            "Warnings" to medicine.warnings,
            "How it works" to medicine.mechanism,
            "Side effects" to medicine.sideEffects,
            "Precautions" to medicine.precautions,
            "Contraindications" to medicine.contraindications,
            "Interactions" to medicine.interactions,
            "Storage" to medicine.storage,
            "When to see a doctor" to medicine.whenToSeeDoctor,
            "Information source" to medicine.source,
            "Retrieved" to medicine.retrievedAt,
            "FDA label identifier" to medicine.sourceRecordId,
            "FDA label effective date" to medicine.sourceEffectiveDate,
        ).forEach { (title, value) -> InfoSection(title, value.ifBlank { unavailable }) }
        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, medicine.sourceUrl.toUri())) }.onFailure { Toast.makeText(context, "Could not open the FDA source.", Toast.LENGTH_SHORT).show() } }) { Text("Open original FDA label") }
    }
}

@Composable
private fun SymptomLibraryScreen(items: List<SymptomInfo>, onSelect: (SymptomInfo) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Symptom information", supportingText = "General information from NHS and MedlinePlus. This is not a diagnosis.")
        if (items.isEmpty()) EmptyState("Information unavailable", "The local symptom information file could not be read.")
        else items.forEach { item -> FeatureCard(item.title, item.summary, "Read sourced information", { onSelect(item) }) }
        WarningBanner("Children, pregnancy, breastfeeding, older adults, and chronic conditions need extra care. Ask a pharmacist or doctor for individual advice.")
    }
}

@Composable
private fun SymptomDetailScreen(item: SymptomInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Symptom library") }
        Text(item.title, style = MaterialTheme.typography.headlineLarge)
        Text(item.summary, style = MaterialTheme.typography.bodyLarge)
        listOf(
            "Possible causes (not a diagnosis)" to item.possibleCauses,
            "Associated symptoms" to item.associatedSymptoms,
            "General self-care information" to item.selfCare,
            "Warning signs" to item.warningSigns,
            "When to seek medical advice" to item.whenToSeeDoctor,
        ).forEach { (title, values) ->
            InfoSection(title, values.joinToString("\n\n") { "• $it" }.ifBlank { "Information unavailable" })
        }
        EmergencyCard("If symptoms are severe or rapidly worsening, seek urgent help. In India, call 112 for an emergency.")
        WarningBanner("For education only. Check product labels and ask a pharmacist or doctor. Extra caution applies to children, pregnancy, breastfeeding, older adults, and chronic conditions.")
        Text("Source: ${item.sourceName}", style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, item.sourceUrl.toUri())) }.onFailure { Toast.makeText(context, "Could not open the source page.", Toast.LENGTH_SHORT).show() } }) { Text("Open original source") }
    }
}

@Composable
private fun LibraryScreen(
    state: MedLensUiState,
    onOpenSaved: (SavedMedicine) -> Unit,
    onOpenViewed: (RecentlyViewed) -> Unit,
    onRemove: (SavedMedicine) -> Unit,
    onSearchAgain: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Your library", supportingText = "Saved medicines, recently viewed entries, and recent searches for this account.")
        OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Filter this library") })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Saved", "Recently viewed", "Recent searches").forEachIndexed { index, label -> FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) }) }
        }
        when (tab) {
            0 -> {
                val rows = state.savedMedicines.filter { it.genericName.contains(filter, true) }
                if (rows.isEmpty()) EmptyState("Nothing saved yet", "Save a medicine from its details page. This list contains only real catalog entries.")
                else rows.forEach { item ->
                    FeatureCard(item.genericName, "Saved on this account", "Open medicine", { onOpenSaved(item) })
                    TextButton(onClick = { onRemove(item) }) { Text("Remove · Undo is available") }
                }
            }
            1 -> {
                val rows = state.recentlyViewed.filter { it.genericName.contains(filter, true) }
                if (rows.isEmpty()) EmptyState("No recent items", "Medicine entries you open will appear here.")
                else rows.forEach { item -> FeatureCard(item.genericName, "Recently viewed", "Open again", { onOpenViewed(item) }) }
            }
            else -> {
                val rows = state.recentSearches.filter { it.query.contains(filter, true) }
                if (rows.isEmpty()) EmptyState("No recent searches", "Searches you submit will appear here.")
                else rows.forEach { item -> TextButton(onClick = { onSearchAgain(item.query) }) { Text(item.query) } }
            }
        }
    }
}

@Composable
private fun AssistantScreen(
    state: MedLensUiState,
    onSend: (String) -> Unit,
    onNew: () -> Unit,
    onClear: () -> Unit,
    onClearAll: () -> Unit,
    onSelectConversation: (ChatSession) -> Unit,
    onSaveMedicine: (String) -> Unit,
    onOpenMedicine: (String) -> Unit,
    onConsent: (Boolean) -> Unit,
) {
    var message by rememberSaveable { mutableStateOf("") }
    var showPrevious by rememberSaveable { mutableStateOf(false) }
    var confirmClearAll by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val speechResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { message = spoken } else Toast.makeText(context, "No speech was recognized. You can type instead.", Toast.LENGTH_SHORT).show()
        }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchSpeechRecognition(context) { intent -> speechResult.launch(intent) }
        else Toast.makeText(context, "Microphone access is needed for voice input. You can type instead.", Toast.LENGTH_LONG).show()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedLensMascot(MascotExpression.THINKING, Modifier.size(60.dp))
            Column(Modifier.weight(1f)) {
                Text("MedLens Assistant", style = MaterialTheme.typography.titleLarge)
                Text(if (state.aiOnline) "AI online · Educational information" else "Offline mode · Educational information", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onNew) { Text("New chat") }
            TextButton(onClick = onClear) { Text("Clear") }
            TextButton(onClick = { showPrevious = !showPrevious }) { Text(if (showPrevious) "Hide history" else "Previous chats") }
            TextButton(onClick = { confirmClearAll = true }) { Text("Clear all") }
        }
        if (showPrevious) {
            Column(Modifier.fillMaxWidth().height(132.dp).verticalScroll(rememberScrollState())) {
                if (state.chatSessions.isEmpty()) Text("No previous conversations", style = MaterialTheme.typography.bodySmall)
                state.chatSessions.forEach { session -> TextButton(onClick = { onSelectConversation(session); showPrevious = false }) { Text(session.title, maxLines = 1) } }
            }
        }
        WarningBanner(if (state.aiOnline) "Your text is sent to the MedLens AI service. Do not share identifying information. Check labels and ask a pharmacist or doctor." else "Your text is processed by the offline assistant on this device. Voice recognition uses Android's speech service and its privacy settings.")
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.chatMessages.isEmpty()) {
                item(key = "assistant-empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmptyState("Start a conversation", "Ask a general question or describe what you want to organize. Avoid sharing identifying details.")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Help me organize a headache question", "What details should I tell my doctor?", "What should I ask a pharmacist?").forEach { suggestion -> CategoryChip(suggestion, false, onClick = { onSend(suggestion) }) }
                        }
                    }
                }
            } else items(state.chatMessages, key = ChatMessage::id) { item ->
                ChatMessageCard(item, clipboard = { clipboard?.setPrimaryClip(ClipData.newPlainText("MedLens message", item.text.substringBefore("\n[[MEDLENS_MEDICINE:"))); Toast.makeText(context, "Message copied.", Toast.LENGTH_SHORT).show() }, saveMedicine = { onSaveMedicine(item.text) }, onOpenMedicine = onOpenMedicine)
            }
            if (state.assistantTyping) item(key = "assistant-typing") { TypingIndicator() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                placeholder = { Text("Write a message") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (message.isNotBlank()) { onSend(message); message = "" } }),
            )
            IconButton(onClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) launchSpeechRecognition(context) { intent -> speechResult.launch(intent) }
                else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }, modifier = Modifier.semantics { contentDescription = "Use voice input" }) { Glyph("mic", "Voice input", tint = MaterialTheme.colorScheme.secondary) }
            IconButton(onClick = { if (message.isNotBlank()) { onSend(message); message = "" } else Toast.makeText(context, "Type or speak a message first.", Toast.LENGTH_SHORT).show() }, modifier = Modifier.semantics { contentDescription = "Send message" }) { Text("Send", color = MaterialTheme.colorScheme.primary) }
        }
    }
    if (confirmClearAll) AlertDialog(onDismissRequest = { confirmClearAll = false }, title = { Text("Clear all conversations?") }, text = { Text("This permanently removes this account's local chat history.") }, confirmButton = { TextButton(onClick = { onClearAll(); confirmClearAll = false }) { Text("Clear all") } }, dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") } })
    if (state.showAiConsent) AlertDialog(onDismissRequest = { onConsent(false) }, title = { Text("Use AI for this conversation?") }, text = { Text("Your symptom or medicine question and recent chat messages will be sent to the MedLens AI service. Do not include your name or other identifying information. You can decline and continue in offline mode, or revoke consent later in Profile. AI responses are educational, not a diagnosis or prescription." ) }, confirmButton = { TextButton(onClick = { onConsent(true) }) { Text("Continue online") } }, dismissButton = { TextButton(onClick = { onConsent(false) }) { Text("Use offline") } })
}

@Composable
private fun ChatMessageCard(item: ChatMessage, clipboard: () -> Unit, saveMedicine: () -> Unit, onOpenMedicine: (String) -> Unit) {
    val fromUser = item.role == "user"
    val medicineNames = Regex("\\[\\[MEDLENS_MEDICINE:([^]]+)]]").findAll(item.text).map { it.groupValues[1] }.toList()
    val visibleText = item.text.replace(Regex("\\n?\\[\\[MEDLENS_MEDICINE:[^]]+]]"), "").trim()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start) {
        ChatBubble(visibleText, fromUser)
        medicineNames.forEach { name -> OutlinedButton(onClick = { onOpenMedicine(name) }) { Text("Open $name details") } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.sentAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = clipboard) { Text("Copy") }
            if (!fromUser) TextButton(onClick = saveMedicine) { Text("Save medicine info") }
        }
    }
}

private fun launchSpeechRecognition(context: android.content.Context, launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }
    try { launch(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(context, "No speech recognition service is available. You can type instead.", Toast.LENGTH_LONG).show() }
}

@Composable
private fun ProfileScreen(state: MedLensUiState, onSave: (String, String) -> Unit, onDarkMode: (Boolean) -> Unit, onClearChats: () -> Unit, onClearSearches: () -> Unit, onDisclaimer: () -> Unit, onLogout: () -> Unit, onSetAiConsent: (Boolean) -> Unit, onClearAllData: () -> Unit) {
    var name by rememberSaveable(state.accountId) { mutableStateOf(state.accountName) }
    var email by rememberSaveable(state.accountId) { mutableStateOf(state.accountEmail) }
    var clearChatsConfirm by rememberSaveable { mutableStateOf(false) }
    var clearSearchConfirm by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var confirmClearAllData by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Profile & settings", supportingText = "Local account · information remains on this device")
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Button(onClick = { onSave(name, email) }, Modifier.fillMaxWidth()) { Text("Save profile") }
        state.authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Dark mode", style = MaterialTheme.typography.titleMedium); Text("Use a calm dark color palette", style = MaterialTheme.typography.bodySmall) }
            Switch(checked = state.darkMode, onCheckedChange = onDarkMode)
        }
        FeatureCard("Privacy notice", "Chats stay on this device unless you consent to AI sharing. Voice input uses Android speech recognition.", "Read notice", { showPrivacy = true })
        TextButton(onClick = { onSetAiConsent(!state.aiConsentAccepted) }) { Text(if (state.aiConsentAccepted) "Revoke AI sharing consent" else "Allow AI sharing for assistant") }
        FeatureCard("Medical disclaimer", DISCLAIMER_TEXT, "Read disclaimer", onDisclaimer)
        TextButton(onClick = { clearChatsConfirm = true }) { Text("Clear chat history") }
        TextButton(onClick = { clearSearchConfirm = true }) { Text("Clear search history") }
        TextButton(onClick = { confirmClearAllData = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Clear all my data") }
        FeatureCard("About MedLens", "MedLens offers educational health information, not diagnosis or treatment.", "MedLens · Version 1.0", { showAbout = true })
        Button(onClick = onLogout, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Log out") }
    }
    if (clearChatsConfirm) AlertDialog(onDismissRequest = { clearChatsConfirm = false }, title = { Text("Clear chat history?") }, text = { Text("All conversations for this local account will be removed.") }, confirmButton = { TextButton(onClick = { onClearChats(); clearChatsConfirm = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { clearChatsConfirm = false }) { Text("Cancel") } })
    if (clearSearchConfirm) AlertDialog(onDismissRequest = { clearSearchConfirm = false }, title = { Text("Clear search history?") }, text = { Text("This removes recent searches for this account.") }, confirmButton = { TextButton(onClick = { onClearSearches(); clearSearchConfirm = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { clearSearchConfirm = false }) { Text("Cancel") } })
    if (confirmClearAllData) AlertDialog(onDismissRequest = { confirmClearAllData = false }, title = { Text("Clear all your data?") }, text = { Text("This permanently deletes your local profile, saved medicines, searches, recently viewed items, AI consent, and all chats from this device. The shared bundled medicine and symptom reference library remains installed. You will be signed out.") }, confirmButton = { TextButton(onClick = { onClearAllData(); confirmClearAllData = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete my data") } }, dismissButton = { TextButton(onClick = { confirmClearAllData = false }) { Text("Cancel") } })
    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false }, title = { Text("Privacy notice") }, text = { Text("Your profile, password hash, bookmarks, searches, recently viewed medicines, and chats are stored locally and scoped to your account. They are not synced unless you explicitly consent to sending chat text and recent turns to the MedLens AI backend. If you use voice input, audio is handled by the Android speech-recognition service and its privacy rules. Uninstalling the app removes its local information.") }, confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } })
    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false }, title = { Text("About MedLens") }, text = { Text("MedLens is an educational health-information app. Symptom articles link to their original NHS or MedlinePlus sources. The offline assistant organizes questions but does not provide medical assessment. Version 1.0.") }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } })
}
