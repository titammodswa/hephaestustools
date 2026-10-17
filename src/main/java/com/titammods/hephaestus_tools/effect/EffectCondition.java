package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface EffectCondition {

    Codec<EffectCondition> CODEC = EffectRegistries.CONDITION_TYPE_REGISTRY.byNameCodec()
            .dispatch(EffectCondition::codec, Function.identity());

    MapCodec<? extends EffectCondition> codec();

    boolean test(EffectContext context);
}
