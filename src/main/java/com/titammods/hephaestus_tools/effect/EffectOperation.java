package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum EffectOperation implements StringRepresentable {
    ADD("add"),
    MULTIPLY("multiply");

    public static final Codec<EffectOperation> CODEC = StringRepresentable.fromEnum(EffectOperation::values);

    private final String name;

    EffectOperation(String name) {
        this.name = name;
    }

    public float apply(float base, float value) {
        return this == MULTIPLY ? base * (1f + value) : base + value;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
