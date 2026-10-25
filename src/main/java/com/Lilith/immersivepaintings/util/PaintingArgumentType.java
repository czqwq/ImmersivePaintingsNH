package com.Lilith.immersivepaintings.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.ServerPaintingManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Author suggestion helper replacing the 1.21 Brigadier argument type (1.7.10 commands only get
 * plain string arrays plus this completion hook).
 */
public final class PaintingArgumentType {

    private PaintingArgumentType() {}

    @SideOnly(Side.CLIENT)
    public static List<String> clientAuthors() {
        List<String> authors = new ArrayList<>();
        for (Painting painting : com.Lilith.immersivepaintings.ClientPaintingManager.getPaintings()
            .values()) {
            if (!painting.is(Painting.Type.DATAPACK) && !authors.contains(painting.author())) {
                authors.add(painting.author());
            }
        }
        return authors;
    }

    public static List<String> getAuthors(MinecraftServer server) {
        List<String> authors = new ArrayList<>();
        if (server == null) return authors;

        for (Painting painting : ServerPaintingManager.getCustomPaintings(server)
            .values()) {
            if (!authors.contains(painting.author())) authors.add(painting.author());
        }
        return authors;
    }

    public static List<String> getAuthorSuggestions(ICommandSender sender, String[] args) {
        List<String> authors = new ArrayList<>();
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            authors.addAll(getAuthors(server));
        } else {
            authors.addAll(clientAuthors());
        }

        List<String> result = new ArrayList<>();
        String prefix = args.length == 0 ? "" : args[args.length - 1];
        for (String author : authors) {
            if (author.toLowerCase()
                .startsWith(prefix.toLowerCase())) {
                result.add(author);
            }
        }
        return result;
    }
}
