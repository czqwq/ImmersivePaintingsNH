package com.Lilith.immersivepaintings.network.payload.s2c;

import net.minecraft.entity.player.EntityPlayer;

import com.Lilith.immersivepaintings.init.ImmersivePaintings;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class OpenGuiPayload implements ImmersivePayload {

    private GuiType guiType = GuiType.EDITOR;
    private int entityId;
    private int minResolution;
    private int maxResolution;
    private boolean showOtherPlayersPaintings;
    private int uploadPermissionLevel;

    public OpenGuiPayload() {}

    public OpenGuiPayload(GuiType guiType, int entityId, int minResolution, int maxResolution,
        boolean showOtherPlayersPaintings, int uploadPermissionLevel) {
        this.guiType = guiType;
        this.entityId = entityId;
        this.minResolution = minResolution;
        this.maxResolution = maxResolution;
        this.showOtherPlayersPaintings = showOtherPlayersPaintings;
        this.uploadPermissionLevel = uploadPermissionLevel;
    }

    public GuiType guiType() {
        return guiType;
    }

    public int entityId() {
        return entityId;
    }

    public int minResolution() {
        return minResolution;
    }

    public int maxResolution() {
        return maxResolution;
    }

    public boolean showOtherPlayersPaintings() {
        return showOtherPlayersPaintings;
    }

    public int uploadPermissionLevel() {
        return uploadPermissionLevel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        guiType = GuiType.values()[buf.readByte()];
        entityId = buf.readInt();
        minResolution = buf.readInt();
        maxResolution = buf.readInt();
        showOtherPlayersPaintings = buf.readBoolean();
        uploadPermissionLevel = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(guiType.ordinal());
        buf.writeInt(entityId);
        buf.writeInt(minResolution);
        buf.writeInt(maxResolution);
        buf.writeBoolean(showOtherPlayersPaintings);
        buf.writeInt(uploadPermissionLevel);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        final GuiType type = guiType;
        final int id = entityId;
        final int min = minResolution;
        final int max = maxResolution;
        final boolean showOthers = showOtherPlayersPaintings;
        final int permissionLevel = uploadPermissionLevel;

        runner.run(() -> {
            if (type == GuiType.EDITOR) {
                ImmersivePaintings.proxy.openPaintingGui(id, min, max, showOthers, permissionLevel);
            }
        });
    }

    public enum GuiType {
        EDITOR
    }

    public static class Handler implements IMessageHandler<OpenGuiPayload, IMessage> {

        @Override
        public IMessage onMessage(OpenGuiPayload message, MessageContext ctx) {
            message.handle(null, ScheduledTasks::client);
            return null;
        }
    }
}
