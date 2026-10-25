package com.Lilith.immersivepaintings.init;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.Lilith.immersivepaintings.ClientPaintingManager;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.client.gui.ImmersivePaintingScreen;
import com.Lilith.immersivepaintings.network.LazyNetworkManager;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingEditPayload;
import com.Lilith.immersivepaintings.registration.Renderers;
import com.Lilith.immersivepaintings.resources.FrameLoader;
import com.Lilith.immersivepaintings.resources.ObjectLoader;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        registerClientRenderers();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        loadClientResources();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScheduledTasks.tickClient();
            LazyNetworkManager.tickClient();
        }
    }

    @Override
    public void registerClientRenderers() {
        Renderers.register();
    }

    @Override
    public void loadClientResources() {
        FrameLoader.load();
        ObjectLoader.load();
    }

    @Override
    public void openPaintingGui(int entityId, int minResolution, int maxResolution, boolean showOtherPlayersPaintings,
        int uploadPermissionLevel) {
        Minecraft.getMinecraft()
            .displayGuiScreen(
                new ImmersivePaintingScreen(
                    entityId,
                    minResolution,
                    maxResolution,
                    showOtherPlayersPaintings,
                    uploadPermissionLevel));
    }

    @Override
    public void refreshPaintingScreen() {
        if (Minecraft.getMinecraft().currentScreen instanceof ImmersivePaintingScreen) {
            ((ImmersivePaintingScreen) Minecraft.getMinecraft().currentScreen).refreshPage();
        }
    }

    @Override
    public void handleRegisterResponse(Optional<ResourceLocation> identifier, String error) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof ImmersivePaintingScreen)) return;
        ImmersivePaintingScreen screen = (ImmersivePaintingScreen) Minecraft.getMinecraft().currentScreen;

        if (error == null || error.isEmpty()) {
            if (screen.entity != null && identifier.isPresent()) {
                Map<PaintingEditPayload.Option, String> options = new HashMap<>();
                options.put(
                    PaintingEditPayload.Option.MOTIVE,
                    identifier.get()
                        .toString());
                options.put(
                    PaintingEditPayload.Option.FRAME,
                    screen.entity.getFrame()
                        .toString());
                options.put(
                    PaintingEditPayload.Option.MATERIAL,
                    screen.entity.getMaterial()
                        .toString());
                NetworkHandler.Client.sendToServer(new PaintingEditPayload(screen.entity.getEntityId(), options));

                if (screen.entity.isGraffiti()) {
                    Minecraft.getMinecraft()
                        .displayGuiScreen(null);
                } else {
                    screen.setPage(ImmersivePaintingScreen.Page.FRAME);
                }
            }
        } else {
            screen.setPage(ImmersivePaintingScreen.Page.CREATE);
            screen.setError(StatCollector.translateToLocal("immersivepaintingsnh.error." + error));
        }
    }

    @Override
    public Optional<Painting> getClientPainting(ResourceLocation identifier) {
        return ClientPaintingManager.getPainting(identifier);
    }

    @Override
    public void setClientScreenError(String error) {
        if (Minecraft.getMinecraft().currentScreen instanceof ImmersivePaintingScreen) {
            ((ImmersivePaintingScreen) Minecraft.getMinecraft().currentScreen).setError(error);
        }
    }

    @Override
    public void setClientScreenPage(String page) {
        if (Minecraft.getMinecraft().currentScreen instanceof ImmersivePaintingScreen) {
            ((ImmersivePaintingScreen) Minecraft.getMinecraft().currentScreen)
                .setPage(ImmersivePaintingScreen.Page.valueOf(page));
        }
    }

    @Override
    public boolean isClient() {
        return true;
    }
}
