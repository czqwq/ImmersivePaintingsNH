package com.Lilith.immersivepaintings.util;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import com.Lilith.immersivepaintings.registration.Configs;

/** Maps the 1.21 permission level checks onto 1.7.10 operator checks. */
public final class Permissions {

    /** Operator level, used by the admin painting item in place of a configurable permission level. */
    public static final int ADMIN_LEVEL = 2;

    private static final String DENIED_ADMIN_ONLY = "immersivepaintingsnh.error.admin_only";
    /** Chat spam guard: one denial message per player per second, attacking can be spammed every tick. */
    private static final int DENIAL_NOTICE_COOLDOWN_TICKS = 20;
    /** Weak keys, so the entries die together with the player instance on logout. */
    private static final Map<EntityPlayer, Long> LAST_DENIED_TICK = new WeakHashMap<>();

    private Permissions() {}

    public static boolean has(EntityPlayer player, int level) {
        if (level <= 0) return true;
        if (!(player instanceof EntityPlayerMP)) return false;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;
        return server.getConfigurationManager()
            .func_152596_g(player.getGameProfile());
    }

    /** True for server operators (level {@link #ADMIN_LEVEL}); false for fake/clientside players. */
    public static boolean isAdmin(EntityPlayer player) {
        return has(player, ADMIN_LEVEL);
    }

    /**
     * Tells the player, in chat, that only operators may touch admin paintings. A {@link ChatComponentTranslation}
     * carries just the translation key over the wire, so the client renders the text in its own language.
     */
    public static void sendDenied(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;

        long now = player.worldObj.getTotalWorldTime();
        Long last = LAST_DENIED_TICK.get(player);
        if (last != null && now - last < DENIAL_NOTICE_COOLDOWN_TICKS) return;
        LAST_DENIED_TICK.put(player, now);

        ((EntityPlayerMP) player).addChatComponentMessage(
            new ChatComponentTranslation(DENIED_ADMIN_ONLY)
                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
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
