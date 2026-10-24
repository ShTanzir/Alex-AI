package com.alexai.assistant.ui.home;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Saves and restores the last 100 chat messages as JSON. */
public final class MessageCodec {
    static final int MAX = 100;

    private MessageCodec() {}

    public static String encode(List<Message> messages) {
        JSONArray arr = new JSONArray();
        int start = Math.max(0, messages.size() - MAX);
        try {
            for (int i = start; i < messages.size(); i++) {
                Message m = messages.get(i);
                arr.put(new JSONObject().put("t", m.text).put("u", m.fromUser).put("e", m.error).put("ts", m.timestamp));
            }
        } catch (JSONException e) {
            return "[]";
        }
        return arr.toString();
    }

    public static List<Message> decode(String json) {
        List<Message> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Message(o.optString("t", ""), o.optBoolean("u"), o.optBoolean("e"),
                        o.optLong("ts", System.currentTimeMillis())));
            }
        } catch (JSONException e) {
            out.clear();
        }
        return out;
    }
}
