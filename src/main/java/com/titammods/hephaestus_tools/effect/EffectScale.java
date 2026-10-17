package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface EffectScale {

    Codec<EffectScale> CODEC = EffectRegistries.SCALE_TYPE_REGISTRY.byNameCodec()
            .dispatch(EffectScale::codec, Function.identity());

    MapCodec<? extends EffectScale> codec();

    float value(EffectContext context);
}
