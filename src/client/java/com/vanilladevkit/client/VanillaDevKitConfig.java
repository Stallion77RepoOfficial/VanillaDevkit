package com.vanilladevkit.client;

import org.lwjgl.glfw.GLFW;

public final class VanillaDevKitConfig {
	public int guiKey = GLFW.GLFW_KEY_G;
	public int clickMultiplier = 1;
	public int flushAction = 0;

	public void normalize() {
		if (clickMultiplier < 1) {
			clickMultiplier = 1;
		} else if (clickMultiplier > 1000) {
			clickMultiplier = 1000;
		}

		if (flushAction != 1) {
			flushAction = 0;
		}
	}
}
