package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.table.MasteryAoe;
import com.titammods.hephaestus_tools.table.MasteryLevel;
import com.titammods.hephaestus_tools.table.ToolMastery;
import com.titammods.hephaestus_tools.tools.aoe.PlayerBlockBreaks;
import com.titammods.hephaestus_tools.tools.helper.ToolDurability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.List;

public final class MasteryInteract {
    private static final ThreadLocal<Boolean> TILLING = ThreadLocal.withInitial(() -> false);

    private MasteryInteract() {}

    public static boolean isTilling() { return TILLING.get(); }

    public static boolean isCultivator(ItemStack tool) {
        return PlayerBlockBreaks.isUsable(tool) && ToolMastery.selected(tool).equals("cultivator");
    }

    public static InteractionResult useFirst(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !PlayerBlockBreaks.isUsable(context.getItemInHand())
                || !player.getAbilities().mayBuild) return InteractionResult.PASS;
        ItemStack tool = context.getItemInHand();
        String mastery = ToolMastery.selected(tool);
        int level = MasteryLevel.of(tool);
        int radius = level >= 30 ? 2 : 1;
        boolean changed = switch (mastery) {
            case "cultivator" -> till(player, context, radius);
            case "homesteader" -> plant(player, context, radius);
            case "reaper", "harvest_sweep", "replanter", "green_thumb" -> {
                int r = mastery.equals("reaper") ? (level >= 30 ? 4 : level >= 20 ? 3 : 2)
                        : (level >= 30 ? 3 : level >= 20 ? 2 : 1);
                ServerLevel world = player.serverLevel();
                List<BlockPos> area = MasteryAoe.square(world, context.getClickedPos(), Direction.UP, r,
                        MasteryAoe::isMatureCrop, true);
                area.removeIf(pos -> !permitted(player, context, pos));
                yield MasteryAoe.harvestCrops(world, player, tool, area,
                        mastery.equals("replanter"), mastery.equals("green_thumb"));
            }
            default -> false;
        };
        return changed ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean permitted(ServerPlayer player, UseOnContext origin, BlockPos pos) {
        ItemStack tool = origin.getItemInHand();
        if (!PlayerBlockBreaks.mayModify(player, tool, pos, origin.getClickedFace())) return false;
        if (pos.equals(origin.getClickedPos())) return true;
        var event = CommonHooks.onRightClickBlock(player, origin.getHand(), pos, hit(pos, origin.getClickedFace()));
        return !event.isCanceled() && !event.getUseItem().isFalse() && !event.getUseBlock().isFalse();
    }

    private static BlockHitResult hit(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
    }

    private static boolean till(ServerPlayer player, UseOnContext origin, int radius) {
        if (origin.getClickedFace() == Direction.DOWN) return false;
        ItemStack tool = origin.getItemInHand();
        boolean changed = false;
        TILLING.set(true);
        try {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = origin.getClickedPos().offset(dx, 0, dz);
                if (!permitted(player, origin, pos)) continue;
                BlockState before = player.level().getBlockState(pos);
                UseOnContext context = new UseOnContext(player, origin.getHand(), hit(pos, origin.getClickedFace()));
                if (before.getToolModifiedState(context, ItemAbilities.HOE_TILL, true) == null) continue;
                if (tool.useOn(context).consumesAction() && player.level().getBlockState(pos) != before) changed = true;
            }
        } finally {
            TILLING.remove();
        }
        if (changed) ToolDurability.hurt(tool, 1, player.serverLevel(), player);
        return changed;
    }

    private static boolean plant(ServerPlayer player, UseOnContext origin, int radius) {
        boolean changed = false;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            BlockPos farm = origin.getClickedPos().offset(dx, 0, dz);
            if (!permitted(player, origin, farm) || !PlayerBlockBreaks.mayModify(player, origin.getItemInHand(), farm.above(), Direction.UP)
                    || !player.level().getBlockState(farm).is(Blocks.FARMLAND) || !player.level().isEmptyBlock(farm.above())) continue;
            ItemStack seed = findSeed(player);
            if (seed.isEmpty()) break;
            UseOnContext context = new UseOnContext(player.level(), player, origin.getHand(), seed, hit(farm, Direction.UP));
            if (seed.useOn(context).consumesAction() && !player.level().isEmptyBlock(farm.above())) changed = true;
        }
        return changed;
    }

    private static ItemStack findSeed(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack seed = inventory.getItem(i);
            if (seed.is(Items.WHEAT_SEEDS) || seed.is(Items.CARROT)
                    || seed.is(Items.POTATO) || seed.is(Items.BEETROOT_SEEDS)) return seed;
        }
        return ItemStack.EMPTY;
    }
}