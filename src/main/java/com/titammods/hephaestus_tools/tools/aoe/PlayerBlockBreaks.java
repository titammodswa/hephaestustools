package com.titammods.hephaestus_tools.tools.aoe;

import com.titammods.hephaestus_tools.event.MasteryEvents;
import com.titammods.hephaestus_tools.event.ToolXpEvents;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Predicate;

public final class PlayerBlockBreaks {
    private record Pending(BlockPos pos, BlockState state, ItemStack held, List<BlockPos> extras, int tick) {}

    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ArrayDeque<Pending>> PENDING = ThreadLocal.withInitial(ArrayDeque::new);

    private PlayerBlockBreaks() {}

    public static boolean isBreaking() { return BREAKING.get(); }

    public static boolean isUsable(ItemStack tool) {
        return ToolStack.isUsable(tool);
    }

    public static void begin(ServerPlayer player, BlockPos pos) {
        ArrayDeque<Pending> pending = PENDING.get();
        int tick = player.server.getTickCount();
        pending.removeIf(stale -> stale.tick() != tick);
        BlockPos origin = pos.immutable();
        pending.push(new Pending(origin, player.level().getBlockState(origin), player.getMainHandItem(),
                HammerAoeBreakHandler.extraBlocks(player, origin), tick));
    }

    public static void finish(ServerPlayer player, BlockPos pos, boolean accepted) {
        ArrayDeque<Pending> pending = PENDING.get();
        Pending current = pending.poll();
        while (current != null && !current.pos().equals(pos)) current = pending.poll();
        if (current == null || !accepted
                || player.level().getBlockState(current.pos()) == current.state()
                || player.getMainHandItem() != current.held()) return;
        ToolXpEvents.afterBreak(player, current.state());
        afterBreak(player, current.pos(), current.state(), current.extras());
    }

    public static void afterBreak(ServerPlayer player, BlockPos origin, BlockState state, List<BlockPos> extras) {
        if (isBreaking() || !isUsable(player.getMainHandItem())) return;
        BREAKING.set(true);
        try {
            ItemStack tool = player.getMainHandItem();
            for (BlockPos pos : extras) breakExtra(player, tool, pos);
            if (player.getMainHandItem() == tool && isUsable(tool)) {
                MasteryEvents.afterBlockBreak(player, origin, state);
            }
        } finally {
            BREAKING.remove();
        }
    }

    public static boolean breakExtra(ServerPlayer player, ItemStack tool, BlockPos pos) {
        return breakExtra(player, tool, pos, state -> false);
    }

    public static boolean breakExtra(ServerPlayer player, ItemStack tool, BlockPos pos, Predicate<BlockState> alsoBreakable) {
        Level level = player.level();
        if (player.getMainHandItem() != tool
                || !mayModify(player, tool, pos, BlockSideHitHandler.getSideHit(player))) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0
                || !(tool.isCorrectToolForDrops(state) || alsoBreakable.test(state))
                || !state.canHarvestBlock(level, pos, player)) return false;
        boolean wasBreaking = isBreaking();
        BREAKING.set(true);
        try {
            boolean accepted = player.gameMode.destroyBlock(pos);
            player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            return accepted && level.getBlockState(pos) != state;
        } finally {
            if (wasBreaking) BREAKING.set(true);
            else BREAKING.remove();
        }
    }

    public static boolean mayModify(ServerPlayer player, ItemStack tool, BlockPos pos, Direction face) {
        Level level = player.level();
        return isUsable(tool) && !player.isSpectator() && level.isInWorldBounds(pos)
                && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, tool);
    }
}