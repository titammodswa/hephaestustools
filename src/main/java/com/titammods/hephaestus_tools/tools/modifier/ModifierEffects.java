package com.titammods.hephaestus_tools.tools.modifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModifierEffects {

    private ModifierEffects() {}

    private static final String NS = "hephaestus_tools";
    private static final Map<ResourceLocation, ModifierEffect> EFFECTS = new LinkedHashMap<>();

    public static final ResourceLocation DIAMOND   = id("diamond");
    public static final ResourceLocation EMERALD   = id("emerald");
    public static final ResourceLocation LAPIS     = id("lapis");
    public static final ResourceLocation NETHERITE = id("netherite");
    public static final ResourceLocation HASTE     = id("haste");

    static {
        register(DIAMOND, (stats, level) -> {
            stats.addDurability(500);
            stats.addAttackDamage(0.5f);
            stats.addMiningSpeed(2);
            stats.setHarvestTier(Tiers.DIAMOND);
        });

        register(EMERALD, (stats, level) -> {
            stats.multiplyDurability(0.5f);
            stats.multiplyAttackDamage(0.25f);
            stats.multiplyMiningSpeed(0.25f);
            stats.setHarvestTier(Tiers.IRON);
        });

        register(LAPIS, (stats, level) -> {
            int capped = Math.min(level, 3);
            stats.fortune += capped;
            stats.looting += capped;
        });

        register(NETHERITE, (stats, level) -> {
            stats.multiplyDurability(0.2f);
            stats.multiplyAttackDamage(0.2f);
            stats.multiplyMiningSpeed(0.25f);
            stats.setHarvestTier(Tiers.NETHERITE);
            stats.indestructible = true;
        });

        register(HASTE, (stats, level) -> {
            stats.addMiningSpeed(4.0f * level);
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    private static void register(ResourceLocation id, ModifierEffect effect) {
        EFFECTS.put(id, effect);
    }

    @Nullable
    public static ModifierEffect get(ResourceLocation id) {
        return EFFECTS.get(id);
    }

    public static boolean isKnown(ResourceLocation id) {
        return EFFECTS.containsKey(id);
    }
}