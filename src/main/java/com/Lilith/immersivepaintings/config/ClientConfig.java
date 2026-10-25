package com.Lilith.immersivepaintings.config;

import net.minecraftforge.common.config.Configuration;

/** Client only rendering settings, stored in the "client" category of the mod config file. */
public class ClientConfig {

    public boolean loadPaintings = true;
    public boolean showOtherPlayersPaintings = true;
    public boolean cacheOtherPlayersPaintings = false;
    public boolean showNSFWPaintings = true;
    public float nsfwBlurAmount = 0.25f;

    public int thumbnailSize = 128;
    public int lodResolutionMinimum = 32;

    public float halfResolutionThreshold = 4.0f;
    public float quarterResolutionThreshold = 8.0f;
    public float eighthResolutionThreshold = 16.0f;
    public float thumbResolutionThreshold = 32.0f;

    public void load(Configuration configuration) {
        String general = "client.general";
        loadPaintings = configuration
            .getBoolean("loadPaintings", general, loadPaintings, "Download and display paintings on remote servers.");
        showOtherPlayersPaintings = configuration.getBoolean(
            "showOtherPlayersPaintings",
            general,
            showOtherPlayersPaintings,
            "Show paintings uploaded by other players.");
        cacheOtherPlayersPaintings = configuration.getBoolean(
            "cacheOtherPlayersPaintings",
            general,
            cacheOtherPlayersPaintings,
            "Persist other players' paintings on disk.");
        showNSFWPaintings = configuration
            .getBoolean("showNSFWPaintings", general, showNSFWPaintings, "Show NSFW paintings without blur.");
        nsfwBlurAmount = configuration
            .getFloat("nsfwBlurAmount", general, nsfwBlurAmount, 0.0f, 1.0f, "Blur strength for NSFW paintings.");

        String advanced = "client.advanced";
        thumbnailSize = configuration
            .getInt("thumbnailSize", advanced, thumbnailSize, 16, 512, "Thumbnail size in pixels.");
        lodResolutionMinimum = configuration.getInt(
            "lodResolutionMinimum",
            advanced,
            lodResolutionMinimum,
            1,
            256,
            "Stop downscaling below this many pixels per block.");
        halfResolutionThreshold = configuration.getFloat(
            "halfResolutionThreshold",
            advanced,
            halfResolutionThreshold,
            0.0f,
            256.0f,
            "Pixel density where half resolution starts.");
        quarterResolutionThreshold = configuration.getFloat(
            "quarterResolutionThreshold",
            advanced,
            quarterResolutionThreshold,
            0.0f,
            256.0f,
            "Pixel density where quarter resolution starts.");
        eighthResolutionThreshold = configuration.getFloat(
            "eighthResolutionThreshold",
            advanced,
            eighthResolutionThreshold,
            0.0f,
            256.0f,
            "Pixel density where eighth resolution starts.");
        thumbResolutionThreshold = configuration.getFloat(
            "thumbResolutionThreshold",
            advanced,
            thumbResolutionThreshold,
            0.0f,
            256.0f,
            "Pixel density where thumbnails are used.");
    }
}
