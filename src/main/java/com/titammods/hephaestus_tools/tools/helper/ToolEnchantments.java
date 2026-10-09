package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.tools.modifier.ModifierEffects;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolEnchantments {
    private ToolEnchantments() {}

    public static final ResourceKey<Enchantment> SILK_TOUCH = key("silk_touch");
    public static final ResourceKey<Enchantment> LOOTING = key("looting");
    public static final ResourceKey<Enchantment> FORTUNE = key("fortune");
    public static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    public static final ResourceKey<Enchantment> SWEEPING_EDGE = key("sweeping_edge");
    public static final ResourceKey<Enchantment> EFFICIENCY = key("efficiency");
    public static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
    public static final ResourceKey<Enchantment> MENDING = key("mending");

    private static final List<ResourceKey<Enchantment>> BANNED = List.of(
            SILK_TOUCH, LOOTING, FORTUNE, SHARPNESS, SWEEPING_EDGE, EFFICIENCY, UNBREAKING, MENDING);

    private static final Map<ResourceKey<Enchantment>, Identifier> MERGED = merged();

    private static Map<ResourceKey<Enchantment>, Identifier> merged() {
        Map<ResourceKey<Enchantment>, Identifier> map = new LinkedHashMap<>();
        map.put(UNBREAKING, ModifierEffects.REINFORCED);
        map.put(EFFICIENCY, ModifierEffects.HASTE);
        map.put(FORTUNE, ModifierEffects.FORTUNE);
        map.put(SHARPNESS, ModifierEffects.SHARPNESS);
        map.put(LOOTING, ModifierEffects.LOOTING);
        map.put(SILK_TOUCH, ModifierEffects.SILK_TOUCH);
        map.put(SWEEPING_EDGE, ModifierEffects.SWEEPING);
        return map;
    }

    public static Map<Identifier, Integer> bonusLevels(ItemStack tool, HolderLookup.Provider registries) {
        Map<Identifier, Integer> bonus = new LinkedHashMap<>();
        if (registries == null || !ToolStack.isInitialized(tool)) return bonus;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments levels = tool.getAllEnchantments(lookup);
        MERGED.forEach((enchantment, modifier) -> {
            int level = lookup.get(enchantment).map(levels::getLevel).orElse(0) - granted(tool, enchantment);
            if (level > 0) bonus.put(modifier, level);
        });
        return bonus;
    }

    public static boolean isMergedLine(Component line) {
        if (isMergedKey(line)) return true;
        for (Component sibling : line.getSiblings()) {
            if (isMergedLine(sibling)) return true;
        }
        return false;
    }

    private static boolean isMergedKey(Component part) {
        if (!(part.getContents() instanceof TranslatableContents contents)) return false;
        for (ResourceKey<Enchantment> enchantment : MERGED.keySet()) {
            String name = enchantment.identifier().toLanguageKey("enchantment");
            if (contents.getKey().equals(name) || contents.getKey().equals(name + ".desc")) return true;
        }
        return false;
    }

    private static int granted(ItemStack tool, ResourceKey<Enchantment> enchantment) {
        if (!ToolStack.isUsable(tool)) return 0;
        if (enchantment == SILK_TOUCH) return modifierLevel(tool, ModifierEffects.SILK_TOUCH);
        if (enchantment == FORTUNE) return Math.max(0, (int) ToolStack.getProperties(tool).getStat(ToolStats.FORTUNE, 0));
        if (enchantment == LOOTING) return Math.max(0, (int) ToolStack.getProperties(tool).getStat(ToolStats.LOOTING, 0));
        return 0;
    }

    private static int modifierLevel(ItemStack tool, Identifier modifier) {
        for (ToolConstructionData.ModifierEntry entry : ToolStack.getModifiers(tool)) {
            if (entry.id().equals(modifier)) return entry.level();
        }
        return 0;
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(path));
    }

    public static List<ResourceKey<Enchantment>> banned() {
        return BANNED;
    }

    public static boolean isBanned(Holder<Enchantment> enchantment) {
        for (ResourceKey<Enchantment> key : BANNED) {
            if (enchantment.is(key)) return true;
        }
        return false;
    }
}