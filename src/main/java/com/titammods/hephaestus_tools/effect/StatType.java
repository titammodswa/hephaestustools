package com.titammods.hephaestus_tools.effect;

import com.mojang.serialization.Codec;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.util.StringRepresentable;

public enum StatType implements StringRepresentable {
    DURABILITY(ToolStats.DURABILITY),
    MINING_SPEED(ToolStats.MINING_SPEED),
    ATTACK_DAMAGE(ToolStats.ATTACK_DAMAGE),
    ATTACK_SPEED(ToolStats.ATTACK_SPEED);

    public static final Codec<StatType> CODEC = StringRepresentable.fromEnum(StatType::values);

    private final String name;

    StatType(String name) {
        this.name = name;
    }

    public void add(ModifierStatContext stats, float value) {
        switch (this) {
            case DURABILITY -> stats.addDurability(value);
            case MINING_SPEED -> stats.addMiningSpeed(value);
            case ATTACK_DAMAGE -> stats.addAttackDamage(value);
            case ATTACK_SPEED -> stats.attackSpeed += value;
        }
    }

    public void multiply(ModifierStatContext stats, float value) {
        switch (this) {
            case DURABILITY -> stats.multiplyDurability(value);
            case MINING_SPEED -> stats.multiplyMiningSpeed(value);
            case ATTACK_DAMAGE -> stats.multiplyAttackDamage(value);
            case ATTACK_SPEED -> stats.attackSpeed *= (1.0f + value);
        }
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
