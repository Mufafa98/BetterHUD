package com.mufafa98.mud.client;

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

    private static final class LayoutInfo {
        private final int itemWidth;
        private final int itemHeight;
        private final int maxTextWidth;
        private final int textHeight;

        private LayoutInfo(int itemWidth, int itemHeight, int maxTextWidth, int textHeight) {
            this.itemWidth = itemWidth;
            this.itemHeight = itemHeight;
            this.maxTextWidth = maxTextWidth;
            this.textHeight = textHeight;
        }
    }

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

        LayoutInfo layout = getLayoutInfo(client, items);

        int itemGap = config.gapBetweenItems;
        int width, height;

        // use layout enum instead of isVertical()
        if (config.layout == ArmorConfig.LayoutOrientation.VERTICAL) {
            width = layout.itemWidth;
            height = count * layout.itemHeight + (count - 1) * itemGap;
        } else {
            width = count * layout.itemWidth + (count - 1) * itemGap;
            height = layout.itemHeight;
        }

        return new Dimension(width, height);
    }

    private ArmorConfig.TextPosition getTextPosition() {
        return config.textPosition != null ? config.textPosition : ArmorConfig.TextPosition.RIGHT;
    }

    private LayoutInfo getLayoutInfo(Minecraft client, ItemStack[] items) {
        int maxTextWidth = getMaxTextWidth(client, items);
        int textHeight = client.font.lineHeight;

        int itemWidth = config.textureSize;
        int itemHeight = config.textureSize;

        if (maxTextWidth > 0) {
            switch (getTextPosition()) {
                case LEFT:
                case RIGHT:
                    itemWidth = config.textureSize + config.gapBetweenIconAndText + maxTextWidth;
                    itemHeight = Math.max(config.textureSize, textHeight);
                    break;
                case TOP:
                case BOTTOM:
                    itemWidth = Math.max(config.textureSize, maxTextWidth);
                    itemHeight = config.textureSize + config.gapBetweenIconAndText + textHeight;
                    break;
                default:
                    break;
            }
        }

        return new LayoutInfo(itemWidth, itemHeight, maxTextWidth, textHeight);
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

        LayoutInfo layout = getLayoutInfo(client, itemsToDisplay);
        int gap = config.gapBetweenItems;

        int baseX = posX;
        int baseY = posY;

        if (config.layout == ArmorConfig.LayoutOrientation.VERTICAL) {
            for (ItemStack item : itemsToDisplay) {
                if (item == null || item.isEmpty())
                    continue;

                renderItem(graphics, client, item, baseX, baseY, layout);
                baseY += layout.itemHeight + gap;
            }
        } else {
            for (ItemStack item : itemsToDisplay) {
                if (item == null || item.isEmpty())
                    continue;

                renderItem(graphics, client, item, baseX, baseY, layout);
                baseX += layout.itemWidth + gap;
            }
        }
    }

    private void renderItem(GuiGraphicsExtractor graphics, Minecraft client, ItemStack item, int itemLeft, int itemTop,
            LayoutInfo layout) {
        int textureSize = config.textureSize;
        int iconGap = config.gapBetweenIconAndText;

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

        int textWidth = !text.isEmpty() ? client.font.width(text) : 0;

        int iconX = itemLeft;
        int iconY = itemTop;
        int textX = itemLeft;
        int textY = itemTop;

        if (layout.maxTextWidth > 0) {
            switch (getTextPosition()) {
                case LEFT:
                    iconX = itemLeft + layout.maxTextWidth + iconGap;
                    iconY = itemTop + (layout.itemHeight - textureSize) / 2;
                    textX = itemLeft + (layout.maxTextWidth - textWidth);
                    textY = itemTop + (layout.itemHeight - layout.textHeight) / 2;
                    break;
                case RIGHT:
                    iconX = itemLeft;
                    iconY = itemTop + (layout.itemHeight - textureSize) / 2;
                    textX = itemLeft + textureSize + iconGap;
                    textY = itemTop + (layout.itemHeight - layout.textHeight) / 2;
                    break;
                case TOP:
                    iconX = itemLeft + (layout.itemWidth - textureSize) / 2;
                    iconY = itemTop + layout.textHeight + iconGap;
                    textX = itemLeft + (layout.itemWidth - textWidth) / 2;
                    textY = itemTop;
                    break;
                case BOTTOM:
                    iconX = itemLeft + (layout.itemWidth - textureSize) / 2;
                    iconY = itemTop;
                    textX = itemLeft + (layout.itemWidth - textWidth) / 2;
                    textY = itemTop + textureSize + iconGap;
                    break;
                default:
                    break;
            }
        }

        graphics.item(item, iconX, iconY);

        if (!text.isEmpty()) {
            graphics.text(client.font, text, textX, textY, color);
        }
    }
}
