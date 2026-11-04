package com.Lilith.immersivepaintings.registration;

import net.minecraft.entity.Entity;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.entity.ImmersiveAdminPaintingEntity;
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
    public static final String ADMIN_PAINTING = "admin_painting";

    /** Paintings are decorative and static, so a vanilla like range is cheap: only the spawn packet is sent once. */
    private static final int MIN_TRACKING_RANGE = 16;
    private static final int MAX_TRACKING_RANGE = 512;

    /** Called after the config is loaded, the tracking range below decides how far the entities are synced. */
    public static void registerEntities(Object mod) {
        int configured = Configs.COMMON.paintingTrackingRange;
        int trackingRange = clampTrackingRange(configured);
        if (trackingRange != configured) {
            Main.LOGGER.warn(
                "common.advanced.paintingTrackingRange = {} is outside {}..{}, using {} blocks instead",
                configured,
                MIN_TRACKING_RANGE,
                MAX_TRACKING_RANGE,
                trackingRange);
        }
        Main.LOGGER.info("Painting entities are synced to players within {} blocks", trackingRange);

        register(ImmersivePaintingEntity.class, PAINTING, 0, mod, trackingRange);
        register(ImmersiveGlowPaintingEntity.class, GLOW_PAINTING, 1, mod, trackingRange);
        register(ImmersiveGraffitiEntity.class, GRAFFITI, 2, mod, trackingRange);
        register(ImmersiveGlowGraffitiEntity.class, GLOW_GRAFFITI, 3, mod, trackingRange);
        register(ImmersiveAdminPaintingEntity.class, ADMIN_PAINTING, 4, mod, trackingRange);
    }

    private static void register(Class<? extends Entity> entityClass, String name, int id, Object mod,
        int trackingRange) {
        // Integer.MAX_VALUE update frequency: paintings never move, they only need the initial spawn packet.
        EntityRegistry.registerModEntity(entityClass, name, id, mod, trackingRange, Integer.MAX_VALUE, false);
    }

    private static int clampTrackingRange(int configured) {
        return Math.max(MIN_TRACKING_RANGE, Math.min(MAX_TRACKING_RANGE, configured));
    }
}
