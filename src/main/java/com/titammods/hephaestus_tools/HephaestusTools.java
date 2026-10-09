package com.titammods.hephaestus_tools;

import com.titammods.hephaestus_tools.config.HephaestusConfig;
import com.titammods.hephaestus_tools.datagen.ModDataGenerators;
import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.registry.ModCreativeTabs;
import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.registry.ModRecipes;
import com.titammods.hephaestus_tools.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(HephaestusTools.MOD_ID)
public class HephaestusTools {

    public static final String MOD_ID = "hephaestus_tools";

    public HephaestusTools(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HephaestusConfig.SPEC, HephaestusConfig.FILE_NAME);
        ModComponents.REGISTRAR.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModRecipes.RECIPE_BOOK_CATEGORIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        ModDataGenerators.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModRecipes::registerRecipeTypes);
    }
}