package com.Lilith.immersivepaintings.network.payload.c2s;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.network.LazyNetworkManager;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.ImageResponsePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;
import com.Lilith.immersivepaintings.util.ImageManipulations;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ImageRequestPayload implements ImmersivePayload {

    private ResourceLocation identifier;
    private boolean thumbnail;

    public ImageRequestPayload() {}

    public ImageRequestPayload(ResourceLocation identifier, boolean thumbnail) {
        this.identifier = identifier;
        this.thumbnail = thumbnail;
    }

    public ResourceLocation identifier() {
        return identifier;
    }

    public boolean thumbnail() {
        return thumbnail;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        identifier = ImmersivePayload.readIdentifier(buf);
        thumbnail = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeIdentifier(buf, identifier);
        buf.writeBoolean(thumbnail);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!(player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        final ResourceLocation id = identifier;
        final boolean thumb = thumbnail;

        runner.run(() -> {
            MinecraftServer server = serverPlayer.mcServer;
            if (server == null || !ServerPaintingManager.getPainting(server, id)
                .isPresent()) return;

            Optional<byte[]> image = ServerPaintingManager.getImageData(id, thumb);
            if (image.isPresent()) {
                ImageManipulations.processByteArrayInChunks(
                    image.get(),
                    (bytes, segment, total) -> LazyNetworkManager
                        .sendToClient(new ImageResponsePayload(id, thumb, bytes, segment, total), serverPlayer));
            } else if (!thumb) {
                // The painting was deleted or does not exist, clear it on the clients.
                NetworkHandler.sendToAllClients(server, new PaintingSyncPayload(id, null));
            }
        });
    }

    public static class Handler implements IMessageHandler<ImageRequestPayload, IMessage> {

        @Override
        public IMessage onMessage(ImageRequestPayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
