package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.LevelValue;
import com.titammods.hephaestus_tools.effect.StatType;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;

public record StatAddEffect(StatType stat, LevelValue amount) implements ToolEffect {

    public static final MapCodec<StatAddEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StatType.CODEC.fieldOf("stat").forGetter(StatAddEffect::stat),
                    LevelValue.CODEC.fieldOf("amount").forGetter(StatAddEffect::amount)
            ).apply(instance, StatAddEffect::new));

    public StatAddEffect(StatType stat, float amount) {
        this(stat, LevelValue.of(amount));
    }

    @Override
    public MapCodec<StatAddEffect> codec() {
        return CODEC;
    }

    @Override
    public void modifyStats(ModifierStatContext stats, int level) {
        stat.add(stats, amount.get(level));
    }
}
