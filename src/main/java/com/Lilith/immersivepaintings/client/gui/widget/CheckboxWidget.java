package com.Lilith.immersivepaintings.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Small replacement for the 1.21 Checkbox widget. */
@SideOnly(Side.CLIENT)
public class CheckboxWidget extends GuiButton {

    private boolean selected;
    private final Consumer<Boolean> onToggle;

    public CheckboxWidget(int id, int x, int y, int width, int height, String label, boolean selected,
        Consumer<Boolean> onToggle) {
        super(id, x, y, width, height, label);
        this.selected = selected;
        this.onToggle = onToggle;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;

        selected = !selected;
        if (onToggle != null) onToggle.accept(selected);
        return true;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int box = 10;
        int boxX = xPosition;
        int boxY = yPosition + (height - box) / 2;
        Gui.drawRect(boxX, boxY, boxX + box, boxY + box, hovered ? 0xFFA0A0A0 : 0xFF808080);
        Gui.drawRect(boxX + 1, boxY + 1, boxX + box - 1, boxY + box - 1, 0xFF000000);
        if (selected) {
            Gui.drawRect(boxX + 2, boxY + 2, boxX + box - 2, boxY + box - 2, 0xFF55FF55);
        }

        drawString(
            mc.fontRenderer,
            displayString,
            xPosition + box + 4,
            yPosition + (height - 8) / 2,
            enabled ? 14737632 : 10526880);
    }
}
