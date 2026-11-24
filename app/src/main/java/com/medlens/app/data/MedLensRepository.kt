package com.medlens.app.data

import android.content.Context
import android.util.Base64
import com.medlens.app.BuildConfig
import com.medlens.app.assistant.AiChatRequest
import com.medlens.app.assistant.AiChatResponse
import com.medlens.app.assistant.AiMedicineFact
import com.medlens.app.assistant.AiTurn
import com.medlens.app.assistant.RemoteAiApi
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.medLensPreferences by preferencesDataStore(name = "medlens_preferences")

class MedLensRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = MedLensDatabase.get(appContext)
    private val medicineDao = database.medicineDao()
    private val dao = database.medLensDao()
    private val preferences = appContext.medLensPreferences
    private val catalogImportMutex = Mutex()
    private val openFdaApi: OpenFdaApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        Retrofit.Builder().baseUrl("https://api.fda.gov/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(OpenFdaApi::class.java)
    }
    val remoteAiConfigured: Boolean get() = BuildConfig.BACKEND_URL.isNotBlank()
    private val remoteAiApi: RemoteAiApi? by lazy {
        BuildConfig.BACKEND_URL.trim().takeIf(String::isNotBlank)?.let { raw ->
            val base = if (raw.endsWith("/")) raw else "$raw/"
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()
            Retrofit.Builder().baseUrl(base).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(RemoteAiApi::class.java)
        }
    }
    @Volatile private var bundledCatalogLoaded = false
    private val medicineSynonyms: Map<String, String> by lazy {
        runCatching {
            val json = appContext.assets.open("medicine_synonyms.json").bufferedReader().use { it.readText() }
            Gson().fromJson<Map<String, String>>(json, object : TypeToken<Map<String, String>>() {}.type)
        }.getOrDefault(emptyMap())
    }

    val onboardingComplete: Flow<Boolean> = preferences.data.map { it[ONBOARDING_COMPLETE] ?: false }
    val activeAccountId: Flow<String?> = preferences.data.map { it[ACTIVE_ACCOUNT_ID] }
    fun darkMode(accountId: String?): Flow<Boolean> = if (accountId == null) flowOf(false) else preferences.data.map { it[darkModeKey(accountId)] ?: false }
    fun aiConsent(accountId: String?): Flow<Boolean?> = if (accountId == null) flowOf(null) else preferences.data.map { values ->
        if (values[aiConsentDecidedKey(accountId)] == true) values[aiConsentKey(accountId)] ?: false else null
    }

    suspend fun completeOnboarding() { preferences.edit { it[ONBOARDING_COMPLETE] = true; it.remove(LEGACY_CHAT_HISTORY) } }
    suspend fun setDarkMode(accountId: String, enabled: Boolean) { preferences.edit { it[darkModeKey(accountId)] = enabled } }
    suspend fun setAiConsent(accountId: String, enabled: Boolean) { preferences.edit { it[aiConsentKey(accountId)] = enabled; it[aiConsentDecidedKey(accountId)] = true } }

    suspend fun remoteAssistant(messages: List<ChatMessage>): AiChatResponse {
        val api = remoteAiApi ?: error("The MedLens backend URL is not configured.")
        val all = allMedicines()
        val conversationText = messages.takeLast(8).joinToString(" ") { it.text }
        val facts = all.filter { medicine ->
            val phrases = listOf(medicine.genericName) + medicine.synonyms.split('|').filter(String::isNotBlank)
            phrases.any { phrase -> phrase.isNotBlank() && conversationText.contains(phrase, ignoreCase = true) }
        }.take(12).map { medicine ->
            val factText = listOf("Uses: ${medicine.uses}", "Warnings: ${medicine.warnings}", "Precautions: ${medicine.precautions}", "Contraindications: ${medicine.contraindications}", "Interactions: ${medicine.interactions}", "Side effects: ${medicine.sideEffects}", "Storage: ${medicine.storage}")
                .filterNot { it.endsWith(": ") }.joinToString("\n").take(5000)
            AiMedicineFact(medicine.genericName, medicine.otcStatus == "OTC", factText)
        }
        val body = AiChatRequest(messages.takeLast(8).map { AiTurn(if (it.role == "assistant") "assistant" else "user", it.text.take(3000)) }, facts)
        var failure: Exception? = null
        for (attempt in 0..2) {
            try { return withTimeout(30_000) { api.chat(body) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                failure = error
                if (error is HttpException && error.code() in 400..499 && error.code() != 429) throw error
                if (attempt < 2) delay(500L * (1L shl attempt))
            }
        }
        throw failure ?: IllegalStateException("Assistant request failed")
    }
    suspend fun logout() { preferences.edit { it.remove(ACTIVE_ACCOUNT_ID) } }
    suspend fun account(id: String): UserAccount? = dao.account(id)

    suspend fun createAccount(name: String, email: String, password: CharArray): UserAccount {
        val normalizedEmail = email.trim().lowercase()
        require(name.trim().isNotEmpty()) { "Enter your name." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid email address." }
        require(password.size >= 8) { "Use a password with at least 8 characters." }
        require(dao.accountByEmail(normalizedEmail) == null) { "An account with this email already exists. Sign in instead." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val account = UserAccount(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = normalizedEmail,
            passwordSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
            passwordHash = derivePasswordHash(password, salt),
        )
        dao.createAccount(account)
        preferences.edit { it[ACTIVE_ACCOUNT_ID] = account.id }
        return account
    }

    suspend fun signIn(email: String, password: CharArray): UserAccount {
        val account = dao.accountByEmail(email.trim().lowercase()) ?: error("No account found for that email.")
        val salt = Base64.decode(account.passwordSalt, Base64.NO_WRAP)
        val supplied = derivePasswordHash(password, salt)
        if (!MessageDigest.isEqual(supplied.toByteArray(), account.passwordHash.toByteArray())) error("Email or password is incorrect.")
        preferences.edit { it[ACTIVE_ACCOUNT_ID] = account.id }
        return account
    }

    suspend fun updateProfile(account: UserAccount, name: String, email: String): UserAccount {
        val normalizedEmail = email.trim().lowercase()
        require(name.trim().isNotEmpty()) { "Enter your name." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid email address." }
        val existing = dao.accountByEmail(normalizedEmail)
        require(existing == null || existing.id == account.id) { "That email is already used by another account." }
        return account.copy(name = name.trim(), email = normalizedEmail).also { dao.updateAccount(it) }
    }

    suspend fun ensureBundledCatalogImported() {
        if (bundledCatalogLoaded) return
        catalogImportMutex.withLock {
            if (bundledCatalogLoaded) return
            val json = appContext.assets.open("medicines.json").bufferedReader().use { it.readText() }
            val medicines: List<Medicine> = Gson().fromJson(json, object : TypeToken<List<Medicine>>() {}.type)
            if (medicines.isNotEmpty()) medicineDao.insertAll(medicines)
            bundledCatalogLoaded = true
        }
    }

    suspend fun searchMedicines(query: String): List<Medicine> {
        ensureBundledCatalogImported()
        return MedicineSearch.find(medicineDao.all(), query, medicineSynonyms)
    }

    suspend fun allMedicines(): List<Medicine> {
        ensureBundledCatalogImported()
        return medicineDao.all()
    }

    suspend fun medicine(name: String): Medicine? {
        ensureBundledCatalogImported()
        val canonical = MedicineSynonymMatcher.canonicalize(name, medicineSynonyms)
        return medicineDao.get(canonical)
            ?: MedicineSearch.find(medicineDao.all(), name, medicineSynonyms).firstOrNull()
    }

    /** User-initiated live lookup. Only a single-ingredient label with explicit OTC/Rx class is cached. */
    suspend fun lookupOpenFda(query: String): Medicine? {
        ensureBundledCatalogImported()
        val canonical = MedicineSynonymMatcher.canonicalize(query, medicineSynonyms).trim()
        require(canonical.isNotBlank()) { "Enter a medicine name to look up." }
        val localMatch = medicineDao.all().firstOrNull { MedicineSynonymMatcher.normalize(it.genericName) == MedicineSynonymMatcher.normalize(canonical) }
        val expected = localMatch?.genericName ?: canonical
        val escaped = expected.replace("\\", "\\\\").replace("\"", "\\\"")
        var lastFailure: Exception? = null
        for (attempt in 0..2) {
            try {
                val response = openFdaApi.findLabels("openfda.generic_name:\"$escaped\"")
                val record = response.results.firstOrNull { item ->
                    val metadata = item.openfda
                    metadata?.genericNames?.size == 1 && metadata.genericNames.first().equals(expected, true) &&
                        metadata.productTypes.orEmpty().any { it.equals("HUMAN OTC DRUG", true) || it.equals("HUMAN PRESCRIPTION DRUG", true) }
                } ?: return null
                val category = localMatch?.category ?: "Other"
                val parsed = OpenFdaLabelParser.parse(record, category, MedicineSynonymMatcher.aliasesFor(expected, medicineSynonyms), expected) ?: return null
                medicineDao.upsert(parsed)
                return parsed
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (failure is HttpException && failure.code() == 404) return null
                lastFailure = failure
                if (attempt < 2) delay(400L * (1L shl attempt))
            }
        }
        throw lastFailure ?: IllegalStateException("OpenFDA lookup failed.")
    }
    suspend fun medicineByQuery(query: String): Medicine? = allMedicines().firstOrNull {
        query.contains(it.genericName, ignoreCase = true) || it.brands.split(',').any { brand -> brand.trim().isNotEmpty() && query.contains(brand.trim(), ignoreCase = true) }
    }
    suspend fun categories(): List<String> = allMedicines().map { it.category }.filter(String::isNotBlank).distinct().sorted()

    suspend fun savedMedicines(accountId: String): List<SavedMedicine> = dao.savedMedicines(accountId)
    suspend fun isSaved(accountId: String, name: String): Boolean = dao.isSaved(accountId, name)
    suspend fun saveMedicine(accountId: String, name: String) = dao.saveMedicine(SavedMedicine(accountId, name))
    suspend fun removeSavedMedicine(accountId: String, name: String) = dao.removeSavedMedicine(accountId, name)
    suspend fun toggleSaved(accountId: String, name: String) {
        if (dao.isSaved(accountId, name)) dao.removeSavedMedicine(accountId, name)
        else dao.saveMedicine(SavedMedicine(accountId, name))
    }
    suspend fun addSearch(accountId: String, query: String) {
        val value = query.trim()
        if (value.isNotEmpty()) {
            dao.addSearch(RecentSearch(accountId, value))
            dao.pruneSearches(accountId)
        }
    }
    suspend fun recentSearches(accountId: String): List<RecentSearch> = dao.recentSearches(accountId)
    suspend fun clearSearches(accountId: String) = dao.clearSearches(accountId)
    suspend fun recentlyViewed(accountId: String): List<RecentlyViewed> = dao.recentlyViewed(accountId)
    suspend fun addViewed(accountId: String, name: String) {
        dao.addViewed(RecentlyViewed(accountId, name))
        dao.pruneViewed(accountId)
    }

    suspend fun createChat(accountId: String, title: String = "New conversation"): ChatSession = ChatSession(UUID.randomUUID().toString(), accountId, title).also { dao.addChatSession(it) }
    suspend fun chatSessions(accountId: String): List<ChatSession> = dao.chatSessions(accountId)
    suspend fun chatSession(id: String, accountId: String): ChatSession? = dao.chatSession(id, accountId)
    suspend fun chatMessages(id: String, accountId: String): List<ChatMessage> = dao.chatMessages(id, accountId)
    suspend fun addChatMessage(sessionId: String, accountId: String, role: String, text: String) {
        dao.addChatMessage(ChatMessage(UUID.randomUUID().toString(), sessionId, accountId, role, text))
        if (role == "user") dao.chatSession(sessionId, accountId)?.let { session ->
            if (session.title == "New conversation") dao.addChatSession(session.copy(title = text.take(42)))
        }
    }
    suspend fun clearCurrentChat(sessionId: String, accountId: String) {
        dao.clearChatMessages(sessionId, accountId)
        dao.chatSession(sessionId, accountId)?.let { dao.addChatSession(it.copy(title = "New conversation")) }
    }
    suspend fun clearAllChats(accountId: String) {
        dao.clearChatMessagesForAccount(accountId)
        dao.clearChatSessions(accountId)
    }
    suspend fun clearAllAccountData(accountId: String) {
        dao.clearAllAccountData(accountId)
        preferences.edit {
            it.remove(ACTIVE_ACCOUNT_ID)
            it.remove(aiConsentKey(accountId))
            it.remove(aiConsentDecidedKey(accountId))
            it.remove(darkModeKey(accountId))
        }
    }

    fun symptomLibrary(): List<SymptomInfo> = runCatching {
        val json = appContext.assets.open("symptom_library.json").bufferedReader().use { it.readText() }
        Gson().fromJson<List<SymptomInfo>>(json, object : TypeToken<List<SymptomInfo>>() {}.type)
    }.getOrDefault(emptyList())

    suspend fun completeDisclaimer() { preferences.edit { it[DISCLAIMER_ACCEPTED] = true } }
    val disclaimerAccepted: Flow<Boolean> = preferences.data.map { it[DISCLAIMER_ACCEPTED] ?: false }

    private fun derivePasswordHash(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, 120_000, 256)
        return try {
            Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP)
        } finally { spec.clearPassword() }
    }

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        fun aiConsentKey(accountId: String) = booleanPreferencesKey("ai_consent_$accountId")
        fun aiConsentDecidedKey(accountId: String) = booleanPreferencesKey("ai_consent_decided_$accountId")
        val LEGACY_CHAT_HISTORY = stringPreferencesKey("chat_history")
        fun darkModeKey(accountId: String) = booleanPreferencesKey("dark_mode_$accountId")
    }
}
