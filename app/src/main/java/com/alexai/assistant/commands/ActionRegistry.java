package com.alexai.assistant.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The explicit allowlist of actions Alex can run. Anything not listed here is rejected. */
public final class ActionRegistry {
    public static final List<String> PANELS = Arrays.asList(
            "wifi", "bluetooth", "mobile", "airplane", "hotspot", "location",
            "battery", "storage", "display", "sound", "accessibility", "settings");
    public static final List<String> UTILITIES = Arrays.asList(
            "calculator", "calendar", "camera", "contacts", "gallery", "maps", "files");
    public static final List<String> MEDIA_KEYS = Arrays.asList("play", "pause", "next", "previous", "stop");
    public static final List<String> VOLUME_DIRECTIONS = Arrays.asList("up", "down", "mute");
    public static final List<String> ON_OFF = Arrays.asList("on", "off");
    public static final List<String> SCROLL_DIRECTIONS = Arrays.asList("up", "down");
    public static final List<String> MEDIA_KINDS = Arrays.asList("image", "video", "audio");
    public static final List<String> MESSAGING_APPS = Arrays.asList("whatsapp", "telegram", "messenger", "instagram");

    public static final class Spec {
        public final String id;
        public final String description;
        public final boolean requiresConfirmation;
        public final List<String> required;
        public final List<String> optional;

        Spec(String id, boolean confirm, String description, List<String> required, List<String> optional) {
            this.id = id;
            this.requiresConfirmation = confirm;
            this.description = description;
            this.required = required;
            this.optional = optional;
        }

        public boolean accepts(String param) {
            return required.contains(param) || optional.contains(param);
        }

        public String signature() {
            List<String> all = new ArrayList<>(required);
            for (String o : optional) all.add(o + "?");
            return id + "(" + String.join(", ", all) + ")";
        }
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    private static List<String> list(String csv) {
        if (csv.isEmpty()) return Collections.emptyList();
        return Arrays.asList(csv.split(","));
    }

    private static void add(String id, boolean confirm, String desc, String required, String optional) {
        SPECS.put(id, new Spec(id, confirm, desc, list(required), list(optional)));
    }

    static {
        add("OPEN_APP", false, "Open an installed app by its name", "app", "");
        add("SHOW_APPS", false, "List installed apps Alex can open", "", "");
        add("GO_HOME", false, "Go to the Android home screen", "", "");
        add("YOUTUBE_OPEN", false, "Open YouTube (website if the app is missing)", "", "");
        add("YOUTUBE_MUSIC_OPEN", false, "Open YouTube Music", "", "");
        add("YOUTUBE_SEARCH", false, "Search YouTube for a query", "query", "");
        add("WEB_SEARCH", false, "Search the web for a query", "query", "");
        add("MAPS_SEARCH", false, "Search or navigate to a place in a maps app", "query", "");
        add("OPEN_SETTINGS_PANEL", false,
                "Open an Android settings screen. panel is one of: " + String.join("|", PANELS), "panel", "");
        add("OPEN_UTILITY", false,
                "Open a utility app. target is one of: " + String.join("|", UTILITIES), "target", "");
        add("OPEN_CLOCK", false, "Open the clock app", "", "");
        add("FLASHLIGHT", false, "Turn the flashlight on or off. state is on|off", "state", "");
        add("VOLUME", false,
                "Change media volume. direction is one of: " + String.join("|", VOLUME_DIRECTIONS), "direction", "");
        add("MEDIA_KEY", false,
                "Send a media key to whatever app is playing. key is one of: " + String.join("|", MEDIA_KEYS), "key", "");
        add("SET_ALARM", false, "Open the clock app to set an alarm. hour is 0-23, minute is 0-59", "hour,minute", "label");
        add("SET_TIMER", false, "Open the clock app to start a timer. seconds is 1-86400", "seconds", "");
        add("DIAL", true, "Open the phone dialer with a number (user presses call)", "number", "");
        add("SMS_DRAFT", true, "Open the messaging app with a drafted SMS (user presses send)", "number,message", "");
        add("NAV_BACK", false, "Press Back in the app underneath Alex (needs the optional Alex accessibility service)", "", "");
        add("NAV_RECENTS", false, "Show recent apps (needs the optional Alex accessibility service)", "", "");
        add("SCROLL", false, "Scroll the app underneath Alex. direction is up|down (needs the optional accessibility service)", "direction", "");
        add("CLOSE_APP", false, "Leave an app by going Home (apps cannot be force-closed)", "app", "");
        add("UNINSTALL_APP", true, "Open Android's uninstall confirmation for an installed app", "app", "");
        add("OPEN_MEDIA", false, "Open Alex's own viewer or player. kind is one of: " + String.join("|", MEDIA_KINDS), "kind", "");
        add("WHATSAPP_CHAT", true, "Open a WhatsApp chat by phone number (with country code) and an optional drafted message; the user presses send", "number", "message");
        add("SHARE_TEXT", true, "Share a drafted text to a messaging app. app is one of: " + String.join("|", MESSAGING_APPS) + ". The user picks the contact and presses send", "app,message", "");
        add("CAPABILITIES", false, "Explain what Alex can do", "", "");
        add("PERMISSIONS_STATUS", false, "Report which permissions or setup steps are missing", "", "");
        add("CANCEL", false, "Cancel the current task and clear conversation context", "", "");
    }

    private ActionRegistry() {}

    public static Spec get(String id) { return SPECS.get(id); }

    public static Collection<Spec> all() { return Collections.unmodifiableCollection(SPECS.values()); }

    /** Human-readable description shown in the confirmation dialog. */
    public static String describe(Action a) {
        switch (a.id) {
            case "DIAL":
                return "Open the phone dialer with " + a.param("number")
                        + ".\n\nYou'll still need to press the call button yourself.";
            case "SMS_DRAFT":
                return "Open your messaging app with a draft to " + a.param("number")
                        + ":\n\n\"" + a.param("message")
                        + "\"\n\nNothing is sent until you press send there.";
            case "UNINSTALL_APP":
                return "Open Android's uninstall confirmation for \"" + a.param("app")
                        + "\".\n\nYou finish the uninstall in Android's own dialog.";
            case "WHATSAPP_CHAT":
                return "Open a WhatsApp chat with " + a.param("number")
                        + (a.param("message").isEmpty() ? "" : " and the draft:\n\n\"" + a.param("message") + "\"")
                        + "\n\nNothing is sent until you press send in WhatsApp.";
            case "SHARE_TEXT":
                return "Open " + a.param("app") + "'s share screen with this text:\n\n\"" + a.param("message")
                        + "\"\n\nYou choose who gets it and press send yourself.";
            default:
                return a.id;
        }
    }
}
