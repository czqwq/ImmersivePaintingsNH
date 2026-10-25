package com.Lilith.immersivepaintings.mixins;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
@SuppressWarnings("unused")
public class LateMixinPlugin implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.ImmersivePaintingsNH.late.json";
    }

    @Override
    public @NotNull List<String> getMixins(Set<String> loadedMods) {
        return Mixins.getLateMixins(loadedMods);
    }
}
