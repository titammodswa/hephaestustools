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
            float bonus = Math.min(-y * 0.004f, 0.5f);
            return base * (1f + bonus);
        }
    },
    STONEBOUND("stonebound") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            return base * (1f + damageFraction(tool) * 0.5f);
        }
    },
    MAINTAINED("maintained") {
        @Override public float modifyMiningSpeed(Player player, ItemStack tool, float base) {
            return base * (1f + (1f - damageFraction(tool)) * 0.25f);
        }
    };

    private final String id;

    MaterialTrait(String id) { this.id = id; }

    public String id() { return id; }

    public void applyStats(ModifierStatContext ctx) { }

    public float modifyMiningSpeed(Player player, ItemStack tool, float base) { return base; }

    protected static float damageFraction(ItemStack tool) {
        int max = tool.getMaxDamage();
        return max <= 0 ? 0f : (float) tool.getDamageValue() / max;
    }

    public static Optional<MaterialTrait> byId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        for (MaterialTrait t : values()) {
            if (t.id.equals(id)) return Optional.of(t);
        }
        return Optional.empty();
    }
}