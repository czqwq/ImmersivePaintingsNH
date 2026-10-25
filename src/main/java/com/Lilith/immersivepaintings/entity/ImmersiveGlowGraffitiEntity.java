package com.Lilith.immersivepaintings.entity;

import net.minecraft.world.World;

import com.Lilith.immersivepaintings.registration.Items;

public class ImmersiveGlowGraffitiEntity extends ImmersivePaintingEntity {

    public ImmersiveGlowGraffitiEntity(World world) {
        super(world);
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    @Override
    public boolean isGraffiti() {
        return true;
    }

    @Override
    public net.minecraft.item.Item getItem() {
        return Items.GLOW_GRAFFITI;
    }
}
