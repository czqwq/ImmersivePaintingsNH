package com.Lilith.immersivepaintings.network;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.Lilith.immersivepaintings.network.payload.ImmersivePayload;
import com.Lilith.immersivepaintings.registration.Configs;

/** Rate limited packet queue, ticked from the Forge tick events. */
public class LazyNetworkManager {

    private static final List<LazyPacket> serverQueue = new LinkedList<>();
    private static final List<LazyPacket> clientQueue = new LinkedList<>();

    private static double cooldownClient = 0.0;
    private static double cooldownServer = 0.0;

    public static void sendToServer(ImmersivePayload payload) {
        synchronized (serverQueue) {
            serverQueue.add(new LazyPacket(payload, null));
        }
    }

    public static void sendToClient(ImmersivePayload payload, EntityPlayerMP player) {
        synchronized (clientQueue) {
            clientQueue.add(new LazyPacket(payload, player));
        }
    }

    public static void tickClient() {
        cooldownClient = Math.max(cooldownClient - 1.0, 0.0);
        while (true) {
            LazyPacket packet;
            synchronized (serverQueue) {
                if (serverQueue.isEmpty() || cooldownClient >= 1.0) return;
                packet = serverQueue.remove(0);
            }
            NetworkHandler.Client.sendToServer(packet.payload);
            cooldownClient += 20.0 / Configs.COMMON.maxPacketsPerSecond;
        }
    }

    public static void tickServer() {
        cooldownServer = Math.max(cooldownServer - 1.0, 0.0);
        while (true) {
            LazyPacket packet;
            synchronized (clientQueue) {
                if (clientQueue.isEmpty() || cooldownServer >= 1.0) return;
                packet = clientQueue.remove(0);
            }
            NetworkHandler.sendToClient(packet.player, packet.payload);
            cooldownServer += 20.0 / Configs.COMMON.maxPacketsPerSecond;
        }
    }

    public static float getRemainingTime() {
        synchronized (serverQueue) {
            return (float) serverQueue.size() / Configs.COMMON.maxPacketsPerSecond;
        }
    }

    private static final class LazyPacket {

        final ImmersivePayload payload;
        final EntityPlayerMP player;

        LazyPacket(ImmersivePayload payload, EntityPlayerMP player) {
            this.payload = payload;
            this.player = player;
        }
    }
}
