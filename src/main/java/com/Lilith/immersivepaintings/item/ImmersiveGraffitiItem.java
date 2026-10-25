package com.Lilith.immersivepaintings.item;

import com.Lilith.immersivepaintings.entity.ImmersiveGraffitiEntity;

public class ImmersiveGraffitiItem extends ImmersivePaintingItem {

    public ImmersiveGraffitiItem(String name) {
        super(name);
    }

    @Override
    protected Class<? extends com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity> getEntityClass() {
        return ImmersiveGraffitiEntity.class;
    }
}
