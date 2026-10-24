package com.alexai.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alexai.assistant.ui.home.Message;
import com.alexai.assistant.ui.home.MessageCodec;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MessageCodecTest {
    @Test public void roundTrips() {
        List<Message> in = new ArrayList<>();
        in.add(new Message("hello", true, false, 1000L));
        in.add(new Message("oops", false, true, 2000L));
        List<Message> out = MessageCodec.decode(MessageCodec.encode(in));
        assertEquals(2, out.size());
        assertEquals("hello", out.get(0).text);
        assertTrue(out.get(0).fromUser);
        assertTrue(out.get(1).error);
        assertEquals(2000L, out.get(1).timestamp);
    }

    @Test public void keepsOnlyLastHundred() {
        List<Message> in = new ArrayList<>();
        for (int i = 0; i < 150; i++) in.add(new Message("m" + i, true, false, i));
        List<Message> out = MessageCodec.decode(MessageCodec.encode(in));
        assertEquals(100, out.size());
        assertEquals("m50", out.get(0).text);
    }

    @Test public void badJsonGivesEmptyList() {
        assertTrue(MessageCodec.decode("not json").isEmpty());
    }
}
