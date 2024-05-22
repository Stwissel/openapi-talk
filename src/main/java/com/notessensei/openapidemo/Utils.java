/* (C) 2023 notessensei, Apache-2.0 license */
package com.notessensei.openapidemo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.Yaml;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.openapi.impl.OpenAPIYamlConstructor;

/**
 * Utility methods
 */
public class Utils {

    public static Optional<BasicAuthHandler> getAuthenticationHandler(final String name) {
        final String className = "com.notessensei.openapidemo.security." + name;
        try {
            Class<?> clazz = Class.forName(className);
            return Optional
                    .of((BasicAuthHandler) clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static Optional<Handler<RoutingContext>> getRouteHandler(final String name) {
        final String className = "com.notessensei.openapidemo.handlers."
                + name.substring(0, 1).toUpperCase() + name.substring(1);
        try {
            Class<?> clazz = Class.forName(className);
            return Optional
                    .of((Handler<RoutingContext>) clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            System.out.println("Handler not found: " + className + " falling back to EchoHandler");
        }
        return Optional.empty();
    }

    /**
     * Load a resource as a JsonObject
     *
     * @param resourceName String
     * @return JsonObject
     */
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

    public static JsonObject jsonFromYamlResource(final String resourceName) {

        try (InputStream in = Utils.class.getResourceAsStream(resourceName)) {
            String result = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final Yaml yaml = new Yaml(new OpenAPIYamlConstructor());
            final Map<Object, Object> doc = yaml.load(result);
            return new JsonObject(jsonify(doc));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Empty json fallback
        return new JsonObject();
    }

    /**
     * Yaml allows map keys of type object, however json always requires key as String,
     * this helper method will ensure we adapt keys to the right type
     *
     * @param yaml yaml map
     * @return json map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonify(Map<Object, Object> yaml) {
        final Map<String, Object> json = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> kv : yaml.entrySet()) {
            Object value = kv.getValue();
            if (value instanceof Map) {
                value = jsonify((Map<Object, Object>) value);
            }
            json.put(kv.getKey().toString(), value);
        }

        return json;
    }

    private Utils() {
        // Only static methods here
    }



}
