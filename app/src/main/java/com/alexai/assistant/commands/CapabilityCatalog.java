package com.alexai.assistant.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Every capability from the PRD with an honest implementation status. */
public final class CapabilityCatalog {
    public enum Status {
        IMPLEMENTED("Working"),
        DEVICE_DEPENDENT("Works on some devices"),
        PLANNED("Planned"),
        UNSUPPORTED("Blocked by Android");

        public final String label;
        Status(String label) { this.label = label; }
    }

    public static final class Entry {
        public final String category, title, notes;
        public final Status status;

        Entry(String category, Status status, String title, String notes) {
            this.category = category;
            this.status = status;
            this.title = title;
            this.notes = notes;
        }

        public boolean matches(String q) {
            String s = q.toLowerCase();
            return title.toLowerCase().contains(s) || category.toLowerCase().contains(s)
                    || notes.toLowerCase().contains(s) || status.label.toLowerCase().contains(s);
        }
    }

    private static final List<Entry> ALL = new ArrayList<>();

    private static void a(String cat, Status st, String title, String notes) {
        ALL.add(new Entry(cat, st, title, notes));
    }

    static {
        Status OK = Status.IMPLEMENTED, DEV = Status.DEVICE_DEPENDENT, PLAN = Status.PLANNED, NO = Status.UNSUPPORTED;
        String AI = "AI & conversation", VOICE = "Voice", MEDIA = "Media", APPS = "Apps & navigation",
                MSG = "Messaging & calls", UTIL = "Utilities & settings", CTRL = "Control & privacy";

        a(AI, OK, "Chat with Gemini", "Needs internet and your own API key (Settings). Model is configurable.");
        a(AI, OK, "Understands varied phrasing", "Common commands match offline; everything else is interpreted by Gemini, then validated locally.");
        a(AI, OK, "Follow-up context", "Remembers the last few turns; expires after 10 minutes or on \"cancel\"/\"start over\".");
        a(AI, OK, "Allowlist + validation of every action", "Unknown actions, bad parameters and bypass attempts are rejected before anything runs.");
        a(AI, OK, "Saved conversation history", "Last 100 messages, stored encrypted on this device. Turn it off or clear it in Settings.");
        a(AI, PLAN, "Editable user messages", "Not built yet.");

        a(VOICE, OK, "Voice input", "Android speech recognizer. Needs microphone permission. Online/offline depends on your device's engine.");
        a(VOICE, DEV, "Bengali voice input", "Works only if your recognizer supports bn-BD. Choose it in Settings.");
        a(VOICE, OK, "Spoken replies", "Android text-to-speech. Separate switches for replies and confirmations.");
        a(VOICE, OK, "Speech rate and pitch", "Adjustable in Settings.");
        a(VOICE, DEV, "Male-sounding voice", "Picks a voice labelled male if your TTS engine has one. A young voice can't be guaranteed.");
        a(VOICE, OK, "Stop listening / stop speaking / cancel", "Tap the Alex button while it is listening, speaking or thinking.");
        a(VOICE, PLAN, "Voice picker screen", "Not built yet.");
        a(VOICE, PLAN, "\"Hey Alex\" wake word", "Not built. Needs a local detector and a visible foreground-service notification.");
        a(VOICE, PLAN, "Notification / quick-tile activation", "Not built yet.");
        a(VOICE, PLAN, "System assistant role", "Not built yet.");

        a(MEDIA, OK, "Open gallery app", "Opens your default gallery.");
        a(MEDIA, OK, "Alex image viewer with zoom", "Pick a photo with Android's file picker; pinch, double-tap, + / - and Fit. Shows name, size and dimensions.");
        a(MEDIA, OK, "Alex video player", "Pick a video; play, pause and seek with on-screen controls. Pauses when you leave the screen.");
        a(MEDIA, OK, "Alex music player", "Pick one audio file; play, pause, seek and jump 10 seconds. Single file only.");
        a(MEDIA, PLAN, "Playlists, background music, media notification", "Not built yet.");
        a(MEDIA, DEV, "Play / pause / next / previous / stop in other apps", "Sends a media key. Works only if the playing app responds to media keys.");
        a(MEDIA, OK, "Open YouTube", "Opens the app, or the website if it isn't installed.");
        a(MEDIA, OK, "Search YouTube", "Opens search results in the YouTube app or browser.");
        a(MEDIA, DEV, "Open YouTube Music", "Opens the app, or music.youtube.com if it isn't installed.");

        a(APPS, OK, "Open an app by name", "Matches installed launcher apps; asks you to be specific if several match.");
        a(APPS, OK, "List installed apps", "Lists launchable apps.");
        a(APPS, OK, "Go to home screen", "Standard Home intent.");
        a(APPS, DEV, "Back, Recents and scroll in other apps", "Needs the optional Alex accessibility service, which you turn on yourself. Alex steps aside, then acts on the app underneath. Some apps block it.");
        a(APPS, PLAN, "Tap and type in other apps", "Not built yet.");
        a(APPS, NO, "Force-close another app", "Android doesn't allow it. \"Close X\" takes you Home and tells you to swipe it away in Recents.");
        a(APPS, PLAN, "Install APK assistance", "Not built yet. Would use the system installer prompt.");
        a(APPS, OK, "Uninstall assistance", "Confirms first, then opens Android's own uninstall dialog. You finish it; Alex can't tell if you did.");

        a(MSG, OK, "Open WhatsApp / Telegram / Messenger / Instagram", "Launches the app if installed.");
        a(MSG, OK, "Draft an SMS", "Shows recipient and text for confirmation, then opens your SMS app. You press send.");
        a(MSG, OK, "Call via dialer", "Confirms the number, then opens the dialer. You press call.");
        a(MSG, DEV, "WhatsApp chat by phone number", "Confirms first, then opens a wa.me chat prefilled. Needs the country code; you press send.");
        a(MSG, DEV, "Share text to WhatsApp / Telegram / Messenger / Instagram", "Confirms first, then opens that app's share screen. You pick the contact and press send. Instagram support varies.");
        a(MSG, PLAN, "Open a specific chat in Telegram, Messenger, Instagram", "Each app limits this; not built yet.");
        a(MSG, NO, "Send messages or place calls automatically", "Alex never sends or calls without you pressing the final button.");

        a(UTIL, OK, "Media volume up / down / mute", "Uses the system audio manager. Some devices block mute.");
        a(UTIL, OK, "Brightness", "Opens Display settings; Alex doesn't change brightness itself.");
        a(UTIL, OK, "Wi-Fi settings", "Opens the screen; Android doesn't let apps toggle Wi-Fi.");
        a(UTIL, OK, "Bluetooth settings", "Opens the screen.");
        a(UTIL, OK, "Mobile network settings", "Opens the screen.");
        a(UTIL, OK, "Airplane mode settings", "Opens the screen.");
        a(UTIL, DEV, "Hotspot settings", "Opens wireless settings; hotspot lives there on most devices.");
        a(UTIL, OK, "Location settings", "Opens the screen.");
        a(UTIL, DEV, "Flashlight", "Needs a camera flash. No camera permission required.");
        a(UTIL, OK, "Set an alarm", "Opens your clock app prefilled; confirm it there.");
        a(UTIL, OK, "Set a timer", "Opens your clock app with the timer set; confirm it there.");
        a(UTIL, OK, "Open clock", "Opens your clock app.");
        a(UTIL, OK, "Calculator", "Opens your default calculator.");
        a(UTIL, OK, "Calendar", "Opens your default calendar.");
        a(UTIL, OK, "Camera", "Opens your camera app.");
        a(UTIL, OK, "Contacts", "Opens your contacts app.");
        a(UTIL, OK, "Maps search / directions", "Opens your maps app with the query.");
        a(UTIL, OK, "Web search", "Opens a search in your browser.");
        a(UTIL, OK, "File picker", "Opens Android's file picker.");
        a(UTIL, OK, "Battery and storage settings", "Opens the screens.");

        a(CTRL, OK, "Ask what Alex can do", "Say \"what can you do?\"; this list is searchable.");
        a(CTRL, OK, "Permission status", "Say \"what permissions are missing?\" or see Settings.");
        a(CTRL, OK, "Confirmation before sensitive actions", "Calls and messages show exactly what will happen first.");
        a(CTRL, OK, "Remove saved API key", "Settings > Delete key.");
        a(CTRL, OK, "Clear conversation", "Settings > Clear conversation.");
        a(CTRL, OK, "Light, dark and system themes", "Settings > Appearance.");
        a(CTRL, OK, "User-defined shortcuts", "Activity tab: save a name and a built-in command; tap to run, long-press to delete. Shortcuts still go through confirmation.");
        a(CTRL, OK, "Recent activity log", "Activity tab lists what Alex did and whether it worked (kept in memory only).");
        a(CTRL, OK, "Accessibility on/off and disclosure", "Settings explains what the service does before sending you to Android's switch.");
    }

    private CapabilityCatalog() {}

    public static List<Entry> all() { return Collections.unmodifiableList(ALL); }

    public static int count(Status s) {
        int n = 0;
        for (Entry e : ALL) if (e.status == s) n++;
        return n;
        }

    public static String summary() {
        return "I know about " + ALL.size() + " capabilities: "
                + count(Status.IMPLEMENTED) + " working, "
                + count(Status.DEVICE_DEPENDENT) + " that depend on your device, "
                + count(Status.PLANNED) + " planned, and " + count(Status.UNSUPPORTED)
                + " that Android blocks. Open the Capabilities tab for the full searchable list. "
                + "Try: \"open YouTube\", \"search YouTube for Java tutorials\", \"set an alarm for 7 am\", "
                + "\"turn on the flashlight\", \"open Bluetooth settings\", \"pause the music\".";
    }
}
