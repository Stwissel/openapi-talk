/* (C) 2023 notessensei, Apache-2.0 license */
package com.notessensei.openapidemo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import io.vertx.core.json.JsonObject;

/**
 * Utility methods
 */
public class Utils {

    public static JsonObject jsonFromResource(final String resourceName) {

        try (InputStream in = Utils.class.getResourceAsStream(resourceName)) {
            String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new JsonObject(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Empty json fallback
        return new JsonObject();
    }

    private Utils() {
        // Only static methods here
    }

}
