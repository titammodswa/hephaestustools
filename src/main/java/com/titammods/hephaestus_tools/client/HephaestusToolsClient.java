package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = HephaestusTools.MOD_ID, dist = Dist.CLIENT)
public class HephaestusToolsClient {

    public HephaestusToolsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
