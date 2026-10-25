package com.Lilith.immersivepaintings.entity;

import net.minecraft.world.World;

import com.Lilith.immersivepaintings.registration.Items;

public class ImmersiveGraffitiEntity extends ImmersivePaintingEntity {

    public ImmersiveGraffitiEntity(World world) {
        super(world);
    }

    @Override
    public boolean isGraffiti() {
        return true;
    }

    @Override
    public net.minecraft.item.Item getItem() {
        return Items.GRAFFITI;
    }
}
