package com.Lilith.immersivepaintings.registration;

import net.minecraft.client.renderer.entity.Render;

import com.Lilith.immersivepaintings.client.render.ImmersivePaintingEntityRenderer;
import com.Lilith.immersivepaintings.entity.ImmersiveGlowGraffitiEntity;
import com.Lilith.immersivepaintings.entity.ImmersiveGlowPaintingEntity;
import com.Lilith.immersivepaintings.entity.ImmersiveGraffitiEntity;
import com.Lilith.immersivepaintings.entity.ImmersivePaintingEntity;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Renderers {

    public static void register() {
        Render renderer = new ImmersivePaintingEntityRenderer();
        RenderingRegistry.registerEntityRenderingHandler(ImmersivePaintingEntity.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(ImmersiveGlowPaintingEntity.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(ImmersiveGraffitiEntity.class, renderer);
        RenderingRegistry.registerEntityRenderingHandler(ImmersiveGlowGraffitiEntity.class, renderer);
    }
}
