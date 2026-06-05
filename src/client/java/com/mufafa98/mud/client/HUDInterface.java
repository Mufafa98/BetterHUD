package com.mufafa98.mud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface HUDInterface {
    void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
