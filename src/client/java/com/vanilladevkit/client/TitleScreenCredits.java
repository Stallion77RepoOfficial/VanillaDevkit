package com.vanilladevkit.client;

import java.lang.reflect.Field;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class TitleScreenCredits {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int STALLION_ORANGE = 0xFFF77E2D;
	private static final int TOP_MARGIN = 3;
	private static final int RIGHT_MARGIN = 3;
	private static final int LINE_SPACING = 2;
	private static final String PREFIX_TEXT = "VanillaDevKit by ";
	private static final String AUTHOR_TEXT = "Stallion77";

	private TitleScreenCredits() {}

	public static void render(GuiGraphics graphics, Font font, int screenWidth) {
		int prefixWidth = font.width(PREFIX_TEXT);
		int authorWidth = font.width(AUTHOR_TEXT);
		int x = screenWidth - RIGHT_MARGIN - prefixWidth - authorWidth;
		int y = TOP_MARGIN + getExistingCreditLineCount() * (font.lineHeight + LINE_SPACING);

		graphics.drawString(font, PREFIX_TEXT, x, y, WHITE, true);
		graphics.drawString(font, AUTHOR_TEXT, x + prefixWidth, y, STALLION_ORANGE, true);
	}

	private static int getExistingCreditLineCount() {
		return Math.max(0, getMeteorCreditLineCount());
	}

	private static int getMeteorCreditLineCount() {
		if (!FabricLoader.getInstance().isModLoaded("meteor-client")) {
			return 0;
		}

		try {
			Class<?> addonManagerClass = Class.forName("meteordevelopment.meteorclient.addons.AddonManager");
			Field addonsField = addonManagerClass.getField("ADDONS");
			Object addons = addonsField.get(null);
			if (addons instanceof List<?> addonList) {
				return addonList.size();
			}
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return 0;
		}

		return 0;
	}
}
