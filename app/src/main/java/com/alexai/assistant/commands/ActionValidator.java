package com.alexai.assistant.commands;

import java.util.Map;
import java.util.regex.Pattern;

/** Local validation of every action, whether it came from the parser or from Gemini. */
public final class ActionValidator {
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()-]{3,20}$");
    private static final int MAX_PARAM = 200;
    private static final int MAX_MESSAGE = 500;

    private ActionValidator() {}

    /** Returns null if the action is valid, otherwise a short reason. */
    public static String validate(Action a) {
        if (a == null || a.id.isEmpty()) return "no action was given";
        ActionRegistry.Spec spec = ActionRegistry.get(a.id);
        if (spec == null) return "\"" + clip(a.id) + "\" isn't one of my supported actions";

        for (Map.Entry<String, String> e : a.params.entrySet()) {
            if (!spec.accepts(e.getKey())) return "unexpected parameter \"" + clip(e.getKey()) + "\"";
            int max = e.getKey().equals("message") ? MAX_MESSAGE : MAX_PARAM;
            if (e.getValue() != null && e.getValue().length() > max) return "parameter \"" + e.getKey() + "\" is too long";
        }
        for (String r : spec.required) {
            if (a.param(r).trim().isEmpty()) return "missing parameter \"" + r + "\"";
        }

        switch (a.id) {
            case "OPEN_SETTINGS_PANEL":
                return oneOf(a.param("panel"), ActionRegistry.PANELS, "panel");
            case "OPEN_UTILITY":
                return oneOf(a.param("target"), ActionRegistry.UTILITIES, "target");
            case "FLASHLIGHT":
                return oneOf(a.param("state"), ActionRegistry.ON_OFF, "state");
            case "VOLUME":
                return oneOf(a.param("direction"), ActionRegistry.VOLUME_DIRECTIONS, "direction");
            case "MEDIA_KEY":
                return oneOf(a.param("key"), ActionRegistry.MEDIA_KEYS, "key");
            case "SET_ALARM": {
                Integer h = parse(a.param("hour"));
                Integer m = parse(a.param("minute"));
                if (h == null || h < 0 || h > 23) return "the alarm hour must be 0-23";
                if (m == null || m < 0 || m > 59) return "the alarm minute must be 0-59";
                return null;
            }
            case "SET_TIMER": {
                Integer s = parse(a.param("seconds"));
                if (s == null || s < 1 || s > 86400) return "the timer must be between 1 second and 24 hours";
                return null;
            }
            case "DIAL":
                return PHONE.matcher(a.param("number").trim()).matches() ? null : "that doesn't look like a phone number";
            case "SMS_DRAFT":
                if (!PHONE.matcher(a.param("number").trim()).matches()) return "that doesn't look like a phone number";
                return null;
            case "SCROLL":
                return oneOf(a.param("direction"), ActionRegistry.SCROLL_DIRECTIONS, "direction");
            case "OPEN_MEDIA":
                return oneOf(a.param("kind"), ActionRegistry.MEDIA_KINDS, "media type");
            case "SHARE_TEXT":
                return oneOf(a.param("app"), ActionRegistry.MESSAGING_APPS, "messaging app");
            case "WHATSAPP_CHAT":
                if (!PHONE.matcher(a.param("number").trim()).matches()) return "that doesn't look like a phone number";
                return null;
            default:
                return null;
        }
    }

    private static String oneOf(String value, java.util.List<String> allowed, String name) {
        return allowed.contains(value.trim().toLowerCase(java.util.Locale.ROOT))
                ? null : "\"" + clip(value) + "\" isn't a supported " + name;
    }

    private static Integer parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static String clip(String s) { return s.length() > 40 ? s.substring(0, 40) + "..." : s; }
}
