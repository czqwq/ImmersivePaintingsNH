package com.Lilith.immersivepaintings.network.payload.s2c;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.init.ImmersivePaintings;
import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PaintingRegisterErrorPayload implements ImmersivePayload {

    private Optional<ResourceLocation> identifier = Optional.empty();
    private String error = "";

    public PaintingRegisterErrorPayload() {}

    public PaintingRegisterErrorPayload(Optional<ResourceLocation> identifier, String error) {
        this.identifier = identifier;
        this.error = error;
    }

    public Optional<ResourceLocation> identifier() {
        return identifier;
    }

    public String error() {
        return error;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        identifier = Optional.ofNullable(ImmersivePayload.readIdentifier(buf));
        error = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ImmersivePayload.writeIdentifier(buf, identifier.orElse(null));
        ByteBufUtils.writeUTF8String(buf, error);
    }

    @Override
    public void handle(EntityPlayer player, Runner runner) {
        final Optional<ResourceLocation> id = identifier;
        final String message = error;
        runner.run(() -> ImmersivePaintings.proxy.handleRegisterResponse(id, message));
    }

    public static class Handler implements IMessageHandler<PaintingRegisterErrorPayload, IMessage> {

        @Override
        public IMessage onMessage(PaintingRegisterErrorPayload message, MessageContext ctx) {
            message.handle(null, ScheduledTasks::client);
            return null;
        }
    }
}
