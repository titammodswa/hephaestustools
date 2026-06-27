package com.titammods.hephaestus_tools.tools.modifier;

import net.minecraft.world.item.Tiers;

public final class ModifierStatContext {

    public float durability;
    public float miningSpeed;
    public float attackDamage;
    public float attackSpeed;
    public int enchantability;
    public Tiers harvestTier;
    public int fortune;
    public int looting;
    public boolean indestructible;

    public ModifierStatContext(float durability, float miningSpeed, float attackDamage,
                               float attackSpeed, int enchantability, Tiers harvestTier) {
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.enchantability = enchantability;
        this.harvestTier = harvestTier;
        this.fortune = 0;
        this.looting = 0;
        this.indestructible = false;
    }

    public void addDurability(float flat) {
        this.durability += flat;
    }
    public void multiplyDurability(float factor) {
        this.durability *= (1.0f + factor);
    }

    public void addMiningSpeed(float flat) {
        this.miningSpeed += flat;
    }

    public void multiplyMiningSpeed(float factor) {
        this.miningSpeed *= (1.0f + factor);
    }

    public void addAttackDamage(float flat) {
        this.attackDamage += flat;
    }

    public void multiplyAttackDamage(float factor) {
        this.attackDamage *= (1.0f + factor);
    }

    public void setHarvestTier(Tiers tier) {
        if (tier.ordinal() > this.harvestTier.ordinal()) {
            this.harvestTier = tier;
        }
    }
}