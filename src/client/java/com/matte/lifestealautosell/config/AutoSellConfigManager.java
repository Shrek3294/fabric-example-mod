package com.matte.lifestealautosell.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.matte.lifestealautosell.LifestealAutoSell;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutoSellConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "lifesteal-auto-sell.json";

	private final Path path;
	private AutoSellConfig config;

	public AutoSellConfigManager() {
		this.path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		this.config = new AutoSellConfig();
	}

	public AutoSellConfig get() {
		return this.config;
	}

	public void load() {
		if (!Files.exists(this.path)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(this.path)) {
			AutoSellConfig loaded = GSON.fromJson(reader, AutoSellConfig.class);
			if (loaded != null) {
				this.config = loaded;
			}
		} catch (JsonSyntaxException | IOException e) {
			LifestealAutoSell.LOGGER.warn("Failed to load config from {}", this.path, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(this.path.getParent());
		} catch (IOException e) {
			LifestealAutoSell.LOGGER.warn("Failed to create config directory {}", this.path.getParent(), e);
			return;
		}

		try (Writer writer = Files.newBufferedWriter(this.path)) {
			GSON.toJson(this.config, writer);
		} catch (IOException e) {
			LifestealAutoSell.LOGGER.warn("Failed to save config to {}", this.path, e);
		}
	}
}

