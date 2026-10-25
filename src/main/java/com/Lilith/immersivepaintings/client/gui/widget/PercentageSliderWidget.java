package com.Lilith.immersivepaintings.client.gui.widget;

import java.util.function.Consumer;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PercentageSliderWidget extends ExtendedSliderWidget<Double> {

    public PercentageSliderWidget(int id, int x, int y, int width, int height, String text, double value,
        Consumer<Double> onApplyValue) {
        this(id, x, y, width, height, text, value, 0.0D, 1.0D, onApplyValue);
    }

    public PercentageSliderWidget(int id, int x, int y, int width, int height, String text, double value, double min,
        double max, Consumer<Double> onApplyValue) {
        super(id, x, y, width, height, text, (value - min) / (max - min), min, max, onApplyValue);
    }

    @Override
    protected void updateMessage() {
        displayString = StatCollector.translateToLocalFormatted(text, (int) (getValue() * 100.0D));
    }

    @Override
    protected Double getValue() {
        return (double) (int) ((sliderValue * (max - min) + min) * 100.0D + 0.5D) / 100.0D;
    }
}
