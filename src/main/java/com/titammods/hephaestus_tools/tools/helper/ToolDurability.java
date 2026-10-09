package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ToolDurability {
    private ToolDurability() {}

    public static void hurt(ItemStack tool, int amount, ServerLevel level, LivingEntity owner) {
        if (amount <= 0 || !ToolStack.isInitialized(tool) || ToolStack.isBroken(tool)
                || !tool.isDamageableItem() || (owner != null && owner.hasInfiniteMaterials())) return;
        int damage = tool.getItem().damageItem(tool, amount, owner, item -> {});
        damage = EnchantmentHelper.processDurabilityChange(level, tool, damage);
        if (damage <= 0) return;
        int next = Math.min(tool.getMaxDamage(), tool.getDamageValue() + damage);
        if (owner instanceof ServerPlayer player) {
            CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, tool, next);
        }
        tool.setDamageValue(next);
    }
}
