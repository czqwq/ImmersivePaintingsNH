package com.Lilith.immersivepaintings.registration;

import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.c2s.ImageRequestPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.ImageUploadPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingDeletePayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingEditPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingFlagPayload;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingRegisterPayload;
import com.Lilith.immersivepaintings.network.payload.s2c.ImageResponsePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.OpenGuiPayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingRegisterErrorPayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/** Registers all 1.7.10 SimpleNetworkWrapper messages. */
public class Network {

    /** SimpleNetworkWrapper channel name is limited to 20 bytes by the vanilla payload packet. */
    private static final String CHANNEL = "ImmersivePaint";

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);

    public static void register() {
        NetworkHandler.init(INSTANCE);

        int id = 0;
        INSTANCE.registerMessage(ImageRequestPayload.Handler.class, ImageRequestPayload.class, id++, Side.SERVER);
        INSTANCE.registerMessage(ImageUploadPayload.Handler.class, ImageUploadPayload.class, id++, Side.SERVER);
        INSTANCE
            .registerMessage(PaintingRegisterPayload.Handler.class, PaintingRegisterPayload.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PaintingEditPayload.Handler.class, PaintingEditPayload.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PaintingFlagPayload.Handler.class, PaintingFlagPayload.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PaintingDeletePayload.Handler.class, PaintingDeletePayload.class, id++, Side.SERVER);

        INSTANCE.registerMessage(ImageResponsePayload.Handler.class, ImageResponsePayload.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(OpenGuiPayload.Handler.class, OpenGuiPayload.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PaintingSyncPayload.Handler.class, PaintingSyncPayload.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(
            PaintingRegisterErrorPayload.Handler.class,
            PaintingRegisterErrorPayload.class,
            id++,
            Side.CLIENT);
    }
}
