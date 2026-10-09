package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchOrigin;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorkbenchSession {
    public enum Phase { OFF, ENTERING, ACTIVE, EXITING }

    private static final double MAX_FRAME_DT = 0.05;

    private static Phase phase = Phase.OFF;
    private static BlockPos tablePos;
    private static BlockState tableState;
    private static WorkbenchOrigin origin;
    private static CameraType oldCameraType;

    private static double sceneSeconds;
    private static double phaseSeconds;
    private static long lastFrameNanos;
    private static double frameDt;
    private static double walkDistance;
    private static boolean exitFinished;

    private static boolean focusWanted;
    private static double focusRaw;

    private static boolean inspectWanted;
    private static double inspectRaw;
    private static double viewYaw, viewTilt, spinVelocity;
    private static boolean viewDragging, autoSpin;
    private static double zoom = 1.0, zoomTarget = 1.0, lastFov = 70.0;

    private static ItemStack preview = ItemStack.EMPTY;
    private static ItemStack output = ItemStack.EMPTY;
    private static List<ItemStack> inputs = List.of();

    private WorkbenchSession() {}

    public static void start(@Nullable ArsenalTableBlockEntity table, @Nullable WorkbenchOrigin newOrigin) {
        WorkbenchTuningStore.ensureLoaded();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || table == null) return;

        boolean resuming = phase != Phase.OFF;

        tablePos = table.getBlockPos().immutable();
        tableState = table.getBlockState();
        if (newOrigin != null) origin = newOrigin;
        else if (!resuming) origin = null;
        walkDistance = 0.0;
        if (origin != null) {
            Vec3 anchor = WorkbenchScene.playerPosition(tablePos, tableState);
            walkDistance = Math.hypot(origin.x() - anchor.x, origin.z() - anchor.z);
        }

        if (!resuming) {
            sceneSeconds = 0.0;
            lastFrameNanos = 0L;
        }
        phaseSeconds = 0.0;
        exitFinished = false;
        phase = Phase.ENTERING;

        if (oldCameraType == null) oldCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);

        WorkbenchCamera.beginEnter(resuming);
    }

    public static void stop() {
        if (phase == Phase.OFF || phase == Phase.EXITING) return;
        phase = Phase.EXITING;
        phaseSeconds = 0.0;
        WorkbenchCamera.beginExit();
    }

    public static void hardReset() {
        if (phase != Phase.OFF) clear();
    }

    private static void clear() {
        Minecraft mc = Minecraft.getInstance();
        if (oldCameraType != null && mc.options != null) mc.options.setCameraType(oldCameraType);
        oldCameraType = null;

        phase = Phase.OFF;
        exitFinished = false;
        tablePos = null;
        tableState = null;
        origin = null;
        sceneSeconds = 0.0;
        phaseSeconds = 0.0;
        lastFrameNanos = 0L;
        frameDt = 0.0;
        walkDistance = 0.0;
        preview = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        inputs = List.of();
        focusWanted = false;
        focusRaw = 0.0;
        inspectWanted = false;
        inspectRaw = 0.0;
        viewYaw = 0.0;
        viewTilt = 0.0;
        spinVelocity = 0.0;
        viewDragging = false;
        autoSpin = false;
        zoom = 1.0;
        zoomTarget = 1.0;
        WorkbenchAssembly.reset();
        WorkbenchCraft.reset();
        WorkbenchAnimation.reset();
        WorkbenchCamera.reset();
    }

    static void frame() {
        if (phase == Phase.OFF) return;

        long now = System.nanoTime();
        double dt = lastFrameNanos == 0L ? 0.0 : Mth.clamp((now - lastFrameNanos) / 1.0e9, 0.0, MAX_FRAME_DT);
        lastFrameNanos = now;
        frameDt = dt;
        sceneSeconds += dt;
        phaseSeconds += dt;

        double focusTarget = focusWanted && phase == Phase.ACTIVE ? 1.0 : 0.0;
        focusRaw += (focusTarget - focusRaw) * (1.0 - Math.exp(-WorkbenchTuning.FOCUS_RATE.get() * dt));
        if (Math.abs(focusTarget - focusRaw) < 1.0e-4) focusRaw = focusTarget;

        double inspectTarget = inspectWanted && phase == Phase.ACTIVE ? 1.0 : 0.0;
        inspectRaw += (inspectTarget - inspectRaw) * (1.0 - Math.exp(-WorkbenchTuning.FOCUS_RATE.get() * dt));
        if (Math.abs(inspectTarget - inspectRaw) < 1.0e-4) inspectRaw = inspectTarget;

        zoom += (zoomTarget - zoom) * (1.0 - Math.exp(-8.0 * dt));
        WorkbenchAssembly.tick(dt);
        WorkbenchCraft.frame(dt);

        if (!viewDragging) {
            double target = autoSpin ? WorkbenchTuning.AUTO_SPIN_SPEED.get() : 0.0;
            spinVelocity += (target - spinVelocity) * (1.0 - Math.exp(-(autoSpin ? 2.0 : 3.5) * dt));
            viewYaw += spinVelocity * dt;
            if (viewYaw > 360.0 || viewYaw < -360.0) viewYaw %= 360.0;
        }

        if (phase == Phase.ENTERING && phaseSeconds >= enterSeconds()) {
            phase = Phase.ACTIVE;
            phaseSeconds = 0.0;
        } else if (phase == Phase.EXITING && phaseSeconds >= exitSeconds()) {
            exitFinished = true;
        }
    }

    static void tick() {
        if (exitFinished) clear();
    }

    public static Phase phase() { return phase; }
    public static boolean isRunning() { return phase != Phase.OFF; }
    public static boolean isSceneActive() { return phase == Phase.ENTERING || phase == Phase.ACTIVE; }
    public static boolean hidesHud() { return isSceneActive(); }

    public static boolean isViewing(BlockPos pos) {
        return isSceneActive() && tablePos != null && tablePos.equals(pos);
    }

    @Nullable public static BlockPos tablePos() { return tablePos; }
    @Nullable public static BlockState tableState() { return tableState; }
    @Nullable public static WorkbenchOrigin origin() { return origin; }
    public static double sceneSeconds() { return sceneSeconds; }
    public static double frameDt() { return frameDt; }
    public static double walkDistance() { return walkDistance; }

    public static void setToolFocus(boolean wanted) { focusWanted = wanted; }

    public static void setInspect(boolean wanted) { inspectWanted = wanted; }

    public static float inspectMix() { return smootherStep((float) inspectRaw); }

    public static void rotateView(double dYaw, double dTilt) {
        viewDragging = true;
        viewYaw += dYaw;
        viewTilt = Mth.clamp(viewTilt + dTilt, -55.0, 55.0);
        spinVelocity = Mth.clamp(dYaw * 40.0, -360.0, 360.0);
    }

    public static void releaseView() { viewDragging = false; }

    public static void zoomBy(double delta) { zoomTarget = Mth.clamp(zoomTarget * Math.pow(0.9, delta), 0.55, 1.7); }
    public static double viewZoom() { return zoom; }
    public static void setLastFov(double fov) { lastFov = fov; }
    public static double lastFov() { return lastFov; }
    public static void setAutoSpin(boolean on) { autoSpin = on; }
    public static boolean autoSpin() { return autoSpin; }
    public static float viewYaw() { return (float) viewYaw; }
    public static float viewTilt() { return (float) viewTilt; }

    public static float toolFocus() { return smootherStep((float) focusRaw); }

    static float progress() {
        return switch (phase) {
            case ENTERING -> (float) Mth.clamp(phaseSeconds / enterSeconds(), 0.0, 1.0);
            case EXITING -> (float) Mth.clamp(phaseSeconds / exitSeconds(), 0.0, 1.0);
            case ACTIVE -> 1.0f;
            case OFF -> 0.0f;
        };
    }

    public static float blend() {
        return switch (phase) {
            case ENTERING -> { float p = progress(); yield 0.55f * smootherStep(p) + 0.45f * (1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p)); }
            case ACTIVE -> 1.0f;
            case EXITING -> 1.0f - smootherStep(progress());
            case OFF -> 0.0f;
        };
    }

    static float smootherStep(float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    private static double enterSeconds() {
        double walk = walkDistance / WorkbenchTuning.WALK_SPEED.get() + 0.35;
        return Mth.clamp(Math.max(WorkbenchTuning.ENTER_SECONDS.get(), walk), 0.1, 3.5);
    }

    private static double exitSeconds() {
        return Mth.clamp(WorkbenchTuning.EXIT_SECONDS.get(), 0.1, 3.0);
    }

    public static void updateItems(BlockPos pos, List<ItemStack> newInputs, ItemStack newPreview, ItemStack newOutput) {
        if (!isViewing(pos)) return;
        List<ItemStack> copy = new ArrayList<>(newInputs.size());
        for (ItemStack stack : newInputs) copy.add(stack.copy());
        inputs = Collections.unmodifiableList(copy);
        preview = newPreview.copy();
        output = newOutput.copy();
    }

    public static List<ItemStack> inputs() { return inputs; }
    public static ItemStack preview() { return preview; }
    public static ItemStack output() { return output; }
}
