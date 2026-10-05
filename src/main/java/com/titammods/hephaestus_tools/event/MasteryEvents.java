package com.titammods.hephaestus_tools.event;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.table.MasteryAoe;
import com.titammods.hephaestus_tools.table.MasteryLevel;
import com.titammods.hephaestus_tools.table.MasteryStreak;
import com.titammods.hephaestus_tools.table.ToolMastery;
import com.titammods.hephaestus_tools.tools.aoe.BlockSideHitHandler;
import com.titammods.hephaestus_tools.tools.aoe.PlayerBlockBreaks;
import com.titammods.hephaestus_tools.tools.helper.ToolCombat;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class MasteryEvents {
    private MasteryEvents() {}

    private static final long STREAK_TICKS = 60, COMBAT_TICKS = 100;
    private static final float HARD_BLOCK = 3.0f;

    private static String mastery(ItemStack tool) {
        if (!ToolStack.isUsable(tool)) return "";
        return ToolMastery.selected(tool);
    }

    private static boolean isDeepBlock(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || s.is(Tags.Blocks.ORES) || s.is(Blocks.DEEPSLATE) || s.is(BlockTags.STONE_ORE_REPLACEABLES);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        ItemStack tool = p.getMainHandItem();
        String m = mastery(tool);
        if (m.isEmpty()) return;
        int lv = MasteryLevel.of(tool);
        long now = p.level().getGameTime();
        float mult = 1f;
        switch (m) {
            case "momentum" -> {
                int cap = lv >= 30 ? 15 : lv >= 20 ? 10 : lv >= 10 ? 5 : 0;
                if (cap > 0) mult += Math.min(MasteryStreak.mineCount(p.getUUID(), now), cap) * (lv >= 30 ? 0.04f : 0.03f);
            }
            case "lumber_rhythm", "harvest_chain" -> {
                int cap = lv >= 30 ? 12 : lv >= 20 ? 8 : lv >= 10 ? 5 : 0;
                if (cap > 0) mult += Math.min(MasteryStreak.mineCount(p.getUUID(), now), cap) * 0.03f;
            }
            case "deep_miner" -> {
                if (lv >= 10 && p.getY() < 32 && isDeepBlock(event.getState()))
                    mult += (lv >= 30 ? 0.30f : lv >= 20 ? 0.20f : 0.10f);
            }
            case "unstoppable" -> {
                float hardness = event.getPosition()
                        .map(pos -> event.getState().getDestroySpeed(p.level(), pos)).orElse(0f);
                if (lv >= 10 && hardness >= HARD_BLOCK)
                    mult += (lv >= 30 ? 0.30f : lv >= 20 ? 0.20f : 0.10f);
            }
        }
        if (mult != 1f) event.setNewSpeed(event.getNewSpeed() * mult);
    }

    public static boolean sparesAoeWear(ItemStack tool, LivingEntity owner) {
        if (!PlayerBlockBreaks.isBreaking() || !mastery(tool).equals("unstoppable")) return false;
        int lv = MasteryLevel.of(tool);
        return owner.getRandom().nextFloat() < (lv >= 30 ? 0.50f : lv >= 20 ? 0.35f : 0.20f);
    }

    public static void afterBlockBreak(ServerPlayer p, BlockPos pos, BlockState state) {
        ItemStack tool = p.getMainHandItem();
        String m = mastery(tool);
        if (m.isEmpty()) return;
        int lv = MasteryLevel.of(tool);
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();

        switch (m) {
            case "momentum" -> MasteryStreak.mine(p.getUUID(), now, STREAK_TICKS);
            case "lumber_rhythm" -> { if (state.is(BlockTags.LOGS)) MasteryStreak.mine(p.getUUID(), now, STREAK_TICKS); }
            case "harvest_chain" -> { if (state.is(BlockTags.CROPS)) MasteryStreak.mine(p.getUUID(), now, STREAK_TICKS); }
            case "deep_miner" -> {
                if (lv >= 20 && state.is(Tags.Blocks.ORES) && p.getRandom().nextFloat() < 0.20f)
                    tool.setDamageValue(Math.max(0, tool.getDamageValue() - 1));
                if (lv >= 30 && p.getY() < 0 && state.is(Tags.Blocks.ORES))
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1));
            }
            case "vein_seeker", "chain_reaction", "motherlode" -> {
                if (!state.is(Tags.Blocks.ORES)) return;
                int limit = lv >= 30 ? 32 : lv >= 20 ? 16 : 8;
                int range = m.equals("chain_reaction") ? (lv >= 30 ? 3 : 2) : 1;
                int broken = flood(level, p, pos,
                        s -> s.is(Tags.Blocks.ORES) && (m.equals("chain_reaction") || s.getBlock() == state.getBlock()),
                        limit, range);
                if (m.equals("motherlode") && broken >= 6) {
                    int extra = broken / 6 + (lv >= 30 ? 1 : 0);
                    for (int i = 0; i < extra; i++) Block.dropResources(state, level, pos);
                }
            }
            case "timberfall", "falling_giant" -> {
                if (!state.is(BlockTags.LOGS)) return;
                flood(level, p, pos, s -> s.is(BlockTags.LOGS), lv >= 30 ? 64 : lv >= 20 ? 32 : 16, 1);
                if (m.equals("falling_giant") && lv >= 20 && p.getRandom().nextFloat() < 0.3f)
                    tool.setDamageValue(Math.max(0, tool.getDamageValue() - 1));
            }
            case "precision_felling" -> {
                if (state.is(BlockTags.LOGS))
                    flood(level, p, pos, s -> s.is(BlockTags.LOGS) && s.getBlock() == state.getBlock(),
                            lv >= 30 ? 32 : lv >= 20 ? 16 : 8, 1);
            }
            case "groundworker" -> {
                List<BlockPos> area = MasteryAoe.square(level, pos, BlockSideHitHandler.getSideHit(p),
                        lv >= 30 ? 2 : 1, MasteryAoe::isGroundwork);
                MasteryAoe.breakBlocks(p, tool, area, MasteryAoe::isGroundwork);
            }
            case "reaper", "harvest_sweep", "replanter", "green_thumb" -> {
                int r = m.equals("reaper") ? (lv >= 30 ? 4 : lv >= 20 ? 3 : 2) : (lv >= 30 ? 3 : lv >= 20 ? 2 : 1);
                List<BlockPos> area = MasteryAoe.square(level, pos, Direction.UP, r, MasteryAoe::isMatureCrop);
                MasteryAoe.harvestCrops(level, p, tool, area, m.equals("replanter"), m.equals("green_thumb"));
            }
            case "aftershock" -> {
                if (lv < 10) return;
                BlockPos back = pos.relative(BlockSideHitHandler.getSideHit(p).getOpposite());
                List<BlockPos> list = new ArrayList<>();
                list.add(back);
                if (lv >= 30) { list.add(back.above()); list.add(back.below()); }
                MasteryAoe.breakBlocks(level, p, tool, list, false);
            }
        }
    }

    private static int flood(Level level, ServerPlayer player, BlockPos origin,
                             Predicate<BlockState> match, int limit, int range) {
        ItemStack tool = player.getMainHandItem();
        int broken = 0;
        Set<BlockPos> vis = new HashSet<>();
        Queue<BlockPos> q = new ArrayDeque<>();
        vis.add(origin);
        q.add(origin);
        while (!q.isEmpty() && broken < limit) {
            BlockPos cur = q.poll();
            for (int dx = -range; dx <= range; dx++) for (int dy = -range; dy <= range; dy++) for (int dz = -range; dz <= range; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos np = cur.offset(dx, dy, dz);
                if (!vis.add(np) || !level.isInWorldBounds(np) || !level.hasChunkAt(np)) continue;
                if (match.test(level.getBlockState(np)) && PlayerBlockBreaks.breakExtra(player, tool, np)) {
                    broken++;
                    q.add(np);
                    if (broken >= limit) return broken;
                }
            }
        }
        return broken;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        ItemStack tool = ToolCombat.tool(event.getEntity(), event.getSource());
        String m = mastery(tool);
        if (m.isEmpty()) return;
        int lv = MasteryLevel.of(tool);
        LivingEntity target = event.getEntity();
        long now = p.level().getGameTime();
        float amount = event.getAmount(), bonus = 1f;
        switch (m) {
            case "backstab" -> {
                Vec3 look = target.getLookAngle().normalize();
                Vec3 toA = p.position().subtract(target.position()).normalize();
                if (look.dot(toA) < -0.4) bonus += lv >= 30 ? 0.75f : lv >= 20 ? 0.50f : 0.25f;
            }
            case "assassin" -> {
                if (MasteryStreak.nextCombatCount(p.getUUID(), target.getId(), now) == 1)
                    bonus += lv >= 30 ? 1.0f : lv >= 20 ? 0.7f : 0.5f;
            }
            case "flurry" -> {
                int h = MasteryStreak.nextCombatCount(p.getUUID(), target.getId(), now);
                bonus += Math.min(h, lv >= 30 ? 8 : lv >= 20 ? 5 : 3) * 0.06f;
            }
            case "duelist" -> {
                int h = MasteryStreak.nextCombatCount(p.getUUID(), target.getId(), now);
                bonus += Math.min(h, lv >= 30 ? 10 : lv >= 20 ? 6 : 4) * 0.05f;
            }
            case "executioner" -> {
                float pct = target.getHealth() / target.getMaxHealth();
                float thr = lv >= 30 ? 0.50f : lv >= 20 ? 0.40f : 0.30f;
                if (pct < thr) bonus += (thr - pct) / thr * (lv >= 30 ? 1.5f : 1.0f);
            }
            case "butcher" -> bonus += Math.min(target.getMaxHealth() / 20f, 1f) * (lv >= 30 ? 0.75f : lv >= 20 ? 0.5f : 0.3f);
            case "crushing_blow" -> {
                if (p.getAttackStrengthScale(0.5f) > 0.95f) bonus += lv >= 30 ? 0.6f : lv >= 20 ? 0.4f : 0.25f;
            }
            case "bloodlust" -> bonus += Math.min(MasteryStreak.killCount(p.getUUID(), now), lv >= 30 ? 8 : lv >= 20 ? 5 : 3) * 0.08f;
            case "blade_dance" -> bonus += Math.min(MasteryStreak.killCount(p.getUUID(), now)
                    + MasteryStreak.nextCombatCount(p.getUUID(), target.getId(), now), lv >= 30 ? 10 : 6) * 0.05f;
            case "war_axe" -> bonus += lv >= 30 ? 0.5f : lv >= 20 ? 0.35f : 0.2f;
            case "hatchet_master" -> bonus += lv >= 30 ? 0.4f : lv >= 20 ? 0.28f : 0.18f;
        }
        if (bonus != 1f) event.setAmount(amount * bonus);
    }

    @SubscribeEvent
    public static void onDamageResolved(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || !(event.getSource().getEntity() instanceof Player p)) return;
        ItemStack tool = ToolCombat.tool(event.getEntity(), event.getSource());
        String m = mastery(tool);
        if (m.isEmpty()) return;
        int lv = MasteryLevel.of(tool);
        LivingEntity target = event.getEntity();
        switch (m) {
            case "assassin", "flurry", "duelist", "blade_dance" ->
                    MasteryStreak.combat(p.getUUID(), target.getId(), p.level().getGameTime(), COMBAT_TICKS);
            case "crushing_blow" -> {
                if (p.getAttackStrengthScale(0.5f) > 0.95f)
                    target.knockback(lv >= 30 ? 1.2f : 0.8f, p.getX() - target.getX(), p.getZ() - target.getZ());
            }
            case "war_axe" -> target.knockback(lv >= 30 ? 0.8f : 0.5f, p.getX() - target.getX(), p.getZ() - target.getZ());
            case "death_sweep" -> {
                if (p.level() instanceof ServerLevel serverLevel) {
                    ToolCombat.secondary(() -> {
                        double radius = lv >= 30 ? 3.5 : lv >= 20 ? 2.5 : 2.0;
                        float fraction = lv >= 30 ? 0.6f : lv >= 20 ? 0.45f : 0.3f;
                        DamageSource source = p.damageSources().playerAttack(p);
                        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                new AABB(target.blockPosition()).inflate(radius))) {
                            if (entity != target && entity != p && entity.isAlive())
                                entity.hurt(source, event.getOriginalDamage() * fraction);
                        }
                    });
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        ToolCombat.killed(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MasteryStreak.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MasteryStreak.clearAll();
    }

    public static void onMeleeKill(ItemStack tool, ServerPlayer player) {
        String m = mastery(tool);
        if (m.equals("bloodlust") || m.equals("blade_dance"))
            MasteryStreak.kill(player.getUUID(), player.level().getGameTime(), COMBAT_TICKS);
    }
}