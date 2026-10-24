package com.alexai.assistant.ui.home;

public final class Message {
    public final String text;
    public final boolean fromUser;
    public final boolean error;
    public final long timestamp;

    public Message(String text, boolean fromUser, boolean error) {
        this(text, fromUser, error, System.currentTimeMillis());
    }

    public Message(String text, boolean fromUser, boolean error, long timestamp) {
        this.text = text;
        this.fromUser = fromUser;
        this.error = error;
        this.timestamp = timestamp;
    }
}
