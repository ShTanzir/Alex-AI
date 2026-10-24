package com.alexai.assistant.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import com.alexai.assistant.storage.SecureSettingsRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Minimal Gemini REST client. Runs off the main thread; callbacks are delivered on the main thread. */
public class GeminiClient {
    public interface Callback {
        void onSuccess(String rawText);
        void onError(String message);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Pattern MODEL = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final int MAX_INPUT = 2000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final int ATTEMPTS = 3;

    private final Context context;
    private final SecureSettingsRepository settings;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private volatile HttpURLConnection current;

    public GeminiClient(Context context, SecureSettingsRepository settings) {
        this.context = context.getApplicationContext();
        this.settings = settings;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public void generate(final List<ConversationManager.Turn> history, String userText, final Callback cb) {
        final String key = settings.getApiKey();
        final String model = settings.getModel();
        if (key.isEmpty()) { fail(cb, "There's no Gemini API key yet. Add one in Settings."); return; }
        if (!MODEL.matcher(model).matches()) { fail(cb, "The model name in Settings isn't valid."); return; }
        if (!isOnline()) { fail(cb, "You're offline, so I can't reach Gemini."); return; }

        final String input = userText.length() > MAX_INPUT ? userText.substring(0, MAX_INPUT) : userText;
        final String persona = settings.getPersona();
        final int gen = ++generation;

        EXEC.execute(new Runnable() {
            @Override public void run() {
                try {
                    String body = buildBody(persona, history, input);
                    String response = post(key, model, body, gen);
                    final String text = GeminiResponseParser.extractText(response);
                    if (gen != generation) return;
                    main.post(new Runnable() { @Override public void run() { if (gen == generation) cb.onSuccess(text); } });
                } catch (GeminiException e) {
                    if (gen == generation) fail(cb, e.getMessage());
                } catch (IOException e) {
                    if (gen == generation) fail(cb, "I couldn't reach Gemini. Check your connection and try again.");
                } catch (JSONException e) {
                    if (gen == generation) fail(cb, "I couldn't build that request.");
                }
            }
        });
    }

    /** Cancels the in-flight request; its callback will not be delivered. */
    public void cancel() {
        generation++;
        final HttpURLConnection c = current;
        if (c != null) {
            new Thread(new Runnable() { @Override public void run() { c.disconnect(); } }).start();
        }
    }

    private void fail(final Callback cb, final String msg) {
        main.post(new Runnable() { @Override public void run() { cb.onError(msg); } });
    }

    private static String buildBody(String persona, List<ConversationManager.Turn> history, String input) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("systemInstruction", new JSONObject().put("parts",
                new JSONArray().put(new JSONObject().put("text", SystemPrompt.build(persona)))));
        JSONArray contents = new JSONArray();
        for (ConversationManager.Turn t : history) contents.put(turn(t.fromUser ? "user" : "model", t.text));
        contents.put(turn("user", input));
        root.put("contents", contents);
        root.put("generationConfig", new JSONObject()
                .put("responseMimeType", "application/json").put("temperature", 0.4));
        return root.toString();
    }

    private static JSONObject turn(String role, String text) throws JSONException {
        return new JSONObject().put("role", role)
                .put("parts", new JSONArray().put(new JSONObject().put("text", text)));
    }

    private String post(String key, String model, String body, int gen) throws GeminiException, IOException {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            if (gen != generation) throw new GeminiException("Cancelled.");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            current = c;
            try {
                c.setRequestMethod("POST");
                c.setConnectTimeout(10000);
                c.setReadTimeout(30000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("x-goog-api-key", key);
                c.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
                int code = c.getResponseCode();
                if (code == 200) return read(c.getInputStream());
                if ((code == 429 || code >= 500) && attempt < ATTEMPTS - 1) {
                    try { Thread.sleep(1000L << attempt); }
                    catch (InterruptedException e) { throw new GeminiException("Cancelled."); }
                    continue;
                }
                throw new GeminiException(messageFor(code));
            } finally {
                current = null;
                c.disconnect();
            }
        }
        throw new GeminiException("Gemini is unavailable right now. Try again later.");
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n, total = 0;
            while ((n = is.read(buf)) > 0) {
                total += n;
                if (total > MAX_RESPONSE_BYTES) throw new IOException("response too large");
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        }
    }

    static String messageFor(int code) {
        switch (code) {
            case 400: return "Gemini rejected the request. Check your API key and model name in Settings.";
            case 401:
            case 403: return "Gemini rejected the API key, or access isn't allowed. Check the key in Settings.";
            case 404: return "Gemini doesn't know that model. Check the model name in Settings.";
            case 429: return "Gemini's rate limit or quota has been reached. Try again in a bit.";
            default:
                return code >= 500 ? "Gemini is having problems right now. Try again later."
                        : "Gemini returned an error (" + code + ").";
        }
    }
}
