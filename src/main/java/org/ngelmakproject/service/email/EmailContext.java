package org.ngelmakproject.service.email;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for email templates, allowing dynamic values to be passed to the
 * template engine.
 */
public class EmailContext {
    private final Map<String, Object> values = new HashMap<>();

    public EmailContext set(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
