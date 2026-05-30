package com.mufafa98.better_hud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.awt.Dimension;

public class ArmorHUD implements HUDInterface {

    private final ArmorConfig config;

    public ArmorHUD() {
        this.config = Config.getInstance().getArmorConfig();
    }

    private static ItemStack[] getEquippedItems(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null)
            return new ItemStack[0];

        return new ItemStack[] {
                player.getItemBySlot(EquipmentSlot.MAINHAND),
                player.getItemBySlot(EquipmentSlot.OFFHAND),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET),
        };
    }

    public Dimension getHudDimensions(Minecraft client) {
        ItemStack[] items = getEquippedItems(client);

        int count = 0;
        for (ItemStack s : items)
            if (!s.isEmpty())
                count++;
        if (count == 0)
            return new Dimension(0, 0);

        int maxTextWidth = getMaxTextWidth(client, items);
        int totalItemWidth = config.textureSize + config.gapBetweenIconAndText + maxTextWidth;

        int itemGap = config.gapBetweenItems;
        int width, height;

        if (config.renderVertically) {
            width = totalItemWidth;
            height = count * config.textureSize + (count - 1) * itemGap;
        } else {
            width = count * totalItemWidth + (count - 1) * itemGap;
            height = config.textureSize;
        }

        return new Dimension(width, height);
    }

    private int getMaxTextWidth(Minecraft client, ItemStack[] items) {
        int maxWidth = 0;
        for (ItemStack item : items) {
            if (item.isEmpty())
                continue;

            String text = "";
            if (item.isDamageableItem()) {
                int remaining = item.getMaxDamage() - item.getDamageValue();
                text = String.valueOf(remaining);
            } else if (item.getCount() > 1) {
                text = String.valueOf(countItemInInventory(client, item));
            }

            if (!text.isEmpty()) {
                int width = client.font.width(text);
                if (width > maxWidth)
                    maxWidth = width;
            }
        }
        return maxWidth;
    }

    private int countItemInInventory(Minecraft client, ItemStack itemToMatch) {
        LocalPlayer player = client.player;
        if (player == null || itemToMatch.isEmpty())
            return 0;

        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == itemToMatch.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        Dimension dim = getHudDimensions(client);
        if (dim.width == 0 || dim.height == 0)
            return;

        int topLeftX = config.getTopLeftX(screenWidth, dim.width);
        int topLeftY = config.getTopLeftY(screenHeight, dim.height);

        renderArmorAt(graphics, topLeftX, topLeftY);
    }

    public void renderArmorAt(GuiGraphicsExtractor graphics, int posX, int posY) {
        Minecraft client = Minecraft.getInstance();
        ItemStack[] itemsToDisplay = getEquippedItems(client);

        int textureSize = config.textureSize;
        int gap = config.gapBetweenItems;

        if (config.renderVertically) {
            int baseX = posX;
            int baseY = posY;

            for (ItemStack item : itemsToDisplay) {
                if (item == null || item.isEmpty())
                    continue;
                graphics.item(item, baseX, baseY);

                String text = "";
                int color = 0xFFFFFFFF;
                if (item.isDamageableItem()) {
                    int remaining = item.getMaxDamage() - item.getDamageValue();
                    text = String.valueOf(remaining);

                    double percent = (double) remaining / item.getMaxDamage() * 100.0;
                    if (percent <= config.lowDurabilityPercentage) {
                        color = config.lowDurabilityColor;
                    } else if (percent <= config.mediumDurabilityPercentage) {
                        color = config.mediumDurabilityColor;
                    }
                } else if (item.getCount() > 1) {
                    text = String.valueOf(countItemInInventory(client, item));
                }

                if (!text.isEmpty()) {
                    int textX = baseX + textureSize + config.gapBetweenIconAndText;
                    int textY = baseY + (textureSize - 9) / 2;
                    graphics.text(client.font, text, textX, textY, color);
                }

                baseY += textureSize + gap;
            }
        }
    }
}
