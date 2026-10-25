package com.Lilith.immersivepaintings.dev;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.client.gui.ImmersivePaintingScreen;
import com.Lilith.immersivepaintings.client.gui.ImmersivePaintingScreen.PixelatorSettings;
import com.Lilith.immersivepaintings.util.ImageManipulations;

/**
 * Dev-only generator for the bundled painting assets. Never runs automatically, use the
 * {@code runDatapackPaintingsGenerator} gradle task or call {@link #run()} manually.
 */
public class DatapackPaintingsGenerator {

    private static final String BASE = "../paintings/";
    private static final String OUTPUT = "src/main/resources/assets/" + Main.DOMAIN + "/paintings/";

    public static void run() throws IOException {
        Files.createDirectories(Paths.get(OUTPUT));

        for (int resolution : new int[] { 16, 32, 64, 128 }) {
            process("Airships - AlexRuizArt.jpg", 5, 3, 0.25, 0, resolution, false);
            process("Autumn - NostalgiaTree.png", 3, 2, 0.0, 0, resolution, false);
            process("Beyond Hill and Dale - Alena Aenami.jpg", 2, 1, 0.25, 12, resolution, false);
            process("Calm Days by the Lake - Scilex.png", 2, 2, 0.25, 12, resolution, false);
            process("Cavern - sennin13.png", 2, 2, 0.25, 12, resolution, false);
            process("concept - BINU BALAN.jpg", 4, 2, 0.25, 12, resolution, false);
            process("Cthulhu - Andree Wallin.jpg", 3, 2, 0.25, 12, resolution, false);
            process("Dark Phoenix - Starkiteckt.png", 4, 2, 0, 32, resolution, false);
            process("Frost Valley - Jorge Jacinto.jpg", 3, 2, 0.25, 12, resolution, false);
            process("Hazy Castle - David Frasheski.jpg", 5, 2, 0, 12, resolution, false);
            process("Ikran Nebula - Starkiteckt.png", 2, 1, 0, 32, resolution, false);
            process("Lake - Sarel Theron.jpg", 3, 2, 0.25, 12, resolution, false);
            process("Life In The Sky - Vladimir Kostuchek.jpg", 3, 2, 0.25, 12, resolution, false);
            process("Nevermore - Josef Barton.jpg", 3, 2, 0.25, 15, resolution, false);
            process("Small Memory - Mikael Gustafsson.jpg", 3, 2, 0.0, 12, resolution, false);
            process("The Ancient Ones - Jessica Woulfe.jpg", 2, 1, 0.5, 12, resolution, false);
            process("The Esteemed Palace - Chris Ostrowski.png", 2, 2, 0.25, 12, resolution, false);
            process("The Lost Legend - Xu Tianhua.jpg", 3, 2, 0.25, 12, resolution, false);
            process("The Torch - Starkiteckt.png", 3, 3, 0, 32, resolution, false);
            process("Tranquil Sunday Walk - Scilex.png", 4, 4, 0.0, 0, resolution, true);
            process("Windmill Town - Darek Zabrocki.jpg", 5, 3, 0.25, 12, resolution, false);
            process("Gothic Landscape - Soroka Alexej.jpg", 3, 2, 0.25, 12, resolution, false);
            process("Deer Landscape - Byzwa Dher.jpg", 2, 1, 0.25, 12, resolution, false);
            process("Venedig - Verena.jpg", 1, 1, 0.25, 12, resolution, false);
        }
    }

    private static void process(String name, int width, int height, double dither, int colors, int resolution,
        boolean pixelArt) throws IOException {
        BufferedImage image = loadImage(name);

        int zoom = ImageManipulations.scanForPixelArtMultiple(image);

        if (image.getWidth() / zoom < width * resolution || image.getHeight() / zoom < height * resolution) {
            return;
        }

        String[] split = name.replace(".png", "")
            .replace(".jpg", "")
            .split(" - ");

        name = OUTPUT + name.toLowerCase(Locale.ROOT)
            .replace(" ", "")
            .replace(".png", "")
            .replace(".jpg", "") + "-" + resolution + "px";

        try (FileWriter writer = new FileWriter(name + ".json")) {
            writer.write(
                String.format(
                    "{\n\t\"name\": \"%s\",\n\t\"author\": \"%s\",\n\t\"resolution\": %d,\n\t\"width\": %d,\n\t\"height\": %d\n}",
                    split[0],
                    split[1],
                    resolution,
                    width,
                    height));
        }

        PixelatorSettings settings = new PixelatorSettings(
            dither / 2,
            colors,
            resolution,
            width,
            height,
            0.5,
            0.5,
            1.0,
            pixelArt);

        File file = new File(name + ".png");
        try {
            ImageIO.write(ImmersivePaintingScreen.pixelateImage(image, settings), "png", file);
        } catch (IOException e) {
            Main.LOGGER.error("could not write datapack image {}", name, e);
        }
    }

    private static BufferedImage loadImage(String path) throws IOException {
        try (FileInputStream stream = new FileInputStream(
            java.nio.file.Paths.get(BASE, path)
                .toFile())) {
            return ImageIO.read(stream);
        }
    }
}
