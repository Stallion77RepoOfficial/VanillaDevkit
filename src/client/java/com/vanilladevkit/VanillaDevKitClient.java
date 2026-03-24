package com.vanilladevkit;

import com.mojang.blaze3d.platform.InputConstants;
import com.vanilladevkit.client.VanillaDevKitConfig;
import com.vanilladevkit.client.VanillaDevKitConfigManager;
import com.vanilladevkit.client.VanillaDevKitHud;
import com.vanilladevkit.client.VanillaDevKitRuntime;
import com.vanilladevkit.client.VanillaDevKitScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class VanillaDevKitClient implements ClientModInitializer {
	private static final String OPEN_GUI_KEY = "key.vanilladevkit.open_gui";
	private static final KeyMapping.Category VANILLA_DEV_KIT_CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VanillaDevKit.MOD_ID, "utility"));

	private static KeyMapping openGuiKeyBinding;

	@Override
	public void onInitializeClient() {
		VanillaDevKitConfig config = VanillaDevKitConfigManager.load();
		VanillaDevKitRuntime.initialize(config);

		openGuiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			OPEN_GUI_KEY,
			InputConstants.Type.KEYSYM,
			config.guiKey,
			VANILLA_DEV_KIT_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		VanillaDevKitHud.register();
	}

	private void onEndClientTick(Minecraft client) {
		if (client.player == null) {
			return;
		}

		while (openGuiKeyBinding.consumeClick()) {
			client.setScreen(new VanillaDevKitScreen(client.screen));
		}
	}
}
