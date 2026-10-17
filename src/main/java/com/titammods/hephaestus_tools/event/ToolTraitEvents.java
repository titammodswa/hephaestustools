package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.effect.ToolEffects;
import com.titammods.hephaestus_tools.tools.helper.ToolCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public class ToolTraitEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        event.setNewSpeed(ToolEffects.modifyBreakSpeed(player, tool, event.getState(),
                event.getPosition().orElse(null), event.getNewSpeed()));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        ItemStack tool = ToolCombat.tool(event.getEntity(), event.getSource());
        event.setAmount(ToolEffects.modifyDamage(attacker, event.getEntity(), tool, event.getAmount()));
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof LivingEntity breaker)) return;
        ToolEffects.modifyDrops(event.getLevel(), breaker, event.getTool(), event.getState(), event.getPos(), event.getDrops());
    }
}
