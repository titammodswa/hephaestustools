package com.titammods.hephaestus_tools.tools.aoe;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public final class HammerAoeBreakHandler {

    private HammerAoeBreakHandler() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof IAoeTool aoeTool)) return;

        Level level = player.level();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = event.getPos();
        BlockState centerState = level.getBlockState(pos);

        HitResult hit = aoeTool.rayTraceBlocks(level, player);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
        if (!aoeTool.isEffectiveOnBlock(tool, centerState, player)) return;

        BlockHitResult brt = (BlockHitResult) hit;
        Direction side = brt.getDirection();
        List<BlockPos> extraBlocks = aoeTool.getExtraBlocks(level, brt, player, tool);

        for (BlockPos extraPos : extraBlocks) {
            BlockState extraState = level.getBlockState(extraPos);
            if (!level.hasChunkAt(extraPos)
                    || !player.mayUseItemAt(extraPos, side, tool)
                    || !extraState.canHarvestBlock(level, extraPos, player)) {
                continue;
            }

            Block extraBlock = extraState.getBlock();
            if (player.getAbilities().instabuild) {
                if (extraState.onDestroyedByPlayer(level, extraPos, player, true, extraState.getFluidState())) {
                    extraBlock.destroy(level, extraPos, extraState);
                }
            } else {
                BlockEntity blockEntity = level.getBlockEntity(extraPos);
                int xp = extraState.getExpDrop(level, extraPos, blockEntity, player, tool);
                tool.getItem().mineBlock(tool, level, extraState, extraPos, player);

                if (extraState.onDestroyedByPlayer(level, extraPos, player, true, extraState.getFluidState())) {
                    extraBlock.destroy(level, extraPos, extraState);
                    extraBlock.playerDestroy(level, player, extraPos, extraState, blockEntity, tool);
                    extraBlock.popExperience(serverLevel, extraPos, xp);
                }
            }

            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, extraPos));
        }
    }
}