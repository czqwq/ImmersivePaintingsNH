package com.Lilith.immersivepaintings;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Main {

    public static final String MOD_ID = "ImmersivePaintingsNH";
    /** Lowercase resource domain, 1.7.10 asset paths must be lowercase. */
    public static final String DOMAIN = "immersivepaintingsnh";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private Main() {}

    public static ResourceLocation locate(String path) {
        return new ResourceLocation(DOMAIN, path);
    }

    /** Called once from the Forge proxy during preInit. */
    public static void init() {
        LOGGER.debug("Initializing {}", MOD_ID);
    }
}
