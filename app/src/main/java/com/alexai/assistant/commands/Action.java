package com.alexai.assistant.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** A requested device action: an id from {@link ActionRegistry} plus string parameters. */
public final class Action {
    public final String id;
    public final Map<String, String> params;

    public Action(String id, Map<String, String> params) {
        this.id = id == null ? "" : id;
        this.params = params == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(params));
    }

    public String param(String key) {
        String v = params.get(key);
        return v == null ? "" : v;
    }
}
