package com.alexai.assistant.utils;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;

import com.alexai.assistant.commands.ActionResult;
import com.alexai.assistant.ui.media.MediaViewerActivity;

import java.util.Locale;

/** System-facing actions: settings screens, utilities, volume, flashlight, alarms, dialer, SMS drafts. */
public class DeviceSettingsManager {
    private final Context context;
    private String torchId;
    private boolean torchSearched;

    public DeviceSettingsManager(Context context) { this.context = context; }

    private ActionResult start(Intent i, String ok, String fail) {
        try {
            context.startActivity(i);
            return ActionResult.ok(ok);
        } catch (ActivityNotFoundException e) {
            return ActionResult.fail(fail);
        } catch (SecurityException e) {
            return ActionResult.fail("Android blocked that action.");
        }
    }

    public ActionResult openPanel(String panel) {
        String action, name;
        switch (panel) {
            case "wifi": action = Settings.ACTION_WIFI_SETTINGS; name = "Wi-Fi settings"; break;
            case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; name = "Bluetooth settings"; break;
            case "mobile": action = Settings.ACTION_NETWORK_OPERATOR_SETTINGS; name = "mobile network settings"; break;
            case "airplane": action = Settings.ACTION_AIRPLANE_MODE_SETTINGS; name = "airplane mode settings"; break;
            case "hotspot": action = Settings.ACTION_WIRELESS_SETTINGS; name = "wireless settings, where hotspot lives on most devices"; break;
            case "location": action = Settings.ACTION_LOCATION_SOURCE_SETTINGS; name = "location settings"; break;
            case "battery": action = Settings.ACTION_BATTERY_SAVER_SETTINGS; name = "battery settings"; break;
            case "storage": action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS; name = "storage settings"; break;
            case "display": action = Settings.ACTION_DISPLAY_SETTINGS; name = "display settings, where you can change brightness"; break;
            case "sound": action = Settings.ACTION_SOUND_SETTINGS; name = "sound settings"; break;
            case "accessibility": action = Settings.ACTION_ACCESSIBILITY_SETTINGS; name = "accessibility settings"; break;
            default: action = Settings.ACTION_SETTINGS; name = "settings"; break;
        }
        return start(new Intent(action), "Opened " + name + ". Android doesn't let me change this for you, so you can do it there.",
                "I couldn't open that settings screen on this device.");
    }

    public ActionResult openUtility(String target) {
        Intent i;
        switch (target) {
            case "calculator": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR); break;
            case "calendar": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR); break;
            case "contacts": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS); break;
            case "gallery": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY); break;
            case "maps": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS); break;
            case "camera": i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA); break;
            default:
                i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                break;
        }
        return start(i, "Opened " + (target.equals("files") ? "the file picker" : target) + ".",
                "I couldn't find a " + target + " app on this device.");
    }

    public ActionResult webSearch(String query) {
        Intent i = new Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query);
        ActionResult r = start(i, "Searching the web for \"" + query + "\".", "");
        if (r.success) return r;
        return start(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))),
                "Searching the web for \"" + query + "\".", "I couldn't find a browser to search with.");
    }

    public ActionResult mapsSearch(String query) {
        return start(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))),
                "Opened maps for \"" + query + "\".", "I couldn't find a maps app on this device.");
    }

    public ActionResult goHome() {
        return start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                "Went to the home screen.", "I couldn't reach the home screen.");
    }

    public ActionResult openClock() {
        return start(new Intent(AlarmClock.ACTION_SHOW_ALARMS), "Opened your clock app.", "I couldn't find a clock app.");
    }

    public ActionResult setAlarm(int hour, int minute, String label) {
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (!label.isEmpty()) i.putExtra(AlarmClock.EXTRA_MESSAGE, label);
        return start(i, String.format(Locale.US, "Opened your clock app with an alarm for %02d:%02d. Check it there; I can't confirm it was saved.", hour, minute),
                "I couldn't find a clock app to set the alarm.");
    }

    public ActionResult setTimer(int seconds) {
        Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        return start(i, "Opened your clock app with a " + describeSeconds(seconds) + " timer. Check it there.",
                "I couldn't find a clock app to set the timer.");
    }

    public ActionResult dial(String number) {
        return start(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))),
                "Opened the dialer with " + number + ". Press the call button when you're ready.",
                "I couldn't find a phone app.");
    }

    public ActionResult smsDraft(String number, String message) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))
                .putExtra("sms_body", message);
        return start(i, "Opened your messaging app with the draft. Nothing is sent until you press send there.",
                "I couldn't find a messaging app.");
    }

    public ActionResult volume(String direction) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ActionResult.fail("I can't reach the audio system.");
        try {
            if (direction.equals("mute")) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                return ActionResult.ok("Asked Android to mute media volume.");
            }
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    direction.equals("up") ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI);
            int now = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            return ActionResult.ok("Media volume is now " + now + " of " + max + ".");
        } catch (SecurityException e) {
            return ActionResult.fail("Android blocked the volume change (Do Not Disturb may be on).");
        }
    }

    public ActionResult mediaKey(String key) {
        int code;
        switch (key) {
            case "play": code = KeyEvent.KEYCODE_MEDIA_PLAY; break;
            case "pause": code = KeyEvent.KEYCODE_MEDIA_PAUSE; break;
            case "next": code = KeyEvent.KEYCODE_MEDIA_NEXT; break;
            case "previous": code = KeyEvent.KEYCODE_MEDIA_PREVIOUS; break;
            default: code = KeyEvent.KEYCODE_MEDIA_STOP; break;
        }
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ActionResult.fail("I can't reach the audio system.");
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        return ActionResult.ok("Sent \"" + key + "\" to the active player. If nothing happened, the app playing audio doesn't respond to media keys.");
    }

    public ActionResult flashlight(boolean on) {
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) return ActionResult.fail("I can't reach the camera system.");
        try {
            if (!torchSearched) {
                torchSearched = true;
                for (String id : cm.getCameraIdList()) {
                    Boolean flash = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (flash != null && flash) { torchId = id; break; }
                }
            }
            if (torchId == null) return ActionResult.fail("This device doesn't have a flashlight I can use.");
            cm.setTorchMode(torchId, on);
            return ActionResult.ok("Flashlight " + (on ? "on" : "off") + ".");
        } catch (CameraAccessException | IllegalArgumentException e) {
            return ActionResult.fail("I couldn't control the flashlight. Another app may be using the camera.");
        }
    }

    public ActionResult openMedia(String kind) {
        Intent i = new Intent(context, MediaViewerActivity.class).putExtra(MediaViewerActivity.EXTRA_KIND, kind);
        return start(i, "Opened Alex's " + kind + " viewer. Pick a file there.", "I couldn't open the viewer.");
    }

    public ActionResult whatsappChat(String number, String message) {
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return ActionResult.fail("WhatsApp needs the full number including the country code.");
        String url = "https://wa.me/" + digits + (message.isEmpty() ? "" : "?text=" + Uri.encode(message));
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp");
        try {
            context.startActivity(i);
            return ActionResult.ok("Opened WhatsApp"
                    + (message.isEmpty() ? "" : " with your draft")
                    + ". Nothing is sent until you press send.");
        } catch (ActivityNotFoundException e) {
            return start(new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    "WhatsApp isn't installed, so I opened the chat link in your browser.",
                    "I couldn't open WhatsApp or a browser.");
        }
    }

    public ActionResult shareText(String app, String message) {
        String pkg;
        switch (app) {
            case "whatsapp": pkg = "com.whatsapp"; break;
            case "telegram": pkg = "org.telegram.messenger"; break;
            case "messenger": pkg = "com.facebook.orca"; break;
            default: pkg = "com.instagram.android"; break;
        }
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, message).setPackage(pkg);
        return start(i, "Opened " + app + "'s share screen with your text. Choose who gets it and press send there.",
                app + " isn't installed or doesn't accept shared text.");
    }

    private static String describeSeconds(int s) {
        if (s % 3600 == 0) return (s / 3600) + "-hour";
        if (s % 60 == 0) return (s / 60) + "-minute";
        return s + "-second";
    }
}
