package com.Lilith.immersivepaintings;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * Immutable painting metadata. Mirrors the upstream 1.21 record, with NBT based (de)serialization
 * replacing the Mojang codecs.
 */
public record Painting(int version, int width, int height, int resolution, String name, String author, UUID authorUUID,
    Type type, EnumSet<Flag> flags, String hash) {

    public static final ResourceLocation DEFAULT_IDENTIFIER = Main.locate("textures/block/frame/canvas.png");

    public Painting(int width, int height, int resolution, String name, String author, UUID authorUUID, Type type,
        EnumSet<Flag> flags, String hash) {
        this(1, width, height, resolution, name, author, authorUUID, type, flags, hash);
    }

    public ResourceLocation location() {
        switch (type) {
            case DATAPACK:
                return Main.locate("datapack/" + hash);
            case XERCA:
                return Main.locate("xerca/" + hash);
            default:
                return Main.locate(authorUUID.toString() + "/" + hash);
        }
    }

    public boolean is(Type t) {
        return type() == t;
    }

    public boolean has(Flag flag) {
        return flags().contains(flag);
    }

    public Painting withHash(String newHash) {
        return new Painting(width, height, resolution, name, author, authorUUID, type, flags, newHash);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("version", version);
        nbt.setInteger("width", width);
        nbt.setInteger("height", height);
        nbt.setInteger("resolution", resolution);
        nbt.setString("name", name);
        nbt.setString("author", author);
        nbt.setString("authorUUID", authorUUID.toString());
        nbt.setString("type", type.name());
        nbt.setLong("flags", Flag.toBits(flags));
        nbt.setString("hash", hash);
        return nbt;
    }

    public static Painting fromNBT(NBTTagCompound nbt) {
        UUID uuid;
        try {
            uuid = UUID.fromString(nbt.getString("authorUUID"));
        } catch (IllegalArgumentException e) {
            uuid = new UUID(0L, 0L);
        }

        Type type;
        try {
            type = Type.valueOf(nbt.getString("type"));
        } catch (IllegalArgumentException e) {
            type = Type.PAINTING;
        }

        return new Painting(
            nbt.getInteger("version"),
            nbt.getInteger("width"),
            nbt.getInteger("height"),
            nbt.getInteger("resolution"),
            nbt.getString("name"),
            nbt.getString("author"),
            uuid,
            type,
            Flag.fromBits(nbt.getLong("flags")),
            nbt.getString("hash"));
    }

    public enum Type {
        DATAPACK,
        XERCA,
        PAINTING
    }

    public enum Size {
        FULL,
        HALF,
        QUARTER,
        EIGHTH,
        THUMBNAIL,
        NSFW
    }

    public enum Flag {

        HIDDEN(1),
        NSFW(2),
        GRAFFITI(4);

        private final long bits;

        Flag(long bits) {
            this.bits = bits;
        }

        public static long toBits(EnumSet<Flag> flags) {
            long result = 0L;
            for (Flag flag : flags) {
                result |= flag.bits;
            }
            return result;
        }

        public static EnumSet<Flag> fromBits(long bits) {
            EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
            for (Flag flag : values()) {
                if ((bits & flag.bits) != 0L) {
                    flags.add(flag);
                }
            }
            return flags;
        }
    }
}
