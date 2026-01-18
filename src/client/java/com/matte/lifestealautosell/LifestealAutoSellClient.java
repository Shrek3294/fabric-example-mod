package com.matte.lifestealautosell;

import com.matte.lifestealautosell.config.AutoSellConfigManager;
import com.matte.lifestealautosell.gui.AutoSellConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class LifestealAutoSellClient implements ClientModInitializer {
	public static final AutoSellConfigManager CONFIG = new AutoSellConfigManager();
	private static final AutoSeller AUTO_SELLER = new AutoSeller();

	private static final KeyMapping OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
		new KeyMapping(
			"key.lifesteal_auto_sell.open_config",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"category.lifesteal_auto_sell.main"
		)
	);

	@Override
	public void onInitializeClient() {
		CONFIG.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_CONFIG_KEY.consumeClick()) {
				Minecraft.getInstance().setScreen(new AutoSellConfigScreen(Minecraft.getInstance().screen, CONFIG));
			}

			AUTO_SELLER.tick(Minecraft.getInstance(), CONFIG.get());
		});
	}
}
