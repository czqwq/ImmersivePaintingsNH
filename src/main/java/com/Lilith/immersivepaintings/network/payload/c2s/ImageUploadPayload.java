package com.Lilith.immersivepaintings.network.payload.c2s;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import com.Lilith.immersivepaintings.network.SegmentManager;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.util.Permissions;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ImageUploadPayload implements ImmersivePayload {

    private static final SegmentManager manager = new SegmentManager();
    public static final Map<String, BufferedImage> uploaded = new HashMap<>();

    private byte[] data = new byte[0];
    private int segment;
    private int totalSegments;

    public ImageUploadPayload() {}

    public ImageUploadPayload(byte[] data, int segment, int totalSegments) {
        this.data = data;
        this.segment = segment;
        this.totalSegments = totalSegments;
    }

    public byte[] data() {
        return data;
    }

    public int segment() {
        return segment;
    }

    public int totalSegments() {
        return totalSegments;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        data = ImmersivePayload.readBytes(buf);
        segment = buf.readInt();
        totalSegments = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeBytes(buf, data);
        buf.writeInt(segment);
        buf.writeInt(totalSegments);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!Permissions.canUpload(player)) return;

        String key = player.getUniqueID()
            .toString();
        int maxWidth = Configs.COMMON.maxUserImageWidth;
        int maxHeight = Configs.COMMON.maxUserImageHeight;
        if (Configs.COMMON.automaticImageResizing) {
            int maxClientSize = 16 * Configs.COMMON.maxPaintingResolution;
            maxWidth = Math.max(maxWidth, maxClientSize);
            maxHeight = Math.max(maxHeight, maxClientSize);
        }
        int maxBytes = (int) Math.min(Integer.MAX_VALUE, (long) maxWidth * maxHeight * 4 + 1024 * 1024);

        manager.handleSegmentedPayload(key, data, segment, totalSegments, maxBytes, maxWidth, maxHeight)
            .ifPresent(image -> runner.run(() -> uploaded.put(key, image)));
    }

    public static void playerLoggedOut(EntityPlayer player) {
        String key = player.getUniqueID()
            .toString();
        manager.clear(key);
        uploaded.remove(key);
    }

    public static class Handler implements IMessageHandler<ImageUploadPayload, IMessage> {

        @Override
        public IMessage onMessage(ImageUploadPayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
