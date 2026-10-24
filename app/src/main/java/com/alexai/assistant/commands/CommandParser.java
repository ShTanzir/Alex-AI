package com.alexai.assistant.commands;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fast, offline matching for common commands. Anything it can't match falls through to Gemini. */
public final class CommandParser {
    private static final int CI = Pattern.CASE_INSENSITIVE;

    private static final Pattern CANCEL = Pattern.compile("^(cancel|stop|never ?mind|forget it|start over|new task)( that| it)?$", CI);
    private static final Pattern HELP = Pattern.compile("^(what can you do|what can alex do|help|what are your capabilities|show capabilities|(show|list) (me )?(the )?(media |all )?commands)$", CI);
    private static final Pattern PERMS = Pattern.compile("^(what permissions are missing|permissions|permission status|check permissions)$", CI);
    private static final Pattern SHOW_APPS = Pattern.compile("^(show|list)( me)?( my| all| the)?( installed)? (apps|applications)$", CI);
    private static final Pattern GO_HOME = Pattern.compile("^(go home|go to (the )?home( screen)?|home screen|show home screen)$", CI);
    private static final Pattern YT_A = Pattern.compile("^(?:(?:open|go to) youtube and )?(?:search|find|look up|look for) (?:on )?youtube (?:for )?(.+)$", CI);
    private static final Pattern YT_B = Pattern.compile("^(?:open|go to) youtube and (?:search|find|look up|look for) (?:for )?(.+)$", CI);
    private static final Pattern YT_C = Pattern.compile("^(?:search|find|look up|look for|play) (.+) on youtube$", CI);
    private static final Pattern ALARM = Pattern.compile("^set (?:an |the )?alarm (?:for|at) (\\d{1,2})(?::(\\d{2}))? ?(am|pm)?$", CI);
    private static final Pattern TIMER = Pattern.compile("^(?:set|start) (?:a |the )?timer (?:for )?(\\d{1,5}) (second|sec|minute|min|hour|hr)s?$", CI);
    private static final Pattern TORCH_A = Pattern.compile("^(?:turn |switch )?(on|off) (?:the )?(?:flashlight|torch)$", CI);
    private static final Pattern TORCH_B = Pattern.compile("^(?:turn |switch )(?:the )?(?:flashlight|torch) (on|off)$", CI);
    private static final Pattern MEDIA_A = Pattern.compile("^(pause|resume|play|stop)(?: the)?(?: music| media| song| playback)?$", CI);
    private static final Pattern MEDIA_B = Pattern.compile("^(?:play |go to |skip to )?(?:the )?(next|previous|skip)(?: track| song)?$", CI);
    private static final Pattern SETTINGS = Pattern.compile("^(?:open|launch|show) (?:the |my )?settings$", CI);
    private static final Pattern PANEL = Pattern.compile("^(?:(?:open|show|go to) )?(?:the |my )?(wi-?fi|bluetooth|mobile data|mobile network|airplane mode|hotspot|location|battery|storage|display|brightness|sound|accessibility)(?: settings)?$", CI);
    private static final Pattern DIAL = Pattern.compile("^(?:call|dial) ([+0-9 ()-]{3,20})$", CI);
    private static final Pattern SMS = Pattern.compile("^(?:text|sms|message) ([+0-9 ()-]{3,20}?) (?:saying|that says|with|:) (.+)$", CI);
    private static final Pattern MAPS_A = Pattern.compile("^(?:navigate to|directions to|take me to) (.+)$", CI);
    private static final Pattern MAPS_B = Pattern.compile("^(?:show|find) (.+) on (?:google )?maps$", CI);
    private static final Pattern WEB = Pattern.compile("^(?:search(?: the web| google)?(?: for)?|google|look up) (.+)$", CI);
    private static final Pattern NAV_BACK = Pattern.compile("^(?:go back|back|press back|navigate back)$", CI);
    private static final Pattern NAV_RECENTS = Pattern.compile("^(?:show |open )?(?:recent apps|recents|app switcher)$", CI);
    private static final Pattern SCROLL = Pattern.compile("^scroll (up|down)$", CI);
    private static final Pattern MEDIA_IMAGE = Pattern.compile("^(?:(?:show|view|open)(?: me)? (?:a |an )?(?:photo|image|picture)(?: viewer)?|(?:photo|image|picture) viewer)$", CI);
    private static final Pattern MEDIA_VIDEO = Pattern.compile("^(?:(?:play|open|watch) (?:a |my |the )?(?:local )?(?:video|movie)s?(?: player)?|video player)$", CI);
    private static final Pattern MEDIA_AUDIO = Pattern.compile("^(?:(?:open|launch)(?: the| my)? (?:music|audio) player|music player|play (?:a |an )?(?:local )?(?:song|track|audio file))$", CI);
    private static final Pattern UNINSTALL = Pattern.compile("^uninstall (?:the )?(?:app )?(.+)$", CI);
    private static final Pattern CLOSE = Pattern.compile("^(?:close|exit|quit) (?:the )?(?:app )?(.+)$", CI);
    private static final Pattern WHATSAPP = Pattern.compile("^(?:send )?(?:a )?whatsapp(?: message)?(?: to)? ([+0-9 ()-]{3,20}?)(?: (?:saying|that says|with|:) (.+))?$", CI);
    private static final Pattern OPEN = Pattern.compile("^(?:open|launch|start|go to) (?:my |the )?(.+?)(?: for me)?$", CI);

    private CommandParser() {}

    public static String normalize(String raw) {
        String t = raw == null ? "" : raw.trim();
        t = t.replaceFirst("(?i)^(?:hey|ok|okay|hi)\\s+alex\\b[,!.:]?\\s*", "");
        t = t.replaceFirst("(?i)^alex\\b[,!.:]?\\s*", "");
        t = t.replaceAll("(?i)\\ba\\.m\\.?", "am").replaceAll("(?i)\\bp\\.m\\.?", "pm");
        t = t.replaceFirst("(?i)\\s+please$", "").replaceFirst("(?i)^please\\s+", "");
        t = t.replaceFirst("[.!?,\\s]+$", "");
        return t.replaceAll("\\s+", " ").trim();
    }

    /** Returns a matching action, or null if the text isn't a recognised built-in command. */
    public static Action parse(String raw) {
        String t = normalize(raw);
        if (t.isEmpty()) return null;
        Matcher m;

        if (CANCEL.matcher(t).matches()) return act("CANCEL");
        if (HELP.matcher(t).matches()) return act("CAPABILITIES");
        if (PERMS.matcher(t).matches()) return act("PERMISSIONS_STATUS");
        if (SHOW_APPS.matcher(t).matches()) return act("SHOW_APPS");
        if (GO_HOME.matcher(t).matches()) return act("GO_HOME");

        if ((m = YT_A.matcher(t)).matches() || (m = YT_B.matcher(t)).matches() || (m = YT_C.matcher(t)).matches()) {
            return act("YOUTUBE_SEARCH", "query", m.group(1).trim());
        }

        if ((m = ALARM.matcher(t)).matches()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            String ap = m.group(3) == null ? "" : m.group(3).toLowerCase(Locale.ROOT);
            if (ap.equals("pm") && h < 12) h += 12;
            if (ap.equals("am") && h == 12) h = 0;
            return act("SET_ALARM", "hour", String.valueOf(h), "minute", String.valueOf(min));
        }
        if ((m = TIMER.matcher(t)).matches()) {
            long n = Long.parseLong(m.group(1));
            String u = m.group(2).toLowerCase(Locale.ROOT);
            long mult = u.startsWith("h") ? 3600 : u.startsWith("m") ? 60 : 1;
            return act("SET_TIMER", "seconds", String.valueOf(n * mult));
        }
        if ((m = TORCH_A.matcher(t)).matches() || (m = TORCH_B.matcher(t)).matches()) {
            return act("FLASHLIGHT", "state", m.group(1).toLowerCase(Locale.ROOT));
        }

        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\bmute\\b.*") && !lower.contains("unmute")) return act("VOLUME", "direction", "mute");
        if (lower.contains("volume") || lower.equals("louder") || lower.equals("quieter")) {
            if (lower.matches(".*\\b(increase|raise|up|louder|higher)\\b.*")) return act("VOLUME", "direction", "up");
            if (lower.matches(".*\\b(decrease|lower|reduce|down|quieter|softer)\\b.*")) return act("VOLUME", "direction", "down");
        }

        if ((m = MEDIA_A.matcher(t)).matches()) {
            String k = m.group(1).toLowerCase(Locale.ROOT);
            return act("MEDIA_KEY", "key", k.equals("resume") ? "play" : k);
        }
        if ((m = MEDIA_B.matcher(t)).matches()) {
            String k = m.group(1).toLowerCase(Locale.ROOT);
            return act("MEDIA_KEY", "key", k.equals("skip") ? "next" : k);
        }

        if (NAV_BACK.matcher(t).matches()) return act("NAV_BACK");
        if (NAV_RECENTS.matcher(t).matches()) return act("NAV_RECENTS");
        if ((m = SCROLL.matcher(t)).matches()) return act("SCROLL", "direction", m.group(1).toLowerCase(Locale.ROOT));
        if (MEDIA_IMAGE.matcher(t).matches()) return act("OPEN_MEDIA", "kind", "image");
        if (MEDIA_VIDEO.matcher(t).matches()) return act("OPEN_MEDIA", "kind", "video");
        if (MEDIA_AUDIO.matcher(t).matches()) return act("OPEN_MEDIA", "kind", "audio");
        if ((m = UNINSTALL.matcher(t)).matches()) return act("UNINSTALL_APP", "app", m.group(1).trim());
        if ((m = CLOSE.matcher(t)).matches()) return act("CLOSE_APP", "app", m.group(1).trim());
        if ((m = WHATSAPP.matcher(t)).matches()) {
            if (m.group(2) == null) return act("WHATSAPP_CHAT", "number", m.group(1).trim());
            return act("WHATSAPP_CHAT", "number", m.group(1).trim(), "message", m.group(2).trim());
        }

        if (SETTINGS.matcher(t).matches()) return act("OPEN_SETTINGS_PANEL", "panel", "settings");
        if ((m = PANEL.matcher(t)).matches()) return act("OPEN_SETTINGS_PANEL", "panel", panelFor(m.group(1)));

        if ((m = DIAL.matcher(t)).matches()) return act("DIAL", "number", m.group(1).trim());
        if ((m = SMS.matcher(t)).matches()) return act("SMS_DRAFT", "number", m.group(1).trim(), "message", m.group(2).trim());
        if ((m = MAPS_A.matcher(t)).matches() || (m = MAPS_B.matcher(t)).matches()) {
            return act("MAPS_SEARCH", "query", m.group(1).trim());
        }
        if ((m = WEB.matcher(t)).matches()) return act("WEB_SEARCH", "query", m.group(1).trim());

        if ((m = OPEN.matcher(t)).matches()) {
            String target = m.group(1).trim();
            String l = target.toLowerCase(Locale.ROOT);
            if (l.contains(" and ")) return null; // multi-step: let Gemini split it
            switch (l) {
                case "youtube": return act("YOUTUBE_OPEN");
                case "youtube music": return act("YOUTUBE_MUSIC_OPEN");
                case "calculator": return act("OPEN_UTILITY", "target", "calculator");
                case "calendar": return act("OPEN_UTILITY", "target", "calendar");
                case "camera": return act("OPEN_UTILITY", "target", "camera");
                case "contacts": return act("OPEN_UTILITY", "target", "contacts");
                case "maps": case "google maps": return act("OPEN_UTILITY", "target", "maps");
                case "gallery": case "photos": case "pictures": return act("OPEN_UTILITY", "target", "gallery");
                case "files": case "file manager": case "file picker": return act("OPEN_UTILITY", "target", "files");
                case "clock": case "alarms": return act("OPEN_CLOCK");
                default: return act("OPEN_APP", "app", target);
            }
        }
        return null;
    }

    private static String panelFor(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.startsWith("wi")) return "wifi";
        if (w.startsWith("mobile")) return "mobile";
        if (w.startsWith("airplane")) return "airplane";
        if (w.equals("brightness")) return "display";
        return w;
    }

    private static Action act(String id, String... kv) {
        Map<String, String> p = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) p.put(kv[i], kv[i + 1]);
        return new Action(id, p);
    }
}
