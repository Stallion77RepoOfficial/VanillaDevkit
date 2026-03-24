package com.vanilladevkit.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public final class VanillaDevKitScreen extends Screen {
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 18;
	private static final int BUTTON_GAP = 4;

	private final Screen parent;

	private StringWidget packetDelayLabel;
	private StringWidget chunkStatusLabel;
	private StringWidget savedGuiLabel;
	private Button packetDelayButton;
	private Button chunkButton;
	private Button openSavedGuiButton;
	private Button clearSavedGuiButton;
	private Button closeWithoutPacketsButton;

	public VanillaDevKitScreen(Screen parent) {
		super(Component.literal("Vanilla DevKit"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int left = centerX - BUTTON_WIDTH / 2;
		int buttonCount = 9;
		int totalHeight = buttonCount * BUTTON_HEIGHT + 8 * BUTTON_GAP + BUTTON_GAP * 2;
		int y = Math.max(10, (this.height - totalHeight) / 2 - 24);

		addRenderableWidget(Button.builder(
			Component.literal("Save & Close GUI"),
			button -> VanillaDevKitRuntime.saveAndCloseContainerScreen(this.parent)
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		openSavedGuiButton = addRenderableWidget(Button.builder(
			Component.literal("Open Saved GUI"),
			button -> VanillaDevKitRuntime.reopenSavedContainerScreen()
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		clearSavedGuiButton = addRenderableWidget(Button.builder(
			Component.literal("Clear Saved GUI"),
			button -> {
				VanillaDevKitRuntime.clearSavedContainerScreen();
				refreshLabels();
			}
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		closeWithoutPacketsButton = addRenderableWidget(Button.builder(
			Component.literal("Close w/o Packets"),
			button -> VanillaDevKitRuntime.closeContainerScreenWithoutPacket(this.parent)
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		packetDelayButton = addRenderableWidget(Button.builder(
			packetDelayButtonText(),
			button -> {
				if (VanillaDevKitRuntime.isPacketDelayEnabled()) {
					VanillaDevKitRuntime.stopPacketDelay();
				} else {
					VanillaDevKitRuntime.startPacketDelay();
				}
				refreshLabels();
			}
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		chunkButton = addRenderableWidget(Button.builder(
			chunkButtonText(),
			button -> {
				if (VanillaDevKitRuntime.areClientChunksUnloaded()) {
					VanillaDevKitRuntime.restoreClientChunks();
				} else {
					VanillaDevKitRuntime.unloadClientChunks();
				}
				refreshLabels();
			}
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		addRenderableWidget(Button.builder(
			clickMultiplierButtonText(),
			button -> {
				VanillaDevKitRuntime.cycleClickMultiplier();
				button.setMessage(clickMultiplierButtonText());
			}
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP;

		addRenderableWidget(Button.builder(
			flushActionButtonText(),
			button -> {
				VanillaDevKitRuntime.toggleDisconnectOnFlush();
				button.setMessage(flushActionButtonText());
			}
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
		y += BUTTON_HEIGHT + BUTTON_GAP * 2;

		addRenderableWidget(Button.builder(
			Component.literal("Close Menu"),
			button -> this.onClose()
		).bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		int infoX = 8;
		int infoY = 8;
		addRenderableOnly(new StringWidget(
			infoX,
			infoY,
			160,
			10,
			Component.literal("Vanilla DevKit").withStyle(ChatFormatting.GOLD),
			this.font
		));
		infoY += 14;

		packetDelayLabel = new StringWidget(infoX, infoY, 200, 10, Component.empty(), this.font);
		addRenderableOnly(packetDelayLabel);
		infoY += 12;

		chunkStatusLabel = new StringWidget(infoX, infoY, 200, 10, Component.empty(), this.font);
		addRenderableOnly(chunkStatusLabel);
		infoY += 12;

		savedGuiLabel = new StringWidget(infoX, infoY, 200, 10, Component.empty(), this.font);
		addRenderableOnly(savedGuiLabel);

		refreshLabels();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(0, 0, this.width, this.height, 0x90000000);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		refreshLabels();
	}

	@Override
	public void onClose() {
		VanillaDevKitConfigManager.save(VanillaDevKitRuntime.createConfigSnapshot());
		Minecraft.getInstance().setScreen(this.parent);
	}

	private void refreshLabels() {
		if (packetDelayLabel != null) {
			boolean packetDelayEnabled = VanillaDevKitRuntime.isPacketDelayEnabled();
			String status = packetDelayEnabled ? "ON (" + VanillaDevKitRuntime.getDelayedPacketCount() + ")" : "OFF";
			ChatFormatting color = packetDelayEnabled ? ChatFormatting.GREEN : ChatFormatting.GRAY;
			packetDelayLabel.setMessage(Component.literal("Packet Delay: ").append(Component.literal(status).withStyle(color)));
		}

		if (chunkStatusLabel != null) {
			boolean clientChunksUnloaded = VanillaDevKitRuntime.areClientChunksUnloaded();
			ChatFormatting color = clientChunksUnloaded ? ChatFormatting.RED : ChatFormatting.GREEN;
			chunkStatusLabel.setMessage(
				Component.literal("Chunks: ").append(Component.literal(VanillaDevKitRuntime.getChunkStatusText()).withStyle(color))
			);
		}

		if (savedGuiLabel != null) {
			boolean hasSavedContainerScreen = VanillaDevKitRuntime.hasSavedContainerScreen();
			ChatFormatting color = hasSavedContainerScreen ? ChatFormatting.AQUA : ChatFormatting.GRAY;
			String status = hasSavedContainerScreen ? "Stored" : "None";
			savedGuiLabel.setMessage(Component.literal("Saved GUI: ").append(Component.literal(status).withStyle(color)));
		}

		if (packetDelayButton != null) {
			packetDelayButton.setMessage(packetDelayButtonText());
		}

		if (chunkButton != null) {
			chunkButton.setMessage(chunkButtonText());
		}

		boolean hasSavedGui = VanillaDevKitRuntime.hasSavedContainerScreen();
		if (openSavedGuiButton != null) {
			openSavedGuiButton.active = hasSavedGui;
		}

		if (clearSavedGuiButton != null) {
			clearSavedGuiButton.active = hasSavedGui;
		}

		if (closeWithoutPacketsButton != null) {
			closeWithoutPacketsButton.active = this.parent instanceof AbstractContainerScreen;
		}
	}

	private static Component packetDelayButtonText() {
		return Component.literal(VanillaDevKitRuntime.isPacketDelayEnabled() ? "Stop Packet Delay" : "Start Packet Delay");
	}

	private static Component chunkButtonText() {
		return Component.literal(VanillaDevKitRuntime.areClientChunksUnloaded() ? "Load Chunks" : "Unload Chunks");
	}

	private static Component clickMultiplierButtonText() {
		return Component.literal("Click Multiplier: ").append(VanillaDevKitRuntime.getClickMultiplier() + "x");
	}

	private static Component flushActionButtonText() {
		return Component.literal("On Flush: ").append(VanillaDevKitRuntime.isDisconnectOnFlush() ? "Disconnect" : "Normal");
	}
}
