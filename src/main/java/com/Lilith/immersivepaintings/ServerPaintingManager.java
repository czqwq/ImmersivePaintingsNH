package com.Lilith.immersivepaintings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;
import com.Lilith.immersivepaintings.util.Cache;
import com.Lilith.immersivepaintings.util.ImageManipulations;

/** Server side custom painting storage, backed by {@link WorldSavedData} (1.7.10 equivalent of SavedData). */
public class ServerPaintingManager extends WorldSavedData {

    private static final String DATA_NAME = Main.MOD_ID;
    private static final Set<UUID> sent = new HashSet<>();

    private static Map<ResourceLocation, Entry<Painting, byte[]>> datapackPaintings = new HashMap<>();
    private final Map<ResourceLocation, Painting> customServerPaintings = new HashMap<>();

    private static final ServerCache paintingCache = new ServerCache("");
    private static final ServerCache thumbnailCache = new ServerCache("_thumbnail");

    public ServerPaintingManager(String name) {
        super(name);
    }

    private static ServerPaintingManager get(MinecraftServer server) {
        World overworld = server.worldServers[0];
        MapStorage storage = overworld.mapStorage;
        ServerPaintingManager data = (ServerPaintingManager) storage.loadData(ServerPaintingManager.class, DATA_NAME);
        if (data == null) {
            data = new ServerPaintingManager(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public static Map<ResourceLocation, Painting> getCustomPaintings(MinecraftServer server) {
        return get(server).customServerPaintings;
    }

    public static Map<ResourceLocation, Entry<Painting, byte[]>> getDatapackPaintings() {
        return datapackPaintings;
    }

    public static void setDatapackPaintings(Map<ResourceLocation, Entry<Painting, byte[]>> paintings) {
        datapackPaintings = paintings;
    }

    public static Optional<Painting> getPainting(MinecraftServer server, ResourceLocation identifier) {
        if (datapackPaintings.containsKey(identifier)) {
            return Optional.of(
                datapackPaintings.get(identifier)
                    .getKey());
        }
        return Optional.ofNullable(getCustomPaintings(server).get(identifier));
    }

    public static Optional<byte[]> getImageData(ResourceLocation identifier, boolean thumbnail) {
        if (datapackPaintings.containsKey(identifier)) {
            return Optional.ofNullable(
                datapackPaintings.get(identifier)
                    .getValue());
        }

        Optional<byte[]> image;
        if (thumbnail) {
            image = thumbnailCache.get(identifier);
        } else {
            image = paintingCache.get(identifier);
        }

        if (!image.isPresent()) {
            Main.LOGGER.error("no image found with identifier {} [thumb={}] in cache", identifier, thumbnail);
        }
        return image;
    }

    public static void registerPainting(MinecraftServer server, ResourceLocation identifier, Painting painting,
        BufferedImage image) {
        if (image == null) return;
        try {
            paintingCache.set(identifier, ImageManipulations.encode(image));
            thumbnailCache.set(
                identifier,
                ImageManipulations.encode(ImageManipulations.resizeImage(image, Painting.Size.THUMBNAIL)));
            getCustomPaintings(server).put(identifier, painting);
            get(server).markDirty();
        } catch (IOException e) {
            Main.LOGGER.error("could not register image {}", identifier, e);
        }
    }

    public static void deregisterPainting(MinecraftServer server, ResourceLocation identifier) {
        getCustomPaintings(server).remove(identifier);
        get(server).markDirty();
        paintingCache.delete(identifier);
        thumbnailCache.delete(identifier);
    }

    public static void updatePainting(MinecraftServer server, ResourceLocation identifier, Painting painting) {
        getCustomPaintings(server).put(identifier, painting);
        get(server).markDirty();
    }

    public static void playerLoggedOut(EntityPlayerMP player) {
        sent.remove(player.getUniqueID());
        com.Lilith.immersivepaintings.network.payload.c2s.ImageUploadPayload.playerLoggedOut(player);
    }

    public static void playerLoggedIn(EntityPlayerMP player) {
        if (sent.contains(player.getUniqueID())) return;

        MinecraftServer server = player.mcServer;
        HashMap<ResourceLocation, Optional<Painting>> paintings = new HashMap<>();
        getDatapackPaintings().forEach((id, entry) -> paintings.put(id, Optional.of(entry.getKey())));
        getCustomPaintings(server).forEach((id, painting) -> paintings.put(id, Optional.of(painting)));

        List<PaintingSyncPayload> payloads = PaintingSyncPayload.splitPaintings(paintings, true);
        for (PaintingSyncPayload payload : payloads) {
            NetworkHandler.sendToClient(player, payload);
        }
        sent.add(player.getUniqueID());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagCompound paintings = nbt.getCompoundTag("paintings");
        for (String key : paintings.func_150296_c()) {
            try {
                ResourceLocation identifier = new ResourceLocation(key);
                if (!paintingCache.exists(identifier)) continue;

                if (!thumbnailCache.exists(identifier)) {
                    Optional<byte[]> source = paintingCache.get(identifier);
                    if (source.isPresent()) {
                        try {
                            thumbnailCache.set(
                                identifier,
                                ImageManipulations.encode(
                                    ImageManipulations.resizeImage(
                                        ImageManipulations.decode(source.get()),
                                        Painting.Size.THUMBNAIL)));
                        } catch (IOException e) {
                            Main.LOGGER.error("could not create thumbnail from stored image", e);
                        }
                    }
                }

                customServerPaintings.put(identifier, Painting.fromNBT(paintings.getCompoundTag(key)));
            } catch (IllegalArgumentException e) {
                Main.LOGGER.error("invalid painting entry {}", key, e);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound paintings = new NBTTagCompound();
        customServerPaintings
            .forEach((identifier, painting) -> paintings.setTag(identifier.toString(), painting.toNBT()));
        nbt.setTag("paintings", paintings);
    }

    private static class ServerCache extends Cache<ResourceLocation, byte[]> {

        private final String suffix;

        ServerCache(String suffix) {
            this.suffix = suffix + ".png";
        }

        @Override
        public String getCachePath(ResourceLocation key) {
            return key.getResourcePath() + suffix;
        }

        @Override
        public byte[] decode(byte[] bytes) {
            return bytes;
        }

        @Override
        public byte[] encode(byte[] image) {
            return image;
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    /** Unused hook kept for parity with the upstream cache helper. */
    static File cacheRoot() {
        return Cache.CACHE_PATH.toFile();
    }
}
