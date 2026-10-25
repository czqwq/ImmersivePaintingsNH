package com.Lilith.immersivepaintings.registration;

import net.minecraft.entity.Entity;

import com.Lilith.immersivepaintings.entity.ImmersiveGlowGraffitiEntity;
import com.Lilith.immersivepaintings.entity.ImmersiveGlowPaintingEntity;
import com.Lilith.immersivepaintings.entity.ImmersiveGraffitiEntity;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;

import cpw.mods.fml.common.registry.EntityRegistry;

/** Registers the four painting entity types (mirrors the upstream Entities class). */
public class Entities {

    public static final String PAINTING = "painting";
    public static final String GLOW_PAINTING = "glow_painting";
    public static final String GRAFFITI = "graffiti";
    public static final String GLOW_GRAFFITI = "glow_graffiti";

    public static void registerEntities(Object mod) {
        register(ImmersivePaintingEntity.class, PAINTING, 0, mod);
        register(ImmersiveGlowPaintingEntity.class, GLOW_PAINTING, 1, mod);
        register(ImmersiveGraffitiEntity.class, GRAFFITI, 2, mod);
        register(ImmersiveGlowGraffitiEntity.class, GLOW_GRAFFITI, 3, mod);
    }

    private static void register(Class<? extends Entity> entityClass, String name, int id, Object mod) {
        EntityRegistry.registerModEntity(entityClass, name, id, mod, 10, Integer.MAX_VALUE, false);
    }
}
