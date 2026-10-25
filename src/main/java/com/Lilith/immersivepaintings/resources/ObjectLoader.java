package com.Lilith.immersivepaintings.resources;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.util.ModResources;

import owens.oobjloader.Builder;
import owens.oobjloader.Face;

/** Loads all bundled OBJ models once on the client. */
public class ObjectLoader {

    public static final Map<ResourceLocation, List<Face>> objects = new HashMap<>();

    public static synchronized void load() {
        objects.clear();

        for (String path : ModResources.list("objects")) {
            if (!path.endsWith(".obj")) continue;
            try (InputStream stream = ModResources.open("objects/" + path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                ArrayList<Face> faces = new Builder(reader).faces;
                objects.put(Main.locate("objects/" + path), faces);
            } catch (Exception e) {
                Main.LOGGER.error("failed loading object {}", path, e);
            }
        }
    }
}
