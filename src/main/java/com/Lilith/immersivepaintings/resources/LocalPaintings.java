package com.Lilith.immersivepaintings.resources;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.Lilith.immersivepaintings.init.ImmersivePaintings;
import com.Lilith.immersivepaintings.registration.Configs;

/**
 * Client side index of the painting packs that are already stored on this computer: the pack that ships inside
 * the mod jar and the pack in {@code config/ImmersivePaintingsNH/paintings}.
 *
 * <p>
 * Entries are keyed by the hash {@link PaintingsLoader} derives from the pack relative path, which is also the
 * hash the server puts into the painting metadata, and keep the raw PNG bytes together with their MD5. Those
 * bytes are byte identical to what the server serves for the same painting, so {@code ClientPaintingManager}
 * can verify them against {@code Painting#contentHash} and skip the download entirely.
 */
public final class LocalPaintings {

    /** A painting pack copy on disk: the MD5 of {@code image} and the raw PNG bytes. */
    public record Entry(String contentHash, byte[] image) {}

    private static final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private LocalPaintings() {}

    /** Indexing is pointless on a dedicated server and unwanted when the client feature is disabled. */
    public static boolean enabled() {
        return Configs.CLIENT.preferLocalPaintings && ImmersivePaintings.proxy != null
            && ImmersivePaintings.proxy.isClient();
    }

    public static void put(String hash, String contentHash, byte[] image) {
        entries.put(hash, new Entry(contentHash, image));
    }

    public static Optional<Entry> get(String hash) {
        return Optional.ofNullable(entries.get(hash));
    }

    public static int size() {
        return entries.size();
    }
}
