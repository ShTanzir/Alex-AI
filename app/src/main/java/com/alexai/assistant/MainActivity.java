package com.alexai.assistant;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alexai.assistant.ui.activity.ActivityFragment;
import com.alexai.assistant.ui.capabilities.CapabilitiesFragment;
import com.alexai.assistant.ui.home.HomeFragment;
import com.alexai.assistant.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private static final String[] TAGS = {"home", "activity", "caps", "settings"};
    private static final String KEY_SELECTED = "selected";

    private final View[] items = new View[4];
    private final ImageView[] icons = new ImageView[4];
    private final TextView[] labels = new TextView[4];
    private View root;
    private View nav;
    private FrameLayout container;
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        nav = findViewById(R.id.nav);
        container = findViewById(R.id.container);

        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), root);
        c.setAppearanceLightStatusBars(!night);
        c.setAppearanceLightNavigationBars(!night);

        items[0] = findViewById(R.id.nav_home);
        items[1] = findViewById(R.id.nav_activity);
        items[2] = findViewById(R.id.nav_caps);
        items[3] = findViewById(R.id.nav_settings);
        icons[0] = findViewById(R.id.nav_home_icon);
        icons[1] = findViewById(R.id.nav_activity_icon);
        icons[2] = findViewById(R.id.nav_caps_icon);
        icons[3] = findViewById(R.id.nav_settings_icon);
        labels[0] = findViewById(R.id.nav_home_label);
        labels[1] = findViewById(R.id.nav_activity_label);
        labels[2] = findViewById(R.id.nav_caps_label);
        labels[3] = findViewById(R.id.nav_settings_label);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            items[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { select(idx); }
            });
        }

        // Hide the floating nav while the keyboard is open.
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                Rect r = new Rect();
                root.getWindowVisibleDisplayFrame(r);
                int screen = root.getRootView().getHeight();
                boolean keyboard = screen - r.bottom > screen * 0.15f;
                nav.setVisibility(keyboard ? View.GONE : View.VISIBLE);
                int pad = keyboard ? 0 : dp(88);
                if (container.getPaddingBottom() != pad) container.setPadding(0, 0, 0, pad);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (selected != 0) {
                    select(0);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // A shortcut tapped in the Activity tab runs on the Home tab, through the normal command router.
        getSupportFragmentManager().setFragmentResultListener("run_command", this,
                new androidx.fragment.app.FragmentResultListener() {
                    @Override public void onFragmentResult(String key, Bundle result) {
                        select(0);
                        getSupportFragmentManager().setFragmentResult("submit_command", result);
                    }
                });

        selected = savedInstanceState == null ? 0 : savedInstanceState.getInt(KEY_SELECTED, 0);
        show(selected);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED, selected);
    }

    private void select(int index) {
        if (index == selected) return;
        selected = index;
        show(index);
    }

    private void show(int index) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null && i == index) {
                tx.add(R.id.container, create(i), TAGS[i]);
            } else if (f != null) {
                if (i == index) tx.show(f); else tx.hide(f);
            }
        }
        tx.commit();
        updateNav(index);
    }

    private Fragment create(int index) {
        switch (index) {
            case 1: return new ActivityFragment();
            case 2: return new CapabilitiesFragment();
            case 3: return new SettingsFragment();
            default: return new HomeFragment();
        }
    }

    private void updateNav(int index) {
        int accent = ContextCompat.getColor(this, R.color.alex_accent);
        int muted = ContextCompat.getColor(this, R.color.alex_text_secondary);
        for (int i = 0; i < items.length; i++) {
            boolean on = i == index;
            items[i].setSelected(on);
            icons[i].setImageTintList(ColorStateList.valueOf(on ? accent : muted));
            labels[i].setTextColor(on ? accent : muted);
        }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
