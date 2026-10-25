package com.Lilith.immersivepaintings.registration;

import net.minecraftforge.common.config.Configuration;

import com.Lilith.immersivepaintings.config.ClientConfig;
import com.Lilith.immersivepaintings.config.CommonConfig;

/** Replaces the upstream fzzy_config registration with a plain Forge configuration. */
public class Configs {

    public static CommonConfig COMMON = new CommonConfig();
    public static ClientConfig CLIENT = new ClientConfig();

    public static void init(Configuration configuration) {
        COMMON.load(configuration);
        CLIENT.load(configuration);
    }
}
