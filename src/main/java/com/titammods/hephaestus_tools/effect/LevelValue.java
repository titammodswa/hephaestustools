package com.titammods.hephaestus_tools.effect;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;

import java.util.List;

public record LevelValue(List<Float> values) {

    public static final Codec<LevelValue> CODEC = Codec.either(Codec.FLOAT, Codec.FLOAT.listOf(1, Integer.MAX_VALUE))
            .xmap(either -> either.map(LevelValue::of, LevelValue::new),
                    value -> value.values().size() == 1 ? Either.left(value.values().getFirst()) : Either.right(value.values()));

    public LevelValue {
        values = List.copyOf(values);
    }

    public static LevelValue of(float value) {
        return new LevelValue(List.of(value));
    }

    public float get(int level) {
        return values.get(Mth.clamp(level, 1, values.size()) - 1);
    }
}
