package com.vanilladevkit.mixin.client;

import com.vanilladevkit.client.VanillaDevKitRuntime;
import com.vanilladevkit.client.VanillaDevKitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class ContainerScreenKeyMixin {
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!VanillaDevKitRuntime.isGuiKeyPressed(event.key())) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.setScreen(new VanillaDevKitScreen(minecraft.screen)));
		cir.setReturnValue(true);
	}
}
