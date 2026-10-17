package com.titammods.hephaestus_tools.effect.scales;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectScale;

public record TargetArmorScale() implements EffectScale {

    public static final TargetArmorScale INSTANCE = new TargetArmorScale();
    public static final MapCodec<TargetArmorScale> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<TargetArmorScale> codec() {
        return CODEC;
    }

    @Override
    public float value(EffectContext context) {
        return context.target() == null ? 0f : context.target().getArmorValue();
    }
}
