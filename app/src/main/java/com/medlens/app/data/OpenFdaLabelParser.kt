package com.medlens.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Converts the verified OpenFDA label fields without adding medical facts. */
object OpenFdaLabelParser {
    private val doseOrDuration = Regex(
        "(?:\\b\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|μg|g|grams?|ml|cc|units?|teaspoons?|tablespoons?|tsp|tbsp|%)\\b|" +
            "\\b\\d+\\s*(?:tablets?|caplets?|capsules?|sprays?|drops?|applications?)\\b|" +
            "\\b(?:once|twice|three times|four times)\\s+daily\\b|" +
            "\\b(?:daily|twice daily|once daily)\\b|" +
            "\\bevery\\s+\\d+\\s+(?:hours?|days?|weeks?|months?)\\b|" +
            "\\b(?:for|within|after|over|under|more than|longer than|at least|no more than|up to)\\s+\\d+\\s+(?:hours?|days?|weeks?|months?)\\b|" +
            "\\b(?:lasts?|persists?|continues?)\\s+(?:for\\s+)?(?:more than|longer than|at least|over)\\s+\\d+\\s+(?:hours?|days?|weeks?|months?)\\b|" +
            "\\b(?:dosage|dose|directions for use)\\b|\\btake\\s+(?:one|two|three|\\d+)\\b)", RegexOption.IGNORE_CASE
    )
    private val blockedHeading = Regex("^(?:dosage and administration|dosage forms and strengths|directions|how to use|dosing information)\\b", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]+>")

    fun parse(record: OpenFdaRecord, category: String, synonyms: List<String> = emptyList(), canonicalName: String? = null): Medicine? {
        val productType = record.openfda?.productTypes.orEmpty().firstOrNull { it.equals("HUMAN OTC DRUG", true) || it.equals("HUMAN PRESCRIPTION DRUG", true) } ?: return null
        val ingredients = record.openfda?.genericNames.orEmpty()
        if (ingredients.size != 1) return null
        if (ingredients.first().contains(',')) return null
        val activeIngredientText = record.activeIngredient.orEmpty().joinToString(" ")
        val activeStrengths = Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|µg|μg|g)\\b", RegexOption.IGNORE_CASE).findAll(activeIngredientText).count()
        if (activeStrengths > 1) return null
        val genericName = (canonicalName ?: ingredients.first()).trim().replace(Regex("\\s+"), " ").lowercase()
        if (genericName.isBlank()) return null
        val fields = linkedMapOf(
            "uses" to clean(record.indications ?: record.purpose),
            "howSupplied" to clean(record.howSupplied),
            "storage" to clean(record.storage),
            "warnings" to clean(record.boxedWarning ?: record.warnings ?: record.warningsAndPrecautions),
            "contraindications" to clean(record.contraindications),
            "sideEffects" to clean(record.adverseReactions),
            "interactions" to clean(record.drugInteractions ?: record.labInteractions),
            "mechanism" to clean(record.mechanism),
            "precautions" to clean(record.precautions ?: record.generalPrecautions ?: record.askDoctor ?: record.askDoctorOrPharmacist),
            "whenToSeeDoctor" to clean(record.stopUse ?: record.askDoctor ?: record.askDoctorOrPharmacist),
        )
        val sourceQuery = java.net.URLEncoder.encode("set_id:\"${record.setId.orEmpty()}\"", Charsets.UTF_8.name())
        val sourceUrl = "https://api.fda.gov/drug/label.json?search=$sourceQuery"
        return Medicine(
            genericName = genericName,
            category = category,
            brands = "",
            uses = fields.getValue("uses"), mechanism = fields.getValue("mechanism"),
            sideEffects = fields.getValue("sideEffects"), precautions = fields.getValue("precautions"),
            contraindications = fields.getValue("contraindications"), interactions = fields.getValue("interactions"),
            storage = fields.getValue("storage"), whenToSeeDoctor = fields.getValue("whenToSeeDoctor"),
            source = "U.S. FDA OpenFDA Drug Product Labeling",
            howSupplied = fields.getValue("howSupplied"), warnings = fields.getValue("warnings"),
            brandAvailability = "Information unavailable", synonyms = synonyms.joinToString("|"),
            otcStatus = if (productType.equals("HUMAN OTC DRUG", true)) "OTC" else "RX",
            retrievedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), sourceRecordId = record.setId.orEmpty(),
            sourceEffectiveDate = record.effectiveTime.orEmpty(), sourceUrl = sourceUrl,
            productType = productType.uppercase(), unavailableFields = fields.filterValues { it.isBlank() }.keys.joinToString(","),
        )
    }

    private fun clean(values: List<String>?): String = values.orEmpty().flatMap { raw ->
        val text = raw.replace(tag, " ").replace("&nbsp;", " ").replace("&amp;", "&").replace(Regex("\\s+"), " ")
        Regex("(?<=[.!?])\\s+(?=[A-Z0-9])").split(text)
    }.map { it.trim().trim('•', '*', '-') }
        .filter { it.isNotBlank() && !blockedHeading.containsMatchIn(it) && !doseOrDuration.containsMatchIn(it) }
        .distinctBy { it.lowercase() }
        .joinToString("\n")
}
