package com.titammods.hephaestus_tools.materials.trait;

import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public enum MaterialTrait {

    LIGHTWEIGHT("lightweight") {
        @Override public void applyStats(ModifierStatContext ctx) {
            ctx.multiplyMiningSpeed(0.05f);
            ctx.attackSpeed += 0.5f;
        }
    },
    FEATHERWEIGHT("featherweight") {
        @Override public void applyStats(ModifierStatContext ctx) {
            ctx.multiplyMiningSpeed(0.07f);
            ctx.attackSpeed += 0.3f;
        }
    },
    SOLID("solid") {
        @Override public void applyStats(ModifierStatContext ctx) {
            ctx.multiplyDurability(0.25f);
        }
    },
    DENSE("dense") {
        @Override public void applyStats(ModifierStatContext ctx) {
            ctx.multiplyDurability(0.40f);
        }
    },
    HEAVY("heavy") {
        @Override public void applyStats(ModifierStatContext ctx) {
            ctx.multiplyAttackDamage(0.20f);
            ctx.attackSpeed -= 0.4f;
        }
    },
    DWARVEN("dwarven") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            int y = player.blockPosition().getY();
            if (y >= 0) return base;
            float bonus = Math.min(-y * 0.004f, 0.5f); // ate +50% no fundo
            return base * (1f + bonus);
        }
    },
    STONEBOUND("stonebound") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            return base * (1f + damageFraction(tool) * 0.5f); // ate +50% quase quebrada
        }
    },
    MAINTAINED("maintained") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            return base * (1f + (1f - damageFraction(tool)) * 0.25f); // ate +25% cheia
        }
    },
    SMITE("smite") {
        @Override public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                                  ItemStack tool, float base) {
            return victim.getType().is(net.minecraft.tags.EntityTypeTags.SENSITIVE_TO_SMITE) ? base + 2.5f : base;
        }
    },
    JAGGED("jagged") {
        @Override public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                                  ItemStack tool, float base) {
            return base + damageFraction(tool) * 3.0f; // ate +3 quase quebrada
        }
    },
    PIERCE("pierce") {
        @Override public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                                  ItemStack tool, float base) {
            return base + Math.min(victim.getArmorValue() * 0.2f, 4.0f);
        }
    },
    CULTIVATED("cultivated") {
        @Override public void onInventoryTick(ItemStack tool, net.minecraft.world.level.Level level, Player player) {
            if (level.getGameTime() % 40L == 0L && tool.isDamaged()) {
                tool.setDamageValue(tool.getDamageValue() - 1);
            }
        }
    },
    MAGNETIC("magnetic") {
        @Override public void onInventoryTick(ItemStack tool, net.minecraft.world.level.Level level, Player player) {
            if (level.isClientSide || level.getGameTime() % 5L != 0L) return;
            double r = 5.0;
            for (net.minecraft.world.entity.item.ItemEntity item :
                    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            player.getBoundingBox().inflate(r))) {
                if (item.hasPickUpDelay() || !item.isAlive()) continue;
                net.minecraft.world.phys.Vec3 pull = player.position().add(0, 0.5, 0)
                        .subtract(item.position()).normalize().scale(0.12);
                item.setDeltaMovement(item.getDeltaMovement().add(pull));
            }
        }
    },
    SHOCK("shock") {
        @Override public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                                  ItemStack tool, float base) {
            return victim.isInWaterOrRain() ? base + 2.5f : base;
        }
    },
    TEMPERATE("temperate") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            return base + coldBonus(player) * 4.0f;
        }
        @Override public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                                  ItemStack tool, float base) {
            return base + coldBonus(attacker) * 1.5f;
        }
    };

    private final String id;

    MaterialTrait(String id) { this.id = id; }

    public String id() { return id; }

    public void applyStats(ModifierStatContext ctx) { }

    public float modifyMiningSpeed(Player player, ItemStack tool, float base) { return base; }

    public float modifyAttackDamage(Player attacker, net.minecraft.world.entity.LivingEntity victim,
                                    ItemStack tool, float base) { return base; }

    public void onInventoryTick(ItemStack tool, net.minecraft.world.level.Level level, Player player) { }

    protected static float damageFraction(ItemStack tool) {
        int max = tool.getMaxDamage();
        return max <= 0 ? 0f : (float) tool.getDamageValue() / max;
    }
    protected static float coldBonus(Player player) {
        float temp = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
        return Math.max(0f, 0.75f - temp);
    }

    public static Optional<MaterialTrait> byId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        for (MaterialTrait t : values()) {
            if (t.id.equals(id)) return Optional.of(t);
        }
        return Optional.empty();
    }
}