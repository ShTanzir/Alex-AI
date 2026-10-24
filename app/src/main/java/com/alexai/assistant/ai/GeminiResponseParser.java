package com.alexai.assistant.ai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class GeminiResponseParser {
    private GeminiResponseParser() {}

    /** Pulls the model's text out of a generateContent response body. */
    public static String extractText(String json) throws GeminiException {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) {
                String reason = "";
                JSONObject feedback = root.optJSONObject("promptFeedback");
                if (feedback != null) reason = feedback.optString("blockReason", "");
                throw new GeminiException(reason.isEmpty()
                        ? "Gemini returned no answer."
                        : "Gemini blocked that request (" + reason + ").");
            }
            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            JSONArray parts = content == null ? null : content.optJSONArray("parts");
            if (parts == null) throw new GeminiException("Gemini returned an empty answer.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                sb.append(parts.getJSONObject(i).optString("text", ""));
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) throw new GeminiException("Gemini returned an empty answer.");
            return text;
        } catch (JSONException e) {
            throw new GeminiException("I couldn't read Gemini's response.");
        }
    }
}
