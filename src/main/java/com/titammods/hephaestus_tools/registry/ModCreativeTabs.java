package com.titammods.hephaestus_tools.registry;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HephaestusTools.MOD_ID);

    public static final Supplier<CreativeModeTab> TOOLS_TAB = TABS.register("tools",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hephaestus_tools.tools"))
                    .icon(() -> new ItemStack(ModItems.PICKAXE.get()))
                    .displayItems((params, output) -> {
                        java.util.List<net.minecraft.world.item.Item> tools = java.util.List.of(
                                ModItems.PICKAXE.get(), ModItems.SLEDGE_HAMMER.get(), ModItems.VEIN_HAMMER.get(),
                                ModItems.MATTOCK.get(), ModItems.EXCAVATOR.get(), ModItems.HAND_AXE.get(),
                                ModItems.BROAD_AXE.get(), ModItems.KAMA.get(), ModItems.SCYTHE.get(),
                                ModItems.DAGGER.get(), ModItems.SWORD.get(), ModItems.CLEAVER.get()
                        );

                        var materials = com.titammods.hephaestus_tools.materials.MaterialManager
                                .getInstance().getAllMaterials();

                        if (materials.isEmpty()) {
                            for (var tool : tools) output.accept(tool);
                        } else {
                            for (var tool : tools) {
                                for (var material : materials) {
                                    if (material.headStats() == null
                                            || material.headStats().equals(
                                            com.titammods.hephaestus_tools.materials.MaterialStats.EMPTY)) {
                                        continue;
                                    }
                                    ItemStack built = com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler
                                            .buildSingleMaterial(tool, material.id());
                                    output.accept(built);
                                }
                            }
                        }

                        java.util.List<net.minecraft.world.item.Item> partItems = java.util.List.of(
                                ModItems.PICK_HEAD.get(), ModItems.HAMMER_HEAD.get(),
                                ModItems.SMALL_AXE_HEAD.get(), ModItems.BROAD_AXE_HEAD.get(),
                                ModItems.ADZE_HEAD.get(), ModItems.LARGE_PLATE.get(),
                                ModItems.SMALL_BLADE.get(), ModItems.LARGE_BLADE.get(),
                                ModItems.TOOL_HANDLE.get(), ModItems.TOUGH_HANDLE.get(),
                                ModItems.TOOL_BINDING.get(), ModItems.TOUGH_BINDING.get()
                        );
                        if (materials.isEmpty()) {
                            for (var part : partItems) output.accept(part);
                        } else {
                            for (var part : partItems) {
                                if (!(part instanceof com.titammods.hephaestus_tools.tools.part.ToolPartItem partItem)) {
                                    output.accept(part);
                                    continue;
                                }
                                for (var material : materials) {
                                    if (material.headStats() == null
                                            || material.headStats().equals(
                                            com.titammods.hephaestus_tools.materials.MaterialStats.EMPTY)) {
                                        continue;
                                    }
                                    output.accept(partItem.withMaterial(material.id()));
                                }
                            }
                        }
                        output.accept(ModItems.PATTERN.get());
                        for (var cast : ModItems.CASTS) output.accept(cast.get());
                        output.accept(ModBlocks.TINKERS_ANVIL.get());
                        output.accept(ModBlocks.SCORCHED_ANVIL.get());
                        output.accept(ModBlocks.PART_BUILDER.get());
                    })
                    .build()
    );
}