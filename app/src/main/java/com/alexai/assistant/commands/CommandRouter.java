package com.alexai.assistant.commands;

import com.alexai.assistant.ai.AiIntentParser;
import com.alexai.assistant.ai.ConversationManager;
import com.alexai.assistant.ai.GeminiClient;
import com.alexai.assistant.storage.SecureSettingsRepository;

import java.util.List;

/**
 * The single path every command takes: parse (locally or via Gemini) -> validate against the
 * allowlist -> confirm if the action requires it -> execute -> report the real result.
 */
public class CommandRouter {
    public interface Listener {
        void onReply(String text, boolean error);
        void onConfirm(String summary, Runnable confirm, Runnable cancel);
    }

    private final SecureSettingsRepository settings;
    private final GeminiClient gemini;
    private final ActionExecutor executor;
    private final ConversationManager conversation;
    private final Listener listener;
    private volatile boolean released;

    public CommandRouter(SecureSettingsRepository settings, GeminiClient gemini, ActionExecutor executor,
                         ConversationManager conversation, Listener listener) {
        this.settings = settings;
        this.gemini = gemini;
        this.executor = executor;
        this.conversation = conversation;
        this.listener = listener;
    }

    public void handle(String text) {
        List<ConversationManager.Turn> history = conversation.recent();
        conversation.add(true, text);

        Action local = CommandParser.parse(text);
        if (local != null) {
            process(local);
            return;
        }
        if (!settings.hasApiKey()) {
            reply("I didn't recognise that as a built-in command. Add a Gemini API key in Settings and I can chat and understand more ways of asking.", false);
            return;
        }
        gemini.generate(history, text, new GeminiClient.Callback() {
            @Override public void onSuccess(String rawText) {
                if (released) return;
                AiIntentParser.AiIntent intent = AiIntentParser.parse(rawText);
                if (intent.action != null) process(intent.action);
                else reply(intent.reply.isEmpty() ? "I didn't get an answer to that." : intent.reply, false);
            }

            @Override public void onError(String message) {
                if (!released) reply(message, true);
            }
        });
    }

    /** Cancels any in-flight Gemini request. */
    public void cancel() { gemini.cancel(); }

    public void release() {
        released = true;
        gemini.cancel();
    }

    private void process(final Action action) {
        String problem = ActionValidator.validate(action);
        if (problem != null) {
            reply("I can't do that: " + problem + ".", true);
            return;
        }
        ActionRegistry.Spec spec = ActionRegistry.get(action.id);
        if (spec.requiresConfirmation) { // decided locally; the model cannot waive it
            final boolean[] done = {false};
            listener.onConfirm(ActionRegistry.describe(action),
                    new Runnable() { @Override public void run() {
                        if (done[0] || released) return;
                        done[0] = true;
                        run(action);
                    } },
                    new Runnable() { @Override public void run() {
                        if (done[0] || released) return;
                        done[0] = true;
                        reply("Cancelled. Nothing was done.", false);
                    } });
        } else {
            run(action);
        }
    }

    private void run(Action action) {
        if (action.id.equals("CANCEL")) conversation.clear();
        ActionResult r = executor.execute(action);
        ActivityLog.add(action.id, r.message, r.success);
        reply(r.message, !r.success);
    }

    private void reply(String text, boolean error) {
        conversation.add(false, text);
        listener.onReply(text, error);
    }
}
