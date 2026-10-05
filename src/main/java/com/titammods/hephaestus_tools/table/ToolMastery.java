package com.titammods.hephaestus_tools.table;

import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolMastery {
    private ToolMastery() {}

    private static final Map<String, List<String>> PATHS = Map.ofEntries(
            Map.entry("pickaxe", List.of("deep_miner", "momentum")),
            Map.entry("sledge_hammer", List.of("demolition", "aftershock", "unstoppable")),
            Map.entry("vein_hammer", List.of("vein_seeker", "chain_reaction", "motherlode")),
            Map.entry("mattock", List.of("groundworker", "cultivator", "homesteader")),
            Map.entry("excavator", List.of("earthmover", "clean_dig")),
            Map.entry("hand_axe", List.of("lumber_rhythm", "precision_felling", "hatchet_master")),
            Map.entry("broad_axe", List.of("timberfall", "falling_giant", "war_axe")),
            Map.entry("kama", List.of("harvest_sweep", "replanter", "green_thumb")),
            Map.entry("scythe", List.of("reaper", "harvest_chain", "death_sweep")),
            Map.entry("dagger", List.of("backstab", "flurry", "assassin")),
            Map.entry("sword", List.of("duelist", "executioner", "blade_dance")),
            Map.entry("cleaver", List.of("butcher", "crushing_blow", "bloodlust"))
    );

    private static final Set<String> OFFERED =
            PATHS.values().stream().flatMap(List::stream).collect(Collectors.toUnmodifiableSet());

    private static final String CHOICE_KEY = "hephaestus_tools_mastery";

    public static String toolId(Item tool) { return BuiltInRegistries.ITEM.getKey(tool).getPath(); }

    public static List<String> forTool(Item tool) { return PATHS.getOrDefault(toolId(tool), List.of()); }

    public static String selected(ItemStack tool) {
        String choice = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(CHOICE_KEY);
        return OFFERED.contains(choice) ? choice : "";
    }

    public static boolean choose(ItemStack tool, int index) {
        if (tool.getCount() != 1 || !(tool.getItem() instanceof ModifiableItem)
                || !ToolStack.isInitialized(tool) || !selected(tool).isEmpty()) return false;
        if (ToolXp.getLevel(tool) < MasteryLevel.T1) return false;
        List<String> paths = forTool(tool.getItem());
        if (index < 0 || index >= paths.size()) return false;
        CompoundTag data = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putString(CHOICE_KEY, paths.get(index));
        tool.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }
}