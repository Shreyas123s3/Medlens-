package com.medlens.app.assistant

import retrofit2.http.Body
import retrofit2.http.POST

data class AiTurn(val role: String, val text: String)
data class AiMedicineFact(val name: String, val otc: Boolean, val facts: String)
data class AiChatRequest(val messages: List<AiTurn>, val medicines: List<AiMedicineFact>, val userContext: Map<String, String> = emptyMap())
data class AiSummary(val situation: String = "", val safeNextSteps: List<String> = emptyList(), val warningSigns: List<String> = emptyList(), val disclaimer: String = "")
data class AiChatResponse(val type: String = "followup", val message: String = "", val questions: List<String> = emptyList(), val summary: AiSummary? = null, val medicineNames: List<String> = emptyList())

interface RemoteAiApi {
    @POST("api/chat")
    suspend fun chat(@Body request: AiChatRequest): AiChatResponse
}
