package com.alexai.assistant.ai;

import com.alexai.assistant.commands.Action;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Parses Gemini's JSON reply. Non-JSON text is treated as a plain chat reply and never executes anything. */
public final class AiIntentParser {
    public static final class AiIntent {
        public final String reply;
        public final Action action;

        AiIntent(String reply, Action action) {
            this.reply = reply;
            this.action = action;
        }
    }

    private AiIntentParser() {}

    public static AiIntent parse(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = nl >= 0 ? t.substring(nl + 1) : "";
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        try {
            JSONObject o = new JSONObject(t);
            String reply = o.optString("reply", "").trim();
            Action action = null;
            JSONObject ao = o.optJSONObject("action");
            if (ao != null) {
                Map<String, String> params = new HashMap<>();
                JSONObject p = ao.optJSONObject("params");
                if (p != null) {
                    Iterator<String> keys = p.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        params.put(k, p.optString(k, ""));
                    }
                }
                action = new Action(ao.optString("id", "").trim(), params);
            }
            return new AiIntent(reply, action);
        } catch (JSONException e) {
            return new AiIntent(t, null);
        }
    }
}
