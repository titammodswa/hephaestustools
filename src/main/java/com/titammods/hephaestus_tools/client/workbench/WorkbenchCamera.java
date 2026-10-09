package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.tables.scene.WorkbenchOrigin;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public final class WorkbenchCamera {
    public record Pose(Vec3 position, float yaw, float pitch) {}

    private static final double MIN_CAMERA_DISTANCE = 0.6;

    @Nullable private static Pose startPose;
    @Nullable private static Pose exitStart;
    @Nullable private static Pose lastPose;

    private static double swayX;
    private static double swayY;
    private static final double SWAY_FOLLOW_RATE = 5.5;
    private static final double SWAY_SIDE = 0.12;
    private static final double SWAY_UP = 0.06;
    private static final double SWAY_TARGET_SHIFT = 0.04;
    private static final float SWAY_YAW_DEGREES = 1.0f;
    private static final float SWAY_PITCH_DEGREES = 0.6f;

    private WorkbenchCamera() {}

    @Nullable
    public static Pose lastPose() { return lastPose; }

    static void beginEnter(boolean resuming) {
        exitStart = null;
        WorkbenchOrigin origin = WorkbenchSession.origin();
        if (resuming) {
            startPose = lastPose;
        } else if (origin != null) {
            startPose = new Pose(origin.eye(), origin.yaw(), origin.pitch());
        } else {
            startPose = null;
        }
    }

    static void beginExit() {
        startPose = null;
        exitStart = lastPose;
    }

    static void reset() {
        startPose = null;
        exitStart = null;
        lastPose = null;
        swayX = 0.0;
        swayY = 0.0;
    }

    @Nullable
    public static Pose update(Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        WorkbenchSession.frame();
        updateSway();

        WorkbenchSession.Phase phase = WorkbenchSession.phase();
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (phase == WorkbenchSession.Phase.OFF || pos == null || state == null) {
            lastPose = null;
            return null;
        }

        Pose result;
        switch (phase) {
            case ENTERING -> {
                if (startPose == null) startPose = new Pose(vanillaPos, vanillaYaw, vanillaPitch);
                float k = WorkbenchSession.blend();
                result = arc(blend(startPose, shotPose(pos, state), k), k);
            }
            case EXITING -> {
                if (exitStart == null) exitStart = shotPose(pos, state);
                float k = 1.0f - WorkbenchSession.blend();
                result = blend(exitStart, firstPersonPose(vanillaPos, vanillaYaw, vanillaPitch), k);
            }
            default -> result = shotPose(pos, state);
        }

        lastPose = result;
        return result;
    }

    private static Pose shotPose(BlockPos pos, BlockState state) {
        float focus = WorkbenchSession.toolFocus();
        float inspect = WorkbenchSession.inspectMix();
        Vec3 padLook = WorkbenchScene.focusLookTarget(pos, state);
        Vec3 look = WorkbenchScene.lookTarget(pos, state).lerp(padLook, focus);
        Vec3 wanted = WorkbenchScene.cameraPosition(pos, state).lerp(WorkbenchScene.focusCameraPosition(pos, state), focus);
        if (inspect > 0.0f) wanted = wanted.lerp(WorkbenchScene.inspectCameraPosition(pos, state), inspect);
        wanted = look.add(wanted.subtract(look).scale(WorkbenchSession.viewZoom()));
        float craft = WorkbenchCraft.mix();
        if (craft > 0.0f) {
            float station = WorkbenchCraft.stationMix();
            boolean tinker = WorkbenchCraft.isTinker();
            wanted = wanted.lerp(tinker ? WorkbenchScene.tinkerCameraPosition(pos, state)
                    : WorkbenchScene.craftCameraPosition(pos, state, station), craft);
            look = look.lerp(tinker ? WorkbenchScene.tinkerLookTarget(pos, state)
                    : WorkbenchScene.craftLookTarget(pos, state, station), craft);
            wanted = look.add(wanted.subtract(look).scale(1.0 - 0.14 * craft * WorkbenchCraft.progress()));
            float take = WorkbenchCraft.takeMix() * craft;
            if (take > 0.0f) {
                wanted = wanted.lerp(WorkbenchScene.takeCameraPosition(pos, state), take);
                look = look.lerp(WorkbenchScene.takeLookTarget(pos, state), take);
            }
        }

        double settle = WorkbenchSession.blend();
        double strength = settle * WorkbenchTuning.PARALLAX.get() * (1.0 - 0.5 * focus) * (1.0 - 0.7 * inspect) * (1.0 - craft);
        float baseYaw = lookAt(wanted, look)[0];
        double yawRad = Math.toRadians(baseYaw);
        Vec3 right = new Vec3(-Math.cos(yawRad), 0.0, -Math.sin(yawRad));
        wanted = wanted.add(right.scale(swayX * SWAY_SIDE * strength)).add(0.0, -swayY * SWAY_UP * strength, 0.0);
        look = look.add(right.scale(-swayX * SWAY_TARGET_SHIFT * strength));

        Vec3 position = keepInsideWorld(look, wanted);

        double t = WorkbenchSession.sceneSeconds();
        position = position.add(
                Math.sin(t * 0.90) * 0.004 * settle,
                Math.sin(t * 0.65) * 0.003 * settle,
                Math.cos(t * 0.80) * 0.004 * settle);

        float[] rotation = lookAt(position, look);
        float kick = WorkbenchCraft.strikePulse() * craft;
        if (kick > 0.001f) {
            position = position.add(0.0, -0.02 * kick, 0.0);
            rotation[1] += 1.1f * kick;
            rotation[0] += (float) Math.sin(t * 53.0) * 0.45f * kick;
        }
        rotation[0] += (float) (swayX * SWAY_YAW_DEGREES * strength);
        rotation[1] += (float) (swayY * SWAY_PITCH_DEGREES * strength);
        return new Pose(position, rotation[0], rotation[1]);
    }

    private static void updateSway() {
        Minecraft mc = Minecraft.getInstance();
        double targetX = 0.0;
        double targetY = 0.0;
        if (mc.screen != null && !mc.mouseHandler.isMouseGrabbed()) {
            double width = Math.max(1, mc.getWindow().getScreenWidth());
            double height = Math.max(1, mc.getWindow().getScreenHeight());
            targetX = Mth.clamp(mc.mouseHandler.xpos() / width * 2.0 - 1.0, -1.0, 1.0);
            targetY = Mth.clamp(mc.mouseHandler.ypos() / height * 2.0 - 1.0, -1.0, 1.0);
        }
        double follow = 1.0 - Math.exp(-SWAY_FOLLOW_RATE * WorkbenchSession.frameDt());
        swayX += (targetX - swayX) * follow;
        swayY += (targetY - swayY) * follow;
    }

    private static Pose firstPersonPose(Vec3 vanillaPos, float vanillaYaw, float vanillaPitch) {
        WorkbenchOrigin origin = WorkbenchSession.origin();
        if (origin != null) return new Pose(origin.eye(), origin.yaw(), origin.pitch());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            return new Pose(mc.player.getEyePosition(1.0f), mc.player.getYRot(), mc.player.getXRot());
        }
        return new Pose(vanillaPos, vanillaYaw, vanillaPitch);
    }

    private static Vec3 keepInsideWorld(Vec3 look, Vec3 wanted) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return wanted;

        Vec3 dir = wanted.subtract(look);
        double length = dir.length();
        if (length < 1.0e-4) return wanted;

        BlockHitResult hit = mc.level.clip(new ClipContext(look, wanted,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == HitResult.Type.MISS) return wanted;

        double distance = hit.getLocation().distanceTo(look) - 0.15;
        distance = Mth.clamp(distance, Math.min(length, MIN_CAMERA_DISTANCE), length);
        return look.add(dir.scale(distance / length));
    }

    private static Pose arc(Pose p, float t) {
        double lift = Math.sin(Math.PI * Mth.clamp(t, 0.0f, 1.0f)) * WorkbenchTuning.TRANSITION_ARC.get();
        return new Pose(p.position().add(0.0, lift, 0.0), p.yaw(), p.pitch() + (float) (lift * 6.0));
    }

    private static Pose blend(Pose a, Pose b, float t) {
        return new Pose(
                a.position().lerp(b.position(), t),
                Mth.rotLerp(t, a.yaw(), b.yaw()),
                Mth.lerp(t, a.pitch(), b.pitch()));
    }

    private static float[] lookAt(Vec3 from, Vec3 target) {
        double dx = target.x - from.x;
        double dy = target.y - from.y;
        double dz = target.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[] {yaw, pitch};
    }
}
