package com.Lilith.immersivepaintings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import com.Lilith.immersivepaintings.resources.LocalPaintings;
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
    /** Texture ids that are currently being read from a local file, so they are only read once. */
    private static final Set<String> localPending = Collections.synchronizedSet(new HashSet<>());
    /** Texture ids that are not available locally, so the network request is used directly. */
    private static final Set<String> localUnavailable = Collections.synchronizedSet(new HashSet<>());

    private static boolean paintingsWereHidden;
    private static final ClientCache paintingCache = new ClientCache();
    private static final ExecutorService service = Executors.newFixedThreadPool(2);

    private static int localFullImages;
    private static long localFullBytes;
    private static int localThumbnails;

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
        if (minecraft.isIntegratedServerRunning() || Configs.CLIENT.cacheOtherPlayersPaintings) return true;

        Optional<Painting> painting = getPainting(identifier);
        if (!painting.isPresent()) return false;

        // Painting pack content is identical for every client, so persisting it is not "persisting other
        // players' paintings"; it is also what allows the painting to be served from disk next time.
        if (Configs.CLIENT.preferLocalPaintings && painting.get()
            .is(Painting.Type.DATAPACK)) return true;

        return minecraft.thePlayer != null && painting.get()
            .authorUUID()
            .equals(minecraft.thePlayer.getUniqueID());
    }

    private static void setImageRequest(ResourceLocation identifier, boolean thumbnail, boolean delete) {
        String id = textureIdentifier(identifier, thumbnail ? Painting.Size.THUMBNAIL : Painting.Size.FULL);
        if (delete) {
            requested.remove(id);
            return;
        }

        if (requested.containsKey(id)) return;

        requested.put(id, true);
        // The bytes may already be on this computer, in which case the server is never asked for them.
        if (requestLocalImage(identifier, thumbnail, id)) return;

        NetworkHandler.Client.sendToServer(new ImageRequestPayload(identifier, thumbnail));
    }

    /**
     * Starts reading an image from the local painting pack or from the download cache instead of asking the
     * server for it. Returns {@code false} when no trustworthy local copy exists, which makes the caller fall
     * back to the network request.
     */
    private static boolean requestLocalImage(ResourceLocation identifier, boolean thumbnail, String textureId) {
        if (!Configs.CLIENT.preferLocalPaintings) return false;
        if (localUnavailable.contains(textureId)) return false;
        if (!localPending.add(textureId)) return true;

        Painting painting = paintings.get(identifier);
        if (painting == null) {
            localPending.remove(textureId);
            localUnavailable.add(textureId);
            return false;
        }

        service.submit(() -> {
            LocalImage read;
            try {
                read = readLocalImage(painting, identifier, thumbnail, textureId);
            } catch (RuntimeException e) {
                Main.LOGGER.error("could not read the local copy of painting {}", textureId, e);
                read = null;
            }

            final LocalImage local = read;
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    localPending.remove(textureId);
                    // The painting was removed while the file was being read.
                    if (!paintings.containsKey(identifier)) return;

                    try {
                        if (local != null) {
                            if (thumbnail) {
                                registerThumbnail(identifier, local.image(), local.alreadyCached(), local.persist());
                                localThumbnails++;
                            } else {
                                registerImage(identifier, local.image(), local.alreadyCached(), local.persist());
                                localFullImages++;
                                localFullBytes += local.bytes();
                            }
                            return;
                        }
                    } catch (RuntimeException e) {
                        Main.LOGGER.error("could not use the local copy of painting {}", textureId, e);
                    }

                    // Missing, unreadable or not matching the server's hash: download it instead.
                    localUnavailable.add(textureId);
                    requested.remove(textureId);
                    setImageRequest(identifier, thumbnail, false);
                });
        });

        return true;
    }

    /** Reads the painting off the render thread. Runs on one of the {@link #service} threads. */
    private static LocalImage readLocalImage(Painting painting, ResourceLocation identifier, boolean thumbnail,
        String textureId) {
        if (painting.is(Painting.Type.DATAPACK) && !isCacheTrusted(painting, identifier)) {
            // Stored files belong to an older version of the painting pack, drop them before anything uses them.
            discardCachedImages(identifier);
        }

        Optional<BufferedImage> cached = paintingCache.get(textureId, true);
        if (cached.isPresent()) {
            return new LocalImage(cached.get(), true, true, thumbnail ? 0L : paintingCache.fileSize(textureId));
        }

        // A full sized image can be downscaled to a thumbnail without downloading the small one.
        if (thumbnail) {
            Optional<BufferedImage> full = paintingCache.get(textureIdentifier(identifier, Painting.Size.FULL), true);
            if (full.isPresent()) {
                return new LocalImage(
                    ImageManipulations.resizeImage(full.get(), Painting.Size.THUMBNAIL),
                    false,
                    true,
                    0L);
            }
        }

        return readPackImage(painting, thumbnail);
    }

    /** Uses the painting pack copy that is on this computer, after checking it against the server's hash. */
    private static LocalImage readPackImage(Painting painting, boolean thumbnail) {
        if (!painting.is(Painting.Type.DATAPACK)) return null;

        Optional<LocalPaintings.Entry> local = LocalPaintings.get(painting.hash());
        if (!local.isPresent()) return null;

        LocalPaintings.Entry entry = local.get();
        String expected = painting.contentHash();
        if (!expected.isEmpty() && !expected.equals(entry.contentHash())) {
            // The pack copy is not the one the server has, so it is not used and the painting is downloaded.
            Main.LOGGER.debug("painting pack copy {} does not match the content hash from the server", painting.hash());
            return null;
        }

        BufferedImage image;
        try {
            image = ImageManipulations.decode(entry.image());
        } catch (IOException e) {
            Main.LOGGER.error("could not decode painting pack copy {}", painting.hash(), e);
            return null;
        }

        if (image == null) return null;
        if (thumbnail) image = ImageManipulations.resizeImage(image, Painting.Size.THUMBNAIL);

        return new LocalImage(image, false, false, thumbnail ? 0L : entry.image().length);
    }

    /**
     * True when whatever is stored in the client cache belongs to the content the server currently has. Painting
     * pack paintings are keyed by their path, so the content hash of the last download is stored next to the
     * cached files and compared here; player uploads are keyed by a hash of their content and need no stamp.
     */
    private static boolean isCacheTrusted(Painting painting, ResourceLocation identifier) {
        if (!painting.is(Painting.Type.DATAPACK)) return true;

        String expected = painting.contentHash();
        if (expected.isEmpty()) return true;

        return expected.equals(readCacheStamp(identifier));
    }

    private static Path cacheStampPath(ResourceLocation identifier) {
        Path path = paintingCache.pathOf(textureIdentifier(identifier, Painting.Size.FULL));
        return path == null ? null : path.resolveSibling(path.getFileName() + ".contenthash");
    }

    private static String readCacheStamp(ResourceLocation identifier) {
        Path path = cacheStampPath(identifier);
        if (path == null) return "";

        try {
            if (!Files.isRegularFile(path)) return "";
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            Main.LOGGER.error("could not read the cache stamp of painting {}", identifier, e);
            return "";
        }
    }

    private static void writeCacheStamp(ResourceLocation identifier) {
        if (!shouldCachePainting(identifier)) return;

        Optional<Painting> painting = getPainting(identifier);
        if (!painting.isPresent() || !painting.get()
            .is(Painting.Type.DATAPACK)) return;
        String contentHash = painting.get()
            .contentHash();
        if (contentHash.isEmpty()) return;

        Path path = cacheStampPath(identifier);
        if (path == null) return;

        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.write(path, contentHash.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Main.LOGGER.error("could not write the cache stamp of painting {}", identifier, e);
        }
    }

    private static void deleteCacheStamp(ResourceLocation identifier) {
        Path path = cacheStampPath(identifier);
        if (path == null) return;

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Main.LOGGER.error("could not delete the cache stamp of painting {}", identifier, e);
        }
    }

    private static synchronized void discardCachedImages(ResourceLocation identifier) {
        if (paintingCache.exists(textureIdentifier(identifier, Painting.Size.FULL))) {
            for (Painting.Size size : Painting.Size.values()) {
                paintingCache.delete(textureIdentifier(identifier, size));
            }
        }

        deleteCacheStamp(identifier);
    }

    /** Logs how much bandwidth the local copies saved this session, then resets the counters. */
    public static void logLocalResolutionSummary() {
        if (localFullImages == 0 && localThumbnails == 0) return;

        Main.LOGGER.info(
            "Read {} paintings from local files this session: {} full images (about {} MB) and "
                + "{} thumbnails were not downloaded",
            localFullImages + localThumbnails,
            localFullImages,
            String.format(Locale.ROOT, "%.1f", localFullBytes / 1024.0 / 1024.0),
            localThumbnails);

        localFullImages = 0;
        localFullBytes = 0L;
        localThumbnails = 0;
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
                false,
                true));
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
        localUnavailable.remove(fullId);
        localUnavailable.remove(thumbId);

        if (arePaintingsHidden()) return;

        boolean cacheable = shouldCachePainting(identifier) && isCacheTrusted(painting, identifier);

        requested.put(fullId, true);
        requested.put(thumbId, true);

        Optional<BufferedImage> thumbnail = cacheable ? paintingCache.get(thumbId, true) : Optional.empty();
        if (thumbnail.isPresent()) {
            registerThumbnail(identifier, thumbnail.get(), true);
        } else {
            requested.remove(thumbId);
        }

        Optional<BufferedImage> full = cacheable ? paintingCache.get(fullId, true) : Optional.empty();
        if (full.isPresent()) {
            registerImage(identifier, full.get(), true);
        } else {
            requested.remove(fullId);
        }
    }

    public static void deregisterPainting(ResourceLocation identifier) {
        if (!paintings.containsKey(identifier)) return;

        paintings.remove(identifier);
        localUnavailable.remove(textureIdentifier(identifier, Painting.Size.FULL));
        localUnavailable.remove(textureIdentifier(identifier, Painting.Size.THUMBNAIL));

        Map<Painting.Size, ResourceLocation> map = textureMap.remove(identifier);
        if (map != null) {
            TextureManager manager = Minecraft.getMinecraft()
                .getTextureManager();
            map.forEach((size, id) -> {
                paintingCache.delete(textureIdentifier(identifier, size));
                manager.deleteTexture(id);
            });
        }

        deleteCacheStamp(identifier);
    }

    private static void registerImageType(ResourceLocation identifier, BufferedImage fullImage, Painting.Size size,
        Painting.Size realSize, final boolean alreadyCached, final boolean persist) {
        textureMap.putIfAbsent(identifier, new HashMap<>());
        Map<Painting.Size, ResourceLocation> mapping = textureMap.get(identifier);
        // The in-memory cache always applies, the disk cache only for images that may be persisted.
        boolean cache = shouldCachePainting(identifier);
        boolean store = cache && persist;

        // Handle the case where an image is too small and multiple sizes collapse into one.
        if (mapping.containsKey(size) && size != realSize) {
            mapping.put(realSize, mapping.get(size));
            return;
        }

        service.submit(() -> {
            BufferedImage target;

            String path = textureIdentifier(identifier, realSize);
            Optional<BufferedImage> image = paintingCache.get(path, store);
            if (image.isPresent()) {
                target = image.get();
            } else {
                if (realSize == Painting.Size.THUMBNAIL) {
                    target = fullImage;
                } else {
                    target = ImageManipulations.resizeImage(fullImage, size);
                }

                if (!alreadyCached) paintingCache.set(path, target, store);
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
        registerThumbnail(identifier, image, alreadyCached, true);
    }

    private static void registerThumbnail(ResourceLocation identifier, BufferedImage image, boolean alreadyCached,
        boolean persist) {
        if (arePaintingsHidden()) return;

        registerImageType(identifier, image, Painting.Size.THUMBNAIL, Painting.Size.THUMBNAIL, alreadyCached, persist);
        if (persist && !alreadyCached) writeCacheStamp(identifier);
        setImageRequest(identifier, true, true);
    }

    public static void registerImage(ResourceLocation identifier, BufferedImage image, boolean alreadyCached) {
        registerImage(identifier, image, alreadyCached, true);
    }

    private static void registerImage(ResourceLocation identifier, BufferedImage image, boolean alreadyCached,
        boolean persist) {
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
        registerImageType(identifier, image, Painting.Size.FULL, Painting.Size.FULL, alreadyCached, persist);
        if (persist && !alreadyCached) writeCacheStamp(identifier);

        Painting.Size halfSize = resolution / 2 < Configs.CLIENT.lodResolutionMinimum ? Painting.Size.FULL
            : Painting.Size.HALF;
        registerImageType(identifier, image, halfSize, Painting.Size.HALF, alreadyCached, persist);

        Painting.Size quarterSize = resolution / 4 < Configs.CLIENT.lodResolutionMinimum ? halfSize
            : Painting.Size.QUARTER;
        registerImageType(identifier, image, quarterSize, Painting.Size.QUARTER, alreadyCached, persist);

        Painting.Size eighthSize = resolution / 8 < Configs.CLIENT.lodResolutionMinimum ? quarterSize
            : Painting.Size.EIGHTH;
        registerImageType(identifier, image, eighthSize, Painting.Size.EIGHTH, alreadyCached, persist);

        setImageRequest(identifier, false, true);
    }

    /**
     * A locally available image. {@code alreadyCached} marks a texture that is already stored on disk as is,
     * {@code persist} whether the texture may be written to the client cache, and {@code bytes} is the
     * download that was avoided.
     */
    private record LocalImage(BufferedImage image, boolean alreadyCached, boolean persist, long bytes) {}

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
