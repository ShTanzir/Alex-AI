package com.alexai.assistant.commands;

import java.util.ArrayList;
import java.util.List;

/** In-memory log of recent actions (newest first). Cleared when the app process ends. */
public final class ActivityLog {
    public static final class Entry {
        public final long time = System.currentTimeMillis();
        public final String actionId, message;
        public final boolean success;

        Entry(String actionId, String message, boolean success) {
            this.actionId = actionId;
            this.message = message;
            this.success = success;
        }
    }

    private static final int MAX = 50;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private ActivityLog() {}

    public static synchronized void add(String actionId, String message, boolean success) {
        ENTRIES.add(0, new Entry(actionId, message, success));
        while (ENTRIES.size() > MAX) ENTRIES.remove(ENTRIES.size() - 1);
    }

    public static synchronized List<Entry> all() { return new ArrayList<>(ENTRIES); }

    public static synchronized void clear() { ENTRIES.clear(); }
}
