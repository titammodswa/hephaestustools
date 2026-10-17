package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.effect.conditions.TargetInTagCondition;
import com.titammods.hephaestus_tools.effect.conditions.TargetWetCondition;
import com.titammods.hephaestus_tools.effect.scales.ColdScale;
import com.titammods.hephaestus_tools.effect.scales.DepthScale;
import com.titammods.hephaestus_tools.effect.scales.DurabilityLeftScale;
import com.titammods.hephaestus_tools.effect.scales.TargetArmorScale;
import com.titammods.hephaestus_tools.effect.scales.WearScale;
import com.titammods.hephaestus_tools.effect.types.BonusDamageEffect;
import com.titammods.hephaestus_tools.effect.types.BreakSpeedEffect;
import com.titammods.hephaestus_tools.effect.types.PullItemsEffect;
import com.titammods.hephaestus_tools.effect.types.RepairEffect;
import com.titammods.hephaestus_tools.effect.types.StatAddEffect;
import com.titammods.hephaestus_tools.effect.types.StatMultiplyEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class EffectRegistries {

    public static final ResourceKey<Registry<MapCodec<? extends ToolEffect>>> EFFECT_TYPE_KEY = key("effect_type");
    public static final ResourceKey<Registry<MapCodec<? extends EffectScale>>> SCALE_TYPE_KEY = key("effect_scale");
    public static final ResourceKey<Registry<MapCodec<? extends EffectCondition>>> CONDITION_TYPE_KEY = key("effect_condition");

    public static final DeferredRegister<MapCodec<? extends ToolEffect>> EFFECT_TYPES =
            DeferredRegister.create(EFFECT_TYPE_KEY, HephaestusTools.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends EffectScale>> SCALE_TYPES =
            DeferredRegister.create(SCALE_TYPE_KEY, HephaestusTools.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends EffectCondition>> CONDITION_TYPES =
            DeferredRegister.create(CONDITION_TYPE_KEY, HephaestusTools.MOD_ID);

    public static final Registry<MapCodec<? extends ToolEffect>> EFFECT_TYPE_REGISTRY = EFFECT_TYPES.makeRegistry(builder -> {});
    public static final Registry<MapCodec<? extends EffectScale>> SCALE_TYPE_REGISTRY = SCALE_TYPES.makeRegistry(builder -> {});
    public static final Registry<MapCodec<? extends EffectCondition>> CONDITION_TYPE_REGISTRY = CONDITION_TYPES.makeRegistry(builder -> {});

    public static final Supplier<MapCodec<StatAddEffect>> STAT_ADD = EFFECT_TYPES.register("stat_add", () -> StatAddEffect.CODEC);
    public static final Supplier<MapCodec<StatMultiplyEffect>> STAT_MULTIPLY = EFFECT_TYPES.register("stat_multiply", () -> StatMultiplyEffect.CODEC);
    public static final Supplier<MapCodec<BreakSpeedEffect>> BREAK_SPEED = EFFECT_TYPES.register("break_speed", () -> BreakSpeedEffect.CODEC);
    public static final Supplier<MapCodec<BonusDamageEffect>> BONUS_DAMAGE = EFFECT_TYPES.register("bonus_damage", () -> BonusDamageEffect.CODEC);
    public static final Supplier<MapCodec<RepairEffect>> REPAIR_OVER_TIME = EFFECT_TYPES.register("repair_over_time", () -> RepairEffect.CODEC);
    public static final Supplier<MapCodec<PullItemsEffect>> PULL_ITEMS = EFFECT_TYPES.register("pull_items", () -> PullItemsEffect.CODEC);

    public static final Supplier<MapCodec<DepthScale>> DEPTH = SCALE_TYPES.register("depth", () -> DepthScale.CODEC);
    public static final Supplier<MapCodec<WearScale>> WEAR = SCALE_TYPES.register("wear", () -> WearScale.CODEC);
    public static final Supplier<MapCodec<DurabilityLeftScale>> DURABILITY_LEFT = SCALE_TYPES.register("durability_left", () -> DurabilityLeftScale.CODEC);
    public static final Supplier<MapCodec<ColdScale>> COLD = SCALE_TYPES.register("cold", () -> ColdScale.CODEC);
    public static final Supplier<MapCodec<TargetArmorScale>> TARGET_ARMOR = SCALE_TYPES.register("target_armor", () -> TargetArmorScale.CODEC);

    public static final Supplier<MapCodec<TargetInTagCondition>> TARGET_IN_TAG = CONDITION_TYPES.register("target_in_tag", () -> TargetInTagCondition.CODEC);
    public static final Supplier<MapCodec<TargetWetCondition>> TARGET_WET = CONDITION_TYPES.register("target_wet", () -> TargetWetCondition.CODEC);

    private EffectRegistries() {}

    public static void register(IEventBus modBus) {
        EFFECT_TYPES.register(modBus);
        SCALE_TYPES.register(modBus);
        CONDITION_TYPES.register(modBus);
    }

    private static <T> ResourceKey<Registry<T>> key(String path) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, path));
    }
}
