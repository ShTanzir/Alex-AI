package com.alexai.assistant.ui.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.alexai.assistant.R;
import com.alexai.assistant.commands.ActionValidator;
import com.alexai.assistant.commands.ActivityLog;
import com.alexai.assistant.commands.CommandParser;
import com.alexai.assistant.storage.SecureSettingsRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Shortcuts manager and recent-action log. A shortcut is a name plus a built-in command phrase; it is
 * run through the same router as typed commands, so validation and confirmation still apply.
 */
public class ActivityFragment extends Fragment {
    private static final int MAX_SHORTCUTS = 20;

    private SecureSettingsRepository settings;
    private LinearLayout shortcutsBox, recentBox;
    private final DateFormat time = DateFormat.getTimeInstance(DateFormat.SHORT);

    private static final class Shortcut {
        final String name, phrase;
        Shortcut(String name, String phrase) { this.name = name; this.phrase = phrase; }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        settings = new SecureSettingsRepository(requireContext());
        shortcutsBox = v.findViewById(R.id.shortcuts);
        recentBox = v.findViewById(R.id.recent);
        v.findViewById(R.id.add_shortcut).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showAddDialog(); }
        });
        refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && shortcutsBox != null) refresh();
    }

    @Override
    public void onDestroyView() {
        shortcutsBox = null;
        recentBox = null;
        super.onDestroyView();
    }

    private void refresh() {
        renderShortcuts();
        renderRecent();
    }

    private void renderShortcuts() {
        shortcutsBox.removeAllViews();
        List<Shortcut> list = load();
        if (list.isEmpty()) {
            shortcutsBox.addView(note("No shortcuts yet."));
            return;
        }
        for (final Shortcut s : list) {
            MaterialButton b = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(s.name + "  -  " + s.phrase);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { run(s); }
            });
            b.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View view) { confirmDelete(s); return true; }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            shortcutsBox.addView(b, lp);
        }
    }

    private void renderRecent() {
        recentBox.removeAllViews();
        List<ActivityLog.Entry> entries = ActivityLog.all();
        if (entries.isEmpty()) {
            recentBox.addView(note("Nothing yet. Actions Alex runs will show up here."));
            return;
        }
        for (ActivityLog.Entry e : entries) {
            TextView t = new TextView(requireContext());
            t.setText(time.format(new Date(e.time)) + "   " + (e.success ? "Done" : "Failed") + " - " + e.message);
            t.setTextSize(13);
            t.setTextColor(ContextCompat.getColor(requireContext(), e.success ? R.color.alex_text : R.color.alex_error));
            t.setPadding(dp(12), dp(8), dp(12), dp(8));
            t.setBackgroundResource(R.drawable.bubble_ai);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            recentBox.addView(t, lp);
        }
    }

    private TextView note(String text) {
        TextView t = new TextView(requireContext());
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(ContextCompat.getColor(requireContext(), R.color.alex_text_secondary));
        t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }

    private void run(Shortcut s) {
        Bundle b = new Bundle();
        b.putString("text", s.phrase);
        getParentFragmentManager().setFragmentResult("run_command", b);
    }

    private void confirmDelete(final Shortcut s) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete shortcut?")
                .setMessage(s.name)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        List<Shortcut> list = load();
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).name.equals(s.name) && list.get(i).phrase.equals(s.phrase)) {
                                list.remove(i);
                                break;
                            }
                        }
                        save(list);
                        if (shortcutsBox != null) renderShortcuts();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDialog() {
        if (load().size() >= MAX_SHORTCUTS) {
            Toast.makeText(requireContext(), "You can save up to " + MAX_SHORTCUTS + " shortcuts.", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        final EditText name = new EditText(requireContext());
        name.setHint("Name (for example: Tutorials)");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText phrase = new EditText(requireContext());
        phrase.setHint("Command (for example: search YouTube for Java tutorials)");
        phrase.setInputType(InputType.TYPE_CLASS_TEXT);
        box.addView(name);
        box.addView(phrase);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New shortcut")
                .setView(box)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        addShortcut(name.getText().toString().trim(), phrase.getText().toString().trim());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addShortcut(String name, String phrase) {
        if (name.isEmpty() || name.length() > 30 || phrase.isEmpty()) {
            toast("Give it a name (up to 30 characters) and a command.");
            return;
        }
        if (CommandParser.parse(phrase) == null || ActionValidator.validate(CommandParser.parse(phrase)) != null) {
            toast("That isn't a built-in command, so it can't be a shortcut. Try \"open YouTube\".");
            return;
        }
        List<Shortcut> list = load();
        list.add(new Shortcut(name, phrase));
        save(list);
        if (shortcutsBox != null) renderShortcuts();
    }

    private List<Shortcut> load() {
        List<Shortcut> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(settings.getShortcutsJson());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Shortcut(o.optString("n"), o.optString("p")));
            }
        } catch (JSONException e) {
            out.clear();
        }
        return out;
    }

    private void save(List<Shortcut> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Shortcut s : list) arr.put(new JSONObject().put("n", s.name).put("p", s.phrase));
        } catch (JSONException ignored) { }
        settings.setShortcutsJson(arr.toString());
    }

    private void toast(String s) { Toast.makeText(requireContext(), s, Toast.LENGTH_LONG).show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
