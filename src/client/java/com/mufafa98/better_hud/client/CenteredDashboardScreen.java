package com.mufafa98.better_hud.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CenteredDashboardScreen extends Screen {
    private final Screen parent;

    public CenteredDashboardScreen(Screen parent) {
        super(Component.literal("Better HUD Dashboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ArmorConfig config = ArmorConfig.getInstance();
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(5);

        // Render Vertically Checkbox
        grid.addChild(Checkbox.builder(Component.literal("Render Vertically"), this.minecraft.font)
                .selected(config.renderVertically)
                .onValueChange((checkbox, value) -> {
                    config.renderVertically = value;
                    config.save();
                }).build(), 0, 0, 1, 2);

        // Margin Controls (Slider + EditBox)
        addSettingRow(grid, 1, "Margin", config.margin, 100, value -> {
            config.margin = value;
            config.save();
        });

        // Gap Controls (Slider + EditBox)
        addSettingRow(grid, 2, "Gap", config.gapBetweenItems, 50, value -> {
            config.gapBetweenItems = value;
            config.save();
        });

        grid.arrangeElements();
        grid.setPosition((this.width - grid.getWidth()) / 2, (this.height - grid.getHeight()) / 2);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void addSettingRow(GridLayout grid, int row, String name, int currentValue, int maxVal,
            java.util.function.Consumer<Integer> onSave) {
        EditBox editBox = new EditBox(this.minecraft.font, 40, 20, Component.literal(name));
        editBox.setValue(String.valueOf(currentValue));

        // 1. Use 'var' instead of 'AbstractSliderButton'
        var slider = new AbstractSliderButton(0, 0, 110, 20, Component.literal(name + ": " + currentValue),
                (double) currentValue / maxVal) {

            // 2. Add a custom public setter
            public void setSliderValue(double newValue) {
                this.value = newValue;
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(name + ": " + (int) (this.value * maxVal)));
            }

            @Override
            protected void applyValue() {
                int val = (int) (this.value * maxVal);
                editBox.setValue(String.valueOf(val));
                onSave.accept(val);
            }
        };

        editBox.setResponder(value -> {
            try {
                int val = Integer.parseInt(value);
                val = Math.max(0, Math.min(val, maxVal));
                // 3. Call the custom setter instead of setValue()
                slider.setSliderValue((double) val / maxVal);
                onSave.accept(val);
            } catch (NumberFormatException ignored) {
            }
        });

        grid.addChild(slider, row, 0);
        grid.addChild(editBox, row, 1);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}