package com.Lilith.immersivepaintings.resources;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.util.Cache;
import com.Lilith.immersivepaintings.util.Json;
import com.Lilith.immersivepaintings.util.ModResources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Loads bundled paintings from the classpath and additional json/png pairs from
 * {@code config/ImmersivePaintingsNH/paintings}. Replaces the 1.21 datapack loader.
 */
public class PaintingsLoader {

    private static final String DATA_TYPE = "paintings";
    private static final Gson GSON = new Gson();

    public static synchronized void load() {
        Map<ResourceLocation, Map.Entry<Painting, byte[]>> paintings = new HashMap<>();

        for (String path : ModResources.list(DATA_TYPE)) {
            if (!path.endsWith(".png")) continue;
            loadEntry(paintings, path, () -> ModResources.open(DATA_TYPE + "/" + path));
        }

        File external = new File(ModResources.gameDirectory(), "config/" + Main.MOD_ID + "/" + DATA_TYPE);
        if (external.isDirectory()) {
            Path root = external.toPath();
            try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                    .filter(
                        path -> path.toString()
                            .endsWith(".png"))
                    .forEach(path -> {
                        String relative = root.relativize(path)
                            .toString()
                            .replace('\\', '/');
                        loadEntry(paintings, "external/" + relative, () -> ModResources.openFile(path.toFile()));
                    });
            } catch (Exception e) {
                Main.LOGGER.error("failed scanning external painting pack {}", external, e);
            }
        }

        ServerPaintingManager.setDatapackPaintings(paintings);
        Main.LOGGER.info("Loaded {} bundled painting images", paintings.size());
    }

    private static void loadEntry(Map<ResourceLocation, Map.Entry<Painting, byte[]>> paintings, String path,
        InputStreamSupplier imageSupplier) {
        String jsonPath = path.substring(0, path.length() - ".png".length()) + ".json";
        try (InputStream jsonStream = openJson(jsonPath)) {
            if (jsonStream == null) {
                Main.LOGGER.error("Couldn't load painting {}: no matching json", path);
                return;
            }

            JsonObject json = GSON
                .fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonElement.class)
                .getAsJsonObject();

            boolean bundled = !path.startsWith("external/");
            if (bundled && !Configs.COMMON.enableBundledPaintings) return;

            byte[] image;
            try (InputStream imageStream = imageSupplier.open()) {
                if (imageStream == null) return;
                image = Cache.readAll(imageStream);
            }

            int width = Json.getInt(json, "width", 1);
            int height = Json.getInt(json, "height", 1);
            int resolution = Json.getInt(json, "resolution", 32);
            String name = Json.getString(json, "name", "unknown");
            String author = Json.getString(json, "author", "unknown");
            boolean graffiti = Json.getBoolean(json, "graffiti", false);
            String hash = path.replaceAll("[^a-zA-Z\\d]", "");

            EnumSet<Painting.Flag> flags = EnumSet.noneOf(Painting.Flag.class);
            if (graffiti) flags.add(Painting.Flag.GRAFFITI);

            Painting painting = new Painting(
                width,
                height,
                resolution,
                name,
                author,
                UUID.randomUUID(),
                Painting.Type.DATAPACK,
                flags,
                hash);
            paintings.put(painting.location(), Map.entry(painting, image));
        } catch (Exception e) {
            Main.LOGGER.error("Couldn't load painting {} ({})", path, e);
        }
    }

    private static InputStream openJson(String path) {
        Optional<InputStream> external = externalJson(path);
        return external.orElseGet(() -> ModResources.open(DATA_TYPE + "/" + path));
    }

    private static Optional<InputStream> externalJson(String path) {
        if (!path.startsWith("external/")) return Optional.empty();
        File file = new File(
            ModResources.gameDirectory(),
            "config/" + Main.MOD_ID + "/" + DATA_TYPE + "/" + path.substring("external/".length()));
        if (!file.isFile()) return Optional.empty();
        return Optional.ofNullable(ModResources.openFile(file));
    }

    private interface InputStreamSupplier {

        InputStream open();
    }
}
