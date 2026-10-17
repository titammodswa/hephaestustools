package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectCondition;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectOperation;
import com.titammods.hephaestus_tools.effect.ScaledAmount;
import com.titammods.hephaestus_tools.effect.ToolEffect;

import java.util.Optional;

public record BreakSpeedEffect(EffectOperation operation, ScaledAmount amount, Optional<EffectCondition> condition)
        implements ToolEffect {

    public static final MapCodec<BreakSpeedEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    EffectOperation.CODEC.optionalFieldOf("operation", EffectOperation.ADD).forGetter(BreakSpeedEffect::operation),
                    ScaledAmount.MAP_CODEC.forGetter(BreakSpeedEffect::amount),
                    EffectCondition.CODEC.optionalFieldOf("condition").forGetter(BreakSpeedEffect::condition)
            ).apply(instance, BreakSpeedEffect::new));

    public BreakSpeedEffect(EffectOperation operation, ScaledAmount amount) {
        this(operation, amount, Optional.empty());
    }

    @Override
    public MapCodec<BreakSpeedEffect> codec() {
        return CODEC;
    }

    @Override
    public float modifyBreakSpeed(EffectContext context, float speed, int level) {
        if (condition.isPresent() && !condition.get().test(context)) return speed;
        return operation.apply(speed, amount.compute(context, level));
    }
}
