package com.titammods.hephaestus_tools.effect.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectCondition;
import com.titammods.hephaestus_tools.effect.EffectContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record TargetInTagCondition(TagKey<EntityType<?>> tag) implements EffectCondition {

    public static final MapCodec<TargetInTagCondition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    TagKey.codec(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(TargetInTagCondition::tag)
            ).apply(instance, TargetInTagCondition::new));

    @Override
    public MapCodec<TargetInTagCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(EffectContext context) {
        return context.target() != null && context.target().getType().builtInRegistryHolder().is(tag);
    }
}
