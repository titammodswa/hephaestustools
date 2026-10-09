package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.client.workbench.WorkbenchCamera.Pose;
import com.titammods.hephaestus_tools.registry.ModSounds;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class WorkbenchAssembly {
    public static final int MAX = 4;
    private static final float[] HOME_F = {-0.27f, -0.27f, 0.13f, 0.13f};
    private static final float[] HOME_A = {-0.36f, 0.36f, -0.40f, 0.40f};
    private static final float PICK_RADIUS = 0.17f, ATTACH_RADIUS = 0.30f;
    private static final float LIMIT_F_MIN = -0.36f, LIMIT_F_MAX = 0.22f, LIMIT_A = 0.46f;

    private static final float[] posF = new float[MAX], posA = new float[MAX];
    private static final float[] restF = new float[MAX], restA = new float[MAX];
    private static final boolean[] present = new boolean[MAX], attached = new boolean[MAX];
    private static final String[] keys = {"", "", "", ""};
    private static int required;
    private static int dragging = -1, hover = -1;
    private static float dragF, dragA, grabF, grabA, pointerF, pointerA;
    private static double completeRaw;
    private static boolean hasSpot;
    private static float spotF, spotA;
    private static int spotFrames;
    private static ItemStack ghost = ItemStack.EMPTY, ghostSolid = ItemStack.EMPTY;
    private static double aimRaw;
    private static boolean wasComplete;
    private static final int[] order = new int[MAX];
    private static int orderCounter;
    private static float aimF, aimA;
    private static long autoAttachUntil;

    private WorkbenchAssembly() {}

    static void reset() {
        for (int i = 0; i < MAX; i++) {
            posF[i] = restF[i] = HOME_F[i];
            posA[i] = restA[i] = HOME_A[i];
            attached[i] = false;
            present[i] = false;
            keys[i] = "";
        }
        dragging = hover = -1;
        completeRaw = 0.0;
        required = 0;
        hasSpot = false;
        ghost = ItemStack.EMPTY;
        ghostSolid = ItemStack.EMPTY;
    }

    public static void setGhost(ItemStack stack) {
        ItemStack next = stack == null ? ItemStack.EMPTY : stack;
        if (!next.isEmpty() && next.getItem() != ghost.getItem())
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.WB_HOLO_BLIP.get(), 1.0f, 0.45f));
        ghost = next;
    }
    public static ItemStack ghost() { return ghost; }

    public static void setGhostSolid(ItemStack stack) { ghostSolid = stack == null ? ItemStack.EMPTY : stack; }
    public static ItemStack ghostSolid() { return ghostSolid; }
    public static int requiredCount() { return required; }

    public static void setDropSpot(float pf, float pa) {
        hasSpot = true;
        spotF = Mth.clamp(pf, LIMIT_F_MIN, LIMIT_F_MAX);
        spotA = Mth.clamp(pa, -LIMIT_A, LIMIT_A);
        spotFrames = 4;
    }

    public static void sync(List<ItemStack> inputs, int requiredParts) {
        required = requiredParts;
        for (int i = 0; i < MAX; i++) {
            ItemStack s = i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY;
            String key = s.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(s.getItem()) + "#" + s.getComponents().hashCode();
            present[i] = !s.isEmpty();
            if (key.equals(keys[i])) continue;
            boolean first = keys[i].isEmpty();
            keys[i] = key;
            attached[i] = false;
            if (dragging == i) dragging = -1;
            if (!present[i]) continue;
            if (hasSpot) {
                posF[i] = restF[i] = spotF;
                posA[i] = restA[i] = spotA;
                spotF = Mth.clamp(spotF - 0.10f, LIMIT_F_MIN, LIMIT_F_MAX);
            } else {
                posF[i] = restF[i] = HOME_F[i];
                posA[i] = restA[i] = HOME_A[i];
            }
            if (net.minecraft.util.Util.getMillis() < autoAttachUntil) {
                attached[i] = true;
                order[i] = ++orderCounter;
                restF[i] = 0.0f;
                restA[i] = 0.0f;
                attachFx(s);
            } else if (!first) landFx(i, s);
        }
        if (spotFrames > 0 && --spotFrames == 0) hasSpot = false;
    }

    static void tick(double dt) {
        double k = 1.0 - Math.exp(-14.0 * dt);
        for (int i = 0; i < MAX; i++) {
            if (!present[i]) continue;
            float tf, ta;
            if (dragging == i) { tf = dragF; ta = dragA; }
            else if (attached[i]) { tf = 0.0f; ta = 0.0f; }
            else { tf = restF[i]; ta = restA[i]; }
            posF[i] += (tf - posF[i]) * k;
            posA[i] += (ta - posA[i]) * k;
        }
        if (dragging >= 0) { aimF = posF[dragging]; aimA = posA[dragging]; }
        aimRaw += ((dragging >= 0 ? 1.0 : 0.0) - aimRaw) * (1.0 - Math.exp(-8.0 * dt));
        boolean nowComplete = isComplete();
        if (nowComplete && !wasComplete && !WorkbenchCraft.isActive()) popFx();
        wasComplete = nowComplete;
        double target = nowComplete ? 1.0 : 0.0;
        completeRaw += (target - completeRaw) * (1.0 - Math.exp(-7.0 * dt));
        if (Math.abs(target - completeRaw) < 1.0e-3) completeRaw = target;
    }

    public static float homeForward(int i) { return HOME_F[Mth.clamp(i, 0, MAX - 1)]; }
    public static float homeAcross(int i) { return HOME_A[Mth.clamp(i, 0, MAX - 1)]; }

    public static boolean isPresent(int i) { return i >= 0 && i < MAX && present[i]; }
    public static boolean isMerged(int i) { return i >= 0 && i < MAX && attached[i]; }
    public static boolean isDragging(int i) { return dragging == i; }
    public static boolean isHover(int i) { return hover == i; }
    public static float forward(int i) { return posF[i]; }
    public static float across(int i) { return posA[i]; }
    public static int mergedCount() {
        int n = 0;
        for (int i = 0; i < MAX; i++) if (present[i] && attached[i]) n++;
        return n;
    }
    public static int placedCount() {
        int n = 0;
        for (int i = 0; i < MAX; i++) if (present[i]) n++;
        return n;
    }

    public static boolean isComplete() {
        int have = placedCount();
        return required >= 2 && have == required && mergedCount() == have;
    }

    public static float completeMix() { return (float) completeRaw; }
    public static boolean hasParts() { return placedCount() > 0; }

    public static int pick(float pf, float pa) {
        int best = -1;
        float bestD = PICK_RADIUS * PICK_RADIUS;
        for (int i = 0; i < MAX; i++) {
            if (!present[i] || attached[i]) continue;
            float df = posF[i] - pf, da = posA[i] - pa;
            float d = df * df + da * da;
            if (d < bestD) { bestD = d; best = i; }
        }
        if (best >= 0) return best;
        if (pf * pf + pa * pa < ATTACH_RADIUS * ATTACH_RADIUS) {
            for (int i = 0; i < MAX; i++)
                if (present[i] && attached[i] && (best < 0 || order[i] > order[best])) best = i;
        }
        return best;
    }

    public static void setHover(int i) { hover = i; }

    public static void beginDrag(int i, float pf, float pa) {
        if (i < 0 || i >= MAX || !present[i]) return;
        dragging = i;
        attached[i] = false;
        grabF = posF[i] - pf;
        grabA = posA[i] - pa;
        dragF = posF[i];
        dragA = posA[i];
        pointerF = pf;
        pointerA = pa;
    }

    public static void drag(float pf, float pa) {
        if (dragging < 0) return;
        pointerF = pf;
        pointerA = pa;
        dragF = Mth.clamp(pf + grabF, LIMIT_F_MIN, LIMIT_F_MAX);
        dragA = Mth.clamp(pa + grabA, -LIMIT_A, LIMIT_A);
        float d = (float) Math.sqrt(pf * pf + pa * pa);
        if (d < ATTACH_RADIUS) {
            float pull = 1.0f - d / ATTACH_RADIUS;
            pull = pull * pull * (3.0f - 2.0f * pull);
            dragF *= 1.0f - 0.8f * pull;
            dragA *= 1.0f - 0.8f * pull;
        }
    }

    public static boolean isDraggingAny() { return dragging >= 0; }
    public static int draggingIndex() { return dragging; }

    public static float aimMix() { return (float) aimRaw; }

    @Nullable
    public static float[] aimAngles() {
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (pos == null || state == null) return null;
        Vec3 target = WorkbenchScene.worldPoint(pos, state, WorkbenchScene.RESULT_FORWARD + aimF,
                WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS + aimA, WorkbenchScene.PART_HEIGHT + 0.08);
        Vec3 shoulder = WorkbenchScene.playerPosition(pos, state).add(0.0, 1.35, 0.0);
        double dx = target.x - shoulder.x, dy = target.y - shoulder.y, dz = target.z - shoulder.z;
        double yawTo = Math.toDegrees(Math.atan2(-dx, dz));
        double pitchTo = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float rel = Mth.clamp(Mth.wrapDegrees((float) yawTo - WorkbenchScene.playerYaw(state)), -75.0f, 75.0f);
        float pitch = Mth.clamp((float) pitchTo, -10.0f, 80.0f);
        return new float[] {(float) (-Math.PI / 2.0 + Math.toRadians(pitch)), (float) Math.toRadians(rel)};
    }

    public static void detachFx(ItemStack part) { snapFx(part, false); }

    public static void attachFx(ItemStack part) { snapFx(part, true); }

    private static void snapFx(ItemStack part, boolean attach) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null || part.isEmpty()) return;
        Vec3 p = WorkbenchScene.worldPoint(pos, state, WorkbenchScene.RESULT_FORWARD,
                WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS, WorkbenchScene.RESULT_HEIGHT);
        String mat = materialKey(part);
        SoundEvent sound;
        float pitch;
        if (has(mat, "wood", "oak", "spruce", "birch", "jungle", "acacia", "cherry", "mangrove", "bamboo", "plank", "stick", "crimson", "warped")) {
            sound = SoundEvents.WOOD_PLACE; pitch = 1.1f;
        } else if (has(mat, "bone")) {
            sound = SoundEvents.BONE_BLOCK_PLACE; pitch = 1.2f;
        } else if (has(mat, "stone", "flint", "granite", "andesite", "diorite", "deepslate", "obsidian", "basalt", "blackstone", "cobble")) {
            sound = SoundEvents.STONE_PLACE; pitch = 1.0f;
        } else {
            sound = SoundEvents.CHAIN_PLACE; pitch = 1.3f;
            if (attach) mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.18f, 1.8f, false);
        }
        mc.level.playLocalSound(p.x, p.y, p.z, sound, SoundSource.BLOCKS, attach ? 0.6f : 0.5f, attach ? pitch : pitch * 0.75f, false);
        mc.level.playLocalSound(p.x, p.y, p.z, (attach ? ModSounds.WB_PART_SNAP : ModSounds.WB_PART_UNSNAP).get(), SoundSource.BLOCKS, 0.8f,
                0.95f + mc.level.getRandom().nextFloat() * 0.1f, false);
        if (!attach) return;
        ItemParticleOption bits = new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(part));
        for (int i = 0; i < 10; i++) {
            double a = mc.level.getRandom().nextDouble() * Math.PI * 2.0;
            double s = 0.03 + mc.level.getRandom().nextDouble() * 0.05;
            mc.level.addParticle(bits, p.x, p.y, p.z, Math.cos(a) * s, 0.06 + mc.level.getRandom().nextDouble() * 0.06, Math.sin(a) * s);
        }
    }

    private static void popFx() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.worldPoint(pos, state, WorkbenchScene.RESULT_FORWARD,
                WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS, WorkbenchScene.RESULT_HEIGHT);
        mc.level.playLocalSound(p.x, p.y, p.z, ModSounds.WB_TOOL_POP.get(), SoundSource.BLOCKS, 0.9f, 1.0f, false);
    }

    public static final int KIND_WOOD = 0, KIND_STONE = 1, KIND_BONE = 2, KIND_METAL = 3;

    public static int materialKind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return KIND_METAL;
        String mat = materialKey(stack);
        if (has(mat, "wood", "oak", "spruce", "birch", "jungle", "acacia", "cherry", "mangrove", "bamboo", "plank", "stick", "crimson", "warped")) return KIND_WOOD;
        if (has(mat, "bone")) return KIND_BONE;
        if (has(mat, "stone", "flint", "granite", "andesite", "diorite", "deepslate", "obsidian", "basalt", "blackstone", "cobble")) return KIND_STONE;
        return KIND_METAL;
    }

    private static void landFx(int i, ItemStack part) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (mc.level == null || pos == null || state == null) return;
        Vec3 p = WorkbenchScene.worldPoint(pos, state, WorkbenchScene.RESULT_FORWARD + restF[i],
                WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS + restA[i], WorkbenchScene.PART_HEIGHT);
        mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.7f, 1.2f + mc.level.getRandom().nextFloat() * 0.2f, false);
        SoundEvent knock = switch (materialKind(part)) {
            case KIND_WOOD -> SoundEvents.WOOD_HIT;
            case KIND_STONE -> SoundEvents.STONE_HIT;
            case KIND_BONE -> SoundEvents.BONE_BLOCK_HIT;
            default -> SoundEvents.CHAIN_HIT;
        };
        mc.level.playLocalSound(p.x, p.y, p.z, knock, SoundSource.BLOCKS, 0.5f, 1.1f, false);
    }

    private static String materialKey(ItemStack part) {
        try {
            if (ToolStack.isInitialized(part)) {
                var m = ToolStack.getMaterial(part, 0);
                if (m != null && !m.isEmpty()) return m.id().toString().toLowerCase(Locale.ROOT);
            }
            if (part.getItem() instanceof ToolPartItem tp && tp.getMaterial(part) != null && !tp.getMaterial(part).isEmpty())
                return tp.getMaterial(part).id().toString().toLowerCase(Locale.ROOT);
        } catch (RuntimeException ignored) {
        }
        return BuiltInRegistries.ITEM.getKey(part.getItem()).toString().toLowerCase(Locale.ROOT);
    }

    private static boolean has(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    public static boolean overTool() {
        return dragging >= 0 && pointerF * pointerF + pointerA * pointerA < ATTACH_RADIUS * ATTACH_RADIUS;
    }

    public static boolean endDrag() {
        if (dragging < 0) return false;
        int d = dragging;
        boolean onTool = overTool();
        dragging = -1;
        if (onTool) {
            attached[d] = true;
            order[d] = ++orderCounter;
            restF[d] = 0.0f;
            restA[d] = 0.0f;
            return true;
        }
        restF[d] = dragF;
        restA[d] = dragA;
        return false;
    }

    public static void expectAutoAttach() { autoAttachUntil = net.minecraft.util.Util.getMillis() + 1500L; }

    public static void attachNow(int i, ItemStack part) {
        if (i < 0 || i >= MAX || !present[i] || attached[i]) return;
        if (dragging == i) dragging = -1;
        attached[i] = true;
        order[i] = ++orderCounter;
        restF[i] = 0.0f;
        restA[i] = 0.0f;
        attachFx(part);
    }

    public static void release(int i) {
        if (i < 0 || i >= MAX || !attached[i]) return;
        attached[i] = false;
        restF[i] = HOME_F[i];
        restA[i] = HOME_A[i];
    }

    public static void gatherAll() {
        dragging = -1;
        for (int i = 0; i < MAX; i++) {
            if (!present[i]) continue;
            attached[i] = true;
            restF[i] = 0.0f;
            restA[i] = 0.0f;
        }
    }

    @Nullable
    public static float[] mouseToPad(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        Pose pose = WorkbenchCamera.lastPose();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (pose == null || pos == null || state == null || guiWidth <= 0 || guiHeight <= 0) return null;

        double yaw = Math.toRadians(pose.yaw());
        double pitch = Math.toRadians(pose.pitch());
        double cp = Math.cos(pitch);
        Vec3 forward = new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 up = right.cross(forward);
        double tanHalf = Math.tan(Math.toRadians(WorkbenchSession.lastFov()) * 0.5);
        double aspect = guiWidth / (double) guiHeight;
        double nx = mouseX / guiWidth * 2.0 - 1.0;
        double ny = 1.0 - mouseY / guiHeight * 2.0;
        Vec3 dir = forward.add(right.scale(nx * tanHalf * aspect)).add(up.scale(ny * tanHalf));
        if (dir.y > -1.0e-4) return null;

        double planeY = pos.getY() + WorkbenchScene.PART_HEIGHT;
        double t = (planeY - pose.position().y) / dir.y;
        if (t <= 0.0) return null;
        Vec3 hit = pose.position().add(dir.scale(t));

        double dx = hit.x - pos.getX() - 0.5;
        double dz = hit.z - pos.getZ() - 0.5;
        Direction f = WorkbenchScene.facing(state);
        Direction s = WorkbenchScene.acrossDir(state);
        double across = dx * s.getStepX() + dz * s.getStepZ() - 0.5;
        double fwd = dx * f.getStepX() + dz * f.getStepZ();
        return new float[] {
                (float) (fwd - WorkbenchScene.RESULT_FORWARD),
                (float) (across - (WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS))};
    }

    @Nullable
    public static float[] padToScreen(float pf, float pa, int guiWidth, int guiHeight) {
        Pose pose = WorkbenchCamera.lastPose();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (pose == null || pos == null || state == null || guiWidth <= 0 || guiHeight <= 0) return null;

        Vec3 world = WorkbenchScene.worldPoint(pos, state,
                WorkbenchScene.RESULT_FORWARD + pf,
                WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS + pa,
                WorkbenchScene.PART_HEIGHT + 0.05);
        double yaw = Math.toRadians(pose.yaw());
        double pitch = Math.toRadians(pose.pitch());
        double cp = Math.cos(pitch);
        Vec3 forward = new Vec3(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 up = right.cross(forward);
        Vec3 rel = world.subtract(pose.position());
        double z = rel.dot(forward);
        if (z < 0.05) return null;
        double tanHalf = Math.tan(Math.toRadians(WorkbenchSession.lastFov()) * 0.5);
        double aspect = guiWidth / (double) guiHeight;
        double nx = rel.dot(right) / (z * tanHalf * aspect);
        double ny = rel.dot(up) / (z * tanHalf);
        return new float[] {(float) ((nx + 1.0) * 0.5 * guiWidth), (float) ((1.0 - ny) * 0.5 * guiHeight)};
    }
}
