package com.titammods.hephaestus_tools.client.model;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.registry.ModItems;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ToolModelWrapper {

    private ToolModelWrapper() {}

    private static final List<DeferredItem<?>> TOOLS = List.of(
            ModItems.PICKAXE, ModItems.SLEDGE_HAMMER, ModItems.VEIN_HAMMER,
            ModItems.MATTOCK, ModItems.EXCAVATOR, ModItems.HAND_AXE,
            ModItems.BROAD_AXE, ModItems.KAMA, ModItems.SCYTHE,
            ModItems.DAGGER, ModItems.SWORD, ModItems.CLEAVER
    );

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (DeferredItem<?> holder : TOOLS) {
            Item tool = holder.get();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(tool);
            if (itemId == null) continue;

            ModelResourceLocation modelLoc = ModelResourceLocation.inventory(itemId);

            event.getModels().computeIfPresent(modelLoc,
                    (location, original) -> new ToolBakedModel(original, tool));
        }
    }
}