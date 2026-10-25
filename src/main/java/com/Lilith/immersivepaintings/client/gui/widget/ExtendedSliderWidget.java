package com.Lilith.immersivepaintings.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Base slider widget using the vanilla button look, replaces AbstractSliderButton. */
@SideOnly(Side.CLIENT)
public abstract class ExtendedSliderWidget<T> extends GuiButton {

    protected final T min;
    protected final T max;
    protected final String text;
    protected final Consumer<T> onApplyValue;
    protected double sliderValue;
    protected boolean dragging;

    private T oldValue;

    protected ExtendedSliderWidget(int id, int x, int y, int width, int height, String text, double value, T min, T max,
        Consumer<T> onApplyValue) {
        super(id, x, y, width, height, "");
        this.min = min;
        this.max = max;
        this.text = text;
        this.sliderValue = value;
        this.onApplyValue = onApplyValue;
        updateMessage();
    }

    protected abstract T getValue();

    protected abstract void updateMessage();

    protected void applyValue() {
        T value = getValue();
        if (oldValue == null || !oldValue.equals(value)) {
            oldValue = value;
            onApplyValue.accept(value);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!enabled || !visible) return false;
        if (mouseX < xPosition || mouseY < yPosition || mouseX >= xPosition + width || mouseY >= yPosition + height)
            return false;

        dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (dragging) updateFromMouse(mouseX);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    private void updateFromMouse(int mouseX) {
        double ratio = (double) (mouseX - (xPosition + 4)) / (double) (width - 8);
        sliderValue = ratio < 0.0D ? 0.0D : (ratio > 1.0D ? 1.0D : ratio);
        updateMessage();
        applyValue();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        mc.getTextureManager()
            .bindTexture(buttonTextures);
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        int state = hovered ? 2 : 1;
        if (!enabled) state = 0;

        drawTexturedModalRect(xPosition, yPosition, 0, 46 + state * 20, width / 2, height);
        drawTexturedModalRect(xPosition + width / 2, yPosition, 200 - width / 2, 46 + state * 20, width / 2, height);

        int knobX = xPosition + (int) (sliderValue * (width - 8));
        GuiScreen.drawRect(knobX, yPosition + 2, knobX + 8, yPosition + height - 2, 0xFFE0E0E0);
        GuiScreen.drawRect(knobX, yPosition + 2, knobX + 8, yPosition + 3, 0xFF808080);

        drawCenteredString(
            mc.fontRenderer,
            displayString,
            xPosition + width / 2,
            yPosition + (height - 8) / 2,
            14737632);
    }
}
