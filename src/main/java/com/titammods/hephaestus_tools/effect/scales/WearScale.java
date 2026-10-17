package com.titammods.hephaestus_tools.effect.scales;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectScale;

public record WearScale() implements EffectScale {

    public static final WearScale INSTANCE = new WearScale();
    public static final MapCodec<WearScale> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<WearScale> codec() {
        return CODEC;
    }

    @Override
    public float value(EffectContext context) {
        return context.wear();
    }
}
