package com.titammods.hephaestus_tools.effect.scales;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectScale;

public record DepthScale(int below) implements EffectScale {

    public static final MapCodec<DepthScale> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("below", 0).forGetter(DepthScale::below)
            ).apply(instance, DepthScale::new));

    @Override
    public MapCodec<DepthScale> codec() {
        return CODEC;
    }

    @Override
    public float value(EffectContext context) {
        return Math.max(0, below - context.holder().blockPosition().getY());
    }
}
