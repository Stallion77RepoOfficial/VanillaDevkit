package net.minecraft.client.multiplayer;

public final class VanillaDevKitClientChunkCacheBridge {
	private VanillaDevKitClientChunkCacheBridge() {}

	public static Object getStorage(ClientChunkCache chunkCache) {
		return chunkCache.storage;
	}
}
