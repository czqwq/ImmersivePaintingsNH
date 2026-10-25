package com.Lilith.immersivepaintings.client.gui.widget;

import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.Lilith.immersivepaintings.Painting;
import com.Lilith.immersivepaintings.registration.Configs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Painting thumbnail button; left and right click have different callbacks. */
@SideOnly(Side.CLIENT)
public class PaintingWidget extends GuiButton {

    private BufferedImage image;
    private ResourceLocation identifier = Painting.DEFAULT_IDENTIFIER;

    private int paintingWidth = 256;
    private int paintingHeight = 256;

    private final Runnable onPressLeft;
    private final Runnable onPressRight;

    public PaintingWidget(int id, int x, int y, int width, int height, Runnable onPressLeft, Runnable onPressRight) {
        super(id, x, y, width, height, "");
        this.onPressLeft = onPressLeft;
        this.onPressRight = onPressRight;
    }

    public BufferedImage getImage() {
        return image;
    }

    /** Used for screenshots where the raw image has to be kept for upload. */
    public void update(ResourceLocation identifier, BufferedImage image) {
        this.image = image;
        update(identifier, 4, 2);
    }

    public void update(ResourceLocation identifier, int paintingWidth, int paintingHeight) {
        int thumbnailSize = Configs.CLIENT.thumbnailSize;
        this.identifier = identifier;
        this.paintingWidth = paintingWidth * thumbnailSize;
        this.paintingHeight = paintingHeight * thumbnailSize;
    }

    public void pressRight() {
        if (onPressRight != null) onPressRight.run();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed && onPressLeft != null) onPressLeft.run();
        return pressed;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        float scale = Math.min((float) width / paintingWidth, (float) height / paintingHeight);
        if (hovered) scale *= 1.1F;

        GL11.glPushMatrix();
        GL11.glTranslatef(
            xPosition + (width - paintingWidth * scale) / 2.0F,
            yPosition + (height - paintingHeight * scale) / 2.0F,
            0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        mc.getTextureManager()
            .bindTexture(identifier);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Gui.func_152125_a(
            0,
            0,
            0.0F,
            0.0F,
            paintingWidth,
            paintingHeight,
            paintingWidth,
            paintingHeight,
            paintingWidth,
            paintingHeight);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
