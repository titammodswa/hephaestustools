package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ToolTraitEvents {

    public static Optional<MaterialTrait> headTrait(ItemStack tool) {
        if (!(tool.getItem() instanceof ModifiableItem) || !ToolStack.isInitialized(tool)) return Optional.empty();
        List<MaterialId> materials = ToolStack.getMaterials(tool);
        if (materials.isEmpty()) return Optional.empty();
        MaterialStats stats = MaterialManager.getInstance().getStatsForSlot(materials.get(0), 0);
        if (stats == null || !stats.hasTrait()) return Optional.empty();
        return MaterialTrait.byId(stats.traitId());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        headTrait(tool).ifPresent(trait ->
                event.setNewSpeed(trait.modifyMiningSpeed(event.getEntity(), tool, event.getNewSpeed())));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        ItemStack tool = attacker.getMainHandItem();
        headTrait(tool).ifPresent(trait ->
                event.setAmount(trait.modifyAttackDamage(attacker, event.getEntity(), tool, event.getAmount())));
    }
}