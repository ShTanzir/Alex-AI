package com.alexai.assistant.ai;

/** A failure with a message that is safe to show to the user. */
public class GeminiException extends Exception {
    public GeminiException(String message) { super(message); }
}
