package com.Lilith.immersivepaintings.registration;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

/** 1.7.10 replacement for the upstream recipe json files. */
public class Recipes {

    public static void register() {
        // painting: paper ring with a dye in the middle.
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(Items.PAINTING),
                "PPP",
                "PDP",
                "PPP",
                'P',
                new ItemStack(net.minecraft.init.Items.paper),
                'D',
                "dye"));

        // graffiti: dye ring around gunpowder and a bucket.
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(Items.GRAFFITI, 8),
                "DDD",
                "DGD",
                "DBD",
                'D',
                "dye",
                'G',
                new ItemStack(net.minecraft.init.Items.gunpowder),
                'B',
                new ItemStack(net.minecraft.init.Items.bucket)));

        // glowing variants: 1.7.10 has no glow ink sac, glowstone dust is the closest equivalent.
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                new ItemStack(Items.GLOW_PAINTING),
                new ItemStack(Items.PAINTING),
                new ItemStack(net.minecraft.init.Items.glowstone_dust)));
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                new ItemStack(Items.GLOW_GRAFFITI),
                new ItemStack(Items.GRAFFITI),
                new ItemStack(net.minecraft.init.Items.glowstone_dust)));
    }
}
