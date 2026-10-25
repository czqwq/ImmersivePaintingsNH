package com.Lilith.immersivepaintings.mixin;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * PacketFixer style fix for the 1.7.10 client to server payload limit.
 *
 * <p>
 * The vanilla {@link C17PacketCustomPayload} caps the payload at 32k. Forge already patched the
 * server to client packet ({@code S3FPacketCustomPayload}) to use a var-short length with a ~2MB
 * limit. This mixin applies the same encoding to the client to server direction. Lengths below
 * 32768 encode byte-identically to the vanilla short, so unpatched peers and other mods are not
 * affected.
 */
@Mixin(C17PacketCustomPayload.class)
public abstract class C17PacketCustomPayloadMixin {

    @Shadow
    private String field_149562_a;

    @Shadow
    private int field_149560_b;

    @Shadow
    private byte[] field_149561_c;

    /** Reads the Forge var-short length, compatible with the vanilla short encoding below 32k. */
    @Overwrite
    public void readPacketData(PacketBuffer data) throws IOException {
        this.field_149562_a = data.readStringFromBuffer(20);
        this.field_149560_b = ByteBufUtils.readVarShort(data);

        if (this.field_149560_b > 0) {
            this.field_149561_c = new byte[this.field_149560_b];
            data.readBytes(this.field_149561_c);
        }
    }

    /** Writes the Forge var-short length, compatible with the vanilla short encoding below 32k. */
    @Overwrite
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(this.field_149562_a);
        ByteBufUtils.writeVarShort(data, this.field_149560_b);

        if (this.field_149561_c != null) {
            data.writeBytes(this.field_149561_c);
        }
    }
}
