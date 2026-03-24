package com.vanilladevkit.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vanilladevkit.VanillaDevKit;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class VanillaDevKitConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("vanilladevkit.json");

	private VanillaDevKitConfigManager() {}

	public static VanillaDevKitConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			VanillaDevKitConfig config = createDefaultConfig();
			save(config);
			return config;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			VanillaDevKitConfig config = GSON.fromJson(reader, VanillaDevKitConfig.class);
			if (config == null) {
				return createDefaultConfig();
			}

			config.normalize();
			return config;
		} catch (IOException exception) {
			VanillaDevKit.LOGGER.error("Failed to load Vanilla DevKit config from {}", CONFIG_PATH, exception);
			return createDefaultConfig();
		}
	}

	public static void save(VanillaDevKitConfig config) {
		try {
			config.normalize();
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			VanillaDevKit.LOGGER.error("Failed to save Vanilla DevKit config to {}", CONFIG_PATH, exception);
		}
	}

	private static VanillaDevKitConfig createDefaultConfig() {
		VanillaDevKitConfig config = new VanillaDevKitConfig();
		config.normalize();
		return config;
	}
}
