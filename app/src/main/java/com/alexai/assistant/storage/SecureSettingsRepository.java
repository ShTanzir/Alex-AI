package com.alexai.assistant.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Preferences. The Gemini API key lives in EncryptedSharedPreferences (Android Keystore);
 * if secure storage is unavailable on a device the key is refused rather than stored in plain text.
 */
public class SecureSettingsRepository {
    public static final String DEFAULT_MODEL = "gemini-2.5-flash";
    public static final String DEFAULT_PERSONA =
            "You are Alex, a young, friendly, concise voice assistant living on the user's Android phone.";

    private static final String KEY_API = "gemini_api_key";
    private final Context app;
    private final SharedPreferences plain;
    private SharedPreferences secure;
    private boolean secureTried;

    public SecureSettingsRepository(Context context) {
        this.app = context.getApplicationContext();
        this.plain = app.getSharedPreferences("alex_settings", Context.MODE_PRIVATE);
    }

    private synchronized SharedPreferences secure() {
        if (!secureTried) {
            secureTried = true;
            try {
                MasterKey key = new MasterKey.Builder(app)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
                secure = EncryptedSharedPreferences.create(app, "alex_secure", key,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (Exception e) {
                secure = null;
            }
        }
        return secure;
    }

    public boolean isSecureStorageAvailable() { return secure() != null; }

    public boolean hasApiKey() { return !getApiKey().isEmpty(); }

    public String getApiKey() {
        SharedPreferences s = secure();
        return s == null ? "" : s.getString(KEY_API, "");
    }

    /** @return false if the key could not be stored securely. */
    public boolean setApiKey(String key) {
        SharedPreferences s = secure();
        if (s == null) return false;
        s.edit().putString(KEY_API, key.trim()).apply();
        return true;
    }

    public void clearApiKey() {
        SharedPreferences s = secure();
        if (s != null) s.edit().remove(KEY_API).apply();
    }

    public String getModel() { return plain.getString("model", DEFAULT_MODEL); }
    public void setModel(String m) { plain.edit().putString("model", m).apply(); }

    public String getPersona() { return plain.getString("persona", DEFAULT_PERSONA); }
    public void setPersona(String p) { plain.edit().putString("persona", p).apply(); }

    public boolean speakResponses() { return plain.getBoolean("speak_responses", true); }
    public void setSpeakResponses(boolean v) { plain.edit().putBoolean("speak_responses", v).apply(); }

    public boolean speakConfirmations() { return plain.getBoolean("speak_confirmations", true); }
    public void setSpeakConfirmations(boolean v) { plain.edit().putBoolean("speak_confirmations", v).apply(); }

    public float getSpeechRate() { return plain.getFloat("speech_rate", 1.0f); }
    public void setSpeechRate(float v) { plain.edit().putFloat("speech_rate", v).apply(); }

    public float getPitch() { return plain.getFloat("pitch", 1.0f); }
    public void setPitch(float v) { plain.edit().putFloat("pitch", v).apply(); }

    public String getLanguageTag() { return plain.getString("language", "en-US"); }
    public void setLanguageTag(String tag) { plain.edit().putString("language", tag).apply(); }

    /** 0 = follow system, 1 = light, 2 = dark. */
    public int getTheme() { return plain.getInt("theme", 0); }
    public void setTheme(int t) { plain.edit().putInt("theme", t).apply(); }

    public boolean keepHistory() { return plain.getBoolean("keep_history", true); }
    public void setKeepHistory(boolean v) { plain.edit().putBoolean("keep_history", v).apply(); }

    /** Chat history is stored in the encrypted store; if that is unavailable it is simply not saved. */
    public String loadHistory() {
        SharedPreferences s = secure();
        return s == null ? "" : s.getString("history", "");
    }

    public void saveHistory(String json) {
        SharedPreferences s = secure();
        if (s != null) s.edit().putString("history", json).apply();
    }

    public void clearHistory() {
        SharedPreferences s = secure();
        if (s != null) s.edit().remove("history").apply();
    }

    public String getShortcutsJson() { return plain.getString("shortcuts", "[]"); }
    public void setShortcutsJson(String json) { plain.edit().putString("shortcuts", json).apply(); }

    public static void applyTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : theme == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
