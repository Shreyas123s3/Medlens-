package com.medlens.app.safety;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmergencyDetectorTest {
    @Test public void detectsEmergencyPhrasesCaseInsensitively() {
        assertTrue(EmergencyDetector.isEmergency("I have severe chest pain"));
        assertTrue(EmergencyDetector.isEmergency("I can't breathe"));
        assertTrue(EmergencyDetector.isEmergency("My face is drooping and speech is slurred"));
    }
    @Test public void allowsNonEmergencyExample() {
        assertFalse(EmergencyDetector.isEmergency("I have a headache and mild fever"));
        assertFalse(EmergencyDetector.isEmergency(""));
    }
    @Test public void detectsThirtyFiveEnglishMisspellingAndHinglishEmergencyPhrases() {
        String[] phrases = {
            "chest pain", "chest hurts", "pressure in my chest", "seene mein dard", "seene me dard",
            "can't breathe", "cannot breathe", "cant breath", "cant brethe", "trouble breathing",
            "difficulty breathing", "shortness of breath", "saans nahi aa rahi", "saans nahin aa rahi",
            "saans nahi le pa raha", "saans nahi le pa rahi", "saans nhi aa rahi", "saas nahi aa rahi",
            "severe allergic reaction", "throat is closing", "swollen tongue", "anaphylaxis",
            "fainted", "fainting", "unconscious", "passed out", "behosh", "hosh nahi",
            "face drooping", "one-sided weakness", "slurred speech", "sudden vision loss",
            "severe bleeding", "bleeding heavily", "spurting blood", "worst headache",
            "sudden severe headache", "thunderclap headache"
        };
        assertTrue("test set should cover at least 30 phrases", phrases.length >= 30);
        for (String phrase : phrases) assertTrue("not detected: " + phrase, EmergencyDetector.isEmergency(phrase));
    }
}
