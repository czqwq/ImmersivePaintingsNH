package com.Lilith.immersivepaintings.resources;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.util.Json;
import com.Lilith.immersivepaintings.util.ModResources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Loads frame definitions from the bundled assets. */
public class FrameLoader {

    public static final Map<ResourceLocation, Frame> frames = new HashMap<>();

    private static final String DEFAULT_FRAME = Main.DOMAIN + ":frame/simple";
    private static final String DEFAULT_MATERIAL = Main.DOMAIN + ":frame/simple/oak";

    public static synchronized void load() {
        frames.clear();
        Gson gson = new Gson();

        for (String path : ModResources.list("frames")) {
            if (!path.endsWith(".json")) continue;
            try (InputStreamReader reader = new InputStreamReader(
                ModResources.open("frames/" + path),
                StandardCharsets.UTF_8)) {
                JsonElement element = gson.fromJson(reader, JsonElement.class);
                JsonObject object = element.getAsJsonObject();
                String id = path.substring(0, path.length() - ".json".length());

                Frame frame = new Frame(
                    new ResourceLocation(Json.getString(object, "frame", DEFAULT_FRAME)),
                    Json.getBoolean(object, "diagonals", false),
                    new ResourceLocation(Json.getString(object, "material", DEFAULT_MATERIAL)));

                frames.put(Main.locate("frames/" + id), frame);
            } catch (Exception e) {
                Main.LOGGER.error("failed loading frame {}", path, e);
            }
        }
    }

    public record Frame(ResourceLocation frame, boolean diagonals, ResourceLocation material) {}
}
