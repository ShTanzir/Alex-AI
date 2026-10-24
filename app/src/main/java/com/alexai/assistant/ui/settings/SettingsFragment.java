package com.alexai.assistant.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.alexai.assistant.R;
import com.alexai.assistant.accessibility.AlexAccessibilityService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.alexai.assistant.storage.SecureSettingsRepository;
import com.alexai.assistant.voice.TextToSpeechManager;

import java.util.Locale;
import java.util.regex.Pattern;

public class SettingsFragment extends Fragment {
    private static final Pattern MODEL = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private SecureSettingsRepository settings;
    private TextToSpeechManager tts;
    private TextView keyStatus, permStatus, rateLabel, pitchLabel, a11yStatus;
    private EditText apiKey, model, persona;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        settings = new SecureSettingsRepository(requireContext());
        tts = new TextToSpeechManager(requireContext(), settings, new TextToSpeechManager.Listener() {
            @Override public void onSpeakingChanged(boolean speaking) {}
        });

        keyStatus = v.findViewById(R.id.key_status);
        permStatus = v.findViewById(R.id.perm_status);
        rateLabel = v.findViewById(R.id.rate_label);
        pitchLabel = v.findViewById(R.id.pitch_label);
        apiKey = v.findViewById(R.id.api_key);
        model = v.findViewById(R.id.model);
        persona = v.findViewById(R.id.persona);

        model.setText(settings.getModel());
        persona.setText(settings.getPersona());

        v.findViewById(R.id.save_key).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveKey(); }
        });
        v.findViewById(R.id.delete_key).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                settings.clearApiKey();
                apiKey.setText("");
                refreshKeyStatus();
                toast("API key deleted");
            }
        });
        v.findViewById(R.id.save_ai).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveAi(); }
        });

        com.google.android.material.materialswitch.MaterialSwitch sr = v.findViewById(R.id.speak_responses);
        com.google.android.material.materialswitch.MaterialSwitch sc = v.findViewById(R.id.speak_confirmations);
        sr.setChecked(settings.speakResponses());
        sc.setChecked(settings.speakConfirmations());
        sr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { settings.setSpeakResponses(on); }
        });
        sc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { settings.setSpeakConfirmations(on); }
        });

        SeekBar rate = v.findViewById(R.id.rate);
        SeekBar pitch = v.findViewById(R.id.pitch);
        rate.setProgress(Math.round((settings.getSpeechRate() - 0.5f) * 100));
        pitch.setProgress(Math.round((settings.getPitch() - 0.5f) * 100));
        updateLabels();
        rate.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override void changed(int p) { settings.setSpeechRate(0.5f + p / 100f); updateLabels(); }
        });
        pitch.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override void changed(int p) { settings.setPitch(0.5f + p / 100f); updateLabels(); }
        });

        RadioGroup lang = v.findViewById(R.id.language);
        lang.check(settings.getLanguageTag().startsWith("bn") ? R.id.lang_bn : R.id.lang_en);
        lang.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) {
                settings.setLanguageTag(id == R.id.lang_bn ? "bn-BD" : "en-US");
            }
        });

        v.findViewById(R.id.test_voice).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (!tts.isReady()) { toast("Text-to-speech isn't ready on this device yet."); return; }
                tts.speak("Hi, I'm Alex. This is how I sound.");
            }
        });

        RadioGroup theme = v.findViewById(R.id.theme);
        int t = settings.getTheme();
        theme.check(t == 1 ? R.id.theme_light : t == 2 ? R.id.theme_dark : R.id.theme_system);
        theme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) {
                int value = id == R.id.theme_light ? 1 : id == R.id.theme_dark ? 2 : 0;
                if (value == settings.getTheme()) return;
                settings.setTheme(value);
                SecureSettingsRepository.applyTheme(value);
            }
        });

        v.findViewById(R.id.open_app_settings).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().getPackageName(), null));
                startActivity(i);
            }
        });
        v.findViewById(R.id.clear_history).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                getParentFragmentManager().setFragmentResult("clear_history", new Bundle());
            }
        });

        a11yStatus = v.findViewById(R.id.a11y_status);
        v.findViewById(R.id.a11y_enable).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showAccessibilityDisclosure(); }
        });

        com.google.android.material.materialswitch.MaterialSwitch keep = v.findViewById(R.id.keep_history);
        keep.setChecked(settings.keepHistory());
        keep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                settings.setKeepHistory(on);
                if (!on) settings.clearHistory();
            }
        });

        refreshKeyStatus();
        refreshPermissions();
    }

    private void showAccessibilityDisclosure() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("About Alex's accessibility service")
                .setMessage("This optional service lets you ask Alex to press Back, open recent apps and scroll in other apps. "
                        + "It only does that when you ask.\n\n"
                        + "It does not read, collect or store what is on your screen, and it can't get past lock screens, "
                        + "passwords or security prompts.\n\n"
                        + "You turn it on yourself in Android's Accessibility settings and can turn it off there at any time. "
                        + "Continue to Android settings?")
                .setPositiveButton("Continue", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                })
                .setNegativeButton("Not now", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permStatus != null) refreshPermissions();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && permStatus != null) {
            refreshPermissions();
            refreshKeyStatus();
        }
    }

    @Override
    public void onDestroyView() {
        if (tts != null) tts.shutdown();
        tts = null;
        permStatus = null;
        super.onDestroyView();
    }

    private void saveKey() {
        String k = apiKey.getText().toString().trim();
        if (k.isEmpty() || k.matches(".*\\s.*")) { toast("Paste a key with no spaces."); return; }
        if (settings.setApiKey(k)) {
            apiKey.setText("");
            toast("API key saved");
        } else {
            toast("Secure storage isn't available on this device, so I didn't save the key.");
        }
        refreshKeyStatus();
    }

    private void saveAi() {
        String m = model.getText().toString().trim();
        if (!MODEL.matcher(m).matches()) { toast("Model names use letters, numbers, dots, dashes and underscores."); return; }
        String p = persona.getText().toString().trim();
        settings.setModel(m);
        settings.setPersona(p.isEmpty() ? SecureSettingsRepository.DEFAULT_PERSONA : p);
        toast("Saved");
    }

    private void refreshKeyStatus() {
        if (keyStatus == null) return;
        String s = !settings.isSecureStorageAvailable()
                ? "Secure storage isn't available on this device, so keys can't be saved."
                : settings.hasApiKey() ? "A key is saved (hidden)." : "No key saved. Get one from Google AI Studio.";
        keyStatus.setText(s);
    }

    private void refreshPermissions() {
        boolean mic = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        permStatus.setText((mic ? "Microphone: allowed. Used only while you've tapped the Alex button.\n"
                : "Microphone: not allowed. Needed for voice input; Alex asks when you tap the button.\n")
                + "Notifications and wake word: not used in this version.");
        a11yStatus.setText(AlexAccessibilityService.isRunning()
                ? "Accessibility service: on. Turn it off any time in Android's Accessibility settings."
                : "Accessibility service: off. Only needed for Back, Recents and scroll in other apps.");
    }

    private void updateLabels() {
        rateLabel.setText(String.format(Locale.US, "Speech rate: %.2fx", settings.getSpeechRate()));
        pitchLabel.setText(String.format(Locale.US, "Pitch: %.2f", settings.getPitch()));
    }

    private void toast(String s) { Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show(); }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        abstract void changed(int progress);

        @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { if (fromUser) changed(p); }
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }
}
