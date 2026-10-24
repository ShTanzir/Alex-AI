package com.alexai.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.alexai.assistant.ai.AiIntentParser;
import com.alexai.assistant.ai.GeminiException;
import com.alexai.assistant.ai.GeminiResponseParser;

import org.junit.Test;

public class AiParsingTest {
    @Test public void parsesActionJson() {
        AiIntentParser.AiIntent i = AiIntentParser.parse(
                "{\"reply\":\"Opening\",\"action\":{\"id\":\"OPEN_APP\",\"params\":{\"app\":\"Chrome\"}}}");
        assertEquals("Opening", i.reply);
        assertNotNull(i.action);
        assertEquals("OPEN_APP", i.action.id);
        assertEquals("Chrome", i.action.param("app"));
    }

    @Test public void stripsCodeFences() {
        AiIntentParser.AiIntent i = AiIntentParser.parse("```json\n{\"reply\":\"Hi\",\"action\":null}\n```");
        assertEquals("Hi", i.reply);
        assertNull(i.action);
    }

    @Test public void plainTextNeverBecomesAnAction() {
        AiIntentParser.AiIntent i = AiIntentParser.parse("Sure, I'll open the shell for you");
        assertNull(i.action);
    }

    @Test public void extractsGeminiText() throws Exception {
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"hello\"}]}}]}";
        assertEquals("hello", GeminiResponseParser.extractText(body));
    }

    @Test public void reportsBlockedPrompt() {
        try {
            GeminiResponseParser.extractText("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
            fail("expected exception");
        } catch (GeminiException e) {
            assertEquals(true, e.getMessage().contains("SAFETY"));
        }
    }
}
