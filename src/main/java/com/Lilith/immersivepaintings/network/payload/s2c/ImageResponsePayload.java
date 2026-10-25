package com.Lilith.immersivepaintings.network.payload.s2c;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.ClientPaintingManager;
import com.Lilith.immersivepaintings.network.SegmentManager;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ImageResponsePayload implements ImmersivePayload {

    private static final SegmentManager manager = new SegmentManager();

    private ResourceLocation identifier;
    private boolean thumbnail;
    private byte[] data = new byte[0];
    private int segment;
    private int totalSegments;

    public ImageResponsePayload() {}

    public ImageResponsePayload(ResourceLocation identifier, boolean thumbnail, byte[] data, int segment,
        int totalSegments) {
        this.identifier = identifier;
        this.thumbnail = thumbnail;
        this.data = data;
        this.segment = segment;
        this.totalSegments = totalSegments;
    }

    public ResourceLocation identifier() {
        return identifier;
    }

    public boolean thumbnail() {
        return thumbnail;
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
        identifier = ImmersivePayload.readIdentifier(buf);
        thumbnail = buf.readBoolean();
        data = ImmersivePayload.readBytes(buf);
        segment = buf.readInt();
        totalSegments = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeIdentifier(buf, identifier);
        buf.writeBoolean(thumbnail);
        ImmersivePayload.writeBytes(buf, data);
        buf.writeInt(segment);
        buf.writeInt(totalSegments);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (ClientPaintingManager.arePaintingsHidden()) return;

        final ResourceLocation id = identifier;
        final boolean thumb = thumbnail;
        final byte[] chunk = data;
        final int index = segment;
        final int total = totalSegments;

        String key = String.valueOf(id);
        if (thumb) key += "_thumbnail";

        manager.handleSegmentedPayload(key, chunk, index, total)
            .ifPresent(image -> runner.run(() -> {
                if (thumb) {
                    ClientPaintingManager.registerThumbnail(id, image, false);
                } else {
                    ClientPaintingManager.registerImage(id, image, false);
                }
            }));
    }

    public static class Handler implements IMessageHandler<ImageResponsePayload, IMessage> {

        @Override
        public IMessage onMessage(ImageResponsePayload message, MessageContext ctx) {
            message.handle(null, ScheduledTasks::client);
            return null;
        }
    }
}
