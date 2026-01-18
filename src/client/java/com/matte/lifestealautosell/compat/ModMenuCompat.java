package com.matte.lifestealautosell.compat;

import com.matte.lifestealautosell.LifestealAutoSellClient;
import com.matte.lifestealautosell.gui.AutoSellConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuCompat implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new AutoSellConfigScreen(parent, LifestealAutoSellClient.CONFIG);
	}
}

