package com.titammods.hephaestus_tools.tools.aoe;

import com.titammods.hephaestus_tools.table.MasteryAoe;
import com.titammods.hephaestus_tools.table.MasteryLevel;
import com.titammods.hephaestus_tools.table.ToolMastery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class AoeBox {
    private AoeBox() {}

    public static int[] masteryBonus(ItemStack stack) {
        String m = ToolMastery.selected(stack);
        if (m == null || m.isEmpty()) return ZERO;
        int lv = MasteryLevel.of(stack);
        if (lv < MasteryLevel.T1) return ZERO;
        switch (m) {
            case "demolition", "earthmover" -> {
                int add = lv >= MasteryLevel.T3 ? 3 : lv >= MasteryLevel.T2 ? 2 : 1;
                return new int[]{add, add, 0};
            }
            case "clean_dig" -> {
                int add = lv >= MasteryLevel.T3 ? 1 : 0;
                return new int[]{add, add, 0};
            }
            default -> { return ZERO; }
        }
    }
    private static final int[] ZERO = {0, 0, 0};
    private static final float BASE_HARDNESS_LIMIT = 3f;

    public static float hardnessLimit(ItemStack stack) {
        if (!ToolMastery.selected(stack).equals("unstoppable")) return BASE_HARDNESS_LIMIT;
        int lv = MasteryLevel.of(stack);
        return lv >= MasteryLevel.T3 ? 8f : lv >= MasteryLevel.T2 ? 6f : lv >= MasteryLevel.T1 ? 4f : BASE_HARDNESS_LIMIT;
    }

    public static boolean isCleanDig(ItemStack stack) {
        return ToolMastery.selected(stack).equals("clean_dig") && MasteryLevel.of(stack) >= MasteryLevel.T1;
    }

    public static int[] effectiveRadii(ItemStack stack, IAoeTool tool) {
        int[] b = masteryBonus(stack);
        return new int[]{ tool.aoeWidth(stack) + b[0], tool.aoeHeight(stack) + b[1], tool.aoeDepth(stack) + b[2] };
    }

    public static List<BlockPos> blocks(Level world, Player player, ItemStack stack, IAoeTool tool,
                                        BlockPos center, Direction side) {
        int[] r = effectiveRadii(stack, tool);
        int w = r[0], h = r[1], d = r[2];
        if (w == 0 && h == 0 && d == 0) return List.of();
        if (side == null) side = Direction.UP;

        BlockState centerState = world.getBlockState(center);
        float refHardness = centerState.getDestroySpeed(world, center);
        float hardnessLimit = hardnessLimit(stack);
        BlockState cleanRef = isCleanDig(stack) ? centerState : null;

        Direction depthDir = side.getOpposite();
        Direction widthDir, heightDir;
        if (side.getAxis() == Direction.Axis.Y) {
            heightDir = player != null ? player.getDirection() : Direction.NORTH;
            widthDir = heightDir.getClockWise();
        } else {
            widthDir = side.getCounterClockWise();
            heightDir = Direction.UP;
        }

        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dd = 0; dd <= d; dd++) {
            for (int dh = -h; dh <= h; dh++) {
                for (int dw = -w; dw <= w; dw++) {
                    if (dw == 0 && dh == 0 && dd == 0) continue;
                    m.set(center);
                    m.move(widthDir, dw).move(heightDir, dh).move(depthDir, dd);
                    if (isEffective(world, m, refHardness, hardnessLimit, cleanRef, stack, tool)) out.add(m.immutable());
                }
            }
        }
        return out;
    }

    private static boolean isEffective(Level world, BlockPos pos, float refHardness, float hardnessLimit,
                                       BlockState cleanRef, ItemStack stack, IAoeTool tool) {
        if (!world.isInWorldBounds(pos) || !world.hasChunkAt(pos)) return false;
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness == -1) return false;
        boolean hardnessOk = refHardness == 0 ? hardness == 0 : (hardness / refHardness) <= hardnessLimit;
        if (!hardnessOk) return false;
        if (cleanRef != null && !MasteryAoe.isCleanDig(cleanRef, state)) return false;
        return tool.isEffectiveOnBlock(stack, state, null);
    }
}