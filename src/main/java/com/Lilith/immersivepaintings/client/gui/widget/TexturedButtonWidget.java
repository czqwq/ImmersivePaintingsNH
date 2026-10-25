package com.Lilith.immersivepaintings.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Material preview button, draws one half of a 64x32 material atlas. */
@SideOnly(Side.CLIENT)
public class TexturedButtonWidget extends GuiButton {

    private final int textureWidth;
    private final int textureHeight;
    private final ResourceLocation texture;

    public TexturedButtonWidget(int id, int x, int y, int width, int height, ResourceLocation texture, int textureWidth,
        int textureHeight, String message, Runnable onPress) {
        super(id, x, y, width, height, message);
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.onPress = onPress;
    }

    private final Runnable onPress;

    public void press() {
        if (onPress != null) onPress.run();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        mc.getTextureManager()
            .bindTexture(texture);
        org.lwjgl.opengl.GL11.glColor4f(hovered ? 1.0F : 1.0F, hovered ? 0.75F : 1.0F, hovered ? 0.75F : 1.0F, 1.0F);
        Gui.func_152125_a(
            xPosition,
            yPosition,
            0.0F,
            enabled ? 0.0F : (float) (textureHeight / 2),
            textureWidth,
            textureHeight / 2,
            width,
            height,
            textureWidth,
            textureHeight);
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int color = enabled ? 0xFFFFFF : 0xA0A0A0;
        drawCenteredString(mc.fontRenderer, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed) press();
        return pressed;
    }
}
