package com.Lilith.immersivepaintings.item;

import com.Lilith.immersivepaintings.entity.ImmersiveGlowGraffitiEntity;

public class ImmersiveGlowGraffitiItem extends ImmersivePaintingItem {

    public ImmersiveGlowGraffitiItem(String name) {
        super(name);
    }

    @Override
    protected Class<? extends com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity> getEntityClass() {
        return ImmersiveGlowGraffitiEntity.class;
    }
}
