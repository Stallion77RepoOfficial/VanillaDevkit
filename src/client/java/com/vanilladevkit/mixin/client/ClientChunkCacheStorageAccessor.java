package com.vanilladevkit.mixin.client;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface ClientChunkCacheStorageAccessor {
	@Accessor("chunkRadius")
	int vanilladevkit$getChunkRadius();

	@Accessor("viewCenterX")
	int vanilladevkit$getViewCenterX();

	@Accessor("viewCenterZ")
	int vanilladevkit$getViewCenterZ();

	@Invoker("replace")
	void vanilladevkit$replace(int index, LevelChunk chunk);
}
