package com.titammods.hephaestus_tools.tools.aoe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class HammerAoeBreakHandler {

    private HammerAoeBreakHandler() {}

    public static List<BlockPos> extraBlocks(ServerPlayer player, BlockPos center) {
        ItemStack tool = player.getMainHandItem();
        if (PlayerBlockBreaks.isBreaking() || !PlayerBlockBreaks.isUsable(tool)
                || !(tool.getItem() instanceof IAoeTool aoeTool)) return List.of();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(center),
                BlockSideHitHandler.getSideHit(player), center, false);
        return aoeTool.getExtraBlocks(player.level(), hit, player, tool);
    }
}