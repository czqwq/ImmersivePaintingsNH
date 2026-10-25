package com.Lilith.immersivepaintings.network.payload;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Painting;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** 1.7.10 equivalent of the upstream CustomPacketPayload interface. */
public interface ImmersivePayload extends IMessage {

    interface Runner {

        void run(Runnable runnable);
    }

    void handle(EntityPlayer player, Runner runner);

    static void writeIdentifier(ByteBuf buf, ResourceLocation identifier) {
        ByteBufUtils.writeUTF8String(buf, identifier == null ? "" : identifier.toString());
    }

    static ResourceLocation readIdentifier(ByteBuf buf) {
        String value = ByteBufUtils.readUTF8String(buf);
        return value == null || value.isEmpty() ? null : new ResourceLocation(value);
    }

    static void writePainting(ByteBuf buf, Painting painting) {
        boolean present = painting != null;
        buf.writeBoolean(present);
        if (present) ByteBufUtils.writeTag(buf, painting.toNBT());
    }

    static Painting readPainting(ByteBuf buf) {
        if (!buf.readBoolean()) return null;
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        return tag == null ? null : Painting.fromNBT(tag);
    }

    static void writeBytes(ByteBuf buf, byte[] data) {
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    static byte[] readBytes(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length > 1024 * 1024 * 16) return new byte[0];
        byte[] data = new byte[length];
        buf.readBytes(data);
        return data;
    }
}
