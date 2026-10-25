package com.Lilith.immersivepaintings.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Small Gson helpers with defaults, replacing the client only {@code net.minecraft.util.JsonUtils}. */
public final class Json {

    private Json() {}

    public static boolean has(JsonObject object, String key) {
        return object != null && object.has(key)
            && !object.get(key)
                .isJsonNull();
    }

    public static String getString(JsonObject object, String key, String fallback) {
        return has(object, key) ? object.get(key)
            .getAsString() : fallback;
    }

    public static int getInt(JsonObject object, String key, int fallback) {
        return has(object, key) ? object.get(key)
            .getAsInt() : fallback;
    }

    public static float getFloat(JsonObject object, String key, float fallback) {
        return has(object, key) ? object.get(key)
            .getAsFloat() : fallback;
    }

    public static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        return has(object, key) ? object.get(key)
            .getAsBoolean() : fallback;
    }

    public static JsonElement getElement(JsonObject object, String key) {
        return object == null ? null : object.get(key);
    }
}
