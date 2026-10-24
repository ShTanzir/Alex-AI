package com.alexai.assistant.apps;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.alexai.assistant.commands.ActionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Resolves and launches installed apps. Relies on the launcher &lt;queries&gt; entry for package visibility. */
public class AppLaunchManager {
    private static final String YOUTUBE = "com.google.android.youtube";
    private static final String YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    private final Context context;

    public AppLaunchManager(Context context) { this.context = context; }

    private static final class Match {
        ResolveInfo info;
        String label;
        String error;
    }

    private List<ResolveInfo> launchable() {
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        return context.getPackageManager().queryIntentActivities(main, 0);
    }

    private Match find(String name) {
        PackageManager pm = context.getPackageManager();
        String q = name.trim().toLowerCase(Locale.ROOT);
        Match m = new Match();
        ResolveInfo exact = null;
        List<ResolveInfo> partial = new ArrayList<>();
        for (ResolveInfo ri : launchable()) {
            String label = ri.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            if (label.equals(q)) { exact = ri; break; }
            if (label.contains(q) || (label.length() >= 4 && q.contains(label))) partial.add(ri);
        }
        ResolveInfo target = exact;
        if (target == null) {
            if (partial.isEmpty()) {
                m.error = "I couldn't find an app called \"" + name + "\". Say \"show installed apps\" to see what I can open.";
                return m;
            }
            if (partial.size() > 1) {
                List<String> labels = new ArrayList<>();
                for (ResolveInfo ri : partial) labels.add(ri.loadLabel(pm).toString());
                Collections.sort(labels);
                m.error = "Several apps match \"" + name + "\": "
                        + String.join(", ", labels.subList(0, Math.min(6, labels.size()))) + ". Which one?";
                return m;
            }
            target = partial.get(0);
        }
        m.info = target;
        m.label = target.loadLabel(pm).toString();
        return m;
    }

    public ActionResult openByName(String name) {
        Match m = find(name);
        if (m.info == null) return ActionResult.fail(m.error);
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(m.info.activityInfo.packageName, m.info.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(i);
            return ActionResult.ok("Opened " + m.label + ".");
        } catch (ActivityNotFoundException | SecurityException e) {
            return ActionResult.fail("Android wouldn't let me open " + m.label + ".");
        }
    }

    /** Opens Android's own uninstall confirmation. Alex cannot uninstall anything silently. */
    public ActionResult uninstall(String name) {
        Match m = find(name);
        if (m.info == null) return ActionResult.fail(m.error);
        String pkg = m.info.activityInfo.packageName;
        if (pkg.equals(context.getPackageName())) {
            return ActionResult.fail("I won't uninstall myself. You can do that from Android's app settings.");
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
            return ActionResult.ok("Opened Android's uninstall confirmation for " + m.label
                    + ". Finish it there; I can't tell whether you did.");
        } catch (ActivityNotFoundException | SecurityException e) {
            return ActionResult.fail("Android wouldn't open the uninstall screen for " + m.label
                    + " (system apps can't be removed this way).");
        }
    }

    public ActionResult listInstalled() {
        PackageManager pm = context.getPackageManager();
        List<String> labels = new ArrayList<>();
        for (ResolveInfo ri : launchable()) labels.add(ri.loadLabel(pm).toString());
        Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
        if (labels.isEmpty()) return ActionResult.fail("I can't see any launchable apps on this device.");
        int shown = Math.min(40, labels.size());
        String more = labels.size() > shown ? " (and " + (labels.size() - shown) + " more)" : "";
        return ActionResult.ok("Apps I can open: " + String.join(", ", labels.subList(0, shown)) + more + ".");
    }

    public ActionResult openYouTube() {
        if (launchPackage(YOUTUBE)) return ActionResult.ok("Opened YouTube.");
        return openUrl("https://www.youtube.com", "YouTube isn't installed, so I opened the website instead.");
    }

    public ActionResult openYouTubeMusic() {
        if (launchPackage(YOUTUBE_MUSIC)) return ActionResult.ok("Opened YouTube Music.");
        return openUrl("https://music.youtube.com", "YouTube Music isn't installed, so I opened the website instead.");
    }

    public ActionResult youtubeSearch(String query) {
        return openUrl("https://www.youtube.com/results?search_query=" + Uri.encode(query),
                "Opened a YouTube search for \"" + query + "\".");
    }

    private boolean launchPackage(String pkg) {
        Intent i = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private ActionResult openUrl(String url, String okMessage) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return ActionResult.ok(okMessage);
        } catch (ActivityNotFoundException e) {
            return ActionResult.fail("I couldn't find a browser or app to open that link.");
        }
    }
}
