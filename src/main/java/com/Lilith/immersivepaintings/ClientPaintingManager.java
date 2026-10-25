package com.Lilith.immersivepaintings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.client.gui.ImmersivePaintingScreen;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.c2s.ImageRequestPayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.util.Cache;
import com.Lilith.immersivepaintings.util.ImageManipulations;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Client side painting metadata and texture cache. */
@SideOnly(Side.CLIENT)
public class ClientPaintingManager {

    private static final Map<ResourceLocation, Painting> paintings = Collections.synchronizedMap(new HashMap<>());
    private static final Map<ResourceLocation, Map<Painting.Size, ResourceLocation>> textureMap = Collections
        .synchronizedMap(new HashMap<>());
    private static final Map<String, Boolean> requested = Collections.synchronizedMap(new HashMap<>());

    private static boolean paintingsWereHidden;
    private static final ClientCache paintingCache = new ClientCache();
    private static final ExecutorService service = Executors.newFixedThreadPool(2);

    public static Map<ResourceLocation, Painting> getPaintings() {
        return paintings;
    }

    public static Optional<Painting> getPainting(ResourceLocation identifier) {
        return Optional.ofNullable(paintings.get(identifier));
    }

    public static boolean arePaintingsHidden() {
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean hidden = !Configs.CLIENT.loadPaintings && !minecraft.isIntegratedServerRunning();
        if (hidden && !paintingsWereHidden) {
            requested.clear();
        }

        paintingsWereHidden = hidden;
        return hidden;
    }

    private static String textureIdentifier(ResourceLocation identifier, Painting.Size size) {
        if (size == Painting.Size.FULL) return identifier.getResourcePath();
        return identifier.getResourcePath() + "_"
            + size.name()
                .toLowerCase();
    }

    private static boolean shouldCachePainting(ResourceLocation identifier) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.isIntegratedServerRunning() || Configs.CLIENT.cacheOtherPlayersPaintings
            || minecraft.thePlayer != null && getPainting(identifier).map(
                p -> p.authorUUID()
                    .equals(minecraft.thePlayer.getUniqueID()))
                .orElse(false);
    }

    private static void setImageRequest(ResourceLocation identifier, boolean thumbnail, boolean delete) {
        String id = textureIdentifier(identifier, thumbnail ? Painting.Size.THUMBNAIL : Painting.Size.FULL);
        if (requested.containsKey(id) == delete) {
            if (delete) {
                requested.remove(id);
            } else {
                requested.put(id, true);
                NetworkHandler.Client.sendToServer(new ImageRequestPayload(identifier, thumbnail));
            }
        }
    }

    private static ResourceLocation getOrNSFW(Map<Painting.Size, ResourceLocation> mapping, ResourceLocation identifier,
        Painting.Size size) {
        if (Configs.CLIENT.showNSFWPaintings) return mapping.get(size);

        Painting painting = paintings.get(identifier);
        if (painting == null || !painting.has(Painting.Flag.NSFW)) return mapping.get(size);

        if (mapping.containsKey(Painting.Size.NSFW)) return mapping.get(Painting.Size.NSFW);

        if (!mapping.containsKey(Painting.Size.THUMBNAIL)) {
            setImageRequest(identifier, true, false);
            return Painting.DEFAULT_IDENTIFIER;
        }

        Optional<BufferedImage> image = paintingCache
            .get(textureIdentifier(identifier, Painting.Size.THUMBNAIL), shouldCachePainting(identifier));
        image.ifPresent(
            bufferedImage -> registerImageType(
                identifier,
                bufferedImage,
                Painting.Size.NSFW,
                Painting.Size.NSFW,
                false));
        return Painting.DEFAULT_IDENTIFIER;
    }

    public static ResourceLocation getImageIdentifier(ResourceLocation identifier, Painting.Size size) {
        if (arePaintingsHidden()) return Painting.DEFAULT_IDENTIFIER;
        if (!paintings.containsKey(identifier)) return Painting.DEFAULT_IDENTIFIER;

        Map<Painting.Size, ResourceLocation> mapping = textureMap.get(identifier);

        if (mapping == null) {
            setImageRequest(identifier, true, false);
            if (size != Painting.Size.THUMBNAIL) setImageRequest(identifier, false, false);
            return Painting.DEFAULT_IDENTIFIER;
        } else if (!mapping.containsKey(size)) {
            boolean isThumbnail = size == Painting.Size.THUMBNAIL;
            setImageRequest(identifier, isThumbnail, false);

            if (!isThumbnail && mapping.containsKey(Painting.Size.THUMBNAIL)) {
                return getOrNSFW(mapping, identifier, Painting.Size.THUMBNAIL);
            }

            return Painting.DEFAULT_IDENTIFIER;
        }

        return getOrNSFW(mapping, identifier, size);
    }

    public static void registerPainting(ResourceLocation identifier, Painting painting) {
        String fullId = textureIdentifier(identifier, Painting.Size.FULL);
        String thumbId = textureIdentifier(identifier, Painting.Size.THUMBNAIL);

        paintings.put(identifier, painting);

        if (arePaintingsHidden()) return;

        requested.put(fullId, true);
        requested.put(thumbId, true);

        Optional<BufferedImage> thumbnail = paintingCache.get(thumbId, shouldCachePainting(identifier));
        if (thumbnail.isPresent()) {
            registerThumbnail(identifier, thumbnail.get(), true);
        } else {
            requested.remove(thumbId);
        }

        Optional<BufferedImage> full = paintingCache.get(fullId, shouldCachePainting(identifier));
        if (full.isPresent()) {
            registerImage(identifier, full.get(), true);
        } else {
            requested.remove(fullId);
        }
    }

    public static void deregisterPainting(ResourceLocation identifier) {
        if (!paintings.containsKey(identifier)) return;

        paintings.remove(identifier);

        Map<Painting.Size, ResourceLocation> map = textureMap.remove(identifier);
        if (map != null) {
            TextureManager manager = Minecraft.getMinecraft()
                .getTextureManager();
            map.forEach((size, id) -> {
                paintingCache.delete(textureIdentifier(identifier, size));
                manager.deleteTexture(id);
            });
        }
    }

    private static void registerImageType(ResourceLocation identifier, BufferedImage fullImage, Painting.Size size,
        Painting.Size realSize, final boolean alreadyCached) {
        textureMap.putIfAbsent(identifier, new HashMap<>());
        Map<Painting.Size, ResourceLocation> mapping = textureMap.get(identifier);
        boolean cache = shouldCachePainting(identifier);

        // Handle the case where an image is too small and multiple sizes collapse into one.
        if (mapping.containsKey(size) && size != realSize) {
            mapping.put(realSize, mapping.get(size));
            return;
        }

        service.submit(() -> {
            BufferedImage target;

            String path = textureIdentifier(identifier, realSize);
            Optional<BufferedImage> image = paintingCache.get(path, cache);
            if (image.isPresent()) {
                target = image.get();
            } else {
                if (realSize == Painting.Size.THUMBNAIL) {
                    target = fullImage;
                } else {
                    target = ImageManipulations.resizeImage(fullImage, size);
                }

                if (!alreadyCached) paintingCache.set(path, target, cache);
            }

            final BufferedImage finalTarget = target;
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    ResourceLocation id = new ResourceLocation(Main.DOMAIN, path);
                    Minecraft.getMinecraft()
                        .getTextureManager()
                        .loadTexture(id, new DynamicTexture(finalTarget));
                    mapping.put(realSize, id);

                    if (size == Painting.Size.THUMBNAIL
                        && Minecraft.getMinecraft().currentScreen instanceof ImmersivePaintingScreen) {
                        ((ImmersivePaintingScreen) Minecraft.getMinecraft().currentScreen).updateWidget(identifier);
                    }
                });
        });
    }

    public static void registerThumbnail(ResourceLocation identifier, BufferedImage image, boolean alreadyCached) {
        if (arePaintingsHidden()) return;

        registerImageType(identifier, image, Painting.Size.THUMBNAIL, Painting.Size.THUMBNAIL, alreadyCached);
        setImageRequest(identifier, true, true);
    }

    public static void registerImage(ResourceLocation identifier, BufferedImage image, boolean alreadyCached) {
        if (arePaintingsHidden()) return;

        if (!paintings.containsKey(identifier) && !alreadyCached) {
            Main.LOGGER.error("no existing painting record for identifier {}", identifier);
            return;
        }

        Map<Painting.Size, ResourceLocation> mapping = textureMap.get(identifier);
        if (mapping != null && mapping.containsKey(Painting.Size.FULL)) return;

        Painting painting = paintings.get(identifier);
        if (painting == null) return;

        int resolution = Math.max(painting.width(), painting.height()) * painting.resolution();
        registerImageType(identifier, image, Painting.Size.FULL, Painting.Size.FULL, alreadyCached);

        Painting.Size halfSize = resolution / 2 < Configs.CLIENT.lodResolutionMinimum ? Painting.Size.FULL
            : Painting.Size.HALF;
        registerImageType(identifier, image, halfSize, Painting.Size.HALF, alreadyCached);

        Painting.Size quarterSize = resolution / 4 < Configs.CLIENT.lodResolutionMinimum ? halfSize
            : Painting.Size.QUARTER;
        registerImageType(identifier, image, quarterSize, Painting.Size.QUARTER, alreadyCached);

        Painting.Size eighthSize = resolution / 8 < Configs.CLIENT.lodResolutionMinimum ? quarterSize
            : Painting.Size.EIGHTH;
        registerImageType(identifier, image, eighthSize, Painting.Size.EIGHTH, alreadyCached);

        setImageRequest(identifier, false, true);
    }

    private static class ClientCache extends Cache<String, BufferedImage> {

        @Override
        public String getCachePath(String key) {
            return key + ".png";
        }

        @Override
        public BufferedImage decode(byte[] bytes) {
            try {
                return ImageManipulations.decode(bytes);
            } catch (IOException e) {
                Main.LOGGER.error("could not read image from client cache", e);
            }
            return null;
        }

        @Override
        public byte[] encode(BufferedImage image) {
            try {
                return ImageManipulations.encode(image);
            } catch (IOException e) {
                Main.LOGGER.error("could not write image to client cache", e);
            }
            return null;
        }
    }
}
