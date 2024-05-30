/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import java.util.Optional;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public enum Storage {
    INSTANCE;

    JsonObject toys = new JsonObject();

    public JsonArray getToys() {
        JsonArray result = new JsonArray();
        toys.forEach(entry -> result.add(entry.getValue()));
        return result;
    }

    public JsonObject addToy(JsonObject toy) {
        String id = toy.getString("Name");
        toys.put(id, toy);
        return toy;
    }


    public Optional<JsonObject> getToy(String name) {
        System.out.println("Searching for:" + name);
        System.out.println(toys.encodePrettily());
        if (toys.containsKey(name)) {
            return Optional.of(toys.getJsonObject(name));
        }
        return Optional.empty();
    }

    public JsonArray getToys(final String color) {

        if (color == null || color.isBlank()) {
            return getToys();
        }

        JsonArray result = new JsonArray();
        toys.stream()
                .filter(v -> v.getValue() instanceof JsonObject)
                .map(v -> (JsonObject) v.getValue())
                .filter(v -> color.equals(v.getString("Color")))
                .forEach(result::add);
        return result;
    }
}
