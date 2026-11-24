package com.medlens.app.data

object MedicineSearch {
    fun find(medicines: List<Medicine>, query: String, synonyms: Map<String, String>): List<Medicine> {
        val normalized = MedicineSynonymMatcher.normalize(query)
        if (normalized.isBlank()) return medicines.sortedBy { it.genericName.lowercase() }

        val canonical = MedicineSynonymMatcher.normalize(MedicineSynonymMatcher.canonicalize(query, synonyms))
        val terms = linkedSetOf(normalized, canonical)
        synonyms.entries.filter { (alias, _) ->
            val cleanAlias = MedicineSynonymMatcher.normalize(alias)
            Regex("(?:^| )${Regex.escape(cleanAlias)}(?:$| )").containsMatchIn(normalized)
        }.forEach { terms += MedicineSynonymMatcher.normalize(it.value) }

        return medicines.mapNotNull { medicine ->
            val candidates = (listOf(medicine.genericName, medicine.category, medicine.brands) + medicine.synonyms.split('|'))
                .map(MedicineSynonymMatcher::normalize)
                .filter(String::isNotBlank)
            val score: Int? = terms.mapNotNull { term -> candidates.mapNotNull { candidate -> score(term, candidate) }.minOrNull() }.minOrNull()
            if (score == null) null else medicine to score
        }.sortedWith(compareBy<Pair<Medicine, Int>> { it.second }.thenBy { it.first.genericName.lowercase() }).map { it.first }
    }

    private fun score(query: String, candidate: String): Int? {
        if (query == candidate) return 0
        if (candidate.startsWith(query)) return 1
        if (candidate.contains(query)) return 2
        val queryWords = query.split(' ').filter(String::isNotBlank)
        val candidateWords = candidate.split(' ').filter(String::isNotBlank)
        if (queryWords.all { word -> candidateWords.any { it == word || it.startsWith(word) } }) return 3
        if (query.length >= 5) {
            val maxDistance = if (query.length >= 10) 2 else 1
            val distance = candidateWords.minOfOrNull { levenshtein(query, it) } ?: return null
            if (distance <= maxDistance) return 4 + distance
        }
        return null
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                val substitution = previous[j] + if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, substitution)
            }
            previous = current
        }
        return previous[right.length]
    }
}
