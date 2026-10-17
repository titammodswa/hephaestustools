package com.titammods.hephaestus_tools.effect.scales;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectScale;

public record ColdScale(float below) implements EffectScale {

    public static final MapCodec<ColdScale> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("below").forGetter(ColdScale::below)
            ).apply(instance, ColdScale::new));

    @Override
    public MapCodec<ColdScale> codec() {
        return CODEC;
    }

    @Override
    public float value(EffectContext context) {
        float temperature = context.level().getBiome(context.holder().blockPosition()).value().getBaseTemperature();
        return Math.max(0f, below - temperature);
    }
}
