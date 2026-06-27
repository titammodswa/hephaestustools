package com.titammods.hephaestus_tools.tools.modifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UpgradeRecipes {

    private UpgradeRecipes() {}

    public record UpgradeRecipe(ResourceLocation modifierId, int maxLevel, int neededPerLevel) {}

    private static final Map<Item, UpgradeRecipe> RECIPES = new LinkedHashMap<>();

    static {
        register(Items.DIAMOND,         ModifierEffects.DIAMOND,   1);
        register(Items.EMERALD,         ModifierEffects.EMERALD,   1);
        register(Items.LAPIS_LAZULI,    ModifierEffects.LAPIS,     3);
        register(Items.NETHERITE_INGOT, ModifierEffects.NETHERITE, 1);
        register(Items.REDSTONE,        ModifierEffects.HASTE,     5, 45);
        register(Items.REDSTONE_BLOCK,  ModifierEffects.HASTE,     5, 5);
    }

    private static void register(Item item, ResourceLocation modifierId, int maxLevel) {
        register(item, modifierId, maxLevel, 1);
    }

    private static void register(Item item, ResourceLocation modifierId, int maxLevel, int neededPerLevel) {
        RECIPES.put(item, new UpgradeRecipe(modifierId, maxLevel, neededPerLevel));
    }

    @Nullable
    public static UpgradeRecipe forItem(Item item) {
        return RECIPES.get(item);
    }
}