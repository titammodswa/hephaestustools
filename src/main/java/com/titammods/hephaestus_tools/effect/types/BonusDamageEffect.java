package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectCondition;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.EffectOperation;
import com.titammods.hephaestus_tools.effect.ScaledAmount;
import com.titammods.hephaestus_tools.effect.ToolEffect;

import java.util.Optional;

public record BonusDamageEffect(EffectOperation operation, ScaledAmount amount, Optional<EffectCondition> condition)
        implements ToolEffect {

    public static final MapCodec<BonusDamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    EffectOperation.CODEC.optionalFieldOf("operation", EffectOperation.ADD).forGetter(BonusDamageEffect::operation),
                    ScaledAmount.MAP_CODEC.forGetter(BonusDamageEffect::amount),
                    EffectCondition.CODEC.optionalFieldOf("condition").forGetter(BonusDamageEffect::condition)
            ).apply(instance, BonusDamageEffect::new));

    public BonusDamageEffect(EffectOperation operation, ScaledAmount amount) {
        this(operation, amount, Optional.empty());
    }

    public BonusDamageEffect(EffectOperation operation, ScaledAmount amount, EffectCondition condition) {
        this(operation, amount, Optional.of(condition));
    }

    @Override
    public MapCodec<BonusDamageEffect> codec() {
        return CODEC;
    }

    @Override
    public float modifyDamage(EffectContext context, float damage, int level) {
        if (condition.isPresent() && !condition.get().test(context)) return damage;
        return operation.apply(damage, amount.compute(context, level));
    }
}
