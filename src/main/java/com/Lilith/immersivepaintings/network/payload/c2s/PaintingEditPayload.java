package com.Lilith.immersivepaintings.network.payload.c2s;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PaintingEditPayload implements ImmersivePayload {

    private int entityId;
    private Map<Option, String> options = new HashMap<>();

    public PaintingEditPayload() {}

    public PaintingEditPayload(int entityId, Map<Option, String> options) {
        this.entityId = entityId;
        this.options = options;
    }

    public int entityId() {
        return entityId;
    }

    public Map<Option, String> options() {
        return options;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        int size = buf.readInt();
        options = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Option option = Option.values()[buf.readByte()];
            options.put(option, ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(options.size());
        options.forEach((option, value) -> {
            buf.writeByte(option.ordinal());
            ByteBufUtils.writeUTF8String(buf, value);
        });
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        if (!(player instanceof net.minecraft.entity.player.EntityPlayerMP)) return;
        final net.minecraft.entity.player.EntityPlayerMP serverPlayer = (net.minecraft.entity.player.EntityPlayerMP) player;
        final int id = entityId;
        final Map<Option, String> values = options;

        runner.run(() -> {
            Entity entity = serverPlayer.worldObj.getEntityByID(id);

            if (!(entity instanceof ImmersivePaintingEntity)) return;
            ImmersivePaintingEntity painting = (ImmersivePaintingEntity) entity;
            if (!player.capabilities.allowEdit || player.getDistanceSqToEntity(painting) > 64.0D) return;

            values.forEach((option, value) -> {
                switch (option) {
                    case MOTIVE: {
                        ResourceLocation motive = new ResourceLocation(value);
                        Optional<Painting> target = ServerPaintingManager.getPainting(serverPlayer.mcServer, motive);
                        if (!target.isPresent()) break;

                        Painting selected = target.get();
                        boolean owner = selected.authorUUID()
                            .equals(player.getUniqueID());
                        boolean operator = player.capabilities.isCreativeMode;
                        boolean allowed = (!selected.has(Painting.Flag.HIDDEN) || owner || operator)
                            && selected.has(Painting.Flag.GRAFFITI) == painting.isGraffiti();
                        if (!allowed) {
                            Main.LOGGER.warn("Player {} tried to use inaccessible painting {}", player, motive);
                            break;
                        }

                        painting.setMotive(motive);
                        break;
                    }
                    case FRAME:
                        painting.setFrame(new ResourceLocation(value));
                        break;
                    case MATERIAL:
                        painting.setMaterial(new ResourceLocation(value));
                        break;
                    case DEPTH_OFFSET:
                        try {
                            painting.setDepthOffset(Integer.parseInt(value));
                        } catch (NumberFormatException ignored) {
                            // keep the previous value
                        }
                        break;
                    case DELETE:
                        break;
                    default:
                        break;
                }
            });
        });
    }

    public enum Option {
        MOTIVE,
        FRAME,
        MATERIAL,
        DELETE,
        DEPTH_OFFSET
    }

    public static class Handler implements IMessageHandler<PaintingEditPayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingEditPayload message, MessageContext ctx) {
            message.handle(ctx.getServerHandler().playerEntity, ScheduledTasks::server);
            return null;
        }
    }
}
