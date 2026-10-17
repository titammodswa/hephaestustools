package com.titammods.hephaestus_tools.datagen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ModDataGenerators {

    private ModDataGenerators() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModDataGenerators::onClientData);
        modEventBus.addListener(ModDataGenerators::onServerData);
    }

    private static void onClientData(GatherDataEvent.Client event) {
        event.createProvider(ModItemModelProvider::new);
    }

    private static void onServerData(GatherDataEvent.Server event) {
        event.createProvider(ModBlockDataProvider::new);
        event.createProvider(ModRecipeProvider::new);
        event.createProvider(PartCastingMeltingProvider::new);
        event.createProvider(TraitProvider::new);
        event.createProvider(TierProvider::new);
    }
}
