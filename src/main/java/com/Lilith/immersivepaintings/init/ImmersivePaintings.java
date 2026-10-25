package com.Lilith.immersivepaintings.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.Lilith.immersivepaintings.Main;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = Main.MOD_ID,
    version = Tags.VERSION,
    name = "Immersive Paintings NH",
    acceptedMinecraftVersions = "[1.7.10]")
public class ImmersivePaintings {

    public static final String MODID = Main.MOD_ID;
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static ImmersivePaintings instance;

    @SidedProxy(
        clientSide = "com.Lilith.immersivepaintings.init.ClientProxy",
        serverSide = "com.Lilith.immersivepaintings.init.CommonProxy")
    public static CommonProxy proxy;

    public ImmersivePaintings() {
        instance = this;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
