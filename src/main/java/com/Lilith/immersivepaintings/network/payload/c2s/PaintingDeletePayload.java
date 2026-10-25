package com.Lilith.immersivepaintings.network.payload.c2s;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PaintingDeletePayload implements ImmersivePayload {

    private ResourceLocation identifier;
    private boolean adminDelete;

    public PaintingDeletePayload() {}

    public PaintingDeletePayload(ResourceLocation identifier, boolean adminDelete) {
        this.identifier = identifier;
        this.adminDelete = adminDelete;
    }

    public ResourceLocation identifier() {
        return identifier;
    }

    public boolean adminDelete() {
        return adminDelete;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        identifier = ImmersivePayload.readIdentifier(buf);
        adminDelete = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeIdentifier(buf, identifier);
        buf.writeBoolean(adminDelete);
    }

    private static void deletePainting(EntityPlayerMP player, ResourceLocation painting) {
        ServerPaintingManager.deregisterPainting(player.mcServer, painting);
        Main.LOGGER.info("Player {} deleted painting {}", player, painting);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!(player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        final ResourceLocation id = identifier;
        final boolean admin = adminDelete;

        runner.run(() -> {
            if (serverPlayer.mcServer == null) return;

            Painting painting = ServerPaintingManager.getCustomPaintings(serverPlayer.mcServer)
                .get(id);
            if (painting == null) return;

            UUID authorUUID = painting.authorUUID();
            if (!(authorUUID.equals(player.getUniqueID()) || player.capabilities.isCreativeMode)) {
                Main.LOGGER.warn("Player {} tried to delete painting {}, which they do not own", player, id);
                return;
            }

            PaintingSyncPayload payload;
            if (admin) {
                Map<ResourceLocation, Optional<Painting>> deletedPaintings = new HashMap<>();
                ServerPaintingManager.getCustomPaintings(serverPlayer.mcServer)
                    .forEach((key, value) -> {
                        if (value.authorUUID()
                            .equals(authorUUID) && !value.is(Painting.Type.DATAPACK)) {
                            deletedPaintings.put(key, Optional.empty());
                        }
                    });

                deletedPaintings.forEach((key, ignored) -> deletePainting(serverPlayer, key));
                payload = new PaintingSyncPayload(deletedPaintings, false);
            } else {
                deletePainting(serverPlayer, id);
                payload = new PaintingSyncPayload(id, null);
            }

            NetworkHandler.sendToAllClients(serverPlayer.mcServer, payload);
        });
    }

    public static class Handler implements IMessageHandler<PaintingDeletePayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingDeletePayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
