package com.alexai.assistant.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexai.assistant.R;
import com.alexai.assistant.ai.GeminiClient;
import com.alexai.assistant.commands.ActionExecutor;
import com.alexai.assistant.commands.CommandRouter;
import com.alexai.assistant.storage.SecureSettingsRepository;
import com.alexai.assistant.ui.dialogs.ConfirmationManager;
import com.alexai.assistant.voice.SpeechInputManager;
import com.alexai.assistant.voice.TextToSpeechManager;

public class HomeFragment extends Fragment {
    private SecureSettingsRepository settings;
    private HomeViewModel vm;
    private GeminiClient gemini;
    private CommandRouter router;
    private SpeechInputManager speech;
    private TextToSpeechManager tts;
    private MessageAdapter adapter;

    private RecyclerView list;
    private AlexFab fab;
    private TextView status, conn;
    private EditText input;

    private boolean busy;
    private boolean micRationaleShown;
    private ActivityResultLauncher<String> micPermission;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        micPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                new androidx.activity.result.ActivityResultCallback<Boolean>() {
                    @Override public void onActivityResult(Boolean granted) {
                        if (granted) {
                            startListening();
                        } else if (fab != null) {
                            addMessage("Microphone permission was denied, so I can't listen. You can still type commands, "
                                    + "or allow the microphone from the Settings tab.", false, true);
                        }
                    }
                });
        getParentFragmentManager().setFragmentResultListener("clear_history", this,
                new androidx.fragment.app.FragmentResultListener() {
                    @Override public void onFragmentResult(@NonNull String key, @NonNull Bundle result) { clearConversation(); }
                });
        getParentFragmentManager().setFragmentResultListener("submit_command", this,
                new androidx.fragment.app.FragmentResultListener() {
                    @Override public void onFragmentResult(@NonNull String key, @NonNull Bundle result) {
                        if (fab != null) submit(result.getString("text", ""));
                    }
                });
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = new SecureSettingsRepository(requireContext());
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        list = view.findViewById(R.id.messages);
        fab = view.findViewById(R.id.fab);
        status = view.findViewById(R.id.status);
        conn = view.findViewById(R.id.conn);
        input = view.findViewById(R.id.input);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        adapter = new MessageAdapter(vm.messages);
        list.setAdapter(adapter);
        if (!vm.historyLoaded) {
            vm.historyLoaded = true;
            if (settings.keepHistory()) vm.messages.addAll(MessageCodec.decode(settings.loadHistory()));
            adapter.notifyDataSetChanged();
        }
        if (vm.messages.isEmpty()) {
            addMessage("Hi, I'm Alex. Tap the purple button and say \"open YouTube\", or type a command. "
                    + "Say \"what can you do?\" to see everything.", false, false);
        } else {
            list.scrollToPosition(vm.messages.size() - 1);
        }

        gemini = new GeminiClient(requireContext(), settings);
        router = new CommandRouter(settings, gemini, new ActionExecutor(requireActivity(), settings),
                vm.conversation, routerListener());
        tts = new TextToSpeechManager(requireContext(), settings, new TextToSpeechManager.Listener() {
            @Override public void onSpeakingChanged(boolean speaking) {
                if (fab == null || busy || (speech != null && speech.isActive())) return;
                setState(speaking ? AlexFab.State.SPEAKING : AlexFab.State.IDLE,
                        speaking ? "Speaking… tap to stop" : "Tap to talk");
            }
        });
        speech = new SpeechInputManager(requireContext(), speechListener());

        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onFabClick(); }
        });
        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { submitTyped(); }
        });
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent e) {
                if (actionId == EditorInfo.IME_ACTION_SEND) { submitTyped(); return true; }
                return false;
            }
        });
        updateConnection();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (conn != null) updateConnection();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && conn != null) updateConnection();
    }

    @Override
    public void onDestroyView() {
        if (router != null) router.release();
        if (speech != null) speech.destroy();
        if (tts != null) tts.shutdown();
        router = null; speech = null; tts = null; gemini = null;
        list = null; fab = null; status = null; conn = null; input = null; adapter = null;
        busy = false;
        super.onDestroyView();
    }

    // ---- interaction -------------------------------------------------------------------------

    private void onFabClick() {
        if (speech.isActive()) { speech.stop(); return; }
        if (tts != null && fab.getState() == AlexFab.State.SPEAKING) { tts.stop(); return; }
        if (busy) {
            router.cancel();
            busy = false;
            setState(AlexFab.State.IDLE, "Tap to talk");
            addMessage("Cancelled.", false, false);
            return;
        }
        tts.stop();
        startListening();
    }

    private void startListening() {
        if (fab == null || busy) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (!micRationaleShown) {
                micRationaleShown = true;
                addMessage("To hear you I need microphone access. It's only used while you've tapped the button. "
                        + "Android will ask now.", false, false);
            }
            micPermission.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        speech.start(settings.getLanguageTag());
    }

    private void submitTyped() {
        if (input == null) return;
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        submit(text);
    }

    private void submit(String text) {
        if (fab == null || text.trim().isEmpty()) return;
        if (busy) {
            Toast.makeText(requireContext(), "Alex is busy. Tap the button to cancel.", Toast.LENGTH_SHORT).show();
            return;
        }
        busy = true;
        addMessage(text, true, false);
        setState(AlexFab.State.THINKING, "Thinking… tap to cancel");
        router.handle(text);
    }

    private void finishWork() {
        busy = false;
        if (fab != null) setState(AlexFab.State.IDLE, "Tap to talk");
    }

    private void clearConversation() {
        if (vm == null || adapter == null) return;
        int n = vm.messages.size();
        vm.messages.clear();
        vm.conversation.clear();
        settings.clearHistory();
        adapter.notifyItemRangeRemoved(0, n);
        Toast.makeText(requireContext(), "Conversation cleared", Toast.LENGTH_SHORT).show();
    }

    // ---- callbacks ---------------------------------------------------------------------------

    private CommandRouter.Listener routerListener() {
        return new CommandRouter.Listener() {
            @Override public void onReply(String text, boolean error) {
                if (fab == null) return;
                finishWork();
                addMessage(text, false, error);
                if (error) {
                    setState(AlexFab.State.ERROR, "Tap to talk");
                }
                if (settings.speakResponses()) tts.speak(text);
            }

            @Override public void onConfirm(String summary, Runnable confirm, Runnable cancel) {
                if (fab == null) return;
                busy = false;
                setState(AlexFab.State.IDLE, "Waiting for your confirmation");
                if (settings.speakConfirmations()) tts.speak("Please confirm. " + summary);
                ConfirmationManager.show(requireActivity(), summary, wrap(confirm, true), wrap(cancel, false));
            }
        };
    }

    private Runnable wrap(final Runnable r, final boolean working) {
        return new Runnable() {
            @Override public void run() {
                if (fab == null) return;
                tts.stop();
                busy = true;
                if (working) setState(AlexFab.State.EXECUTING, "Working…");
                r.run();
            }
        };
    }

    private SpeechInputManager.Listener speechListener() {
        return new SpeechInputManager.Listener() {
            @Override public void onListening() { setState(AlexFab.State.LISTENING, "Listening… tap to finish"); }

            @Override public void onPartial(String text) { if (status != null) status.setText(text); }

            @Override public void onFinal(String text) { submit(text); }

            @Override public void onError(String message) {
                if (fab == null) return;
                setState(AlexFab.State.ERROR, message);
                fab.postDelayed(new Runnable() {
                    @Override public void run() {
                        if (fab != null && !busy && fab.getState() == AlexFab.State.ERROR) {
                            setState(AlexFab.State.IDLE, "Tap to talk");
                        }
                    }
                }, 3000);
            }
        };
    }

    // ---- ui helpers --------------------------------------------------------------------------

    private void setState(AlexFab.State s, String statusText) {
        if (fab == null) return;
        fab.setState(s);
        status.setText(statusText);
    }

    private void addMessage(String text, boolean fromUser, boolean error) {
        vm.messages.add(new Message(text, fromUser, error));
        persist();
        if (adapter != null && list != null) {
            adapter.notifyItemInserted(vm.messages.size() - 1);
            list.scrollToPosition(vm.messages.size() - 1);
        }
    }

    private void persist() {
        if (settings != null && settings.keepHistory()) settings.saveHistory(MessageCodec.encode(vm.messages));
    }

    private void updateConnection() {
        String s;
        if (!settings.hasApiKey()) s = "Chat needs an API key (Settings)";
        else if (!gemini.isOnline()) s = "Offline";
        else s = "Gemini ready";
        conn.setText(s);
    }
}
