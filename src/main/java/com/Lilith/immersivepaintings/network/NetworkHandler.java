package com.Lilith.immersivepaintings.network;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Sending helpers around the 1.7.10 SimpleNetworkWrapper channel. */
public class NetworkHandler {

    private static SimpleNetworkWrapper network;

    public static void init(SimpleNetworkWrapper wrapper) {
        network = wrapper;
    }

    public static void sendToClient(EntityPlayerMP player, ImmersivePayload payload) {
        if (network == null || player == null) return;
        network.sendTo(payload, player);
    }

    public static void sendToAllClients(MinecraftServer server, ImmersivePayload payload) {
        if (network == null || server == null) return;
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            network.sendTo(payload, player);
        }
    }

    public static SimpleNetworkWrapper getNetwork() {
        return network;
    }

    @SideOnly(Side.CLIENT)
    public static class Client {

        public static void sendToServer(ImmersivePayload payload) {
            if (network == null) return;
            network.sendToServer(payload);
        }
    }
}
