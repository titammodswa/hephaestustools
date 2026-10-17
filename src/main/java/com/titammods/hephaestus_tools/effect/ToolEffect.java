package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;
import java.util.function.Function;

public interface ToolEffect {

    Codec<ToolEffect> CODEC = EffectRegistries.EFFECT_TYPE_REGISTRY.byNameCodec()
            .dispatch(ToolEffect::codec, Function.identity());

    MapCodec<? extends ToolEffect> codec();

    default void modifyStats(ModifierStatContext stats, int level) {}

    default float modifyBreakSpeed(EffectContext context, float speed, int level) {
        return speed;
    }

    default float modifyDamage(EffectContext context, float amount, int level) {
        return amount;
    }

    default void inventoryTick(EffectContext context, int level) {}

    default boolean ticksWhileBroken() {
        return false;
    }

    default void modifyDrops(EffectContext context, List<ItemEntity> drops, int level) {}
}
