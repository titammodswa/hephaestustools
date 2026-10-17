package com.titammods.hephaestus_tools.tools.modifier;

import com.titammods.hephaestus_tools.materials.tier.BuiltInTiers;
import com.titammods.hephaestus_tools.tools.stat.HarvestTier;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModifierEffects {

    private ModifierEffects() {}

    private static final String NS = "hephaestus_tools";
    private static final Map<Identifier, ModifierEffect> EFFECTS = new LinkedHashMap<>();

    public static final Identifier DIAMOND    = id("diamond");
    public static final Identifier EMERALD    = id("emerald");
    public static final Identifier LAPIS      = id("lapis");
    public static final Identifier NETHERITE  = id("netherite");
    public static final Identifier HASTE      = id("haste");
    public static final Identifier REINFORCED = id("reinforced");
    public static final Identifier FORTUNE    = id("fortune");
    public static final Identifier SHARPNESS  = id("sharpness");
    public static final Identifier LOOTING    = id("looting");
    public static final Identifier SILK_TOUCH = id("silk_touch");
    public static final Identifier FLAME      = id("flame");
    public static final Identifier SWEEPING   = id("sweeping");

    static {
        register(DIAMOND, (stats, level) -> {
            stats.addDurability(500);
            stats.addAttackDamage(0.5f);
            stats.addMiningSpeed(2);
            stats.setHarvestTier(HarvestTier.of(BuiltInTiers.DIAMOND));
        });

        register(EMERALD, (stats, level) -> {
            stats.multiplyDurability(0.5f);
            stats.multiplyAttackDamage(0.25f);
            stats.multiplyMiningSpeed(0.25f);
            stats.setHarvestTier(HarvestTier.of(BuiltInTiers.IRON));
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
            stats.setHarvestTier(HarvestTier.of(BuiltInTiers.NETHERITE));
            stats.indestructible = true;
        });

        register(HASTE, (stats, level) -> stats.addMiningSpeed(4.0f * level));

        register(REINFORCED, (stats, level) -> stats.addDurability(250f * level));
        register(FORTUNE,    (stats, level) -> stats.fortune += level);
        register(SHARPNESS,  (stats, level) -> stats.addAttackDamage(1.0f * level));
        register(LOOTING,    (stats, level) -> stats.looting += level);
        register(SILK_TOUCH, (stats, lvl) -> {});
        register(FLAME,      (stats, lvl) -> {});
        register(SWEEPING,   (stats, lvl) -> {});
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    private static void register(Identifier id, ModifierEffect effect) {
        EFFECTS.put(id, effect);
    }

    public static ModifierEffect get(Identifier id) {
        return EFFECTS.get(id);
    }

    public static boolean isKnown(Identifier id) {
        return EFFECTS.containsKey(id);
    }
}
