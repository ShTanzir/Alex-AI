package com.alexai.assistant.ai;

import java.util.ArrayList;
import java.util.List;

/** Short-lived conversation context. Expires after 10 minutes of silence or when cleared. */
public class ConversationManager {
    public static final class Turn {
        public final boolean fromUser;
        public final String text;

        Turn(boolean fromUser, String text) {
            this.fromUser = fromUser;
            this.text = text.length() > 500 ? text.substring(0, 500) : text;
        }
    }

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_TURNS = 8;
    private final List<Turn> turns = new ArrayList<>();
    private long lastActivity;

    public synchronized void add(boolean fromUser, String text) {
        expireIfStale();
        turns.add(new Turn(fromUser, text));
        while (turns.size() > MAX_TURNS) turns.remove(0);
        lastActivity = System.currentTimeMillis();
    }

    /** Recent turns, always starting with a user turn (a Gemini requirement). */
    public synchronized List<Turn> recent() {
        expireIfStale();
        List<Turn> copy = new ArrayList<>(turns);
        while (!copy.isEmpty() && !copy.get(0).fromUser) copy.remove(0);
        return copy;
    }

    public synchronized void clear() { turns.clear(); }

    private void expireIfStale() {
        if (!turns.isEmpty() && System.currentTimeMillis() - lastActivity > TTL_MS) turns.clear();
    }
}
