package com.mufafa98.mud.client;

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
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CenteredDashboardScreen extends Screen {
    private static final int CONTROL_HEIGHT = 20;
    private static final int COL1_WIDTH = 110;
    private static final int COL2_WIDTH = 40;
    private static final int COL_PADDING = 5;

    private final Screen parent;
    private ScrollableLayout layout;

    public CenteredDashboardScreen(Screen parent) {
        super(Component.literal("MUD Dashboard"));
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

        ArmorConfig.LayoutOrientation currentLayout = config.armor.layout != null
                ? config.armor.layout
                : ArmorConfig.LayoutOrientation.VERTICAL;

        rows.cycle("Layout", new String[] { "VERTICAL", "HORIZONTAL" }, currentLayout.name(), value -> {
            config.armor.layout = ArmorConfig.LayoutOrientation.valueOf(value);
            config.save();
        });

        ArmorConfig.TextPosition currentTextPosition = config.armor.textPosition != null
                ? config.armor.textPosition
                : ArmorConfig.TextPosition.RIGHT;

        rows.cycle("Text Position", new String[] { "LEFT", "RIGHT", "TOP", "BOTTOM" }, currentTextPosition.name(),
                value -> {
                    config.armor.textPosition = ArmorConfig.TextPosition.valueOf(value);
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
            LinearLayout rowLayout = LinearLayout.horizontal();

            EditBox editBox = new EditBox(mc.font, COL2_WIDTH, CONTROL_HEIGHT, Component.literal(name));
            editBox.setValue(String.valueOf(currentValue));

            var slider = new AbstractSliderButton(0, 0, COL1_WIDTH, CONTROL_HEIGHT,
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

            rowLayout.addChild(slider);
            rowLayout.addChild(editBox, settings -> settings.paddingLeft(COL_PADDING));

            grid.addChild(rowLayout, row++, 0);
            row++;

            return newValue -> {
                slider.setSliderValue((double) newValue / maxVal);

                if (!editBox.isFocused()) {
                    editBox.setValue(String.valueOf(newValue));
                }
            };
        }

        private void color(String labelText, int currentColor, Consumer<Integer> onSave) {
            LinearLayout rowLayout = LinearLayout.horizontal();
            this.grid.addChild(new StringWidget(Component.literal(labelText), mc.font), row, 0, 1, 1);

            EditBox hexBox = new EditBox(mc.font, COL1_WIDTH, CONTROL_HEIGHT, Component.literal(labelText));
            hexBox.setValue(Integer.toHexString(currentColor).toUpperCase());

            var preview = new net.minecraft.client.gui.components.AbstractWidget(0, 0, COL2_WIDTH,
                    CONTROL_HEIGHT, Component.empty()) {
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

            rowLayout.addChild(hexBox);
            rowLayout.addChild(preview, settings -> settings.paddingLeft(COL_PADDING));

            this.grid.addChild(rowLayout, row++, 0);
            this.row += 2;
        }

        private void checkbox(String label, boolean selected, Consumer<Boolean> onSave) {
            this.grid.addChild(Checkbox.builder(Component.literal(label), mc.font)
                    .selected(selected)
                    .onValueChange((checkbox, value) -> onSave.accept(value))
                    .build(), this.row++, 0, 1, 1);
        }

        private void cycle(String label, String[] values, String currentValue, Consumer<String> onSave) {
            final int[] index = { 0 };
            for (int i = 0; i < values.length; i++) {
                if (values[i].equalsIgnoreCase(currentValue)) {
                    index[0] = i;
                    break;
                }
            }

            Button button = Button.builder(
                    Component.literal(label + ": " + values[index[0]]),
                    btn -> {
                        index[0] = (index[0] + 1) % values.length;
                        String newValue = values[index[0]];
                        btn.setMessage(Component.literal(label + ": " + newValue));
                        onSave.accept(newValue);
                    })
                    .size(COL1_WIDTH + COL_PADDING + COL2_WIDTH, CONTROL_HEIGHT)
                    .build();

            grid.addChild(button, row++, 0, 1, 1);
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