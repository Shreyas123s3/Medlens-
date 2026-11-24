package com.medlens.app.assistant;

public final class OfflineAssistantService implements AiService {
    @Override public String answer(String message) {
        return "I can help organize your question, but I cannot diagnose, prescribe, suggest a medicine, or give doses or treatment durations. "
                + "How long has this been happening, and what else have you noticed? If this concerns a child, pregnancy or breastfeeding, an older adult, or a chronic condition, contact a pharmacist or doctor for personal advice. Check product labels. If symptoms are severe or rapidly worsening, seek urgent help.";
    }
}
