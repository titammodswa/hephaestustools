package com.titammods.hephaestus_tools.effect.conditions;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.effect.EffectCondition;
import com.titammods.hephaestus_tools.effect.EffectContext;

public record TargetWetCondition() implements EffectCondition {

    public static final TargetWetCondition INSTANCE = new TargetWetCondition();
    public static final MapCodec<TargetWetCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<TargetWetCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(EffectContext context) {
        return context.target() != null && context.target().isInWaterOrRain();
    }
}
