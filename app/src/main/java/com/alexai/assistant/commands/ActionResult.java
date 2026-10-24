package com.alexai.assistant.commands;

public final class ActionResult {
    public final boolean success;
    public final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ActionResult ok(String message) { return new ActionResult(true, message); }

    public static ActionResult fail(String message) { return new ActionResult(false, message); }
}
