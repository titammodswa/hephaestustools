package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record ScaledAmount(LevelValue amount, Optional<EffectScale> scale, Optional<Float> max) {

    public static final MapCodec<ScaledAmount> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    LevelValue.CODEC.fieldOf("amount").forGetter(ScaledAmount::amount),
                    EffectScale.CODEC.optionalFieldOf("scale").forGetter(ScaledAmount::scale),
                    Codec.FLOAT.optionalFieldOf("max").forGetter(ScaledAmount::max)
            ).apply(instance, ScaledAmount::new));

    public static ScaledAmount of(float amount) {
        return new ScaledAmount(LevelValue.of(amount), Optional.empty(), Optional.empty());
    }

    public static ScaledAmount scaled(float amount, EffectScale scale) {
        return new ScaledAmount(LevelValue.of(amount), Optional.of(scale), Optional.empty());
    }

    public static ScaledAmount scaled(float amount, EffectScale scale, float max) {
        return new ScaledAmount(LevelValue.of(amount), Optional.of(scale), Optional.of(max));
    }

    public float compute(EffectContext context, int level) {
        float value = amount.get(level);
        if (scale.isPresent()) value = scale.get().value(context) * value;
        if (max.isPresent()) value = Math.min(value, max.get());
        return value;
    }
}
