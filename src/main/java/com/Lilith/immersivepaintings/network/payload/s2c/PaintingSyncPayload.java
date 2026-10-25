package com.Lilith.immersivepaintings.network.payload.s2c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.ClientPaintingManager;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.init.ImmersivePaintings;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.registration.Configs;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PaintingSyncPayload implements ImmersivePayload {

    private Map<ResourceLocation, Optional<Painting>> paintings = new HashMap<>();
    private boolean clear;

    public PaintingSyncPayload() {}

    public PaintingSyncPayload(Map<ResourceLocation, Optional<Painting>> paintings, boolean clear) {
        this.paintings = paintings;
        this.clear = clear;
    }

    public PaintingSyncPayload(ResourceLocation identifier, Painting painting) {
        this.paintings = new HashMap<>();
        this.paintings.put(identifier, Optional.ofNullable(painting));
        this.clear = false;
    }

    public Map<ResourceLocation, Optional<Painting>> paintings() {
        return paintings;
    }

    public boolean clear() {
        return clear;
    }

    /** Break paintings up into smaller batches to stay below the 1.7.10 packet limit. */
    public static List<PaintingSyncPayload> splitPaintings(Map<ResourceLocation, Optional<Painting>> paintings,
        boolean clearFirst) {
        List<PaintingSyncPayload> result = new ArrayList<>();
        List<Map.Entry<ResourceLocation, Optional<Painting>>> entries = new ArrayList<>(paintings.entrySet());
        int interval = Math.max(1, Configs.COMMON.packetSplitInterval);

        for (int start = 0; start < entries.size(); start += interval) {
            Map<ResourceLocation, Optional<Painting>> batch = new HashMap<>();
            for (int i = start; i < Math.min(entries.size(), start + interval); i++) {
                batch.put(
                    entries.get(i)
                        .getKey(),
                    entries.get(i)
                        .getValue());
            }
            result.add(new PaintingSyncPayload(batch, start == 0 && clearFirst));
        }

        if (result.isEmpty()) result.add(new PaintingSyncPayload(new HashMap<>(), clearFirst));
        return result;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        paintings = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation identifier = ImmersivePayload.readIdentifier(buf);
            boolean present = buf.readBoolean();
            Painting painting = present ? Painting.fromNBT(ByteBufUtils.readTag(buf)) : null;
            paintings.put(identifier, Optional.ofNullable(painting));
        }
        clear = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(paintings.size());
        paintings.forEach((identifier, painting) -> {
            ImmersivePayload.writeIdentifier(buf, identifier);
            boolean present = painting.isPresent();
            buf.writeBoolean(present);
            if (present) ByteBufUtils.writeTag(
                buf,
                painting.get()
                    .toNBT());
        });
        buf.writeBoolean(clear);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        final Map<ResourceLocation, Optional<Painting>> entries = paintings;
        final boolean clearFirst = clear;

        runner.run(() -> {
            if (clearFirst) {
                ClientPaintingManager.getPaintings()
                    .clear();
            }

            entries.forEach((identifier, painting) -> {
                if (painting.isPresent()) {
                    ClientPaintingManager.registerPainting(identifier, painting.get());
                } else {
                    ClientPaintingManager.deregisterPainting(identifier);
                }
            });

            ImmersivePaintings.proxy.refreshPaintingScreen();
        });
    }

    public static class Handler implements IMessageHandler<PaintingSyncPayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingSyncPayload message, MessageContext ctx) {
            message.handle(null, ScheduledTasks::client);
            return null;
        }
    }
}
