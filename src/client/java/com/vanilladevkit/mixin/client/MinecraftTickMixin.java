package com.vanilladevkit.mixin.client;

import com.vanilladevkit.client.VanillaDevKitRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftTickMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void onClientTick(CallbackInfo ci) {
		VanillaDevKitRuntime.tick();
	}
}
