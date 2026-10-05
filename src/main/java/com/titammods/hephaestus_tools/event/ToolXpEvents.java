package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.table.ToolRole;
import com.titammods.hephaestus_tools.table.ToolUpgrades;
import com.titammods.hephaestus_tools.table.ToolXp;
import com.titammods.hephaestus_tools.tools.helper.ToolCombat;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Set;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ToolXpEvents {
    private ToolXpEvents() {}

    private static ItemStack tool(ServerPlayer p) {
        ItemStack t = p.getMainHandItem();
        return ToolStack.isUsable(t) ? t : ItemStack.EMPTY;
    }

    public static void afterBreak(ServerPlayer p, BlockState st) {
        afterBreak(p, tool(p), st);
    }

    public static void afterBreak(ServerPlayer p, ItemStack tool, BlockState st) {
        if (!ToolStack.isUsable(tool)) return;
        Set<ToolRole> roles = ToolUpgrades.rolesOf(tool.getItem());
        int xp = 0;
        if (roles.contains(ToolRole.MINING)) {
            if (st.is(Tags.Blocks.ORES)) xp = 5;
            else if (st.is(BlockTags.LOGS)) xp = 3;
            else if (tool.isCorrectToolForDrops(st)) xp = 2;
        }
        if (xp == 0 && roles.contains(ToolRole.HARVEST) && st.is(BlockTags.CROPS)) xp = 2;
        if (xp > 0) ToolXp.addXp(tool, p, xp);
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (e.getNewDamage() <= 0) return;
        ItemStack tool = ToolCombat.tool(e.getEntity(), e.getSource());
        if (tool.isEmpty() || !ToolUpgrades.rolesOf(tool.getItem()).contains(ToolRole.COMBAT)) return;
        ToolXp.addXp(tool, p, Math.max(1, (int) (e.getNewDamage() / 3f)));
    }

    public static void onMeleeKill(ItemStack tool, ServerPlayer player) {
        if (ToolUpgrades.rolesOf(tool.getItem()).contains(ToolRole.COMBAT)) ToolXp.addXp(tool, player, 5);
    }
}