package com.Lilith.immersivepaintings.network.payload.c2s;

import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

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

public class PaintingFlagPayload implements ImmersivePayload {

    private ResourceLocation identifier;
    private Painting.Flag flag = Painting.Flag.HIDDEN;
    private boolean value;

    public PaintingFlagPayload() {}

    public PaintingFlagPayload(ResourceLocation identifier, Painting.Flag flag, boolean value) {
        this.identifier = identifier;
        this.flag = flag;
        this.value = value;
    }

    public ResourceLocation identifier() {
        return identifier;
    }

    public Painting.Flag flag() {
        return flag;
    }

    public boolean value() {
        return value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        identifier = ImmersivePayload.readIdentifier(buf);
        flag = Painting.Flag.values()[buf.readByte()];
        value = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeIdentifier(buf, identifier);
        buf.writeByte(flag.ordinal());
        buf.writeBoolean(value);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!(player instanceof EntityPlayerMP)) return;
        final EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        final ResourceLocation id = identifier;
        final Painting.Flag targetFlag = flag;
        final boolean newValue = value;

        runner.run(() -> {
            if (serverPlayer.mcServer == null) return;
            if (targetFlag != Painting.Flag.HIDDEN && targetFlag != Painting.Flag.NSFW) return;

            Painting painting = ServerPaintingManager.getCustomPaintings(serverPlayer.mcServer)
                .get(id);
            if (painting == null) return;
            if (!(painting.authorUUID()
                .equals(player.getUniqueID()) || player.capabilities.isCreativeMode)) return;

            EnumSet<Painting.Flag> flags = EnumSet.copyOf(painting.flags());
            if (newValue) {
                flags.add(targetFlag);
            } else {
                flags.remove(targetFlag);
            }

            Painting updated = new Painting(
                painting.version(),
                painting.width(),
                painting.height(),
                painting.resolution(),
                painting.name(),
                painting.author(),
                painting.authorUUID(),
                painting.type(),
                flags,
                painting.hash());
            ServerPaintingManager.updatePainting(serverPlayer.mcServer, id, updated);
            NetworkHandler.sendToAllClients(serverPlayer.mcServer, new PaintingSyncPayload(id, updated));
        });
    }

    public static class Handler implements IMessageHandler<PaintingFlagPayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingFlagPayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
