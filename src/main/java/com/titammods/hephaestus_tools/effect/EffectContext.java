package com.titammods.hephaestus_tools.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record EffectContext(Level level, LivingEntity holder, ItemStack tool, @Nullable LivingEntity target,
                            @Nullable BlockState state, @Nullable BlockPos pos) {

    public static EffectContext holding(LivingEntity holder, ItemStack tool) {
        return new EffectContext(holder.level(), holder, tool, null, null, null);
    }

    public static EffectContext mining(LivingEntity holder, ItemStack tool, BlockState state, @Nullable BlockPos pos) {
        return new EffectContext(holder.level(), holder, tool, null, state, pos);
    }

    public static EffectContext attacking(LivingEntity holder, ItemStack tool, LivingEntity target) {
        return new EffectContext(holder.level(), holder, tool, target, null, null);
    }

    public float wear() {
        int max = tool.getMaxDamage();
        return max <= 0 ? 0f : (float) tool.getDamageValue() / max;
    }
}
