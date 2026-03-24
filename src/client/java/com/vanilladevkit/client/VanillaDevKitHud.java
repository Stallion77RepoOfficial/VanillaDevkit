package com.vanilladevkit.client;

import com.vanilladevkit.VanillaDevKit;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class VanillaDevKitHud {
	private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(VanillaDevKit.MOD_ID, "hud");

	private VanillaDevKitHud() {}

	public static void register() {
		HudElementRegistry.addLast(HUD_ELEMENT_ID, VanillaDevKitHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.player == null || minecraft.screen instanceof VanillaDevKitScreen) {
			return;
		}

		int x = 8;
		int y = 4;
		draw(graphics, x, y, Component.literal("Vanilla DevKit").withStyle(ChatFormatting.GOLD));

		boolean packetDelayEnabled = VanillaDevKitRuntime.isPacketDelayEnabled();
		String packetDelayStatus = packetDelayEnabled ? "ON (" + VanillaDevKitRuntime.getDelayedPacketCount() + ")" : "OFF";
		draw(
			graphics,
			x,
			y + 12,
			Component.literal("Packet Delay: ")
				.append(Component.literal(packetDelayStatus).withStyle(packetDelayEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY))
		);

		boolean clientChunksUnloaded = VanillaDevKitRuntime.areClientChunksUnloaded();
		draw(
			graphics,
			x,
			y + 24,
			Component.literal("Chunks: ")
				.append(Component.literal(VanillaDevKitRuntime.getChunkStatusText()).withStyle(clientChunksUnloaded ? ChatFormatting.RED : ChatFormatting.GREEN))
		);

		boolean hasSavedContainerScreen = VanillaDevKitRuntime.hasSavedContainerScreen();
		draw(
			graphics,
			x,
			y + 36,
			Component.literal("Saved GUI: ")
				.append(Component.literal(hasSavedContainerScreen ? "Stored" : "None").withStyle(hasSavedContainerScreen ? ChatFormatting.AQUA : ChatFormatting.GRAY))
		);
	}

	private static void draw(GuiGraphics graphics, int x, int y, Component text) {
		graphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFFFFFF, true);
	}
}
