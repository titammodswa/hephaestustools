package com.titammods.hephaestus_tools.effect;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.trait.ToolTraits;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ToolEffects {

    private ToolEffects() {}

    public static List<ActiveEffect> collect(Item tool, List<MaterialId> materials) {
        return ToolTraits.effects(tool, materials);
    }

    public static List<ActiveEffect> collect(ItemStack tool) {
        return collect(tool.getItem(), ToolStack.getMaterials(tool));
    }

    public static void modifyStats(Item tool, List<MaterialId> materials, ModifierStatContext stats) {
        for (ActiveEffect active : collect(tool, materials)) active.effect().modifyStats(stats, active.level());
    }

    public static float modifyBreakSpeed(LivingEntity holder, ItemStack tool, BlockState state, @Nullable BlockPos pos, float speed) {
        if (!ToolStack.isUsable(tool)) return speed;
        EffectContext context = EffectContext.mining(holder, tool, state, pos);
        for (ActiveEffect active : collect(tool)) speed = active.effect().modifyBreakSpeed(context, speed, active.level());
        return speed;
    }

    public static float modifyDamage(LivingEntity attacker, LivingEntity target, ItemStack tool, float amount) {
        if (!ToolStack.isUsable(tool)) return amount;
        EffectContext context = EffectContext.attacking(attacker, tool, target);
        for (ActiveEffect active : collect(tool)) amount = active.effect().modifyDamage(context, amount, active.level());
        return amount;
    }

    public static void inventoryTick(ItemStack tool, ServerLevel level, LivingEntity holder) {
        if (!ToolStack.isInitialized(tool)) return;
        EffectContext context = new EffectContext(level, holder, tool, null, null, null);
        for (ActiveEffect active : collect(tool)) {
            if (ToolStack.isUsable(tool) || active.effect().ticksWhileBroken()) {
                active.effect().inventoryTick(context, active.level());
            }
        }
    }

    public static void modifyDrops(ServerLevel level, LivingEntity breaker, ItemStack tool, BlockState state, BlockPos pos,
                                   List<ItemEntity> drops) {
        if (!ToolStack.isUsable(tool)) return;
        EffectContext context = new EffectContext(level, breaker, tool, null, state, pos);
        for (ActiveEffect active : collect(tool)) active.effect().modifyDrops(context, drops, active.level());
    }
}
