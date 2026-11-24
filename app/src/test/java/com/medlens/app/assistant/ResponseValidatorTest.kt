package com.medlens.app.assistant

import com.medlens.app.data.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseValidatorTest {
    private fun medicine(name: String, otc: String) = Medicine(name,"test","","fact","","","","","","","","source",otcStatus=otc)
    @Test fun dropsUnknownAndPrescriptionNames() {
        val result = ResponseValidator.validate(AiChatResponse(message="General educational information.", medicineNames=listOf("OTC one", "Rx one", "unknown")), listOf(medicine("OTC one","OTC"), medicine("Rx one","RX")))
        assertEquals(listOf("OTC one"), result.medicineNames)
    }
    @Test fun blocksDosePatternsAndMedicineSuggestions() {
        val result = ResponseValidator.validate(AiChatResponse(message="Take 10 mg daily.", medicineNames=listOf("OTC one")), listOf(medicine("OTC one","OTC")))
        assertTrue(result.blocked)
        assertTrue(result.medicineNames.isEmpty())
        assertTrue(!result.message.contains("10 mg"))
    }
    @Test fun removesPrescriptionNamesFromResponseText() {
        val result = ResponseValidator.validate(AiChatResponse(message="Ask your doctor about Rx one."), listOf(medicine("Rx one", "RX")))
        assertTrue(!result.message.contains("Rx one", ignoreCase = true))
        assertTrue(result.message.contains("this medicine"))
    }
}
