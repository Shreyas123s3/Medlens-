package com.medlens.app.data

import java.text.Normalizer
import java.util.Locale

object MedicineSynonymMatcher {
    fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    fun canonicalize(query: String, synonyms: Map<String, String>): String {
        val normalized = normalize(query)
        synonyms.entries.sortedByDescending { normalize(it.key).length }.firstOrNull { normalize(it.key) == normalized }?.let { return it.value }
        return synonyms.entries.sortedByDescending { normalize(it.key).length }
            .firstOrNull { entry -> Regex("(?:^| )${Regex.escape(normalize(entry.key))}(?:$| )").containsMatchIn(normalized) }
            ?.value ?: query.trim()
    }

    fun aliasesFor(canonicalName: String, synonyms: Map<String, String>): List<String> {
        val canonical = normalize(canonicalName)
        return synonyms.filterValues { normalize(it) == canonical }.keys.toList()
    }
}
