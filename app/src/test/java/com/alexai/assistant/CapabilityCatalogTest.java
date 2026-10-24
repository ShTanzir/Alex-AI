package com.alexai.assistant;

import static org.junit.Assert.assertTrue;

import com.alexai.assistant.commands.CapabilityCatalog;

import org.junit.Test;

public class CapabilityCatalogTest {
    @Test public void hasAtLeastFortyDocumentedCapabilities() {
        assertTrue(CapabilityCatalog.all().size() >= 40);
    }

    @Test public void everyEntryHasNotes() {
        for (CapabilityCatalog.Entry e : CapabilityCatalog.all()) assertTrue(e.title, !e.notes.isEmpty());
    }
}
