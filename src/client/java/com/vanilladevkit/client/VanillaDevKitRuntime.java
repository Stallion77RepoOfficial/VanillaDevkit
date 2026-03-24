package com.vanilladevkit.client;

import com.vanilladevkit.mixin.client.ClientChunkCacheStorageAccessor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.VanillaDevKitClientChunkCacheBridge;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.lwjgl.glfw.GLFW;

public final class VanillaDevKitRuntime {
	private static final int FLUSH_ACTION_NORMAL = 0;
	private static final int FLUSH_ACTION_DISCONNECT = 1;
	private static final int[] CLICK_MULTIPLIER_STEPS = {1, 5, 20, 50, 100, 250, 500, 1000};

	private static VanillaDevKitConfig config = new VanillaDevKitConfig();
	private static boolean packetDelayEnabled;
	private static boolean suppressNextContainerClosePacket;
	private static boolean clientChunksUnloaded;
	private static boolean guiKeyWasDown;
	private static AbstractContainerMenu savedContainerMenu;
	private static Screen savedContainerScreen;
	private static ClientLevel trackedLevel;

	private static final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
	private static final Map<Long, LevelChunk> stashedChunks = new LinkedHashMap<>();

	private VanillaDevKitRuntime() {}

	public static void initialize(VanillaDevKitConfig initialConfig) {
		config = copyConfig(initialConfig);
	}

	public static boolean isGuiKeyPressed(int keyCode) {
		return keyCode == config.guiKey;
	}

	public static boolean shouldInterceptPacket(Packet<?> packet) {
		if (packet instanceof ServerboundContainerClosePacket && suppressNextContainerClosePacket) {
			suppressNextContainerClosePacket = false;
			return true;
		}

		if (!packetDelayEnabled || packet instanceof ServerboundKeepAlivePacket) {
			return false;
		}

		if (packet instanceof ServerboundContainerClickPacket) {
			for (int i = 0; i < config.clickMultiplier; i++) {
				delayedPackets.add(packet);
			}
		} else {
			delayedPackets.add(packet);
		}

		return true;
	}

	public static void saveAndCloseContainerScreen(Screen parentScreen) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		if (!(parentScreen instanceof AbstractContainerScreen)) {
			clearSavedContainerScreen();
			minecraft.setScreen(null);
			return;
		}

		savedContainerScreen = parentScreen;
		savedContainerMenu = minecraft.player.containerMenu;
		closeContainerWithoutPacket(minecraft);
	}

	public static void reopenSavedContainerScreen() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || savedContainerScreen == null || savedContainerMenu == null) {
			return;
		}

		minecraft.player.containerMenu = savedContainerMenu;
		minecraft.setScreen(savedContainerScreen);
	}

	public static void closeContainerScreenWithoutPacket(Screen parentScreen) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		if (parentScreen instanceof AbstractContainerScreen) {
			closeContainerWithoutPacket(minecraft);
			return;
		}

		suppressNextContainerClosePacket = false;
		minecraft.setScreen(null);
	}

	public static void clearSavedContainerScreen() {
		savedContainerScreen = null;
		savedContainerMenu = null;
	}

	public static void startPacketDelay() {
		packetDelayEnabled = true;
	}

	public static void stopPacketDelay() {
		if (!packetDelayEnabled) {
			return;
		}

		packetDelayEnabled = false;
		flushDelayedPackets();
	}

	public static void unloadClientChunks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}

		if (!(minecraft.level.getChunkSource() instanceof ClientChunkCache chunkCache)) {
			return;
		}

		stashedChunks.clear();
		int radius = Math.max(8, minecraft.options.getEffectiveRenderDistance() + 6);
		int centerChunkX = Mth.floor(minecraft.player.getX()) >> 4;
		int centerChunkZ = Mth.floor(minecraft.player.getZ()) >> 4;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				LevelChunk chunk = chunkCache.getChunk(centerChunkX + dx, centerChunkZ + dz, ChunkStatus.FULL, false);
				if (chunk != null) {
					stashedChunks.put(toChunkKey(chunk.getPos()), chunk);
				}
			}
		}

		if (stashedChunks.isEmpty()) {
			clientChunksUnloaded = false;
			refreshChunkRendering(minecraft);
			return;
		}

		for (LevelChunk chunk : stashedChunks.values()) {
			chunkCache.drop(chunk.getPos());
		}

		clientChunksUnloaded = true;
		refreshChunkRendering(minecraft);
	}

	public static void restoreClientChunks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}

		if (!(minecraft.level.getChunkSource() instanceof ClientChunkCache chunkCache)) {
			return;
		}

		if (stashedChunks.isEmpty()) {
			clientChunksUnloaded = false;
			refreshChunkRendering(minecraft);
			return;
		}

		try {
			Object storageObject = VanillaDevKitClientChunkCacheBridge.getStorage(chunkCache);
			if (!(storageObject instanceof ClientChunkCacheStorageAccessor storage)) {
				refreshChunkRendering(minecraft);
				return;
			}

			int chunkRadius = storage.vanilladevkit$getChunkRadius();
			int viewCenterX = storage.vanilladevkit$getViewCenterX();
			int viewCenterZ = storage.vanilladevkit$getViewCenterZ();
			int viewRange = chunkRadius * 2 + 1;
			int restoredChunkCount = 0;
			Iterator<Map.Entry<Long, LevelChunk>> iterator = stashedChunks.entrySet().iterator();

			while (iterator.hasNext()) {
				LevelChunk chunk = iterator.next().getValue();
				ChunkPos pos = chunk.getPos();
				if (!isChunkInRange(pos.x, pos.z, viewCenterX, viewCenterZ, chunkRadius)) {
					continue;
				}

				if (chunkCache.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) != null) {
					continue;
				}

				int index = Math.floorMod(pos.z, viewRange) * viewRange + Math.floorMod(pos.x, viewRange);
				storage.vanilladevkit$replace(index, chunk);
				minecraft.level.onChunkLoaded(pos);
				prepareChunkForRendering(minecraft, chunkCache, chunk);
				iterator.remove();
				restoredChunkCount++;
			}

			if (restoredChunkCount == 0) {
				refreshChunkRendering(minecraft);
				return;
			}

			chunkCache.getLightEngine().runLightUpdates();
		} catch (Throwable throwable) {
			refreshChunkRendering(minecraft);
			return;
		}

		clientChunksUnloaded = !stashedChunks.isEmpty();
		if (!clientChunksUnloaded && minecraft.level != null) {
			minecraft.level.clearTintCaches();
		}
		refreshChunkRendering(minecraft);
	}

	public static void cycleClickMultiplier() {
		config.clickMultiplier = nextClickMultiplier(config.clickMultiplier);
	}

	public static void toggleDisconnectOnFlush() {
		config.flushAction = isDisconnectOnFlush() ? FLUSH_ACTION_NORMAL : FLUSH_ACTION_DISCONNECT;
	}

	public static boolean isPacketDelayEnabled() {
		return packetDelayEnabled;
	}

	public static int getDelayedPacketCount() {
		return delayedPackets.size();
	}

	public static boolean areClientChunksUnloaded() {
		return clientChunksUnloaded;
	}

	public static String getChunkStatusText() {
		return clientChunksUnloaded ? "Unloaded" : "Loaded";
	}

	public static boolean hasSavedContainerScreen() {
		return savedContainerScreen != null && savedContainerMenu != null;
	}

	public static int getClickMultiplier() {
		return config.clickMultiplier;
	}

	public static boolean isDisconnectOnFlush() {
		return config.flushAction == FLUSH_ACTION_DISCONNECT;
	}

	public static VanillaDevKitConfig createConfigSnapshot() {
		return copyConfig(config);
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		syncRuntimeWithSession(minecraft);

		if (minecraft.player == null) {
			return;
		}

		boolean guiKeyDown = isGuiKeyCurrentlyDown(minecraft);
		if (guiKeyDown && minecraft.screen != null && !(minecraft.screen instanceof VanillaDevKitScreen) && !guiKeyWasDown) {
			Screen parentScreen = minecraft.screen;
			minecraft.execute(() -> minecraft.setScreen(new VanillaDevKitScreen(parentScreen)));
		}

		guiKeyWasDown = guiKeyDown;
	}

	private static void closeContainerWithoutPacket(Minecraft minecraft) {
		suppressNextContainerClosePacket = true;
		minecraft.setScreen(null);
	}

	private static void flushDelayedPackets() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getConnection() != null) {
			Packet<?> packet;
			while ((packet = delayedPackets.poll()) != null) {
				minecraft.getConnection().send(packet);
			}
		} else {
			delayedPackets.clear();
		}

		if (isDisconnectOnFlush() && minecraft.getConnection() != null) {
			minecraft.getConnection().getConnection().disconnect(Component.empty());
		}
	}

	private static void syncRuntimeWithSession(Minecraft minecraft) {
		if (minecraft.level != trackedLevel) {
			trackedLevel = minecraft.level;
			resetTransientState();
			return;
		}

		if (minecraft.player == null && hasTransientState()) {
			resetTransientState();
		}
	}

	private static boolean hasTransientState() {
		return packetDelayEnabled
			|| suppressNextContainerClosePacket
			|| clientChunksUnloaded
			|| !delayedPackets.isEmpty()
			|| !stashedChunks.isEmpty()
			|| hasSavedContainerScreen();
	}

	private static void resetTransientState() {
		packetDelayEnabled = false;
		suppressNextContainerClosePacket = false;
		clientChunksUnloaded = false;
		guiKeyWasDown = false;
		delayedPackets.clear();
		stashedChunks.clear();
		clearSavedContainerScreen();
	}

	private static void refreshChunkRendering(Minecraft minecraft) {
		if (minecraft.level != null) {
			minecraft.level.clearTintCaches();
		}

		minecraft.levelRenderer.allChanged();
	}

	private static void prepareChunkForRendering(Minecraft minecraft, ClientChunkCache chunkCache, LevelChunk chunk) {
		ChunkPos pos = chunk.getPos();
		LevelLightEngine lightEngine = chunkCache.getLightEngine();
		LevelChunkSection[] sections = chunk.getSections();

		lightEngine.setLightEnabled(pos, true);
		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			int sectionY = minecraft.level.getSectionYFromSectionIndex(sectionIndex);
			lightEngine.updateSectionStatus(SectionPos.of(pos, sectionY), sections[sectionIndex].hasOnlyAir());
		}

		minecraft.level.setSectionRangeDirty(
			pos.x - 1,
			minecraft.level.getMinSectionY(),
			pos.z - 1,
			pos.x + 1,
			minecraft.level.getMaxSectionY(),
			pos.z + 1
		);
		minecraft.levelRenderer.onChunkReadyToRender(pos);
	}

	private static boolean isChunkInRange(int chunkX, int chunkZ, int centerChunkX, int centerChunkZ, int chunkRadius) {
		return Math.abs(chunkX - centerChunkX) <= chunkRadius && Math.abs(chunkZ - centerChunkZ) <= chunkRadius;
	}

	private static int nextClickMultiplier(int currentMultiplier) {
		for (int i = 0; i < CLICK_MULTIPLIER_STEPS.length; i++) {
			if (currentMultiplier < CLICK_MULTIPLIER_STEPS[i]) {
				return CLICK_MULTIPLIER_STEPS[i];
			}

			if (currentMultiplier == CLICK_MULTIPLIER_STEPS[i]) {
				return CLICK_MULTIPLIER_STEPS[(i + 1) % CLICK_MULTIPLIER_STEPS.length];
			}
		}

		return CLICK_MULTIPLIER_STEPS[0];
	}

	private static VanillaDevKitConfig copyConfig(VanillaDevKitConfig source) {
		VanillaDevKitConfig copy = new VanillaDevKitConfig();
		copy.guiKey = source.guiKey;
		copy.clickMultiplier = source.clickMultiplier;
		copy.flushAction = source.flushAction;
		copy.normalize();
		return copy;
	}

	private static long toChunkKey(ChunkPos pos) {
		return ((long)pos.x << 32) ^ (pos.z & 0xFFFFFFFFL);
	}

	private static boolean isGuiKeyCurrentlyDown(Minecraft minecraft) {
		long windowHandle = minecraft.getWindow().handle();
		return windowHandle != 0L && GLFW.glfwGetKey(windowHandle, config.guiKey) == GLFW.GLFW_PRESS;
	}
}
