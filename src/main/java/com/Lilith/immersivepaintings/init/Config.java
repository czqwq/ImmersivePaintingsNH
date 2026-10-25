package com.Lilith.immersivepaintings.init;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.Lilith.immersivepaintings.registration.Configs;

public class Config {

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();
        Configs.init(configuration);
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
