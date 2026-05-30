package com.mufafa98.better_hud.client;

import java.util.function.Consumer;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
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
        Config config = Config.getInstance();
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(5);

        // Store the updaters so the sliders can push each other
        @SuppressWarnings("unchecked")
        Consumer<Integer>[] updaters = new Consumer[2];

        // Red Warning
        updaters[0] = addSettingRow(grid, 4, "Red Warn %", config.armor.lowDurabilityPercentage, 100, value -> {
            config.armor.lowDurabilityPercentage = value;
            // Constraint: Red cannot be higher than Yellow
            if (value > config.armor.mediumDurabilityPercentage) {
                config.armor.mediumDurabilityPercentage = value;
                if (updaters[1] != null)
                    updaters[1].accept(value); // Push yellow slider up
            }
            config.save();
        });

        // Yellow Warning
        updaters[1] = addSettingRow(grid, 5, "Yellow Warn %", config.armor.mediumDurabilityPercentage, 100, value -> {
            config.armor.mediumDurabilityPercentage = value;
            // Constraint: Yellow cannot be lower than Red
            if (value < config.armor.lowDurabilityPercentage) {
                config.armor.lowDurabilityPercentage = value;
                if (updaters[0] != null)
                    updaters[0].accept(value); // Push red slider down
            }
            config.save();
        });

        addColorSetting(grid, 1, 2, "Red Warning Hex:", config.armor.lowDurabilityColor, color -> {
            config.armor.lowDurabilityColor = color;
            config.save();
        });

        // Place Yellow Hex at Row 3, Column 2 (Right next to Gap)
        addColorSetting(grid, 3, 2, "Yellow Warning Hex:", config.armor.mediumDurabilityColor, color -> {
            config.armor.mediumDurabilityColor = color;
            config.save();
        });

        // Render Vertically Checkbox
        grid.addChild(Checkbox.builder(Component.literal("Render Vertically"), this.minecraft.font)
                .selected(config.armor.renderVertically)
                .onValueChange((checkbox, value) -> {
                    config.armor.renderVertically = value;
                    config.save();
                }).build(), 0, 0, 1, 2);

        // Slider for Center X (0-100)
        addSettingRow(grid, 1, "Center X %", (int) (config.armor.centerX * 100), 100, percent -> {
            config.armor.centerX = percent / 100.0;
            config.save();
        });

        // Center Y
        addSettingRow(grid, 2, "Center Y %", (int) (config.armor.centerY * 100), 100, percent -> {
            config.armor.centerY = percent / 100.0;
            config.save();
        });

        // Gap Controls (Slider + EditBox)
        addSettingRow(grid, 3, "Gap", config.armor.gapBetweenItems, 50, value -> {
            config.armor.gapBetweenItems = value;
            config.save();
        });

        grid.arrangeElements();
        grid.setPosition((this.width - grid.getWidth()) / 2, (this.height - grid.getHeight()) / 2);
        grid.visitWidgets(this::addRenderableWidget);
    }

    private void addColorSetting(GridLayout grid, int startRow, int startCol, String labelText, int currentColor,
            java.util.function.Consumer<Integer> onSave) {
        // 1. Label on Top
        grid.addChild(new StringWidget(Component.literal(labelText), this.minecraft.font), startRow, startCol, 1, 2);
        // 2. Input Box
        EditBox hexBox = new EditBox(this.minecraft.font, 110, 20, Component.literal(labelText));
        hexBox.setValue(Integer.toHexString(currentColor).toUpperCase());
        // 3. Color Preview Block to the right (20x20)
        var preview = new net.minecraft.client.gui.components.AbstractWidget(0, 0, 20, 20, Component.empty()) {
            private int color = currentColor;

            public void updateColor(int newColor) {
                this.color = newColor;
            }

            @Override
            public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX,
                    int mouseY, float delta) {
                // Draw a white border (slightly larger background)
                graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                        this.getY() + this.height + 1, 0xFFFFFFFF);

                // Fill the inner rectangle with the current color
                graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                        this.color);
            }

            @Override
            protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            }
        };

        hexBox.setResponder(val -> {
            try {
                int newColor = (int) Long.parseLong(val.replace("#", ""), 16);
                preview.updateColor(newColor); // Update preview live
                onSave.accept(newColor);
            } catch (NumberFormatException ignored) {
                // Ignore while typing invalid hex
            }
        });

        // Place input and preview on the row beneath the label
        grid.addChild(hexBox, startRow + 1, startCol);
        grid.addChild(preview, startRow + 1, startCol + 1);
    }

    private Consumer<Integer> addSettingRow(GridLayout grid, int row, String name, int currentValue, int maxVal,
            Consumer<Integer> onSave) {
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

        // Add this return statement at the bottom
        return new Consumer<Integer>() {
            @Override
            public void accept(Integer newValue) {
                slider.setSliderValue((double) newValue / maxVal);

                if (!editBox.isFocused()) {
                    editBox.setValue(String.valueOf(newValue));
                }
            }
        };
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}