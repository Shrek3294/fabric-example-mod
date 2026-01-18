package com.matte.lifestealautosell;

import com.matte.lifestealautosell.config.AutoSellConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AutoSeller {
	private enum State {
		IDLE,
		COMMAND_SENT,
		MOVING_ITEMS,
		CLICK_CONFIRM,
		COOLDOWN
	}

	private enum MovePhase {
		NONE,
		PICKUP_FROM_PLAYER,
		PLACE_IN_GUI,
		RETURN_TO_PLAYER
	}

	private State state = State.IDLE;
	private int stateTicks = 0;
	private int actionDelayTicks = 0;
	private int cooldownTicks = 0;

	private Set<Item> cachedItemsToSell = Set.of();
	private int cachedItemsHash = 0;
	private Item cachedConfirmItem = Items.LIME_DYE;
	private Item cachedConfirmItemAlt = Items.GREEN_DYE;
	private String cachedConfirmItemId = "minecraft:lime_dye";

	private MovePhase movePhase = MovePhase.NONE;
	private int moveFromSlot = -1;
	private int moveToSlot = -1;

	public void tick(Minecraft minecraft, AutoSellConfig config) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			reset();
			return;
		}

		if (!config.enabled) {
			reset();
			return;
		}

		rebuildCachesIfNeeded(config);

		if (actionDelayTicks > 0) {
			actionDelayTicks--;
		}

		switch (state) {
			case IDLE -> tickIdle(minecraft, player, config);
			case COMMAND_SENT -> tickCommandSent(minecraft, player, config);
			case MOVING_ITEMS -> tickMovingItems(minecraft, player, config);
			case CLICK_CONFIRM -> tickClickConfirm(minecraft, player, config);
			case COOLDOWN -> tickCooldown(minecraft);
		}
	}

	private void tickIdle(Minecraft minecraft, LocalPlayer player, AutoSellConfig config) {
		stateTicks = 0;

		if (minecraft.screen != null) {
			return;
		}

		if (!shouldTrigger(player, config)) {
			return;
		}

		sendCommand(player, config);
		state = State.COMMAND_SENT;
	}

	private void tickCommandSent(Minecraft minecraft, LocalPlayer player, AutoSellConfig config) {
		stateTicks++;
		if (stateTicks > 100) {
			reset();
			return;
		}

		if (!isContainerScreenOpen(minecraft, player)) {
			return;
		}

		if (findConfirmSlot(player.containerMenu, player, cachedConfirmItem, cachedConfirmItemAlt) == -1) {
			return;
		}

		stateTicks = 0;
		state = State.MOVING_ITEMS;
		actionDelayTicks = Math.max(0, config.actionDelayTicks);
	}

	private void tickMovingItems(Minecraft minecraft, LocalPlayer player, AutoSellConfig config) {
		stateTicks++;
		if (!isContainerScreenOpen(minecraft, player)) {
			reset();
			return;
		}
		if (actionDelayTicks > 0) {
			return;
		}

		AbstractContainerMenu menu = player.containerMenu;
		if (!menu.getCarried().isEmpty()) {
			// Don't fight the player's cursor or a stuck carried stack.
			return;
		}

		if (movePhase == MovePhase.NONE) {
			int fromSlot = findNextPlayerSlot(menu, player, cachedItemsToSell);
			if (fromSlot == -1) {
				stateTicks = 0;
				state = State.CLICK_CONFIRM;
				actionDelayTicks = Math.max(0, config.actionDelayTicks);
				return;
			}

			ItemStack stackToMove = menu.getSlot(fromSlot).getItem();
			int toSlot = findTargetSellSlot(menu, player, stackToMove);
			if (toSlot == -1) {
				stateTicks = 0;
				state = State.CLICK_CONFIRM;
				actionDelayTicks = Math.max(0, config.actionDelayTicks);
				return;
			}

			this.moveFromSlot = fromSlot;
			this.moveToSlot = toSlot;
			this.movePhase = MovePhase.PICKUP_FROM_PLAYER;
		}

		switch (movePhase) {
			case PICKUP_FROM_PLAYER -> {
				clickSlot(minecraft, player, menu, moveFromSlot);
				movePhase = MovePhase.PLACE_IN_GUI;
				actionDelayTicks = Math.max(0, config.actionDelayTicks);
			}
			case PLACE_IN_GUI -> {
				clickSlot(minecraft, player, menu, moveToSlot);
				movePhase = menu.getCarried().isEmpty() ? MovePhase.NONE : MovePhase.RETURN_TO_PLAYER;
				actionDelayTicks = Math.max(0, config.actionDelayTicks);
			}
			case RETURN_TO_PLAYER -> {
				clickSlot(minecraft, player, menu, moveFromSlot);
				movePhase = MovePhase.NONE;
				actionDelayTicks = Math.max(0, config.actionDelayTicks);
			}
			case NONE -> {
			}
		}
	}

	private void tickClickConfirm(Minecraft minecraft, LocalPlayer player, AutoSellConfig config) {
		stateTicks++;
		if (!isContainerScreenOpen(minecraft, player)) {
			reset();
			return;
		}
		if (actionDelayTicks > 0) {
			return;
		}

		AbstractContainerMenu menu = player.containerMenu;
		if (!menu.getCarried().isEmpty()) {
			return;
		}

		int confirmSlot = findConfirmSlot(menu, player, cachedConfirmItem, cachedConfirmItemAlt);
		if (confirmSlot == -1) {
			if (stateTicks > 40) {
				state = State.COOLDOWN;
				cooldownTicks = Math.max(0, config.cooldownTicks);
			}
			return;
		}

		clickSlot(minecraft, player, menu, confirmSlot);

		if (config.closeScreenAfterConfirm) {
			player.closeContainer();
		}

		state = State.COOLDOWN;
		cooldownTicks = Math.max(0, config.cooldownTicks);
		actionDelayTicks = Math.max(0, config.actionDelayTicks);
	}

	private void tickCooldown(Minecraft minecraft) {
		if (minecraft.screen == null) {
			if (cooldownTicks <= 0) {
				reset();
				return;
			}
		}

		cooldownTicks--;
		if (cooldownTicks <= 0) {
			reset();
		}
	}

	private void reset() {
		state = State.IDLE;
		stateTicks = 0;
		actionDelayTicks = 0;
		cooldownTicks = 0;
		movePhase = MovePhase.NONE;
		moveFromSlot = -1;
		moveToSlot = -1;
	}

	private boolean shouldTrigger(LocalPlayer player, AutoSellConfig config) {
		if (cachedItemsToSell.isEmpty()) {
			return false;
		}

		if (config.triggerMode == AutoSellConfig.TriggerMode.ITEM_COUNT_AT_LEAST) {
			int threshold = Math.max(0, config.itemCountThreshold);
			if (threshold == 0) return false;
			return countSelectedItems(player, cachedItemsToSell) >= threshold;
		}

		int emptySlots = countEmptyMainInventorySlots(player);
		return emptySlots <= Math.max(0, config.emptySlotsThreshold);
	}

	private static int countEmptyMainInventorySlots(LocalPlayer player) {
		int empty = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.isEmpty()) empty++;
		}
		return empty;
	}

	private static int countSelectedItems(LocalPlayer player, Set<Item> items) {
		int count = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!stack.isEmpty() && items.contains(stack.getItem())) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void sendCommand(LocalPlayer player, AutoSellConfig config) {
		String command = config.command == null ? "sell" : config.command.trim();
		if (command.startsWith("/")) command = command.substring(1);
		if (command.isEmpty()) command = "sell";

		if (config.debugChat) {
			player.displayClientMessage(Component.literal("[AutoSell] Running /" + command), true);
		}

		player.connection.sendCommand(command);
	}

	private static boolean isContainerScreenOpen(Minecraft minecraft, LocalPlayer player) {
		if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
			return false;
		}
		return player.containerMenu != null && player.containerMenu != player.inventoryMenu;
	}

	private static int findNextPlayerSlot(AbstractContainerMenu menu, LocalPlayer player, Set<Item> itemsToSell) {
		List<Slot> slots = menu.slots;
		int playerStart = Math.max(0, slots.size() - 36);
		for (int i = 0; i < slots.size(); i++) {
			if (!isPlayerInventorySlot(slots, player, i, playerStart)) continue;
			Slot slot = slots.get(i);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) continue;
			if (!itemsToSell.contains(stack.getItem())) continue;
			return i;
		}
		return -1;
	}

	private static int findTargetSellSlot(AbstractContainerMenu menu, LocalPlayer player, ItemStack stackToPlace) {
		List<Slot> slots = menu.slots;
		int playerStart = Math.max(0, slots.size() - 36);
		for (int i = 0; i < slots.size(); i++) {
			if (isPlayerInventorySlot(slots, player, i, playerStart)) continue;
			Slot slot = slots.get(i);
			if (!slot.getItem().isEmpty()) continue;
			if (!slot.mayPlace(stackToPlace)) continue;
			return i;
		}
		return -1;
	}

	private static int findConfirmSlot(AbstractContainerMenu menu, LocalPlayer player, Item confirmItem, Item confirmItemAlt) {
		List<Slot> slots = menu.slots;
		int playerStart = Math.max(0, slots.size() - 36);
		for (int i = 0; i < slots.size(); i++) {
			if (isPlayerInventorySlot(slots, player, i, playerStart)) continue;
			Slot slot = slots.get(i);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) continue;
			Item item = stack.getItem();
			if (item == confirmItem || (confirmItemAlt != null && item == confirmItemAlt)) {
				return i;
			}
		}
		return -1;
	}

	private static void clickSlot(Minecraft minecraft, LocalPlayer player, AbstractContainerMenu menu, int slotIndex) {
		if (minecraft.gameMode == null) return;
		minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0, ClickType.PICKUP, player);
	}

	private static boolean isPlayerInventorySlot(List<Slot> slots, LocalPlayer player, int index, int playerStart) {
		Slot slot = slots.get(index);
		return slot.container == player.getInventory() || index >= playerStart;
	}

	private void rebuildCachesIfNeeded(AutoSellConfig config) {
		int itemsHash = List.copyOf(config.itemIdsToSell == null ? List.<String>of() : config.itemIdsToSell).hashCode();
		if (itemsHash != cachedItemsHash) {
			cachedItemsHash = itemsHash;
			cachedItemsToSell = resolveItems(config.itemIdsToSell);
		}

		String confirmItemId = config.confirmItemId == null ? "" : config.confirmItemId;
		if (!confirmItemId.equals(cachedConfirmItemId)) {
			cachedConfirmItemId = confirmItemId;
			cachedConfirmItem = resolveItem(confirmItemId, Items.GREEN_DYE);
			cachedConfirmItemAlt = cachedConfirmItem == Items.GREEN_DYE
				? Items.LIME_DYE
				: (cachedConfirmItem == Items.LIME_DYE ? Items.GREEN_DYE : null);
		}
	}

	private static Set<Item> resolveItems(List<String> ids) {
		if (ids == null || ids.isEmpty()) return Set.of();

		Set<Item> items = new HashSet<>();
		for (String id : ids) {
			Item item = resolveItem(id, Items.AIR);
			if (item != Items.AIR) {
				items.add(item);
			}
		}
		return Set.copyOf(items);
	}

	private static Item resolveItem(String id, Item fallback) {
		if (id == null) return fallback;
		String trimmed = id.trim();
		if (trimmed.isEmpty()) return fallback;

		ResourceLocation location = ResourceLocation.tryParse(trimmed);
		if (location == null) return fallback;

		return BuiltInRegistries.ITEM.getOptional(location).orElse(fallback);
	}
}
