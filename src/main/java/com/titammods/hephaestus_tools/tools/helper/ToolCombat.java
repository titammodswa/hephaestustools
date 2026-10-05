package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.event.MasteryEvents;
import com.titammods.hephaestus_tools.event.ToolXpEvents;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ToolCombat {
    private static final ThreadLocal<Boolean> SECONDARY = ThreadLocal.withInitial(() -> false);

    private ToolCombat() {}

    public static ItemStack tool(LivingEntity target, DamageSource source) {
        if (SECONDARY.get() || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) {
            return ItemStack.EMPTY;
        }
        ItemStack tool = player.getMainHandItem();
        return ToolStack.isUsable(tool) ? tool : ItemStack.EMPTY;
    }

    public static void killed(LivingEntity target, DamageSource source) {
        ItemStack tool = tool(target, source);
        if (!tool.isEmpty() && source.getEntity() instanceof ServerPlayer player) {
            ToolXpEvents.onMeleeKill(tool, player);
            MasteryEvents.onMeleeKill(tool, player);
        }
    }

    public static void secondary(Runnable damage) {
        boolean previous = SECONDARY.get();
        SECONDARY.set(true);
        try {
            damage.run();
        } finally {
            if (previous) SECONDARY.set(true);
            else SECONDARY.remove();
        }
    }
}