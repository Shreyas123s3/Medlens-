package com.medlens.app.assistant

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantRepositoryTest {
    @Test fun usesOfflineFallbackWhenRemoteFails() = runBlocking {
        val result = AssistantRepository().respond("general question", true, true, emptyList()) { error("network failure") }
        assertFalse(result.online)
        assertTrue(result.text.contains("cannot diagnose"))
    }
    @Test fun emergencyNeverInvokesRemote() = runBlocking {
        var called = false
        val result = AssistantRepository().respond("saans nahi aa rahi", true, true, emptyList()) { called=true; AiChatResponse() }
        assertTrue(result.emergency)
        assertFalse(called)
        assertTrue(result.text.contains("112"))
    }
    @Test fun noConsentUsesOfflineResponse() = runBlocking {
        var called = false
        val result = AssistantRepository().respond("question", false, true, emptyList()) { called=true; AiChatResponse() }
        assertFalse(called)
        assertFalse(result.online)
    }
    @Test fun server500AndRateLimit429FallBackOffline() = runBlocking {
        for (code in listOf(500, 429)) {
            val error = HttpException(Response.error<AiChatResponse>(code, "unavailable".toResponseBody("text/plain".toMediaType())))
            val result = AssistantRepository().respond("question", true, true, emptyList()) { throw error }
            assertFalse("HTTP $code must select offline mode", result.online)
            assertTrue(result.text.contains("cannot diagnose"))
        }
    }
    @Test fun slowBackendTimesOutAndFallsBackOffline() = runBlocking {
        val result = AssistantRepository().respond("question", true, true, emptyList()) {
            withTimeout(1) { delay(10_000); AiChatResponse() }
        }
        assertFalse(result.online)
        assertTrue(result.text.contains("cannot diagnose"))
    }
}
