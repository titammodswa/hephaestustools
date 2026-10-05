package com.titammods.hephaestus_tools.table;

import com.titammods.hephaestus_tools.event.ToolXpEvents;
import com.titammods.hephaestus_tools.tools.aoe.PlayerBlockBreaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MasteryAoe {
    private MasteryAoe() {}

    public static List<BlockPos> square(Level level, Player player, int r, Predicate<BlockState> match) {
        List<BlockPos> out = new ArrayList<>();
        HitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (!(hit instanceof BlockHitResult brt) || brt.getType() != HitResult.Type.BLOCK || brt.getDirection() == null) return out;
        return square(level, brt.getBlockPos(), brt.getDirection(), r, match);
    }

    public static List<BlockPos> square(Level level, BlockPos c, Direction face, int r, Predicate<BlockState> match) {
        return square(level, c, face, r, match, false);
    }

    public static List<BlockPos> square(Level level, BlockPos c, Direction face, int r, Predicate<BlockState> match,
                                        boolean includeCenter) {
        List<BlockPos> out = new ArrayList<>();
        Direction d1, d2;
        switch (face.getAxis()) {
            case Y -> { d1 = Direction.SOUTH; d2 = Direction.EAST; }
            case X -> { d1 = Direction.UP;    d2 = Direction.SOUTH; }
            default -> { d1 = Direction.UP;   d2 = Direction.EAST; }
        }
        for (int i = -r; i <= r; i++) for (int j = -r; j <= r; j++) {
            if (!includeCenter && i == 0 && j == 0) continue;
            BlockPos p = c.relative(d1, i).relative(d2, j);
            if (!level.isInWorldBounds(p) || !level.hasChunkAt(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!level.isEmptyBlock(p) && s.getDestroySpeed(level, p) >= 0 && match.test(s)) out.add(p);
        }
        return out;
    }

    public static BlockPos behind(Level level, Player player) {
        HitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (!(hit instanceof BlockHitResult brt) || brt.getType() != HitResult.Type.BLOCK) return null;
        return brt.getBlockPos().relative(brt.getDirection().getOpposite());
    }

    public static void breakBlocks(ServerLevel level, ServerPlayer player, ItemStack tool, List<BlockPos> positions, boolean freeDurability) {
        for (BlockPos p : positions) {
            PlayerBlockBreaks.breakExtra(player, tool, p);
        }
    }

    public static void breakBlocks(ServerPlayer player, ItemStack tool, List<BlockPos> positions, Predicate<BlockState> alsoBreakable) {
        for (BlockPos p : positions) {
            PlayerBlockBreaks.breakExtra(player, tool, p, alsoBreakable);
        }
    }

    public static boolean harvestCrops(ServerLevel level, ServerPlayer player, ItemStack tool, List<BlockPos> positions, boolean replant, boolean partial) {
        boolean changed = false;
        for (BlockPos p : positions) {
            if (!player.getAbilities().mayBuild || !PlayerBlockBreaks.mayModify(player, tool, p, Direction.UP)) continue;
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(st)) continue;
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, p, st, player)).isCanceled()) continue;
            if (replant || partial) {
                int age = partial ? Math.max(1, crop.getMaxAge() / 2) : 0;
                BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, p);
                if (!level.setBlock(p, crop.getStateForAge(age), 3)) continue;
                if (EventHooks.onBlockPlace(player, snapshot, Direction.UP)) {
                    level.setBlock(p, st, 3);
                    continue;
                }
            } else {
                if (!level.removeBlock(p, false)) continue;
            }
            if (!player.getAbilities().instabuild) Block.dropResources(st, level, p, null, player, tool);
            ToolXpEvents.afterBreak(player, tool, st);
            changed = true;
            player.connection.send(new ClientboundBlockUpdatePacket(level, p));
        }
        return changed;
    }

    public static boolean isCrop(BlockState s) { return s.getBlock() instanceof CropBlock; }

    public static boolean isMatureCrop(BlockState s) { return s.getBlock() instanceof CropBlock c && c.isMaxAge(s); }

    public static boolean isEarth(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.REPLACEABLE_BY_TREES)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY);
    }

    public static boolean isGroundwork(BlockState s) { return isEarth(s) && s.getFluidState().isEmpty(); }

    public static boolean notOre(BlockState s) { return !s.is(Tags.Blocks.ORES); }

    public static boolean isCleanDig(BlockState center, BlockState s) {
        if (s.hasBlockEntity() || !notOre(s)) return false;
        return s.getBlock() == center.getBlock() || (isEarth(center) && isEarth(s));
    }
}