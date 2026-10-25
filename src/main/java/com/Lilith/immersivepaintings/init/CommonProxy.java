package com.Lilith.immersivepaintings.init;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.network.LazyNetworkManager;
import com.Lilith.immersivepaintings.registration.Command;
import com.Lilith.immersivepaintings.registration.Entities;
import com.Lilith.immersivepaintings.registration.Items;
import com.Lilith.immersivepaintings.registration.Network;
import com.Lilith.immersivepaintings.registration.Recipes;
import com.Lilith.immersivepaintings.resources.PaintingsLoader;
import com.Lilith.immersivepaintings.util.ScheduledTasks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        Main.init();

        Network.register();
        Items.registerItems((name, item) -> GameRegistry.registerItem(item, name));
        Entities.registerEntities(ImmersivePaintings.instance);
        Recipes.register();

        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        PaintingsLoader.load();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        Command.registerCommands(event);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScheduledTasks.tickServer();
            LazyNetworkManager.tickServer();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ServerPaintingManager.playerLoggedIn((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ServerPaintingManager.playerLoggedOut((EntityPlayerMP) event.player);
        }
    }

    // ------------------------------------------------------------------
    // Client only hooks, overridden by ClientProxy.
    // ------------------------------------------------------------------

    public void openPaintingGui(int entityId, int minResolution, int maxResolution, boolean showOtherPlayersPaintings,
        int uploadPermissionLevel) {}

    public void refreshPaintingScreen() {}

    public void handleRegisterResponse(Optional<ResourceLocation> identifier, String error) {}

    public Optional<Painting> getClientPainting(ResourceLocation identifier) {
        return Optional.empty();
    }

    public void setClientScreenError(String error) {}

    public void setClientScreenPage(String page) {}

    public void registerClientRenderers() {}

    public void loadClientResources() {}

    public boolean isClient() {
        return false;
    }
}
