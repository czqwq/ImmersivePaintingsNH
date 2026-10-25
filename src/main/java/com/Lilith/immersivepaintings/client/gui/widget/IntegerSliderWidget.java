package com.Lilith.immersivepaintings.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class IntegerSliderWidget extends ExtendedSliderWidget<Integer> {

    public IntegerSliderWidget(int id, int x, int y, int width, int height, String text, int value, int min, int max,
        Consumer<Integer> onApplyValue) {
        super(id, x, y, width, height, text, (double) (value - min) / (double) (max - min), min, max, onApplyValue);
    }

    @Override
    protected void updateMessage() {
        displayString = StatCollector.translateToLocalFormatted(text, getValue());
    }

    @Override
    protected Integer getValue() {
        return (int) (sliderValue * (max - min) + min);
    }
}
