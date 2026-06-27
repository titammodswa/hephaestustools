package com.titammods.hephaestus_tools;

import com.titammods.hephaestus_tools.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(HephaestusTools.MOD_ID)
public class HephaestusTools {

    public static final String MOD_ID = "hephaestus_tools";

    public HephaestusTools(IEventBus modBus) {
        ModComponents.REGISTRAR.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModRecipes::registerRecipeTypes);
    }
}