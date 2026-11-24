package com.medlens.app.assistant;

import org.junit.Test;
import static org.junit.Assert.*;

public class QueryClassifierTest {
    @Test public void classifiesMedicineQueries() {
        assertTrue(QueryClassifier.isMedicineQuery("What are ibuprofen side effects?"));
        assertTrue(QueryClassifier.isMedicineQuery("Tell me about a tablet"));
    }
    @Test public void classifiesSymptomQueries() {
        assertFalse(QueryClassifier.isMedicineQuery("I have a headache and mild fever"));
        assertFalse(QueryClassifier.isMedicineQuery(null));
    }
    @Test public void symptomContextWinsWhenMedicineNameIsAlsoPresent() {
        assertTrue(QueryClassifier.isSymptomQuestion("I take ibuprofen and I have a headache"));
        assertFalse(QueryClassifier.isSymptomQuestion("What is ibuprofen used for?"));
    }
}
