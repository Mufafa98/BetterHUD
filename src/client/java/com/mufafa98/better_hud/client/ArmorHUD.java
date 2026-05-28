package com.mufafa98.better_hud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ArmorHUD implements HUDInterface {

    public ArmorHUD() {
    }

    private static ItemStack[] getEquippedItems(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null)
            return new ItemStack[0];

        return new ItemStack[] {
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.MAINHAND),
                player.getItemBySlot(EquipmentSlot.OFFHAND)
        };
    }

    private static int countItemInInventory(Minecraft client, ItemStack itemToMatch) {
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
        renderArmorAt(graphics, graphics.guiWidth(), graphics.guiHeight());
    }

    public static void renderArmorAt(GuiGraphicsExtractor graphics, int startX, int startY) {
        Minecraft client = Minecraft.getInstance();
        ItemStack[] itemsToDisplay = getEquippedItems(client);
        ArmorConfig config = ArmorConfig.getInstance();

        int textureSize = 16;
        int gap = config.gapBetweenItems;
        int margin = config.margin;

        if (config.renderVertically) {
            int baseX = startX - textureSize - margin;
            int baseY = startY - margin;

            for (ItemStack item : itemsToDisplay) {
                if (item == null || item.isEmpty())
                    continue;

                baseY -= textureSize;
                graphics.item(item, baseX, baseY);

                String text = "";
                int color = 0xFFFFFFFF;
                if (item.isDamageableItem()) {
                    text = String.valueOf(item.getMaxDamage() - item.getDamageValue());
                    if (item.getMaxDamage() - item.getDamageValue() <= item.getMaxDamage() * 0.1) {
                        color = 0xFFFF0000;
                    }
                } else if (item.getCount() > 1) {
                    text = String.valueOf(countItemInInventory(client, item));
                }

                if (!text.isEmpty()) {
                    int textWidth = client.font.width(text);
                    graphics.text(client.font, text, baseX - textWidth - 4, baseY + 4, color);
                }

                baseY -= gap;
            }
        }
    }
}
