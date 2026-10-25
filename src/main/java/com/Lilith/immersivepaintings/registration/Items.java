package com.Lilith.immersivepaintings.registration;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.item.ImmersiveGlowGraffitiItem;
import com.Lilith.immersivepaintings.item.ImmersiveGlowPaintingItem;
import com.Lilith.immersivepaintings.item.ImmersiveGraffitiItem;
import com.Lilith.immersivepaintings.item.ImmersivePaintingItem;

public class Items {

    public static final ImmersivePaintingItem PAINTING = new ImmersivePaintingItem("painting");
    public static final ImmersiveGlowPaintingItem GLOW_PAINTING = new ImmersiveGlowPaintingItem("glow_painting");
    public static final ImmersiveGraffitiItem GRAFFITI = new ImmersiveGraffitiItem("graffiti");
    public static final ImmersiveGlowGraffitiItem GLOW_GRAFFITI = new ImmersiveGlowGraffitiItem("glow_graffiti");

    public static final CreativeTabs PAINTING_TAB;

    static {
        PAINTING_TAB = new CreativeTabs(Main.DOMAIN) {

            @Override
            public Item getTabIconItem() {
                return PAINTING;
            }
        };

        PAINTING.setCreativeTab(PAINTING_TAB);
        GLOW_PAINTING.setCreativeTab(PAINTING_TAB);
        GRAFFITI.setCreativeTab(PAINTING_TAB);
        GLOW_GRAFFITI.setCreativeTab(PAINTING_TAB);
    }

    public static void registerItems(RegisterHelper<Item> helper) {
        helper.register("painting", PAINTING);
        helper.register("glow_painting", GLOW_PAINTING);
        helper.register("graffiti", GRAFFITI);
        helper.register("glow_graffiti", GLOW_GRAFFITI);
    }
}
