package com.medlens.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineSearchTest {
    private val items = listOf(
        medicine("acetaminophen", "Pain and fever", "paracetamol|apap"),
        medicine("albuterol", "Breathing", "salbutamol"),
        medicine("epinephrine", "Emergency", "adrenaline"),
    )
    private val aliases = mapOf("paracetamol" to "acetaminophen", "salbutamol" to "albuterol", "adrenaline" to "epinephrine")

    @Test fun exactAndSynonymSearch() {
        assertEquals("acetaminophen", MedicineSearch.find(items, "paracetamol", aliases).first().genericName)
        assertEquals("albuterol", MedicineSearch.find(items, "salbutamol", aliases).first().genericName)
        assertEquals("epinephrine", MedicineSearch.find(items, "adrenaline", aliases).first().genericName)
    }
    @Test fun genericPartialAndMisspellingSearch() {
        assertEquals("acetaminophen", MedicineSearch.find(items, "acetaminophen", aliases).first().genericName)
        assertEquals("acetaminophen", MedicineSearch.find(items, "acetamin", aliases).first().genericName)
        assertEquals("acetaminophen", MedicineSearch.find(items, "acetaminophn", aliases).first().genericName)
        assertTrue(MedicineSearch.find(items, "no such drug", aliases).isEmpty())
    }

    private fun medicine(name: String, category: String, synonyms: String) = Medicine(
        genericName=name, category=category, brands="", uses="", mechanism="", sideEffects="", precautions="",
        contraindications="", interactions="", storage="", whenToSeeDoctor="", source="test", synonyms=synonyms,
    )
}
