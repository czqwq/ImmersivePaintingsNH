package com.Lilith.immersivepaintings.item;

import com.Lilith.immersivepaintings.entity.ImmersiveGlowPaintingEntity;

public class ImmersiveGlowPaintingItem extends ImmersivePaintingItem {

    public ImmersiveGlowPaintingItem(String name) {
        super(name);
    }

    @Override
    protected Class<? extends com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity> getEntityClass() {
        return ImmersiveGlowPaintingEntity.class;
    }
}
