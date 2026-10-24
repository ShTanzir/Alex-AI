package com.alexai.assistant.commands;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.alexai.assistant.accessibility.AlexAccessibilityService;
import com.alexai.assistant.apps.AppLaunchManager;
import com.alexai.assistant.storage.SecureSettingsRepository;
import com.alexai.assistant.utils.DeviceSettingsManager;

/** Runs already-validated actions through Android APIs. Never executes anything not in the registry. */
public final class ActionExecutor {
    private final Context context;
    private final SecureSettingsRepository settings;
    private final AppLaunchManager apps;
    private final DeviceSettingsManager device;

    public ActionExecutor(Context context, SecureSettingsRepository settings) {
        this.context = context;
        this.settings = settings;
        this.apps = new AppLaunchManager(context);
        this.device = new DeviceSettingsManager(context);
    }

    public ActionResult execute(Action a) {
        try {
            switch (a.id) {
                case "OPEN_APP": return apps.openByName(a.param("app").trim());
                case "SHOW_APPS": return apps.listInstalled();
                case "GO_HOME": return device.goHome();
                case "YOUTUBE_OPEN": return apps.openYouTube();
                case "YOUTUBE_MUSIC_OPEN": return apps.openYouTubeMusic();
                case "YOUTUBE_SEARCH": return apps.youtubeSearch(a.param("query").trim());
                case "WEB_SEARCH": return device.webSearch(a.param("query").trim());
                case "MAPS_SEARCH": return device.mapsSearch(a.param("query").trim());
                case "OPEN_SETTINGS_PANEL": return device.openPanel(lower(a.param("panel")));
                case "OPEN_UTILITY": return device.openUtility(lower(a.param("target")));
                case "OPEN_CLOCK": return device.openClock();
                case "FLASHLIGHT": return device.flashlight(lower(a.param("state")).equals("on"));
                case "VOLUME": return device.volume(lower(a.param("direction")));
                case "MEDIA_KEY": return device.mediaKey(lower(a.param("key")));
                case "SET_ALARM":
                    return device.setAlarm(Integer.parseInt(a.param("hour").trim()),
                            Integer.parseInt(a.param("minute").trim()), a.param("label").trim());
                case "SET_TIMER": return device.setTimer(Integer.parseInt(a.param("seconds").trim()));
                case "DIAL": return device.dial(a.param("number").trim());
                case "SMS_DRAFT": return device.smsDraft(a.param("number").trim(), a.param("message"));
                case "NAV_BACK":
                    return viaAccessibility("Pressing Back.", true, new java.util.function.Supplier<ActionResult>() {
                        @Override public ActionResult get() { return AlexAccessibilityService.back(); } });
                case "NAV_RECENTS":
                    return viaAccessibility("Opening recent apps.", false, new java.util.function.Supplier<ActionResult>() {
                        @Override public ActionResult get() { return AlexAccessibilityService.recents(); } });
                case "SCROLL": {
                    final boolean down = lower(a.param("direction")).equals("down");
                    return viaAccessibility("Scrolling " + (down ? "down" : "up") + ".", true,
                            new java.util.function.Supplier<ActionResult>() {
                                @Override public ActionResult get() { return AlexAccessibilityService.scroll(down); } });
                }
                case "CLOSE_APP": {
                    ActionResult home = device.goHome();
                    if (!home.success) return home;
                    return ActionResult.ok("I can't force-close " + a.param("app").trim()
                            + ", so I took you to the home screen. To fully close it, swipe it away in Recents.");
                }
                case "UNINSTALL_APP": return apps.uninstall(a.param("app").trim());
                case "OPEN_MEDIA": return device.openMedia(lower(a.param("kind")));
                case "WHATSAPP_CHAT": return device.whatsappChat(a.param("number").trim(), a.param("message").trim());
                case "SHARE_TEXT": return device.shareText(lower(a.param("app")), a.param("message"));
                case "CAPABILITIES": return ActionResult.ok(CapabilityCatalog.summary());
                case "PERMISSIONS_STATUS": return permissionsStatus();
                case "CANCEL": return ActionResult.ok("Okay, cancelled. Starting fresh.");
                default: return ActionResult.fail("That action isn't available yet.");
            }
        } catch (RuntimeException e) {
            return ActionResult.fail("That action failed (" + e.getClass().getSimpleName() + ").");
        }
    }

    /**
     * Back/Recents/scroll act on whatever app is in front. Alex steps aside first so they act on the
     * app underneath, then reports the real result in a toast.
     */
    private ActionResult viaAccessibility(String what, boolean stepAside,
                                          final java.util.function.Supplier<ActionResult> op) {
        if (!AlexAccessibilityService.isRunning()) return ActionResult.fail(AlexAccessibilityService.OFF_MESSAGE);
        final Context app = context.getApplicationContext();
        if (stepAside && context instanceof Activity) ((Activity) context).moveTaskToBack(true);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                ActionResult r = op.get();
                ActivityLog.add("ACCESSIBILITY", r.message, r.success);
                Toast.makeText(app, r.message, Toast.LENGTH_SHORT).show();
            }
        }, stepAside ? 700 : 100);
        return ActionResult.ok(what + (stepAside ? " Alex steps aside first so this acts on the app underneath." : ""));
    }

    private ActionResult permissionsStatus() {
        boolean mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        StringBuilder sb = new StringBuilder();
        sb.append(mic ? "Microphone: allowed. " : "Microphone: not allowed yet. I'll ask when you tap the Alex button, or you can allow it in the Settings tab. ");
        sb.append(settings.hasApiKey() ? "Gemini API key: saved. " : "Gemini API key: missing. Add one in Settings to chat with Gemini. ");
        sb.append(AlexAccessibilityService.isRunning()
                ? "Accessibility service: on. "
                : "Accessibility service: off (optional; needed for Back, Recents and scroll in other apps). ");
        sb.append("Wake word isn't part of this version.");
        return ActionResult.ok(sb.toString());
    }

    private static String lower(String s) { return s.trim().toLowerCase(java.util.Locale.ROOT); }
}
