package com.alexai.assistant;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.alexai.assistant.commands.Action;
import com.alexai.assistant.commands.ActionRegistry;
import com.alexai.assistant.commands.ActionValidator;
import com.alexai.assistant.commands.CommandParser;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ActionValidatorTest {
    private static Action a(String id, String... kv) {
        Map<String, String> p = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) p.put(kv[i], kv[i + 1]);
        return new Action(id, p);
    }

    @Test public void rejectsUnknownAction() { assertNotNull(ActionValidator.validate(a("RUN_SHELL", "cmd", "ls"))); }
    @Test public void rejectsMissingParam() { assertNotNull(ActionValidator.validate(a("OPEN_APP"))); }
    @Test public void rejectsExtraParam() { assertNotNull(ActionValidator.validate(a("OPEN_APP", "app", "x", "shell", "ls"))); }
    @Test public void rejectsBadEnum() { assertNotNull(ActionValidator.validate(a("OPEN_SETTINGS_PANEL", "panel", "../etc"))); }
    @Test public void rejectsBadAlarm() { assertNotNull(ActionValidator.validate(CommandParser.parse("set an alarm for 25"))); }
    @Test public void rejectsBadPhone() { assertNotNull(ActionValidator.validate(a("DIAL", "number", "rm -rf /"))); }
    @Test public void acceptsValid() { assertNull(ActionValidator.validate(CommandParser.parse("open youtube"))); }

    @Test public void callsAndMessagesAlwaysNeedConfirmation() {
        assertTrue(ActionRegistry.get("DIAL").requiresConfirmation);
        assertTrue(ActionRegistry.get("SMS_DRAFT").requiresConfirmation);
    }

    @Test public void rejectsUnknownMessagingApp() { assertNotNull(ActionValidator.validate(a("SHARE_TEXT", "app", "signal", "message", "hi"))); }
    @Test public void acceptsKnownMessagingApp() { assertNull(ActionValidator.validate(a("SHARE_TEXT", "app", "telegram", "message", "hi"))); }

    @Test public void uninstallAndSharingNeedConfirmation() {
        assertTrue(ActionRegistry.get("UNINSTALL_APP").requiresConfirmation);
        assertTrue(ActionRegistry.get("SHARE_TEXT").requiresConfirmation);
        assertTrue(ActionRegistry.get("WHATSAPP_CHAT").requiresConfirmation);
    }
}
