package com.Lilith.immersivepaintings.network.payload.c2s;

import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.PaintingRegistration;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingRegisterErrorPayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PaintingRegisterPayload implements ImmersivePayload {

    private int width;
    private int height;
    private int resolution;
    private String name = "";
    private EnumSet<Painting.Flag> flags = EnumSet.noneOf(Painting.Flag.class);

    public PaintingRegisterPayload() {}

    public PaintingRegisterPayload(int width, int height, int resolution, String name, EnumSet<Painting.Flag> flags) {
        this.width = width;
        this.height = height;
        this.resolution = resolution;
        this.name = name;
        this.flags = flags;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int resolution() {
        return resolution;
    }

    public String name() {
        return name;
    }

    public EnumSet<Painting.Flag> flags() {
        return flags;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        width = buf.readInt();
        height = buf.readInt();
        resolution = buf.readInt();
        name = ByteBufUtils.readUTF8String(buf);
        flags = Painting.Flag.fromBits(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeInt(resolution);
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeLong(Painting.Flag.toBits(flags));
    }

    private static void paintingRegisterError(EntityPlayerMP player, String error, ResourceLocation identifier) {
        NetworkHandler.sendToClient(player, new PaintingRegisterErrorPayload(Optional.ofNullable(identifier), error));
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!(player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        final int paintingWidth = width;
        final int paintingHeight = height;
        final int paintingResolution = resolution;
        final String paintingName = name;
        final EnumSet<Painting.Flag> paintingFlags = flags;

        runner.run(() -> {
            BufferedImage image = ImageUploadPayload.uploaded.remove(
                serverPlayer.getUniqueID()
                    .toString());
            Painting painting = new Painting(
                paintingWidth,
                paintingHeight,
                paintingResolution,
                paintingName,
                serverPlayer.getGameProfile()
                    .getName(),
                serverPlayer.getUniqueID(),
                Painting.Type.PAINTING,
                paintingFlags,
                "");
            ResourceLocation identifier = PaintingRegistration
                .register(serverPlayer, image, painting, error -> paintingRegisterError(serverPlayer, error, null));
            if (identifier != null) {
                paintingRegisterError(serverPlayer, "", identifier);
            }
        });
    }

    public static class Handler implements IMessageHandler<PaintingRegisterPayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingRegisterPayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
