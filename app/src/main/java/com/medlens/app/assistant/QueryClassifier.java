package com.medlens.app.assistant;

import java.util.Locale;

public final class QueryClassifier {
    private QueryClassifier() {}
    public static boolean isMedicineQuery(String text) {
        if (text == null) return false;
        String q = text.toLowerCase(Locale.ROOT);
        return q.contains("medicine") || q.contains("medication") || q.contains("tablet")
                || q.contains("capsule") || q.contains("drug") || q.contains("side effect")
                || q.contains("brand name") || q.contains("generic name")
                || q.contains("dose") || q.contains("dosage") || q.contains("prescription");
    }
    public static boolean isSymptomQuestion(String text) {
        if (text == null) return false;
        String q = text.toLowerCase(Locale.ROOT);
        return q.contains("i have") || q.contains("i feel") || q.contains("i am feeling") || q.contains("i'm feeling")
                || q.contains("my symptoms") || q.contains("symptom") || q.contains("headache") || q.contains("fever")
                || q.contains("cough") || q.contains("sore throat") || q.contains("pain") || q.contains("hurt")
                || q.contains("nausea") || q.contains("vomit") || q.contains("diarrhea") || q.contains("rash")
                || q.contains("pregnant") || q.contains("breastfeeding") || q.contains("child has") || q.contains("baby has");
    }
}
