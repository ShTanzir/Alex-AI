package com.alexai.assistant;

import static org.junit.Assert.assertEquals;

import com.alexai.assistant.commands.ActivityLog;

import org.junit.Test;

public class ActivityLogTest {
    @Test public void newestFirstAndCapped() {
        ActivityLog.clear();
        for (int i = 0; i < 60; i++) ActivityLog.add("A" + i, "m" + i, true);
        assertEquals(50, ActivityLog.all().size());
        assertEquals("A59", ActivityLog.all().get(0).actionId);
    }
}
