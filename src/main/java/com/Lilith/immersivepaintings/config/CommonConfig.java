package com.Lilith.immersivepaintings.config;

import net.minecraftforge.common.config.Configuration;

/** Server and gameplay settings, stored in the "common" category of the mod config file. */
public class CommonConfig {

    public boolean testIfSpaceEmpty = false;
    public boolean paintingsHaveCollision = false;
    public boolean canOnlyBeDestroyedByPlayer = false;
    public boolean showOtherPlayersPaintings = true;
    public boolean automaticImageResizing = false;
    public boolean enableBundledPaintings = true;
    public int uploadPermissionLevel = 0;

    public int maxUserImageWidth = 4096;
    public int maxUserImageHeight = 4096;
    public int maxUserImages = 1000;

    public int minPaintingResolution = 8;
    public int maxPaintingResolution = 256;

    public int maxPacketsPerSecond = 20;
    public int packetSize = 32 * 1024;
    public int packetSplitInterval = 250;

    public void load(Configuration configuration) {
        String general = "common.general";
        testIfSpaceEmpty = configuration
            .getBoolean("testIfSpaceEmpty", general, testIfSpaceEmpty, "Prevent placement if the space is not empty.");
        paintingsHaveCollision = configuration.getBoolean(
            "paintingsHaveCollision",
            general,
            paintingsHaveCollision,
            "Paintings act as a solid collision box.");
        canOnlyBeDestroyedByPlayer = configuration.getBoolean(
            "canOnlyBeDestroyedByPlayer",
            general,
            canOnlyBeDestroyedByPlayer,
            "Paintings can only be removed by player hits.");
        showOtherPlayersPaintings = configuration.getBoolean(
            "showOtherPlayersPaintings",
            general,
            showOtherPlayersPaintings,
            "Allow selecting paintings uploaded by other players.");
        automaticImageResizing = configuration.getBoolean(
            "automaticImageResizing",
            general,
            automaticImageResizing,
            "Resize oversized uploads instead of rejecting them.");
        enableBundledPaintings = configuration
            .getBoolean("enableBundledPaintings", general, enableBundledPaintings, "Load the bundled painting pack.");
        uploadPermissionLevel = configuration.getInt(
            "uploadPermissionLevel",
            general,
            uploadPermissionLevel,
            0,
            4,
            "0 = everyone, 2 = operators, 4 = server owners.");

        String advanced = "common.advanced";
        maxUserImageWidth = configuration
            .getInt("maxUserImageWidth", advanced, maxUserImageWidth, 1, 8192, "Maximum width of a user image.");
        maxUserImageHeight = configuration
            .getInt("maxUserImageHeight", advanced, maxUserImageHeight, 1, 8192, "Maximum height of a user image.");
        maxUserImages = configuration
            .getInt("maxUserImages", advanced, maxUserImages, 1, 100000, "Maximum paintings per user.");
        minPaintingResolution = configuration
            .getInt("minPaintingResolution", advanced, minPaintingResolution, 1, 256, "Minimum pixels per block.");
        maxPaintingResolution = configuration
            .getInt("maxPaintingResolution", advanced, maxPaintingResolution, 1, 256, "Maximum pixels per block.");
        maxPacketsPerSecond = configuration
            .getInt("maxPacketsPerSecond", advanced, maxPacketsPerSecond, 1, 1000, "Upload/download rate limit.");
        packetSize = configuration
            .getInt("packetSize", advanced, packetSize, 1024, 1024 * 1024, "Size of an image packet chunk in bytes.");
        packetSplitInterval = configuration
            .getInt("packetSplitInterval", advanced, packetSplitInterval, 1, 10000, "Paintings per sync packet.");
    }
}
