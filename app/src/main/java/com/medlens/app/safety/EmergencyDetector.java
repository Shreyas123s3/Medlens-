package com.medlens.app.safety;

import java.util.Locale;

public final class EmergencyDetector {
    private EmergencyDetector() {}
    private static final String[][] GROUPS = {
        {"chest pain", "chest hurts", "pain in my chest", "pressure in my chest", "seene mein dard", "seene me dard", "seena dard", "chest paIn"},
        {"can't breathe", "cannot breathe", "can t breathe", "cant breath", "cant brethe", "trouble breathing", "difficulty breathing", "shortness of breath", "breathless", "not able to breathe", "saans nahi aa rahi", "saans nahin aa rahi", "saans nahi le pa raha", "saans nahi le pa rahi", "saans nhi aa rahi", "saas nahi aa rahi"},
        {"throat is closing", "swelling of my tongue", "swollen tongue", "severe allergic", "anaphylaxis", "face swelling and breathing"},
        {"fainted", "fainting", "unconscious", "not waking", "passed out", "behosh", "hosh nahi", "not conscious"},
        {"face drooping", "face is drooping", "one-sided weakness", "one side is weak", "slurred speech", "speech is slurred", "sudden weakness", "can't lift my arm", "cannot lift my arm", "sudden confusion", "sudden vision loss", "face is uneven", "cannot move one side"},
        {"severe bleeding", "bleeding heavily", "won't stop bleeding", "cannot stop bleeding", "bleeding won't stop", "spurting blood", "blood will not stop"},
        {"worst headache", "sudden severe headache", "thunderclap headache", "sudden worst headache", "headache suddenly severe"}
    };
    public static boolean isEmergency(String text) {
        String s = text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replace("can't", "cannot").replace("can’t", "cannot")
                .replace("won't", "will not").replace("won’t", "will not")
                .replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        for (String[] group : GROUPS) for (String term : group) {
            String normalizedTerm = term.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
            if (s.contains(normalizedTerm)) return true;
        }
        return false;
    }
}
