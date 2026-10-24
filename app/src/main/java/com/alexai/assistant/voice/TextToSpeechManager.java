package com.alexai.assistant.voice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.alexai.assistant.storage.SecureSettingsRepository;

import java.util.Locale;
import java.util.Set;

public class TextToSpeechManager {
    public interface Listener {
        void onSpeakingChanged(boolean speaking);
    }

    private final SecureSettingsRepository settings;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ready;
    private String appliedLanguage = "";

    public TextToSpeechManager(Context context, SecureSettingsRepository settings, Listener listener) {
        this.settings = settings;
        this.listener = listener;
        tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS || tts == null) return;
                ready = true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { notifySpeaking(true); }
                    @Override public void onDone(String id) { notifySpeaking(false); }
                    @Override public void onError(String id) { notifySpeaking(false); }
                });
            }
        });
    }

    public boolean isReady() { return ready; }

    public void speak(String text) {
        if (!ready || tts == null || text == null || text.trim().isEmpty()) return;
        applyLanguage();
        tts.setSpeechRate(settings.getSpeechRate());
        tts.setPitch(settings.getPitch());
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alex-" + System.nanoTime());
    }

    public void stop() {
        if (tts != null) tts.stop();
        notifySpeaking(false);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ready = false;
    }

    private void applyLanguage() {
        String tag = settings.getLanguageTag();
        if (tag.equals(appliedLanguage)) return;
        appliedLanguage = tag;
        Locale locale = Locale.forLanguageTag(tag);
        int r = tts.setLanguage(locale);
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault());
            return;
        }
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return;
            for (Voice v : voices) {
                String name = v.getName().toLowerCase(Locale.ROOT);
                if (v.getLocale().getLanguage().equals(locale.getLanguage())
                        && name.contains("male") && !name.contains("female")) {
                    tts.setVoice(v);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // Some engines throw here; the default voice is fine.
        }
    }

    private void notifySpeaking(final boolean speaking) {
        main.post(new Runnable() { @Override public void run() { listener.onSpeakingChanged(speaking); } });
    }
}
