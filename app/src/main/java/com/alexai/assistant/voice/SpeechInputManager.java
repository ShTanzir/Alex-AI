package com.alexai.assistant.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/** Wraps Android's SpeechRecognizer. Must be used from the main thread. */
public class SpeechInputManager {
    public interface Listener {
        void onListening();
        void onPartial(String text);
        void onFinal(String text);
        /** The message is user-facing. */
        void onError(String message);
    }

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private boolean active;

    public SpeechInputManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public boolean isAvailable() { return SpeechRecognizer.isRecognitionAvailable(context); }

    public boolean isActive() { return active; }

    public void start(String languageTag) {
        if (active) return;
        if (!isAvailable()) {
            listener.onError("This device has no speech recognition service. You can type instead.");
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { listener.onListening(); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onEvent(int eventType, Bundle params) {}

                @Override public void onError(int error) {
                    active = false;
                    listener.onError(describe(error));
                }

                @Override public void onResults(Bundle results) {
                    active = false;
                    String best = first(results);
                    if (best.isEmpty()) listener.onError("I didn't catch that.");
                    else listener.onFinal(best);
                }

                @Override public void onPartialResults(Bundle partial) {
                    String p = first(partial);
                    if (!p.isEmpty()) listener.onPartial(p);
                }
            });
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        active = true;
        recognizer.startListening(i);
    }

    /** Stops listening and delivers whatever was heard so far. */
    public void stop() {
        if (recognizer != null && active) recognizer.stopListening();
    }

    public void destroy() {
        active = false;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    private static String first(Bundle b) {
        ArrayList<String> list = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return list == null || list.isEmpty() ? "" : list.get(0).trim();
    }

    private static String describe(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "I didn't catch that.";
            case SpeechRecognizer.ERROR_AUDIO: return "There was a problem with the microphone.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Speech recognition needs a network connection on this device.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission is missing.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Speech recognition is busy. Try again in a moment.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: return "That language isn't available for speech recognition on this device.";
            default: return "Speech recognition failed. You can type instead.";
        }
    }
}
