package com.titammods.hephaestus_tools.materials.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.ToolEffect;

import java.util.List;

public record TraitDefinition(int maxLevel, List<ToolEffect> effects) {

    public static final Codec<TraitDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_level", 1).forGetter(TraitDefinition::maxLevel),
                    ToolEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(TraitDefinition::effects)
            ).apply(instance, TraitDefinition::new));

    public TraitDefinition {
        effects = List.copyOf(effects);
    }
}
