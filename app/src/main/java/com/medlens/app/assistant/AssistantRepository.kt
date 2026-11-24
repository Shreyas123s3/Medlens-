package com.medlens.app.assistant

import com.medlens.app.data.Medicine
import com.medlens.app.safety.EmergencyDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

data class AssistantResult(val text: String, val emergency: Boolean, val online: Boolean, val medicineNames: List<String> = emptyList())

class AssistantRepository(private val offline: OfflineAssistantService = OfflineAssistantService()) {
    suspend fun respond(
        text: String,
        consent: Boolean,
        remoteConfigured: Boolean,
        medicines: List<Medicine>,
        remote: suspend () -> AiChatResponse,
    ): AssistantResult {
        if (EmergencyDetector.isEmergency(text)) return AssistantResult("This may be an emergency. Call 112 in India now or go to the nearest emergency department. Do not wait for an online response.", true, false)
        if (consent && remoteConfigured) {
            try {
                val response = remote()
                if (response.type == "emergency") return AssistantResult("This may be an emergency. Call 112 in India now or go to the nearest emergency department. Do not wait for an online response.", true, true)
                val validated = ResponseValidator.validate(response, medicines)
                return AssistantResult(validated.message, false, true, validated.medicineNames)
            } catch (_: TimeoutCancellationException) { /* timeout falls back offline */ }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* graceful offline fallback */ }
        }
        return AssistantResult(offline.answer(text), false, false)
    }
}
