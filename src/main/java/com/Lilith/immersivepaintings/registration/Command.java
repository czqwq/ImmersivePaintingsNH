package com.Lilith.immersivepaintings.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;
import com.Lilith.immersivepaintings.network.NetworkHandler;
import com.Lilith.immersivepaintings.network.payload.c2s.PaintingDeletePayload;
import com.Lilith.immersivepaintings.network.payload.s2c.PaintingSyncPayload;

import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Admin command, replacing the 1.21 Brigadier command. */
public class Command extends CommandBase {

    public static void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new Command());
    }

    @Override
    public String getCommandName() {
        return Main.DOMAIN;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + Main.DOMAIN + " delete <author>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2 || !"delete".equals(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String author = args[1];
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) throw new CommandException("immersivepaintingsnh.command.error.no_server");

        Optional<Map.Entry<ResourceLocation, Painting>> painting = ServerPaintingManager.getCustomPaintings(server)
            .entrySet()
            .stream()
            .filter(
                entry -> entry.getValue()
                    .author()
                    .equals(author))
            .findFirst();

        if (!painting.isPresent()) {
            throw new CommandException("immersivepaintingsnh.command.error.no_player", author);
        }

        if (sender instanceof EntityPlayerMP) {
            new PaintingDeletePayload(
                painting.get()
                    .getKey(),
                true).handle((EntityPlayerMP) sender, Runnable::run);
        } else {
            deleteAllPaintingsOfAuthor(
                server,
                painting.get()
                    .getValue()
                    .authorUUID());
        }

        func_152373_a(sender, this, "immersivepaintingsnh.command.success.delete", author);
    }

    private static void deleteAllPaintingsOfAuthor(MinecraftServer server, java.util.UUID authorUUID) {
        Map<ResourceLocation, Painting> paintings = ServerPaintingManager.getCustomPaintings(server);
        List<ResourceLocation> toDelete = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Painting> entry : paintings.entrySet()) {
            if (entry.getValue()
                .authorUUID()
                .equals(authorUUID)) {
                toDelete.add(entry.getKey());
            }
        }

        Map<ResourceLocation, Optional<Painting>> payload = new HashMap<>();
        for (ResourceLocation identifier : toDelete) {
            ServerPaintingManager.deregisterPainting(server, identifier);
            payload.put(identifier, Optional.empty());
        }

        if (!payload.isEmpty()) {
            NetworkHandler.sendToAllClients(server, new PaintingSyncPayload(payload, false));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "delete");
        }

        if (args.length == 2 && "delete".equals(args[0])) {
            List<String> authors = com.Lilith.immersivepaintings.util.PaintingArgumentType
                .getAuthors(MinecraftServer.getServer());
            return getListOfStringsMatchingLastWord(args, authors.toArray(new String[0]));
        }

        return new ArrayList<String>();
    }
}
