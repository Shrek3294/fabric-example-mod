package com.matte.lifestealautosell.config;

import java.util.ArrayList;
import java.util.List;

public final class AutoSellConfig {
	public boolean enabled = true;

	public TriggerMode triggerMode = TriggerMode.EMPTY_SLOTS_AT_MOST;
	public int emptySlotsThreshold = 0;
	public int itemCountThreshold = 0;

	public String command = "sell";
	public String confirmItemId = "minecraft:green_dye";

	public List<String> itemIdsToSell = new ArrayList<>(List.of("minecraft:cobblestone"));

	public int actionDelayTicks = 2;
	public int cooldownTicks = 40;
	public boolean closeScreenAfterConfirm = true;

	public boolean debugChat = false;

	public enum TriggerMode {
		EMPTY_SLOTS_AT_MOST,
		ITEM_COUNT_AT_LEAST
	}
}

