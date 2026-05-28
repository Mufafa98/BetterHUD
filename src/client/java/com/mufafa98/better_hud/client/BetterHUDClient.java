package com.mufafa98.better_hud.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mufafa98.better_hud.BetterHUD;

public class BetterHUDClient implements ClientModInitializer {
	private static KeyMapping configKeyBinding;
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(BetterHUD.MOD_ID, "better_hud"));

	@Override
	public void onInitializeClient() {
		configKeyBinding = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.BetterHUD.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKeyBinding.consumeClick()) {
				client.setScreen(new CenteredDashboardScreen(client.screen));
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath(BetterHUD.MOD_ID, "armor_layer"),
				BetterHUDClient::renderArmor);
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

	public static void renderArmor(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
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

				// Move up by the texture size to draw the item
				baseY -= textureSize;
				graphics.item(item, baseX, baseY);

				String text = "";
				if (item.isDamageableItem()) {
					text = String.valueOf(item.getMaxDamage() - item.getDamageValue());
				} else if (item.getCount() > 1) {
					text = String.valueOf(countItemInInventory(client, item));
				}

				if (!text.isEmpty()) {
					int textWidth = client.font.width(text);
					// Text is offset slightly to the left of the item
					graphics.text(client.font, text, baseX - textWidth - 4, baseY + 4, 0xFFFFFFFF);
				}

				baseY -= gap;
			}
		}
	}
}