package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.LevelValue;
import com.titammods.hephaestus_tools.effect.StatType;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;

public record StatMultiplyEffect(StatType stat, LevelValue amount) implements ToolEffect {

    public static final MapCodec<StatMultiplyEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StatType.CODEC.fieldOf("stat").forGetter(StatMultiplyEffect::stat),
                    LevelValue.CODEC.fieldOf("amount").forGetter(StatMultiplyEffect::amount)
            ).apply(instance, StatMultiplyEffect::new));

    public StatMultiplyEffect(StatType stat, float amount) {
        this(stat, LevelValue.of(amount));
    }

    @Override
    public MapCodec<StatMultiplyEffect> codec() {
        return CODEC;
    }

    @Override
    public void modifyStats(ModifierStatContext stats, int level) {
        stat.multiply(stats, amount.get(level));
    }
}
