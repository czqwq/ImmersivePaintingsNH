package com.Lilith.immersivepaintings.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.Lilith.immersivepaintings.registration.Configs;

/** Maps the 1.21 permission level checks onto 1.7.10 operator checks. */
public final class Permissions {

    private Permissions() {}

    public static boolean has(EntityPlayer player, int level) {
        if (level <= 0) return true;
        if (!(player instanceof EntityPlayerMP)) return false;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;
        return server.getConfigurationManager()
            .func_152596_g(player.getGameProfile());
    }

    public static boolean canUpload(EntityPlayer player) {
        return has(player, Configs.COMMON.uploadPermissionLevel);
    }

    /** 1.7.10 has no adventure-mode API on the player, so read the world game type. */
    public static boolean isAdventure(EntityPlayer player) {
        return player.worldObj.getWorldInfo()
            .getGameType()
            .getID() == 2;
    }
}
