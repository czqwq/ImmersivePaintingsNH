package com.Lilith.immersivepaintings.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.Lilith.immersivepaintings.Painting.Size;
import com.Lilith.immersivepaintings.registration.Configs;

/** Platform independent image helpers, ported from upstream (AWT only). */
public class ImageManipulations {

    public interface ChunkConsumer {

        void accept(byte[] data, int segment, int totalSegments);
    }

    public static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    public static BufferedImage decode(byte[] bytes, int maxWidth, int maxHeight) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (reader.getWidth(0) > maxWidth || reader.getHeight(0) > maxHeight) return null;
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    public static byte[] encode(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", stream);
        return stream.toByteArray();
    }

    public static void processByteArrayInChunks(byte[] input, ChunkConsumer consumer) {
        int packetSize = Math.max(1024, Configs.COMMON.packetSize);
        int splits = (int) Math.ceil((double) input.length / packetSize);
        int split = 0;
        for (int i = 0; i < input.length; i += packetSize) {
            byte[] chunk = Arrays.copyOfRange(input, i, Math.min(input.length, i + packetSize));
            consumer.accept(chunk, split, splits);
            split++;
        }
    }

    public static BufferedImage resizeImage(BufferedImage in, Size size) {
        int w = in.getWidth();
        int h = in.getHeight();

        switch (size) {
            case FULL:
                return in;
            case HALF:
                w /= 2;
                h /= 2;
                break;
            case QUARTER:
                w /= 4;
                h /= 4;
                break;
            case EIGHTH:
                w /= 8;
                h /= 8;
                break;
            case THUMBNAIL: {
                float z = Math.min((float) Configs.CLIENT.thumbnailSize / w, (float) Configs.CLIENT.thumbnailSize / h);

                // Keep tiny thumbnails readable relative to huge source images.
                if (z < 0.032F) z = 0.032F;

                if (z < 1.0F) {
                    w *= z;
                    h *= z;
                }

                if (w == in.getWidth()) return in;
                break;
            }
            case NSFW:
                return blur(in, (int) Math.max(1.0F, Configs.CLIENT.thumbnailSize * Configs.CLIENT.nsfwBlurAmount));
            default:
                break;
        }

        w = Math.max(1, w);
        h = Math.max(1, h);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        resize(out, in, (float) in.getWidth() / w, 0, 0);
        return out;
    }

    /** Separable box blur replacing the upstream TwelveMonkeys dependency. */
    public static BufferedImage blur(BufferedImage in, int radius) {
        int width = in.getWidth();
        int height = in.getHeight();
        if (radius <= 0 || width <= 1 || height <= 1) return in;

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] src = in.getRGB(0, 0, width, height, null, 0, width);
        int[] tmp = new int[src.length];
        int[] dst = new int[src.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    int sx = clampInt(x + dx, 0, width - 1);
                    int color = src[y * width + sx];
                    a += (color >>> 24) & 0xFF;
                    r += (color >> 16) & 0xFF;
                    g += (color >> 8) & 0xFF;
                    b += color & 0xFF;
                    count++;
                }
                tmp[y * width + x] = (a / count) << 24 | (r / count) << 16 | (g / count) << 8 | b / count;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int sy = clampInt(y + dy, 0, height - 1);
                    int color = tmp[sy * width + x];
                    a += (color >>> 24) & 0xFF;
                    r += (color >> 16) & 0xFF;
                    g += (color >> 8) & 0xFF;
                    b += color & 0xFF;
                    count++;
                }
                dst[y * width + x] = (a / count) << 24 | (r / count) << 16 | (g / count) << 8 | b / count;
            }
        }

        out.setRGB(0, 0, width, height, dst, 0, width);
        return out;
    }

    private static int HSBtoARGB(float[] hsv, int alpha) {
        int colorNoAlpha = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 16777215;
        return alpha << 24 | colorNoAlpha;
    }

    public static void resize(BufferedImage image, BufferedImage source, float zoom, int ox, int oy) {
        ColorModel sourceModel = source.getColorModel();

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int red = 0, green = 0, blue = 0, alpha = 0;
                int samples = 0;
                for (int px = Math.max(0, (int) (ox + zoom * x)); px
                    < Math.min(source.getWidth(), ox + zoom * (x + 1)); px++) {
                    for (int py = Math.max(0, (int) (oy + zoom * y)); py
                        < Math.min(source.getHeight(), oy + zoom * (y + 1)); py++) {
                        Object elements = source.getRaster()
                            .getDataElements(px, py, null);

                        red += sourceModel.getRed(elements);
                        green += sourceModel.getGreen(elements);
                        blue += sourceModel.getBlue(elements);
                        alpha += sourceModel.getAlpha(elements);

                        samples++;
                    }
                }

                if (samples > 0) {
                    red /= samples;
                    green /= samples;
                    blue /= samples;
                    alpha /= samples;
                }

                image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
    }

    public static void dither(BufferedImage image, double dither) {
        ColorModel model = image.getColorModel();

        float[] hsv = new float[3];
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Object elements = image.getRaster()
                    .getDataElements(x, y, null);
                Color.RGBtoHSB(model.getRed(elements), model.getGreen(elements), model.getBlue(elements), hsv);

                for (int i = 1; i < 3; i++) {
                    if (x % 2 == y % 2) {
                        hsv[i] = (float) Math.min(1.0F, hsv[i] + dither * 0.5);
                    } else {
                        hsv[i] = (float) Math.max(0.0F, hsv[i] - dither * 0.5);
                    }
                }

                image.setRGB(x, y, HSBtoARGB(hsv, model.getAlpha(elements)));
            }
        }
    }

    public static void reduceColors(BufferedImage image, int bins) {
        float[] hsv = new float[3];
        float[][] hist = new float[3][256];

        final int EXCLUDE_HUE = 1;

        ColorModel model = image.getColorModel();

        int base = image.getWidth() * image.getHeight();
        for (int channel = EXCLUDE_HUE; channel < 3; channel++) {
            for (int x = 0; x < 256; x++) {
                hist[channel][x] = base / 255.0F;
            }
        }

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Object elements = image.getRaster()
                    .getDataElements(x, y, null);
                Color.RGBtoHSB(model.getRed(elements), model.getGreen(elements), model.getBlue(elements), hsv);

                for (int i = 0; i < 3; i++) {
                    hist[i][toByte(hsv[i])]++;
                }
            }
        }

        int binSize = (image.getWidth() * image.getHeight() + base) / bins;
        float[][] lookup = new float[3][256];
        for (int channel = EXCLUDE_HUE; channel < 3; channel++) {
            int start = 0;
            for (int bin = 0; bin < bins; bin++) {
                int end = start;
                int sum = 0;
                int pixels = 0;
                while (pixels <= binSize && end < 256) {
                    float v = hist[channel][end];
                    pixels += (int) v;
                    sum += (int) (end * v);
                    end++;
                }

                if (pixels > 0) {
                    for (int b = start; b < end; b++) {
                        lookup[channel][b] = (float) sum / pixels / 255.0F;
                    }
                }

                start = end;
            }
        }

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Object elements = image.getRaster()
                    .getDataElements(x, y, null);
                Color.RGBtoHSB(model.getRed(elements), model.getGreen(elements), model.getBlue(elements), hsv);

                for (int channel = EXCLUDE_HUE; channel < 3; channel++) {
                    hsv[channel] = lookup[channel][toByte(hsv[channel])];
                }

                image.setRGB(x, y, HSBtoARGB(hsv, model.getAlpha(elements)));
            }
        }
    }

    private static int toByte(float v) {
        return clampInt((int) (v * 255), 0, 255);
    }

    public static int clampInt(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static final int MAX_PIXEL_ART_MULTIPLE = 64;
    private static final int TILE_COLOR_TOLERANCE = 16;
    private static final int MAX_OFF_COLOR_PIXELS_PER_TILE = 1;

    public static int scanForPixelArtMultiple(BufferedImage image) {
        int maxMultiple = Math.min(MAX_PIXEL_ART_MULTIPLE, Math.min(image.getWidth(), image.getHeight()));
        for (int multiple = maxMultiple; multiple > 1; multiple--) {
            if (image.getWidth() % multiple == 0 && image.getHeight() % multiple == 0
                && isPixelArtMultiple(image, multiple)) {
                return multiple;
            }
        }

        return 1;
    }

    private static boolean isPixelArtMultiple(BufferedImage image, int multiple) {
        for (int tileX = 0; tileX < image.getWidth(); tileX += multiple) {
            for (int tileY = 0; tileY < image.getHeight(); tileY += multiple) {
                if (!isSingleColorTile(image, tileX, tileY, multiple)) return false;
            }
        }
        return true;
    }

    private static boolean isSingleColorTile(BufferedImage image, int tileX, int tileY, int size) {
        long red = 0;
        long green = 0;
        long blue = 0;
        long alpha = 0;
        int samples = size * size;

        for (int x = tileX; x < tileX + size; x++) {
            for (int y = tileY; y < tileY + size; y++) {
                int color = image.getRGB(x, y);
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
                alpha += (color >> 24) & 0xFF;
            }
        }

        int averageRed = (int) (red / samples);
        int averageGreen = (int) (green / samples);
        int averageBlue = (int) (blue / samples);
        int averageAlpha = (int) (alpha / samples);
        int offColorPixels = 0;

        for (int x = tileX; x < tileX + size; x++) {
            for (int y = tileY; y < tileY + size; y++) {
                int color = image.getRGB(x, y);
                if (!isCloseColor(color, averageRed, averageGreen, averageBlue, averageAlpha)
                    && ++offColorPixels > MAX_OFF_COLOR_PIXELS_PER_TILE) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isCloseColor(int color, int red, int green, int blue, int alpha) {
        return Math.abs(((color >> 16) & 0xFF) - red) <= TILE_COLOR_TOLERANCE
            && Math.abs(((color >> 8) & 0xFF) - green) <= TILE_COLOR_TOLERANCE
            && Math.abs((color & 0xFF) - blue) <= TILE_COLOR_TOLERANCE
            && Math.abs(((color >> 24) & 0xFF) - alpha) <= TILE_COLOR_TOLERANCE;
    }
}
