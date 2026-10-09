package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class RemoteCraft {
    private static final double HAMMER_START = 0.9, HAMMER_START_QUICK = 0.55, PHASE0 = 0.35, HANDOFF = 0.25, HAMMER_TAKEN = 0.30,
            TAIL = 3.2, GRAB = 0.45, RAISE = 0.60, PUT_DOWN = 0.55;

    private static final class Entry {
        BlockPos pos;
        boolean tinker;
        ItemStack subject;
        long startNanos;
        int strikes;
        double pulse;
        boolean anim;
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static ItemStack hammer = ItemStack.EMPTY;

    private RemoteCraft() {}

    public static void start(int entityId, BlockPos pos, boolean tinker, ItemStack subject) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == entityId) return;
        Entry e = new Entry();
        e.pos = pos;
        e.tinker = tinker;
        e.subject = subject == null ? ItemStack.EMPTY : subject;
        e.startNanos = System.nanoTime();
        ENTRIES.put(entityId, e);
        if (mc.level != null && mc.level.getEntity(entityId) instanceof net.minecraft.world.entity.Avatar player)
            e.anim = WorkbenchPlayerAnim.play(player, tinker);
    }

    public static void clear() { ENTRIES.clear(); }

    public static boolean isActive(int entityId) { return ENTRIES.containsKey(entityId); }

    public static boolean isAnimated(int entityId) {
        Entry e = ENTRIES.get(entityId);
        return e != null && e.anim;
    }

    private static double hammerStart(Entry e) { return e.tinker ? HAMMER_START_QUICK : HAMMER_START; }

    private static double time(Entry e) { return (System.nanoTime() - e.startNanos) / 1.0e9; }

    private static double duration(Entry e) {
        if (e.tinker) return HAMMER_START_QUICK + (2.0 - PHASE0) / WorkbenchTuning.CRAFT_STRIKE_RATE.get() + 0.12 + TAIL;
        return WorkbenchTuning.CRAFT_SECONDS.get();
    }

    private static double since(Entry e) { return time(e) - (duration(e) - TAIL); }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ENTRIES.clear(); return; }
        Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            var me = it.next();
            Entry e = me.getValue();
            double t = time(e);
            if (t > duration(e) + 0.3 || mc.level.getEntity(me.getKey()) == null) { it.remove(); continue; }
            e.pulse *= 0.6;
            if (e.anim) continue;
            double rate = WorkbenchTuning.CRAFT_STRIKE_RATE.get();
            double end = duration(e) - TAIL;
            if (t < hammerStart(e) || t > end) continue;
            int n = (int) Math.floor((t - hammerStart(e)) * rate + PHASE0);
            if (n <= e.strikes) continue;
            e.strikes = n;
            blow(e, t);
        }
    }

    public static void animStrike(int entityId) {
        Entry e = ENTRIES.get(entityId);
        if (e != null) blow(e, time(e));
    }

    private static void blow(Entry e, double t) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        e.pulse = 1.0;
        double end = duration(e) - TAIL;
        BlockState state = mc.level.getBlockState(e.pos);
        double share = WorkbenchTuning.CRAFT_ANVIL_SHARE.get();
        double station = e.tinker ? 1.0 : (t < hammerStart(e) + (end - hammerStart(e)) * share ? 0.0 : 1.0);
        Vec3 p = WorkbenchScene.workPoint(e.pos, state, station, 0.075);
        mc.level.playLocalSound(p.x, p.y, p.z, station < 0.5 ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_PLACE,
                SoundSource.PLAYERS, 0.4f, 1.1f + mc.level.getRandom().nextFloat() * 0.2f, false);
        for (int i = 0; i < 8; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            mc.level.addParticle(ParticleTypes.CRIT, p.x, p.y, p.z, Math.cos(a) * 0.1, 0.12, Math.sin(a) * 0.1);
        }
    }

    public static ItemStack handItem(int entityId) {
        Entry e = ENTRIES.get(entityId);
        if (e == null) return ItemStack.EMPTY;
        double s = since(e);
        if (s - HANDOFF >= GRAB && s < TAIL - 0.18 && !e.subject.isEmpty()) return e.subject;
        if (time(e) >= HAMMER_TAKEN && s < HANDOFF) {
            if (hammer.isEmpty()) hammer = new ItemStack(ModItems.SLEDGE_HAMMER.get());
            return hammer;
        }
        return ItemStack.EMPTY;
    }

    private static float smooth(double t) {
        float x = (float) Mth.clamp(t, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0f - 15.0f) + 10.0f);
    }

    public static void applyPose(int entityId, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        Entry e = ENTRIES.get(entityId);
        if (e == null) return;
        double t = time(e);
        float in = smooth(t / 0.6) * (1.0f - smooth((t - duration(e)) / 0.3 + 1.0));
        if (in <= 0.0f) return;
        double rate = WorkbenchTuning.CRAFT_STRIKE_RATE.get();
        float angle = -1.75f;
        double end = duration(e) - TAIL;
        if (t >= HAMMER_START && t <= end) {
            double ph = (t - HAMMER_START) * rate;
            float f = (float) (ph - Math.floor(ph));
            angle = f < 0.6f ? Mth.lerp(smooth(f / 0.6f), -1.05f, -2.30f)
                    : Mth.lerp(((f - 0.6f) / 0.4f) * ((f - 0.6f) / 0.4f), -2.30f, -1.05f);
        }
        rightArm.xRot = Mth.lerp(in, rightArm.xRot, angle);
        float rise = Mth.clamp((-1.05f - angle) / 1.25f, 0.0f, 1.0f);
        rightArm.yRot = Mth.lerp(in, rightArm.yRot, -0.10f + 0.45f * rise);
        rightArm.zRot = Mth.lerp(in, rightArm.zRot, 0.12f - 0.70f * rise);
        leftArm.xRot = Mth.lerp(in, leftArm.xRot, -1.0f);
        leftArm.yRot = Mth.lerp(in, leftArm.yRot, 0.30f);
        head.xRot = Mth.lerp(in, head.xRot, 0.45f);
        double s = since(e);
        if (s > GRAB && !e.subject.isEmpty()) {
            float raise = smooth((s - GRAB) / RAISE) * (1.0f - smooth((s - (TAIL - PUT_DOWN)) / (PUT_DOWN - 0.18)));
            rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -1.45f);
            rightArm.yRot = Mth.lerp(raise, rightArm.yRot, -0.50f);
            head.xRot = Mth.lerp(raise, head.xRot, 0.30f);
        }
    }
}
