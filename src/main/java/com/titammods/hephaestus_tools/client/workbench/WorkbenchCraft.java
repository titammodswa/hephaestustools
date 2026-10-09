package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.hephaestus_tools.registry.ModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import net.minecraft.world.item.ItemStack;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public final class WorkbenchCraft {
    private static final boolean DRAWER = false;
    private static final double HAMMER_START_TINKER = 0.9, HAMMER_START_BUILD = DRAWER ? 1.30 : 0.9, TAIL = 3.2, MOVE = 0.7, GRAB = 0.45, RAISE = 0.60;

    private static boolean active;
    private static double time;
    private static double mixRaw;
    private static int strikes;
    private static double strikePulse, pulseSmooth;
    @Nullable private static Runnable onFinish;
    private static ItemStack subject = ItemStack.EMPTY;
    private static ItemStack hammer = ItemStack.EMPTY;
    private static double takeRaw;
    private static boolean tinker;
    private static boolean animPending, animDriven;
    private static boolean quick;
    private static final int QUICK_STRIKES = 2;
    private static final double HAMMER_START_QUICK = 0.55;
    private static final double PHASE0 = 0.35, HANDOFF = 0.25;
    private static final double PUT_DOWN = 0.55;
    private static final double DRAWER_OPEN_AT = 0.45, DRAWER_OPEN_DUR = 0.22, HAMMER_TAKEN = DRAWER ? 0.82 : 0.30, DRAWER_CLOSE_AT = 0.92, DRAWER_CLOSE_DUR = 0.18;
    private static ItemStack infuse = ItemStack.EMPTY;
    private static int kind = WorkbenchAssembly.KIND_METAL;
    private static double polishTime, polishSpark, ambientTimer = 2.0, glowTimer;
    private static final double INFUSE_START = 0.20, INFUSE_END = 1.05, POLISH = 0.9;

    private WorkbenchCraft() {}

    static void reset() {
        active = false;
        time = 0.0;
        mixRaw = 0.0;
        strikes = 0;
        strikePulse = 0.0;
        pulseSmooth = 0.0;
        onFinish = null;
        if (animDriven) WorkbenchPlayerAnim.stop(Minecraft.getInstance().player);
        animPending = false;
        animDriven = false;
        subject = ItemStack.EMPTY;
        takeRaw = 0.0;
        infuse = ItemStack.EMPTY;
        polishTime = 0.0;
    }

    public static boolean start(Runnable finish) { return start(ItemStack.EMPTY, finish); }

    public static boolean start(ItemStack result, Runnable finish) { return start(result, ItemStack.EMPTY, finish); }

    public static boolean startTinker(ItemStack tool, ItemStack material, Runnable finish) {
        if (active || !WorkbenchSession.isSceneActive()) return false;
        boolean ok = start(tool, ItemStack.EMPTY, finish);
        quick = ok;
        return ok;
    }

    public static boolean isQuick() { return active && quick; }

    public static boolean isTinker() { return active && tinker; }

    public static boolean start(ItemStack result, ItemStack material, Runnable finish) {
        if (!active && WorkbenchSession.isSceneActive()) {
            tinker = false;
            quick = false;
            subject = result == null ? ItemStack.EMPTY : result.copy();
            infuse = material == null ? ItemStack.EMPTY : material.copy();
            kind = WorkbenchAssembly.materialKind(subject);
        }
        if (active || !WorkbenchSession.isSceneActive()) return false;
        active = true;
        time = 0.0;
        strikes = 0;
        onFinish = finish;
        animPending = true;
        WorkbenchAssembly.gatherAll();
        return true;
    }

    static void frame(double dt) {
        double target = active ? 1.0 : 0.0;
        mixRaw += (target - mixRaw) * (1.0 - Math.exp(-(active ? 2.6 : 5.0) * dt));
        if (Math.abs(target - mixRaw) < 1.0e-3) mixRaw = target;
        strikePulse *= Math.exp(-9.0 * dt);
        pulseSmooth += (strikePulse - pulseSmooth) * (1.0 - Math.exp(-16.0 * dt));
        morphPulse *= Math.exp(-4.5 * dt);
        double takeTarget = active && handT() >= GRAB * 0.5 && sinceTail() < TAIL - PUT_DOWN && !subject.isEmpty() ? 1.0 : 0.0;
        takeRaw += (takeTarget - takeRaw) * (1.0 - Math.exp(-3.2 * dt));
        angleSmooth += (rawHammerAngle() - angleSmooth) * (1.0 - Math.exp(-34.0 * dt));

        ambient(dt);
        polishFrame(dt);
        if (animDriven && !active && !WorkbenchPlayerAnim.isPlaying(Minecraft.getInstance().player)) animDriven = false;

        if (!active) return;
        if (animPending) {
            animPending = false;
            animDriven = WorkbenchPlayerAnim.play(Minecraft.getInstance().player, quick);
        }
        double before = time;
        time += dt;
        if (!infuse.isEmpty() && before < INFUSE_END && time >= INFUSE_END) infuseFx();
        if (!infuse.isEmpty() && before < INFUSE_START && time >= INFUSE_START) whoosh(0.0);
        if (!tinker && !quick && before < anvilEnd() && time >= anvilEnd()) whoosh(0.0);
        if (DRAWER && !tinker && before < DRAWER_OPEN_AT && time >= DRAWER_OPEN_AT) drawerSound(true);
        if (DRAWER && !tinker && before < DRAWER_CLOSE_AT + 0.12 && time >= DRAWER_CLOSE_AT + 0.12) drawerSound(false);
        double grabAt = duration() - TAIL + HANDOFF + GRAB;
        if (!subject.isEmpty() && before < grabAt && time >= grabAt)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.WB_CRAFT_DONE.get(), 1.0f, 0.7f));
        if (!infuse.isEmpty() && raiseWeight() > 0.3f) {
            glowTimer -= dt;
            if (glowTimer <= 0.0) { glowTimer = 0.5; glowFx(); }
        }
        double rate = tinker ? WorkbenchTuning.TINKER_TAP_RATE.get() : WorkbenchTuning.CRAFT_STRIKE_RATE.get();
        double windowEnd = duration() - TAIL;
        if (time >= hammerStart() && time <= windowEnd) {
            int n = (int) Math.floor((time - hammerStart()) * rate + PHASE0);
            if (n > strikes) {
                strikes = n;
                if (!moving() && !animDriven) strike();
            }
        }
        if (time >= duration()) {
            active = false;
            Runnable finish = onFinish;
            onFinish = null;
            if (finish != null) finish.run();
        }
    }

    private static double hammerStart() { return tinker ? HAMMER_START_TINKER : quick ? HAMMER_START_QUICK : HAMMER_START_BUILD; }

    private static double duration() {
        if (tinker) return WorkbenchTuning.TINKER_SECONDS.get();
        if (quick) return HAMMER_START_QUICK + (QUICK_STRIKES - PHASE0) / WorkbenchTuning.CRAFT_STRIKE_RATE.get() + 0.12 + TAIL;
        return WorkbenchTuning.CRAFT_SECONDS.get() + (HAMMER_START_BUILD - HAMMER_START_TINKER);
    }

    public static float tinkerTurn() {
        if (!isTinker()) return 0.0f;
        double t = Mth.clamp(time - hammerStart(), 0.0, duration() - TAIL - hammerStart());
        double step = Math.floor(t / 1.1);
        double f = Mth.clamp((t - step * 1.1) / 0.35, 0.0, 1.0);
        return (float) ((step + f * f * (3.0 - 2.0 * f)) * 90.0);
    }

    private static double anvilEnd() {
        if (quick) return hammerStart();
        return hammerStart() + (duration() - TAIL - hammerStart()) * WorkbenchTuning.CRAFT_ANVIL_SHARE.get();
    }

    private static double sinceTail() { return time - (duration() - TAIL); }

    private static double handT() { return sinceTail() - HANDOFF; }

    public static boolean holdsResult() { return active && !subject.isEmpty() && handT() >= GRAB && sinceTail() < TAIL - 0.18; }

    public static boolean holdsHammer() { return active && !tinker && time >= HAMMER_TAKEN && sinceTail() < HANDOFF; }

    public static boolean hammerInDrawer() { return DRAWER && active && !tinker && time < HAMMER_TAKEN && time > DRAWER_OPEN_AT + 0.1; }

    public static boolean drawerEnabled() { return DRAWER; }

    public static float drawerOpen() {
        if (!DRAWER || !active || tinker) return 0.0f;
        if (time < DRAWER_CLOSE_AT) {
            float t = (float) Mth.clamp((time - DRAWER_OPEN_AT) / DRAWER_OPEN_DUR, 0.0, 1.0);
            float u = t - 1.0f;
            return Math.min(1.04f, 1.0f + 2.0f * u * u * u + 1.0f * u * u);
        }
        float t = (float) Mth.clamp((time - DRAWER_CLOSE_AT) / DRAWER_CLOSE_DUR, 0.0, 1.0);
        return 1.0f - t * t * t;
    }

    public static float drawerReach() {
        if (!DRAWER || !active || tinker) return 0.0f;
        return bump(DRAWER_OPEN_AT + 0.12, HAMMER_TAKEN + 0.22);
    }

    public static float drawerLeftReach() {
        if (!DRAWER || !active || tinker) return 0.0f;
        return Math.max(bump(DRAWER_OPEN_AT - 0.22, DRAWER_OPEN_AT + DRAWER_OPEN_DUR + 0.10), bump(DRAWER_CLOSE_AT - 0.06, DRAWER_CLOSE_AT + DRAWER_CLOSE_DUR + 0.12));
    }

    private static float bump(double from, double to) {
        return (float) Math.sin(Math.PI * Mth.clamp((time - from) / (to - from), 0.0, 1.0));
    }

    public static ItemStack handItem() {
        if (holdsResult()) return subject;
        if (holdsHammer()) {
            if (hammer.isEmpty()) hammer = new ItemStack(ModItems.SLEDGE_HAMMER.get());
            return hammer;
        }
        return ItemStack.EMPTY;
    }

    public static float reachWeight() {
        if (!active || subject.isEmpty() || handT() < 0.0) return 0.0f;
        float in = WorkbenchSession.smootherStep((float) Mth.clamp(handT() / GRAB, 0.0, 1.0));
        float out = WorkbenchSession.smootherStep((float) Mth.clamp((sinceTail() - (TAIL - 0.18)) / 0.18, 0.0, 1.0));
        return in * (1.0f - out);
    }

    public static float raiseWeight() {
        if (!active || subject.isEmpty() || handT() < GRAB) return 0.0f;
        float t = (float) Mth.clamp((handT() - GRAB) / RAISE, 0.0, 1.0);
        float u = t - 1.0f;
        float up = 1.0f + 2.4f * u * u * u + 1.4f * u * u;
        float down = WorkbenchSession.smootherStep((float) Mth.clamp((sinceTail() - (TAIL - PUT_DOWN)) / (PUT_DOWN - 0.18), 0.0, 1.0));
        return up * (1.0f - down);
    }

    public static float admireTurn() {
        float r = raiseWeight();
        return 0.0f;
    }

    public static float takeMix() { return WorkbenchSession.smootherStep((float) takeRaw); }

    private static boolean moving() { return active && !tinker && !quick && time > anvilEnd() && time < anvilEnd() + MOVE; }

    public static float stationMix() {
        if (!active || tinker || quick) return 1.0f;
        return WorkbenchSession.smootherStep((float) Mth.clamp((time - anvilEnd()) / MOVE, 0.0, 1.0));
    }

    public static float carryAmount() {
        float s = stationMix();
        return moving() ? 4.0f * s * (1.0f - s) : 0.0f;
    }

    public static float heat() {
        if (!active || kind == WorkbenchAssembly.KIND_WOOD || kind == WorkbenchAssembly.KIND_BONE) return 0.0f;
        return 1.0f - stationMix();
    }

    public static float infuseProgress() {
        if (!active || infuse.isEmpty() || time < INFUSE_START || time > INFUSE_END) return -1.0f;
        return (float) ((time - INFUSE_START) / (INFUSE_END - INFUSE_START));
    }

    public static ItemStack infuseItem() { return infuse; }

    public static float flightStation() { return tinker || quick ? 1.0f : 0.0f; }

    public static void polish() {
        polishTime = POLISH;
        polishSpark = 0.0;
        Vec3 p = padPoint();
        Minecraft mc = Minecraft.getInstance();
        if (p != null && mc.level != null)
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1.1f, false);
    }

    public static float polishWeight() {
        return polishTime > 0.0 ? (float) Math.sin(Math.PI * (1.0 - polishTime / POLISH)) : 0.0f;
    }

    @Nullable
    private static Vec3 padPoint() {
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (pos == null || state == null) return null;
        return WorkbenchScene.workPoint(pos, state, 1.0, WorkbenchScene.RESULT_HEIGHT - WorkbenchScene.TABLE_TOP);
    }

    private static void polishFrame(double dt) {
        if (polishTime <= 0.0) return;
        polishTime -= dt;
        polishSpark -= dt;
        Minecraft mc = Minecraft.getInstance();
        Vec3 p = padPoint();
        if (polishSpark > 0.0 || p == null || mc.level == null) return;
        polishSpark = 0.11;
        for (int i = 0; i < 2; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            mc.level.addParticle(ParticleTypes.CRIT, p.x, p.y, p.z, Math.cos(a) * 0.12, 0.05, Math.sin(a) * 0.12);
        }
    }

    private static void ambient(double dt) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (!WorkbenchSession.isSceneActive() || mc.level == null || pos == null || state == null) return;
        Vec3 anvil = WorkbenchScene.workPoint(pos, state, 0.0, 0.05);
        ambientTimer -= dt;
        if (ambientTimer <= 0.0) {
            ambientTimer = 1.8 + mc.level.getRandom().nextDouble() * 2.6;
            mc.level.playLocalSound(anvil.x, anvil.y, anvil.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.22f, 0.85f + mc.level.getRandom().nextFloat() * 0.3f, false);
        }
    }

    private static void drawerSound(boolean open) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.worldPoint(pos, state, 0.45, 0.45, 0.6);
        mc.level.playLocalSound(p.x, p.y, p.z, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS,
                0.7f, open ? 1.25f : 1.35f, false);
    }

    private static void whoosh(double station) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.workPoint(pos, state, station, 0.3);
        mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.WB_WHOOSH.get(), SoundSource.BLOCKS, 0.6f,
                0.95f + mc.level.getRandom().nextFloat() * 0.1f, false);
    }

    private static void tap() {
        strikePulse = 0.35;
        Minecraft mc = Minecraft.getInstance();
        Vec3 p = padPoint();
        if (mc.level == null || p == null) return;
        mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.WB_TINKER_TAP.get(), SoundSource.BLOCKS, 0.55f,
                0.9f + mc.level.getRandom().nextFloat() * 0.25f, false);
        if (strikes % 3 == 0)
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.18f, 1.6f, false);
        for (int i = 0; i < 1; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            mc.level.addParticle(infuse.isEmpty() ? ParticleTypes.ENCHANT : ParticleTypes.ELECTRIC_SPARK,
                    p.x + Math.cos(a) * 0.12, p.y + 0.05, p.z + Math.sin(a) * 0.12, Math.cos(a) * 0.03, 0.04, Math.sin(a) * 0.03);
        }
    }

    private static void infuseFx() {
        strikePulse = 0.6;
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.workPoint(pos, state, flightStation(), 0.08);
        mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.WB_INFUSE.get(), SoundSource.BLOCKS, 0.9f, 1.0f, false);
        mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.3f, 1.3f, false);
        for (int i = 0; i < 6; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            mc.level.addParticle(ParticleTypes.ENCHANT, p.x + Math.cos(a) * 0.4, p.y + 0.3, p.z + Math.sin(a) * 0.4,
                    -Math.cos(a) * 0.4, -0.2, -Math.sin(a) * 0.4);
        }
        for (int i = 0; i < 1; i++)
            mc.level.addParticle(ParticleTypes.END_ROD, p.x, p.y + 0.05, p.z,
                    (mc.level.getRandom().nextDouble() - 0.5) * 0.06, 0.03, (mc.level.getRandom().nextDouble() - 0.5) * 0.06);
    }

    private static void glowFx() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.takeLookTarget(pos, state);
        mc.level.addParticle(ParticleTypes.END_ROD,
                p.x + (mc.level.getRandom().nextDouble() - 0.5) * 0.35, p.y + (mc.level.getRandom().nextDouble() - 0.3) * 0.35,
                p.z + (mc.level.getRandom().nextDouble() - 0.5) * 0.35, 0.0, 0.01, 0.0);
    }

    private static void strike() {
        if (tinker) { tap(); return; }
        strikePulse = 1.0;
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        float station = stationMix();
        boolean anvil = station < 0.5f;
        Vec3 p = WorkbenchScene.workPoint(pos, state, station, 0.075);
        if (anvil) {
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.40f,
                    0.95f + mc.level.getRandom().nextFloat() * 0.15f, false);
        } else {
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35f,
                    1.35f + mc.level.getRandom().nextFloat() * 0.2f, false);
        }
        ParticleOptions bits = switch (kind) {
            case WorkbenchAssembly.KIND_WOOD -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());
            case WorkbenchAssembly.KIND_STONE -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            case WorkbenchAssembly.KIND_BONE -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
            default -> ParticleTypes.CRIT;
        };
        for (int i = 0; i < (anvil ? 6 : 4); i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            double s = 0.06 + mc.level.getRandom().nextDouble() * 0.08;
            mc.level.addParticle(bits, p.x, p.y + 0.03, p.z,
                    Math.cos(a) * s, 0.08 + mc.level.getRandom().nextDouble() * 0.10, Math.sin(a) * s);
        }
        if (kind != WorkbenchAssembly.KIND_METAL)
            mc.level.playLocalSound(p.x, p.y, p.z, kind == WorkbenchAssembly.KIND_WOOD ? SoundEvents.WOOD_HIT
                    : kind == WorkbenchAssembly.KIND_BONE ? SoundEvents.BONE_BLOCK_HIT : SoundEvents.STONE_HIT,
                    SoundSource.BLOCKS, 0.7f, 0.9f, false);
    }

    public static boolean isActive() { return active; }

    public static float mix() { return WorkbenchSession.smootherStep((float) mixRaw); }

    public static boolean isBusy() { return active || mixRaw > 0.08; }

    public static float progress() { return active ? (float) Mth.clamp(time / duration(), 0.0, 1.0) : 0.0f; }

    public static float strikePulse() { return (float) strikePulse; }

    public static void animStrike() { if (active) strike(); }

    public static float strikePulseSmooth() { return (float) pulseSmooth; }

    public static boolean showResult() { return active && time >= duration() - TAIL; }

    public static float poseWeight() {
        if (animDriven) return 0.0f;
        return WorkbenchSession.smootherStep((float) Mth.clamp((mixRaw - 0.25) / 0.75, 0.0, 1.0));
    }

    public static float hammerAngle() { return (float) angleSmooth; }

    private static double morphPulse;
    public static void bumpMorph() {
        morphPulse = 1.0;
        Minecraft mc = Minecraft.getInstance();
        Vec3 p = padPoint();
        if (mc.level == null || p == null) return;
        mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.WB_HOLO_BLIP.get(), SoundSource.BLOCKS, 0.8f, 1.25f, false);
        for (int i = 0; i < 5; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            mc.level.addParticle(ParticleTypes.ENCHANT, p.x + Math.cos(a) * 0.35, p.y + 0.25, p.z + Math.sin(a) * 0.35,
                    -Math.cos(a) * 0.3, 0.1, -Math.sin(a) * 0.3);
        }
    }
    public static float morph() { return (float) morphPulse; }

    private static double angleSmooth = -1.05;

    private static float rawHammerAngle() { return rawHammerAngleAt(time); }

    public static float hammerAngleLag(double delay) {
        float rest = (float) WorkbenchTuning.ARM_PITCH.get();
        if (!active) return rest;
        double sum = 0.0;
        for (int i = -3; i <= 3; i += 2) sum += rawHammerAngleAt(Math.max(0.0, time - delay + i * 0.015));
        return (float) (sum / 4.0);
    }

    public static int swingIndex() {
        if (!active || tinker) return 0;
        return (int) Math.floor(Math.max(0.0, time - hammerStart()) * WorkbenchTuning.CRAFT_STRIKE_RATE.get() + PHASE0);
    }

    private static float rnd(int n, int salt) {
        double v = Math.sin(n * 12.9898 + salt * 78.233) * 43758.5453;
        return (float) ((v - Math.floor(v)) * 2.0 - 1.0);
    }

    public static float swingRand(int salt) { return rnd(swingIndex(), salt); }

    private static boolean movingAt(double t) { return active && !tinker && !quick && t > anvilEnd() && t < anvilEnd() + MOVE; }

    private static float rawHammerAngleAt(double time) {
        float rest = (float) WorkbenchTuning.ARM_PITCH.get();
        double rate = WorkbenchTuning.CRAFT_STRIKE_RATE.get();
        double windowEnd = duration() - TAIL;
        float ready = swingAt(PHASE0);
        if (!active || time < hammerStart()) return active ? ready : rest;
        if (movingAt(time)) return ready;
        if (time > windowEnd) {
            float from = swingAt((windowEnd - hammerStart()) * rate + PHASE0);
            float k = (float) Mth.clamp((time - windowEnd) / (HANDOFF + 0.15), 0.0, 1.0);
            return Mth.lerp(k * k * (3.0f - 2.0f * k), from, rest);
        }
        return swingAt((time - hammerStart()) * rate + PHASE0);
    }

    private static float swingAt(double phase) {
        float up = -2.30f, down = -1.05f;
        float f = (float) (phase - Math.floor(phase));
        up += 0.12f * rnd((int) Math.floor(phase), 1);
        if (f < 0.12f) {
            float k = f / 0.12f;
            float sn = (float) Math.sin(k * Math.PI);
            return down - 0.22f * sn * sn;
        }
        if (f < 0.58f) {
            float k = (f - 0.12f) / 0.46f;
            k = k * k * (3.0f - 2.0f * k);
            return Mth.lerp(k, down, up);
        }
        if (f < 0.72f) {
            return up - 0.04f * (float) Math.sin((f - 0.58f) / 0.14f * Math.PI);
        }
        float k = (f - 0.72f) / 0.28f;
        return Mth.lerp(k * k * k, up, down);
    }
}
