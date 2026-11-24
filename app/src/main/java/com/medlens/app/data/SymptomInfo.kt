package com.medlens.app.data

data class SymptomInfo(
    val id: String,
    val title: String,
    val summary: String,
    val possibleCauses: List<String>,
    val associatedSymptoms: List<String>,
    val selfCare: List<String>,
    val warningSigns: List<String>,
    val whenToSeeDoctor: List<String>,
    val sourceName: String,
    val sourceUrl: String,
)
