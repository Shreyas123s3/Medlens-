package com.medlens.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFdaLabelParserTest {
    @Test fun parsesOnlySourcedFieldsAndFiltersDoseSentences() {
        val row = OpenFdaRecord(
            setId="fixture-1", effectiveTime="20260101",
            indications=listOf("For temporary relief of minor aches and pains. Take 2 tablets every 4 hours."),
            warnings=listOf("Stop use if an allergic reaction occurs."),
            openfda=OpenFdaMetadata(listOf("Acetaminophen"), listOf("HUMAN OTC DRUG"), listOf("ORAL")),
        )
        val medicine = OpenFdaLabelParser.parse(row,"Pain and fever", listOf("paracetamol"))!!
        assertEquals("For temporary relief of minor aches and pains.", medicine.uses)
        assertEquals("Stop use if an allergic reaction occurs.", medicine.warnings)
        assertTrue(medicine.unavailableFields.contains("mechanism"))
        assertEquals("OTC", medicine.otcStatus)
        assertEquals("Information unavailable", medicine.brandAvailability)
        assertEquals("fixture-1", medicine.sourceRecordId)
    }
    @Test fun rejectsUnknownRegulatoryClassAndCombination() {
        assertNull(OpenFdaLabelParser.parse(OpenFdaRecord(openfda=OpenFdaMetadata(listOf("x"), listOf("UNKNOWN"))),"Other"))
        assertNull(OpenFdaLabelParser.parse(OpenFdaRecord(openfda=OpenFdaMetadata(listOf("x", "y"), listOf("HUMAN OTC DRUG"))),"Other"))
        assertNull(OpenFdaLabelParser.parse(OpenFdaRecord(openfda=OpenFdaMetadata(listOf("Drug A, Drug B"), listOf("HUMAN OTC DRUG"))),"Other"))
        assertNull(OpenFdaLabelParser.parse(OpenFdaRecord(activeIngredient=listOf("Drug A 200 mg; Drug B 10 mg"), openfda=OpenFdaMetadata(listOf("Drug A"), listOf("HUMAN OTC DRUG"))),"Other"))
    }
}
