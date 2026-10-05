package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.tools.helper.ToolCombat;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ToolTraitEvents {

    public static List<MaterialTrait> toolTraits(ItemStack tool) {
        if (!ToolStack.isUsable(tool)) return List.of();
        return MaterialTrait.collect(ToolStack.getMaterials(tool));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        List<MaterialTrait> traits = toolTraits(tool);
        if (traits.isEmpty()) return;
        float speed = event.getNewSpeed();
        for (MaterialTrait t : traits) speed = t.modifyMiningSpeed(event.getEntity(), tool, speed);
        event.setNewSpeed(speed);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        ItemStack tool = ToolCombat.tool(event.getEntity(), event.getSource());
        List<MaterialTrait> traits = toolTraits(tool);
        if (traits.isEmpty()) return;
        float amount = event.getAmount();
        for (MaterialTrait t : traits) amount = t.modifyAttackDamage(attacker, event.getEntity(), tool, amount);
        event.setAmount(amount);
    }
}