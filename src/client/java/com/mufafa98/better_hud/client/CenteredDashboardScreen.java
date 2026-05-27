package com.mufafa98.better_hud.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
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
        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(5);

        grid.addChild(Checkbox.builder(Component.literal("Render Vertically"), this.minecraft.font)
                .selected(ArmorConfig.renderVertically)
                .onValueChange((checkbox, value) -> {
                    ArmorConfig.renderVertically = value;
                    ArmorConfig.save();
                })
                .build(), 0, 0);

        grid.addChild(new AbstractSliderButton(0, 0, 150, 20, Component.literal("Margin: " + ArmorConfig.margin),
                ArmorConfig.margin / 50.0) {

            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Margin: " + ArmorConfig.margin));
            }

            @Override
            protected void applyValue() {
                ArmorConfig.margin = (int) (this.value * 50);
                ArmorConfig.save();
            }
        }, 1, 0);

        grid.arrangeElements();
        grid.setPosition((this.width - grid.getWidth()) / 2, (this.height - grid.getHeight()) / 2);
        grid.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}