package com.matte.lifestealautosell.gui;

import com.matte.lifestealautosell.config.AutoSellConfig;
import com.matte.lifestealautosell.config.AutoSellConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AutoSellConfigScreen extends Screen {
	private final Screen parent;
	private final AutoSellConfigManager configManager;

	private EditBox itemsBox;
	private EditBox commandBox;
	private EditBox confirmItemBox;
	private EditBox thresholdBox;

	private Button enabledButton;
	private Button modeButton;

	public AutoSellConfigScreen(Screen parent, AutoSellConfigManager configManager) {
		super(Component.literal("Auto Sell Config"));
		this.parent = parent;
		this.configManager = configManager;
	}

	@Override
	protected void init() {
		AutoSellConfig config = configManager.get();

		int contentWidth = Math.min(360, this.width - 40);
		int left = (this.width - contentWidth) / 2;
		int y = 40;

		this.enabledButton = this.addRenderableWidget(
			Button.builder(getEnabledLabel(config.enabled), button -> {
				config.enabled = !config.enabled;
				button.setMessage(getEnabledLabel(config.enabled));
			}).bounds(left, y, contentWidth, 20).build()
		);

		y += 24;
		this.modeButton = this.addRenderableWidget(
			Button.builder(getModeLabel(config.triggerMode), button -> {
				config.triggerMode = nextMode(config.triggerMode);
				button.setMessage(getModeLabel(config.triggerMode));
				updateThresholdHint();
			}).bounds(left, y, contentWidth, 20).build()
		);

		y += 28;
		this.itemsBox = new EditBox(this.font, left, y, contentWidth, 20, Component.literal("Items"));
		this.itemsBox.setValue(String.join(",", config.itemIdsToSell));
		this.addRenderableWidget(this.itemsBox);

		y += 24;
		this.thresholdBox = new EditBox(this.font, left, y, contentWidth, 20, Component.literal("Threshold"));
		this.thresholdBox.setValue(Integer.toString(getThresholdForMode(config)));
		updateThresholdHint();
		this.addRenderableWidget(this.thresholdBox);

		y += 24;
		this.commandBox = new EditBox(this.font, left, y, contentWidth, 20, Component.literal("Command"));
		this.commandBox.setValue(config.command == null ? "sell" : config.command);
		this.addRenderableWidget(this.commandBox);

		y += 24;
		this.confirmItemBox = new EditBox(this.font, left, y, contentWidth, 20, Component.literal("Confirm item"));
		this.confirmItemBox.setValue(config.confirmItemId == null ? "minecraft:green_dye" : config.confirmItemId);
		this.addRenderableWidget(this.confirmItemBox);

		y += 32;
		int half = (contentWidth - 8) / 2;
		this.addRenderableWidget(
			Button.builder(Component.literal("Save"), button -> {
				applyAndSave();
				Minecraft.getInstance().setScreen(parent);
			}).bounds(left, y, half, 20).build()
		);
		this.addRenderableWidget(
			Button.builder(Component.literal("Cancel"), button -> Minecraft.getInstance().setScreen(parent))
				.bounds(left + half + 8, y, half, 20)
				.build()
		);
	}

	private static Component getEnabledLabel(boolean enabled) {
		return Component.literal("Enabled: " + (enabled ? "ON" : "OFF"));
	}

	private static Component getModeLabel(AutoSellConfig.TriggerMode mode) {
		return Component.literal("Trigger: " + mode.name());
	}

	private static AutoSellConfig.TriggerMode nextMode(AutoSellConfig.TriggerMode mode) {
		return mode == AutoSellConfig.TriggerMode.EMPTY_SLOTS_AT_MOST
			? AutoSellConfig.TriggerMode.ITEM_COUNT_AT_LEAST
			: AutoSellConfig.TriggerMode.EMPTY_SLOTS_AT_MOST;
	}

	private int getThresholdForMode(AutoSellConfig config) {
		return config.triggerMode == AutoSellConfig.TriggerMode.EMPTY_SLOTS_AT_MOST
			? config.emptySlotsThreshold
			: config.itemCountThreshold;
	}

	private void setThresholdForMode(AutoSellConfig config, int value) {
		if (config.triggerMode == AutoSellConfig.TriggerMode.EMPTY_SLOTS_AT_MOST) {
			config.emptySlotsThreshold = Math.max(0, Math.min(36, value));
		} else {
			config.itemCountThreshold = Math.max(0, value);
		}
	}

	private void updateThresholdHint() {
		AutoSellConfig config = configManager.get();
		if (this.thresholdBox == null) return;

		if (config.triggerMode == AutoSellConfig.TriggerMode.EMPTY_SLOTS_AT_MOST) {
			this.thresholdBox.setHint(Component.literal("Empty slots at most (0 = full)"));
		} else {
			this.thresholdBox.setHint(Component.literal("Total count of selected items"));
		}
	}

	private void applyAndSave() {
		AutoSellConfig config = configManager.get();

		String itemsRaw = this.itemsBox.getValue();
		List<String> parsedItems = parseCommaList(itemsRaw);
		config.itemIdsToSell = new ArrayList<>(parsedItems);

		config.command = sanitizeCommand(this.commandBox.getValue());
		config.confirmItemId = sanitizeId(this.confirmItemBox.getValue());

		int threshold = safeParseInt(this.thresholdBox.getValue(), getThresholdForMode(config));
		setThresholdForMode(config, threshold);

		configManager.save();
	}

	private static int safeParseInt(String raw, int fallback) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static List<String> parseCommaList(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(AutoSellConfigScreen::sanitizeId)
			.filter(s -> !s.isEmpty())
			.distinct()
			.collect(Collectors.toList());
	}

	private static String sanitizeCommand(String raw) {
		if (raw == null) return "sell";
		String trimmed = raw.trim();
		if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
		return trimmed.isEmpty() ? "sell" : trimmed;
	}

	private static String sanitizeId(String raw) {
		if (raw == null) return "";
		return raw.trim().toLowerCase(Locale.ROOT);
	}
}

