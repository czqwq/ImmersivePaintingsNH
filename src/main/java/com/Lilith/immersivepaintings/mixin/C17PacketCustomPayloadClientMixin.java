package com.Lilith.immersivepaintings.mixin;

import net.minecraft.network.play.client.C17PacketCustomPayload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Client only part of the payload limit fix: the {@code (String, byte[])} constructor of
 * {@link C17PacketCustomPayload} is {@code @SideOnly(CLIENT)} and throws above 32k. Raise its
 * limit to the same ~2MB bound Forge uses for the server to client direction.
 */
@Mixin(C17PacketCustomPayload.class)
public class C17PacketCustomPayloadClientMixin {

    /** Same bound as S3FPacketCustomPayload, keeps the whole packet inside the 21 bit netty frame. */
    private static final int MAX_PAYLOAD = 0x1FFF9A;

    /** Replaces the vanilla 32k constant with the Forge bound. */
    @ModifyConstant(method = "<init>(Ljava/lang/String;[B)V", constant = @Constant(intValue = 32767), require = 0)
    private int immersivePaintings$maxPayload(int original) {
        return MAX_PAYLOAD;
    }
}
