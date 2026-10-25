package com.Lilith.immersivepaintings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.util.ImageManipulations;
import com.Lilith.immersivepaintings.util.Permissions;

/** Validates and stores an uploaded image, ported from upstream. */
public class PaintingRegistration {

    private static void addSettingsToHash(MessageDigest md5, Painting painting) {
        md5.update(
            ("settings:" + painting
                .width() + ":" + painting.height() + ":" + painting.resolution() + ":" + painting.name() + ":")
                    .getBytes(StandardCharsets.UTF_8));
        for (Painting.Flag flag : Painting.Flag.values()) {
            if (painting.has(flag)) {
                md5.update((flag.name() + ":").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Nullable
    public static ResourceLocation register(EntityPlayerMP player, @Nullable BufferedImage image, Painting painting,
        Consumer<String> onError) {
        int width = painting.width();
        int height = painting.height();
        int resolution = painting.resolution();
        String name = painting.name();

        if (!Permissions.canUpload(player)) {
            onError.accept("no_permission");
            return null;
        }

        if (image == null) {
            onError.accept("image_load_failed");
            return null;
        }

        MinecraftServer server = player.mcServer;
        long count = ServerPaintingManager.getCustomPaintings(server)
            .values()
            .stream()
            .filter(
                p -> p.authorUUID()
                    .equals(player.getUniqueID()))
            .count();
        if (count >= Configs.COMMON.maxUserImages) {
            onError.accept("limit_reached");
            return null;
        }

        if (width < 1 || width > 16
            || height < 1
            || height > 16
            || resolution < Configs.COMMON.minPaintingResolution
            || resolution > Configs.COMMON.maxPaintingResolution
            || name.length() > 256) {
            onError.accept("image_load_failed");
            return null;
        }

        if (image.getWidth() > Configs.COMMON.maxUserImageWidth
            || image.getHeight() > Configs.COMMON.maxUserImageHeight) {
            if (!Configs.COMMON.automaticImageResizing) {
                onError.accept("too_large");
                return null;
            }

            float z = Math.min(
                (float) Configs.COMMON.maxUserImageWidth / image.getWidth(),
                (float) Configs.COMMON.maxUserImageHeight / image.getHeight());

            BufferedImage newImage = new BufferedImage(
                Math.max(1, (int) (image.getWidth() * z)),
                Math.max(1, (int) (image.getHeight() * z)),
                BufferedImage.TYPE_INT_ARGB);
            ImageManipulations.resize(newImage, image, 1 / z, 0, 0);
            image = newImage;
        }

        try {
            byte[] encodedImage = ImageManipulations.encode(image);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(encodedImage);
            addSettingsToHash(md5, painting);
            String hash = String.format("%032x", new BigInteger(1, md5.digest()));
            painting = painting.withHash(hash);
            ResourceLocation identifier = painting.location();

            ServerPaintingManager.registerPainting(server, identifier, painting, image);
            NetworkHandler.sendToAllClients(server, new PaintingSyncPayload(identifier, painting));
            return identifier;
        } catch (NoSuchAlgorithmException e) {
            Main.LOGGER.error("failed to hash painting {}", painting.location(), e);
            onError.accept("hash_failed");
        } catch (IOException e) {
            Main.LOGGER.error("failed to encode painting {}", painting.location(), e);
            onError.accept("hash_failed");
        }

        return null;
    }
}
