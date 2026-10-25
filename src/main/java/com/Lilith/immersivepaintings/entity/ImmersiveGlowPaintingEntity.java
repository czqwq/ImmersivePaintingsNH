package com.Lilith.immersivepaintings.entity;

import net.minecraft.world.World;

import com.Lilith.immersivepaintings.registration.Items;

public class ImmersiveGlowPaintingEntity extends ImmersivePaintingEntity {

    public ImmersiveGlowPaintingEntity(World world) {
        super(world);
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    @Override
    public net.minecraft.item.Item getItem() {
        return Items.GLOW_PAINTING;
    }
}
