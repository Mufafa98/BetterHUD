package com.mufafa98.mud.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mufafa98.mud.MUD;

public class MUDClient implements ClientModInitializer {
	private static KeyMapping configKeyBinding;
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MUD.MOD_ID, "mud"));

	@Override
	public void onInitializeClient() {
		HUDInterface[] elements = {
				new ArmorHUD()
		};

		configKeyBinding = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.MUD.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKeyBinding.consumeClick()) {
				client.setScreen(new CenteredDashboardScreen(client.screen));
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath(MUD.MOD_ID, "armor_layer"),
				elements[0]::render);
	}

}