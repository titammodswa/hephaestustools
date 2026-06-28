package com.titammods.hephaestus_tools.tools.aoe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public interface IAoeTool {

    default int getAoeRadius(ItemStack stack) {
        return 1;
    }

    @Nullable
    default HitResult rayTraceBlocks(Level level, Player player) {
        return Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
    }

    default List<BlockPos> getExtraBlocks(Level world, @Nullable BlockHitResult rt, Player player, ItemStack stack) {
        List<BlockPos> positions = new ArrayList<>();
        if (player.isCrouching() || rt == null || rt.getType() != HitResult.Type.BLOCK || rt.getDirection() == null) {
            return positions;
        }

        BlockPos pos = rt.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!isEffectiveOnBlock(stack, state, player)) {
            return positions;
        }

        Direction dir1, dir2;
        switch (rt.getDirection().getAxis()) {
            case Y -> { dir1 = Direction.SOUTH; dir2 = Direction.EAST; }
            case X -> { dir1 = Direction.UP;    dir2 = Direction.SOUTH; }
            default -> { dir1 = Direction.UP;   dir2 = Direction.EAST; }
        }

        int r = getAoeRadius(stack);
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                if (i == 0 && j == 0) continue;
                attemptAddExtraBlock(world, state, pos.relative(dir1, i).relative(dir2, j), stack, player, positions);
            }
        }
        return positions;
    }

    default boolean isEffectiveOnBlock(ItemStack stack, BlockState state, Player player) {
        return stack.getItem().isCorrectToolForDrops(stack, state) && stack.getDestroySpeed(state) > 1f;
    }

    default void attemptAddExtraBlock(Level world, BlockState center, BlockPos pos, ItemStack stack, Player player, List<BlockPos> list) {
        BlockState state = world.getBlockState(pos);
        if (state.getDestroySpeed(world, pos) < 0) return;
        if (!world.isEmptyBlock(pos)
                && areBlocksSimilar(center, state)
                && isEffectiveOnBlock(stack, state, player)) {
            list.add(pos);
        }
    }

    static boolean areBlocksSimilar(BlockState a, BlockState b) {
        if (a.getBlock() == b.getBlock()) return true;
        boolean oreA = a.is(Tags.Blocks.ORES);
        boolean oreB = b.is(Tags.Blocks.ORES);
        if (!oreA && oreB) return false;
        int la = guessHarvestLevel(a);
        int lb = guessHarvestLevel(b);
        return la >= lb || lb == 0;
    }

    static int guessHarvestLevel(BlockState state) {
        if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) return 4;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    int VEIN_CAP = 64;

    default List<BlockPos> veinExtraBlocks(Level world, @Nullable BlockHitResult rt, Player player, ItemStack stack, int maxDistance) {
        List<BlockPos> result = new ArrayList<>();
        if (player.isCrouching() || rt == null || rt.getType() != HitResult.Type.BLOCK) return result;

        BlockPos origin = rt.getBlockPos();
        BlockState state = world.getBlockState(origin);
        if (!isEffectiveOnBlock(stack, state, player)) return result;

        Block target = state.getBlock();
        Set<BlockPos> visited = new HashSet<>();
        Queue<DistancePos> queue = new ArrayDeque<>();
        visited.add(origin);
        if (maxDistance > 0) enqueueVeinNeighbors(origin, 1, visited, queue);

        while (!queue.isEmpty() && result.size() < VEIN_CAP) {
            DistancePos dp = queue.remove();
            if (world.getBlockState(dp.pos).is(target)) {
                if (dp.distance < maxDistance) enqueueVeinNeighbors(dp.pos, dp.distance + 1, visited, queue);
                result.add(dp.pos);
            }
        }
        return result;
    }

    private static void enqueueVeinNeighbors(BlockPos pos, int distance, Set<BlockPos> visited, Queue<DistancePos> queue) {
        for (Direction dir : Direction.values()) {
            BlockPos offset = pos.relative(dir);
            if (visited.add(offset)) {
                queue.add(new DistancePos(offset, distance));
            }
        }
    }

    record DistancePos(BlockPos pos, int distance) {}

    int TREE_CAP = 100;

    default List<BlockPos> treeExtraBlocks(Level world, @Nullable BlockHitResult rt, Player player, ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        if (player.isCrouching() || rt == null || rt.getType() != HitResult.Type.BLOCK) return result;

        BlockPos origin = rt.getBlockPos();
        BlockState state = world.getBlockState(origin);
        if (!isEffectiveOnBlock(stack, state, player)) return result;

        Block target = state.getBlock();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < TREE_CAP) {
            BlockPos pos = queue.remove();
            for (int dy = 1; dy >= -1; dy--) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos n = pos.offset(dx, dy, dz);
                        if (!visited.add(n)) continue;
                        if (world.getBlockState(n).is(target)) {
                            result.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return result;
    }
}