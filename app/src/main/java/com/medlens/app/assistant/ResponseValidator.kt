package com.medlens.app.assistant

import com.medlens.app.data.Medicine

data class ValidatedAssistantResponse(val message: String, val medicineNames: List<String>, val blocked: Boolean)

object ResponseValidator {
    private val unsafe = Regex("(?:\\b\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|μg|ml|cc|units?|teaspoons?|tablespoons?|tsp|tbsp|tablets?|caplets?|capsules?|sprays?|drops?)\\b|\\btake\\s+(?:one|two|three|\\d+)\\b|\\b(?:daily|twice daily|once daily)\\b|\\bevery\\s+\\d+\\s+(?:hours?|days?|weeks?|months?)\\b|\\b\\d+\\s+times\\s+(?:a|per)\\s+day\\b|\\b(?:for|within|after|over|under|more than|longer than|at least|up to)\\s+\\d+\\s+(?:hours?|days?|weeks?|months?)\\b)", RegexOption.IGNORE_CASE)

    fun validate(response: AiChatResponse, medicines: List<Medicine>): ValidatedAssistantResponse {
        val eligible = medicines.filter { it.otcStatus == "OTC" }
        val names = response.medicineNames.mapNotNull { name -> eligible.firstOrNull { it.genericName.equals(name, true) }?.genericName }.distinct()
        val content = render(response)
        if (unsafe.containsMatchIn(content)) return ValidatedAssistantResponse("I can share general educational information, but I cannot provide doses or treatment durations. Check the product label and ask a pharmacist or doctor.", emptyList(), true)
        val safeContent = medicines.filter { it.otcStatus != "OTC" }.fold(content) { text, medicine ->
            val patterns = (listOf(medicine.genericName) + medicine.synonyms.split('|')).filter(String::isNotBlank)
            patterns.fold(text) { current, name -> current.replace(Regex("(?<![A-Za-z0-9])${Regex.escape(name)}(?![A-Za-z0-9])", RegexOption.IGNORE_CASE), "this medicine") }
        }
        return ValidatedAssistantResponse(safeContent, names, false)
    }

    private fun render(response: AiChatResponse): String = buildList {
        response.message.takeIf(String::isNotBlank)?.let(::add)
        response.questions.take(2).filter(String::isNotBlank).forEach(::add)
        response.summary?.let { summary ->
            summary.situation.takeIf(String::isNotBlank)?.let(::add)
            summary.safeNextSteps.filter(String::isNotBlank).take(4).forEach { add("General information: $it") }
            summary.warningSigns.filter(String::isNotBlank).take(4).forEach { add("Warning sign: $it") }
            summary.disclaimer.takeIf(String::isNotBlank)?.let(::add)
        }
    }.joinToString("\n\n").ifBlank { "Information unavailable. Check the product label and ask a pharmacist or doctor." }
}
