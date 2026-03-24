package com.vanilladevkit.mixin.client;

import com.vanilladevkit.client.TitleScreenCredits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void vanilladevkit$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		TitleScreenCredits.render(graphics, client.font, client.getWindow().getGuiScaledWidth());
	}
}
