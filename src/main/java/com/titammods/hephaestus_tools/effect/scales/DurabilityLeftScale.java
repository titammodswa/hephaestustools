package com.titammods.hephaestus_tools.effect.scales;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectScale;

public record DurabilityLeftScale() implements EffectScale {

    public static final DurabilityLeftScale INSTANCE = new DurabilityLeftScale();
    public static final MapCodec<DurabilityLeftScale> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<DurabilityLeftScale> codec() {
        return CODEC;
    }

    @Override
    public float value(EffectContext context) {
        return 1f - context.wear();
    }
}
