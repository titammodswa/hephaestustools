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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ToolTraitEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof ModifiableItem) || !ToolStack.isInitialized(tool)) return;

        List<MaterialId> materials = ToolStack.getMaterials(tool);
        if (materials.isEmpty()) return;
        MaterialId head = materials.get(0);
        MaterialStats stats = MaterialManager.getInstance().getStatsForSlot(head, 0);
        if (stats == null || !stats.hasTrait()) return;

        MaterialTrait.byId(stats.traitId()).ifPresent(trait ->
                event.setNewSpeed(trait.modifyMiningSpeed(player, tool, event.getNewSpeed())));
    }
}