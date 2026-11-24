package com.medlens.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.medlens.app.assistant.OfflineAssistantService
import com.medlens.app.assistant.AssistantRepository
import com.medlens.app.assistant.QueryClassifier
import com.medlens.app.data.ChatMessage
import com.medlens.app.data.ChatSession
import com.medlens.app.data.MedLensRepository
import com.medlens.app.data.Medicine
import com.medlens.app.data.RecentSearch
import com.medlens.app.data.RecentlyViewed
import com.medlens.app.data.SavedMedicine
import com.medlens.app.data.SymptomInfo
import com.medlens.app.data.UserAccount
import com.medlens.app.safety.EmergencyDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MedLensUiState(
    val isInitialized: Boolean = false,
    val onboardingComplete: Boolean = false,
    val accountId: String? = null,
    val accountName: String = "",
    val accountEmail: String = "",
    val darkMode: Boolean = false,
    val aiConsentReviewed: Boolean = false,
    val aiConsentAccepted: Boolean = false,
    val showAiConsent: Boolean = false,
    val aiOnline: Boolean = false,
    val assistantTyping: Boolean = false,
    val pendingAiMessage: String? = null,
    val disclaimerAccepted: Boolean = false,
    val showDisclaimer: Boolean = false,
    val authError: String? = null,
    val isSubmitting: Boolean = false,
    val query: String = "",
    val medicines: List<Medicine> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val ascending: Boolean = true,
    val savedMedicines: List<SavedMedicine> = emptyList(),
    val recentSearches: List<RecentSearch> = emptyList(),
    val recentlyViewed: List<RecentlyViewed> = emptyList(),
    val isLoading: Boolean = false,
    val onlineLookupInProgress: Boolean = false,
    val errorMessage: String? = null,
    val selectedMedicine: Medicine? = null,
    val selectedMedicineSaved: Boolean = false,
    val symptomLibrary: List<SymptomInfo> = emptyList(),
    val selectedSymptom: SymptomInfo? = null,
    val chatSessions: List<ChatSession> = emptyList(),
    val activeChatId: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val emergencyMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MedLensViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = MedLensRepository(application)
    private val assistant = OfflineAssistantService()
    private val assistantRepository = AssistantRepository(assistant)
    private val _state = MutableStateFlow(MedLensUiState(
        query = savedStateHandle.get<String>("catalogQuery").orEmpty(),
        selectedCategory = savedStateHandle.get<String>("catalogCategory"),
        symptomLibrary = repository.symptomLibrary(),
    ))
    val state: StateFlow<MedLensUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.onboardingComplete, repository.activeAccountId, repository.disclaimerAccepted) { onboarded, accountId, disclaimer ->
                listOf(onboarded, accountId, disclaimer)
            }.collect { values ->
                val onboarded = values[0] as Boolean
                val accountId = values[1] as String?
                val disclaimer = values[2] as Boolean
                val previousId = _state.value.accountId
                _state.update { it.copy(isInitialized = true, onboardingComplete = onboarded, accountId = accountId, disclaimerAccepted = disclaimer, showDisclaimer = false) }
                if (previousId != accountId) {
                    if (accountId == null) _state.update { it.copy(accountName = "", accountEmail = "", savedMedicines = emptyList(), recentSearches = emptyList(), recentlyViewed = emptyList(), chatSessions = emptyList(), chatMessages = emptyList(), activeChatId = null, showAiConsent = false, pendingAiMessage = null, aiConsentReviewed = false, aiConsentAccepted = false, aiOnline = false, assistantTyping = false, emergencyMessage = null) }
                    else loadAccount(accountId)
                }
            }
        }
        viewModelScope.launch {
            repository.activeAccountId.flatMapLatest(repository::darkMode).collect { enabled ->
                _state.update { it.copy(darkMode = enabled) }
            }
        }
        viewModelScope.launch {
            repository.activeAccountId.flatMapLatest(repository::aiConsent).collect { accepted ->
                _state.update { it.copy(aiConsentReviewed = accepted != null, aiConsentAccepted = accepted == true, aiOnline = accepted == true && repository.remoteAiConfigured) }
            }
        }
    }

    private suspend fun loadAccount(id: String) {
        try {
            val result = withContext(Dispatchers.IO) {
                Triple(repository.account(id), repository.categories(), repository.savedMedicines(id))
            }
            val account = result.first
            if (account == null) {
                repository.logout()
                return
            }
            _state.update { it.copy(accountName = account.name, accountEmail = account.email, categories = result.second, savedMedicines = result.third, errorMessage = null) }
            refreshRecents(id)
            ensureChat(id)
            savedStateHandle.get<String>("selectedMedicineName")?.let { name ->
                withContext(Dispatchers.IO) { repository.medicine(name) }?.let { medicine ->
                    _state.update { it.copy(selectedMedicine = medicine, selectedMedicineSaved = repository.isSaved(id, name)) }
                }
            }
            savedStateHandle.get<String>("selectedSymptomId")?.let { symptomId ->
                _state.value.symptomLibrary.firstOrNull { it.id == symptomId }?.let { symptom -> _state.update { it.copy(selectedSymptom = symptom) } }
            }
            search(_state.value.query)
        } catch (_: Exception) {
            _state.update { it.copy(errorMessage = "Your local profile could not be loaded. Please try again.") }
        }
    }

    private suspend fun refreshRecents(accountId: String) {
        val searches = withContext(Dispatchers.IO) { repository.recentSearches(accountId) }
        val viewed = withContext(Dispatchers.IO) { repository.recentlyViewed(accountId) }
        _state.update { it.copy(recentSearches = searches, recentlyViewed = viewed) }
    }

    private suspend fun ensureChat(accountId: String): String {
        val sessions = withContext(Dispatchers.IO) { repository.chatSessions(accountId) }
        val restoredId = savedStateHandle.get<String>("activeChatId")
        val selected = sessions.firstOrNull { it.id == restoredId } ?: sessions.firstOrNull() ?: withContext(Dispatchers.IO) { repository.createChat(accountId) }
        savedStateHandle["activeChatId"] = selected.id
        val messages = withContext(Dispatchers.IO) { repository.chatMessages(selected.id, accountId) }
        _state.update { it.copy(chatSessions = if (sessions.any { s -> s.id == selected.id }) sessions else listOf(selected) + sessions, activeChatId = selected.id, chatMessages = messages) }
        return selected.id
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
            repository.completeDisclaimer()
        }
    }

    fun createAccount(name: String, email: String, password: String) = authenticate {
        repository.createAccount(name, email, password.toCharArray())
    }

    fun signIn(email: String, password: String) = authenticate {
        repository.signIn(email, password.toCharArray())
    }

    private fun authenticate(action: suspend () -> UserAccount) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, authError = null) }
            try { withContext(Dispatchers.IO) { action() } }
            catch (e: Exception) { _state.update { it.copy(authError = e.message ?: "Could not complete sign in. Try again.") } }
            finally { _state.update { it.copy(isSubmitting = false) } }
        }
    }

    fun updateProfile(name: String, email: String) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            _state.update { it.copy(authError = null) }
            try {
                val account = withContext(Dispatchers.IO) { repository.account(id) ?: error("Profile unavailable.") }
                val updated = withContext(Dispatchers.IO) { repository.updateProfile(account, name, email) }
                _state.update { it.copy(accountName = updated.name, accountEmail = updated.email) }
            } catch (e: Exception) { _state.update { it.copy(authError = e.message ?: "Could not update profile.") } }
        }
    }

    fun setDarkMode(value: Boolean) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch { repository.setDarkMode(id, value) }
    }
    fun revokeAiConsent() {
        val id = _state.value.accountId ?: return
        viewModelScope.launch { repository.setAiConsent(id, false) }
    }
    fun setAiConsentPreference(enabled: Boolean) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            repository.setAiConsent(id, enabled)
            _state.update { it.copy(aiConsentReviewed = true, aiConsentAccepted = enabled, aiOnline = enabled && repository.remoteAiConfigured) }
        }
    }
    fun logout() { viewModelScope.launch { repository.logout() } }

    fun search(query: String, record: Boolean = false) {
        val accountId = _state.value.accountId
        savedStateHandle["catalogQuery"] = query
        _state.update { it.copy(query = query, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.searchMedicines(query) }
                val filtered = applySortAndCategory(result)
                if (_state.value.query == query) _state.update { it.copy(medicines = filtered, isLoading = false) }
                if (record && accountId != null && query.isNotBlank()) {
                    withContext(Dispatchers.IO) { repository.addSearch(accountId, query) }
                    refreshRecents(accountId)
                }
            } catch (_: Exception) { _state.update { it.copy(isLoading = false, errorMessage = "The local medicine catalog could not be loaded.") } }
        }
    }

    fun lookupMedicineOnline(query: String, onFound: (Medicine?) -> Unit = {}) {
        if (query.isBlank() || _state.value.onlineLookupInProgress) return
        viewModelScope.launch {
            _state.update { it.copy(onlineLookupInProgress = true, errorMessage = null) }
            try {
                val medicine = withContext(Dispatchers.IO) { repository.lookupOpenFda(query) }
                if (medicine != null) {
                    val rows = withContext(Dispatchers.IO) { repository.allMedicines() }
                    _state.update { it.copy(categories = rows.map(Medicine::category).distinct().sorted(), medicines = applySortAndCategory(rows), onlineLookupInProgress = false) }
                    onFound(medicine)
                } else _state.update { it.copy(onlineLookupInProgress = false, errorMessage = "No matching single-ingredient FDA label was found. Try the generic name.") }
            } catch (_: Exception) {
                _state.update { it.copy(onlineLookupInProgress = false, errorMessage = "Could not reach OpenFDA. Check your connection and try again.") }
            }
        }
    }

    private fun applySortAndCategory(items: List<Medicine>): List<Medicine> {
        val state = _state.value
        val filtered = state.selectedCategory?.let { category -> items.filter { it.category.equals(category, true) } } ?: items
        return if (state.ascending) filtered.sortedBy { it.genericName.lowercase() } else filtered.sortedByDescending { it.genericName.lowercase() }
    }

    fun selectCategory(category: String?) {
        savedStateHandle["catalogCategory"] = category
        _state.update { it.copy(selectedCategory = category) }
        search(_state.value.query)
    }

    fun toggleSort() {
        val ascending = !_state.value.ascending
        val medicines = if (ascending) _state.value.medicines.sortedBy { it.genericName.lowercase() }
        else _state.value.medicines.sortedByDescending { it.genericName.lowercase() }
        _state.update { it.copy(ascending = ascending, medicines = medicines) }
    }

    fun routeHomeQuery(query: String, openAssistant: (String) -> Unit, openExplore: () -> Unit, openDetails: () -> Unit) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            val id = _state.value.accountId ?: return@launch
            withContext(Dispatchers.IO) { repository.addSearch(id, cleaned) }
            refreshRecents(id)
            if (EmergencyDetector.isEmergency(cleaned)) {
                openAssistant(cleaned)
                return@launch
            }
            val medicine = withContext(Dispatchers.IO) { repository.medicineByQuery(cleaned) }
            if (medicine != null && !QueryClassifier.isSymptomQuestion(cleaned)) {
                openMedicine(medicine)
                openDetails()
            } else if (QueryClassifier.isMedicineQuery(cleaned)) {
                search(cleaned)
                openExplore()
            } else {
                openAssistant(cleaned)
            }
        }
    }

    fun openMedicine(medicine: Medicine) {
        val accountId = _state.value.accountId ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { repository.isSaved(accountId, medicine.genericName) }
            withContext(Dispatchers.IO) { repository.addViewed(accountId, medicine.genericName) }
            _state.update { it.copy(selectedMedicine = medicine, selectedMedicineSaved = saved, errorMessage = null) }
            savedStateHandle["selectedMedicineName"] = medicine.genericName
            refreshRecents(accountId)
        }
    }

    fun openMedicineByName(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.medicine(name) }?.let(::openMedicine)
        }
    }

    fun openSavedMedicine(item: SavedMedicine) {
        viewModelScope.launch {
            val medicine = withContext(Dispatchers.IO) { repository.medicine(item.genericName) }
            if (medicine == null) _state.update { it.copy(errorMessage = "This saved item is no longer in the local catalog.") }
            else openMedicine(medicine)
        }
    }

    fun openRecentlyViewed(item: RecentlyViewed) {
        viewModelScope.launch {
            val medicine = withContext(Dispatchers.IO) { repository.medicine(item.genericName) }
            medicine?.let(::openMedicine)
        }
    }

    fun closeMedicine() {
        savedStateHandle.remove<String>("selectedMedicineName")
        _state.update { it.copy(selectedMedicine = null) }
    }

    fun loadLibrary() {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { repository.savedMedicines(id) }
                _state.update { it.copy(savedMedicines = saved, errorMessage = null) }
                refreshRecents(id)
            } catch (_: Exception) { _state.update { it.copy(errorMessage = "Your library could not be loaded.") } }
        }
    }

    fun toggleSelectedMedicineSaved() {
        val medicine = _state.value.selectedMedicine ?: return
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.toggleSaved(id, medicine.genericName) }
                val isSaved = withContext(Dispatchers.IO) { repository.isSaved(id, medicine.genericName) }
                _state.update { it.copy(selectedMedicineSaved = isSaved) }
                loadLibrary()
            } catch (_: Exception) { _state.update { it.copy(errorMessage = "The saved item could not be updated. Please try again.") } }
        }
    }

    fun removeSaved(item: SavedMedicine) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.removeSavedMedicine(id, item.genericName) }
            _state.update { it.copy(savedMedicines = it.savedMedicines.filterNot { saved -> saved.genericName == item.genericName }) }
            if (_state.value.selectedMedicine?.genericName == item.genericName) _state.update { it.copy(selectedMedicineSaved = false) }
        }
    }

    fun undoRemoveSaved(item: SavedMedicine) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.saveMedicine(id, item.genericName) }
            loadLibrary()
        }
    }

    fun clearSearchHistory() {
        val id = _state.value.accountId ?: return
        viewModelScope.launch { withContext(Dispatchers.IO) { repository.clearSearches(id) }; _state.update { it.copy(recentSearches = emptyList()) } }
    }

    fun selectSymptom(info: SymptomInfo) {
        savedStateHandle["selectedSymptomId"] = info.id
        _state.update { it.copy(selectedSymptom = info) }
    }

    fun openAssistant(initialQuery: String? = null) {
        val accountId = _state.value.accountId ?: return
        viewModelScope.launch {
            if (_state.value.activeChatId == null) ensureChat(accountId)
            if (!initialQuery.isNullOrBlank()) sendMessage(initialQuery)
        }
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (!EmergencyDetector.isEmergency(clean) && repository.remoteAiConfigured && !_state.value.aiConsentReviewed) {
            _state.update { it.copy(showAiConsent = true, pendingAiMessage = clean) }
            return
        }
        val accountId = _state.value.accountId ?: return
        viewModelScope.launch {
            val chatId = _state.value.activeChatId ?: ensureChat(accountId)
            sendMessage(clean, chatId)
        }
    }

    fun answerAiConsent(accepted: Boolean) {
        val accountId = _state.value.accountId
        val pending = _state.value.pendingAiMessage
        _state.update { it.copy(showAiConsent = false, pendingAiMessage = null) }
        if (accountId == null) return
        viewModelScope.launch {
            repository.setAiConsent(accountId, accepted)
            _state.update { it.copy(aiConsentReviewed = true, aiConsentAccepted = accepted, aiOnline = accepted && repository.remoteAiConfigured) }
            if (!pending.isNullOrBlank()) sendMessage(pending)
        }
    }

    private suspend fun sendMessage(text: String, sessionId: String) {
        val input = text.trim()
        val accountId = _state.value.accountId ?: return
        if (input.isEmpty()) return
        withContext(Dispatchers.IO) { repository.addChatMessage(sessionId, accountId, "user", input) }
        _state.update { it.copy(assistantTyping = true) }
        val emergency = EmergencyDetector.isEmergency(input)
        val catalogMedicine = if (emergency) null else withContext(Dispatchers.IO) { repository.medicineByQuery(input) }
        val responseResult = if (emergency) {
            assistantRepository.respond(input, false, false, emptyList()) { error("Emergency must never call AI") }
        } else if (catalogMedicine != null && !QueryClassifier.isSymptomQuestion(input)) {
            assistantRepository.respond(input, false, false, listOf(catalogMedicine)) { error("Medicine lookup stays local") }.copy(
                text = "The local catalog entry for ${catalogMedicine.genericName} says: ${catalogMedicine.uses.ifBlank { "Information unavailable" }}. Source: ${catalogMedicine.source.ifBlank { "Information unavailable" }}. Check the product label and ask a pharmacist or doctor. This is educational information, not a diagnosis or prescription; no dose or treatment duration is provided."
            )
        } else if (QueryClassifier.isMedicineQuery(input)) {
            assistantRepository.respond(input, false, false, emptyList()) { error("Not sent online") }.copy(
                text = "I could not find a matching medicine record in this local catalog. Please check the product label and ask a pharmacist or doctor. I cannot diagnose, prescribe, or give doses or treatment durations."
            )
        } else {
            val messagesBeforeReply = withContext(Dispatchers.IO) { repository.chatMessages(sessionId, accountId) }
            val catalogRecords = withContext(Dispatchers.IO) { repository.allMedicines() }
            assistantRepository.respond(input, _state.value.aiConsentAccepted, repository.remoteAiConfigured, catalogRecords) {
                repository.remoteAssistant(messagesBeforeReply)
            }
        }
        val storedResponse = responseResult.text + responseResult.medicineNames.joinToString(separator = "", prefix = "") { "\n[[MEDLENS_MEDICINE:${it}]]" }
        withContext(Dispatchers.IO) { repository.addChatMessage(sessionId, accountId, "assistant", storedResponse) }
        val messages = withContext(Dispatchers.IO) { repository.chatMessages(sessionId, accountId) }
        val sessions = withContext(Dispatchers.IO) { repository.chatSessions(accountId) }
        _state.update { it.copy(activeChatId = sessionId, chatMessages = messages, chatSessions = sessions, emergencyMessage = if (responseResult.emergency) responseResult.text else null, aiOnline = responseResult.online, assistantTyping = false) }
    }

    fun newChat() {
        val accountId = _state.value.accountId ?: return
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { repository.createChat(accountId) }
            savedStateHandle["activeChatId"] = session.id
            _state.update { it.copy(activeChatId = session.id, chatMessages = emptyList(), chatSessions = listOf(session) + it.chatSessions, emergencyMessage = null) }
        }
    }

    fun selectChat(session: ChatSession) {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            val messages = withContext(Dispatchers.IO) { repository.chatMessages(session.id, id) }
            savedStateHandle["activeChatId"] = session.id
            _state.update { it.copy(activeChatId = session.id, chatMessages = messages, emergencyMessage = null) }
        }
    }

    fun clearCurrentChat() {
        val id = _state.value.accountId ?: return
        val chatId = _state.value.activeChatId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.clearCurrentChat(chatId, id) }
            _state.update { it.copy(chatMessages = emptyList(), emergencyMessage = null) }
        }
    }

    fun clearAllChats() {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.clearAllChats(id) }
            val newSession = withContext(Dispatchers.IO) { repository.createChat(id) }
            savedStateHandle["activeChatId"] = newSession.id
            _state.update { it.copy(chatSessions = listOf(newSession), activeChatId = newSession.id, chatMessages = emptyList()) }
        }
    }

    fun clearAllMyData() {
        val id = _state.value.accountId ?: return
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { repository.clearAllAccountData(id) } }
            catch (_: Exception) { _state.update { it.copy(errorMessage = "Your local account data could not be cleared. Please try again.") } }
        }
    }

    fun saveMentionedMedicine(text: String, onResult: (Boolean) -> Unit) {
        val accountId = _state.value.accountId ?: return onResult(false)
        viewModelScope.launch {
            val medicine = withContext(Dispatchers.IO) {
                repository.allMedicines().firstOrNull { candidate ->
                    text.contains(candidate.genericName, ignoreCase = true)
                }
            }
            if (medicine == null) { onResult(false); return@launch }
            withContext(Dispatchers.IO) { repository.saveMedicine(accountId, medicine.genericName) }
            loadLibrary()
            onResult(true)
        }
    }

    fun dismissEmergency() = _state.update { it.copy(emergencyMessage = null) }
    fun showDisclaimerAgain() = _state.update { it.copy(showDisclaimer = true) }
    fun hideDisclaimer() = _state.update { it.copy(showDisclaimer = false) }
    fun clearError() = _state.update { it.copy(errorMessage = null, authError = null) }
}
