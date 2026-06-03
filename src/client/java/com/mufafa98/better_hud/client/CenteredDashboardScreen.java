package com.mufafa98.better_hud.client;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CenteredDashboardScreen extends Screen {
    private static final int CONTROL_HEIGHT = 20;
    private static final int SLIDER_WIDTH = 110;
    private static final int VALUE_BOX_WIDTH = 40;
    private static final int HEX_BOX_WIDTH = 110;
    private static final int COLOR_PREVIEW_SIZE = 20;

    private final Screen parent;
    private ScrollableLayout layout;

    public CenteredDashboardScreen(Screen parent) {
        super(Component.literal("Better HUD Dashboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Config config = Config.getInstance();

        GridLayout grid = buildGrid(config);

        int maxHeight = (int) (this.height * 0.8);
        layout = new ScrollableLayout(Minecraft.getInstance(), grid, maxHeight);
        layout.arrangeElements();

        int layoutHeight = Math.min(grid.getHeight(), maxHeight);
        layout.setPosition((this.width - grid.getWidth()) / 2, (this.height - layoutHeight) / 2);
        layout.visitWidgets(this::addRenderableWidget);
    }

    private GridLayout buildGrid(Config config) {
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(5);

        RowBuilder rows = new RowBuilder(grid);

        IntConsumer[] updaters = new IntConsumer[2];

        rows.checkbox("Render Vertically", config.armor.renderVertically, value -> {
            config.armor.renderVertically = value;
            config.save();
        });

        rows.slider("Center X %", (int) (config.armor.centerX * 100), 100, percent -> {
            config.armor.centerX = percent / 100.0;
            config.save();
        });

        rows.slider("Center Y %", (int) (config.armor.centerY * 100), 100, percent -> {
            config.armor.centerY = percent / 100.0;
            config.save();
        });

        rows.slider("Gap", config.armor.gapBetweenItems, 50, value -> {
            config.armor.gapBetweenItems = value;
            config.save();
        });

        updaters[0] = rows.slider("Red Warn %", config.armor.lowDurabilityPercentage, 100, value -> {
            config.armor.lowDurabilityPercentage = value;
            if (value > config.armor.mediumDurabilityPercentage) {
                config.armor.mediumDurabilityPercentage = value;
                if (updaters[1] != null)
                    updaters[1].accept(value);
            }
            config.save();
        });

        rows.color("Red Warning Hex:", config.armor.lowDurabilityColor, color -> {
            config.armor.lowDurabilityColor = color;
            config.save();
        });

        updaters[1] = rows.slider("Yellow Warn %", config.armor.mediumDurabilityPercentage, 100, value -> {
            config.armor.mediumDurabilityPercentage = value;
            if (value < config.armor.lowDurabilityPercentage) {
                config.armor.lowDurabilityPercentage = value;
                if (updaters[0] != null)
                    updaters[0].accept(value);
            }
            config.save();
        });

        rows.color("Yellow Warning Hex:", config.armor.mediumDurabilityColor, color -> {
            config.armor.mediumDurabilityColor = color;
            config.save();
        });

        return grid;
    }

    private final class RowBuilder {
        private final GridLayout grid;
        private final Minecraft mc = Minecraft.getInstance();
        private int row = 0;

        private RowBuilder(GridLayout grid) {
            this.grid = grid;
        }

        private IntConsumer slider(String name, int currentValue, int maxVal, IntConsumer onSave) {
            EditBox editBox = new EditBox(mc.font, VALUE_BOX_WIDTH, CONTROL_HEIGHT, Component.literal(name));
            editBox.setValue(String.valueOf(currentValue));

            var slider = new AbstractSliderButton(0, 0, SLIDER_WIDTH, CONTROL_HEIGHT,
                    Component.literal(name + ": " + currentValue),
                    (double) currentValue / maxVal) {

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
                    int val = clamp(Integer.parseInt(value), 0, maxVal);
                    slider.setSliderValue((double) val / maxVal);
                    onSave.accept(val);
                } catch (NumberFormatException ignored) {
                }
            });

            grid.addChild(slider, row, 0);
            grid.addChild(editBox, row, 1);
            row++;

            return newValue -> {
                slider.setSliderValue((double) newValue / maxVal);

                if (!editBox.isFocused()) {
                    editBox.setValue(String.valueOf(newValue));
                }
            };
        }

        private void color(String labelText, int currentColor, Consumer<Integer> onSave) {
            this.grid.addChild(new StringWidget(Component.literal(labelText), mc.font), row, 0, 1, 2);

            EditBox hexBox = new EditBox(mc.font, HEX_BOX_WIDTH, CONTROL_HEIGHT, Component.literal(labelText));
            hexBox.setValue(Integer.toHexString(currentColor).toUpperCase());

            var preview = new net.minecraft.client.gui.components.AbstractWidget(0, 0, COLOR_PREVIEW_SIZE,
                    COLOR_PREVIEW_SIZE, Component.empty()) {
                private int color = currentColor;

                public void updateColor(int newColor) {
                    this.color = newColor;
                }

                @Override
                public void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX,
                        int mouseY, float delta) {
                    graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                            this.getY() + this.height + 1, 0xFFFFFFFF);

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
                    preview.updateColor(newColor);
                    onSave.accept(newColor);
                } catch (NumberFormatException ignored) {
                }
            });

            grid.addChild(hexBox, row + 1, 0);
            grid.addChild(preview, row + 1, 1);

            this.row += 2;
        }

        private void checkbox(String label, boolean selected, Consumer<Boolean> onSave) {
            this.grid.addChild(Checkbox.builder(Component.literal(label), mc.font)
                    .selected(selected)
                    .onValueChange((checkbox, value) -> onSave.accept(value))
                    .build(), this.row++, 0, 1, 2);
        }

        private void cycle(String label, int[] values, int currentValue, IntConsumer onSave) {
            final int[] index = { 0 };
            for (int i = 0; i < values.length; i++) {
                if (values[i] == currentValue) {
                    index[0] = i;
                    break;
                }
            }

            Button button = Button.builder(
                    Component.literal(label + ": " + values[index[0]]),
                    btn -> {
                        index[0] = (index[0] + 1) % values.length;
                        int newValue = values[index[0]];
                        btn.setMessage(Component.literal(label + ": " + newValue));
                        onSave.accept(newValue);
                    }).build();

            grid.addChild(button, row++, 0, 1, 2);
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(value, max));
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }
}