package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.tables.scene.WorkbenchOrigin;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public final class WorkbenchAnimation {
    public enum Clip { IDLE, PLACE_ITEM, GRAB_ITEM, INSPECT, HAMMER, ATTACH, ASSEMBLE, TAKE_RESULT }

    private static final double STRIDE_PHASE_PER_BLOCK = 5.4;

    private static Clip current = Clip.IDLE;

    private static final float[] FILTER_RATE = {14, 14, 14,  18, 18, 18,  400, 24, 24,  24, 24, 24,  30, 30,  20, 20, 20, 20, 20, 20,  20};
    private static final float[] filtered = new float[21];
    private static double filterLast;
    private static boolean filterInit;

    private WorkbenchAnimation() {}

    public static Clip current() { return current; }

    public static void play(Clip clip) { current = clip; }

    static void reset() { current = Clip.IDLE; filterInit = false; }

    public static float poseWeight() {
        if (WorkbenchSession.phase() == WorkbenchSession.Phase.EXITING) return 0.0f;
        return WorkbenchSession.blend();
    }

    public static void applyProceduralPose(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm,
                                           ModelPart rightLeg, ModelPart leftLeg, float u, double seconds) {
        final var rIn = rightArm.getInitialPose();
        final var lIn = leftArm.getInitialPose();
        final var hIn = head.getInitialPose();
        final float rsx = rIn.x(), rsy = rIn.y(), rsz = rIn.z(), lsx = lIn.x(), lsy = lIn.y(), lsz = lIn.z();
        final float hx = hIn.x(), hy = hIn.y(), hz = hIn.z();
        float breathe = (float) Math.sin(seconds * 2.4) * 0.03f;
        float work = (float) Math.sin(seconds * 4.4) * 0.025f;

        float speed = 16.0f * u * u * (1.0f - u) * (1.0f - u);
        float phase = (float) (WorkbenchSession.walkDistance() * u * STRIDE_PHASE_PER_BLOCK);
        float legSwing = Mth.cos(phase) * 0.95f * Math.min(1.0f, speed * 1.6f);

        float reach = smooth((u - 0.72f) / 0.28f);

        float c = WorkbenchCraft.poseWeight();
        float lean = Mth.lerp(c, (float) WorkbenchTuning.BODY_LEAN.get() * reach, WorkbenchCraft.isTinker() ? 0.10f : 0.06f);
        float inward = (float) WorkbenchTuning.ARM_INWARD.get();
        float armPitch = (float) WorkbenchTuning.ARM_PITCH.get();

        float dy = 0.0f, dz = 0.0f, dip = 0.0f;
        float shRx = 0, shRy = 0, shRz = 0, shLx = 0, shLy = 0, shLz = 0;

        body.xRot = lean + breathe * 0.25f;
        body.yRot = 0.0f;
        body.zRot = 0.0f;

        head.xRot = Mth.lerp(reach, 0.0f, 0.34f) - breathe * 0.35f;
        head.yRot = 0.0f;
        head.zRot = 0.0f;
        if (WorkbenchSession.phase() == WorkbenchSession.Phase.ENTERING) {
            BlockState st = WorkbenchSession.tableState();
            if (st != null) {
                float rel = Mth.clamp(Mth.wrapDegrees(WorkbenchScene.playerYaw(st) - visualBodyYaw()), -55.0f, 55.0f);
                head.yRot = (float) Math.toRadians(rel) * smooth((u - 0.25f) / 0.40f);
            }
        }

        rightLeg.xRot = legSwing + Mth.lerp(reach, 0.0f, 0.03f);
        leftLeg.xRot = -legSwing + Mth.lerp(reach, 0.0f, -0.03f);
        rightLeg.yRot = 0.0f;
        leftLeg.yRot = 0.0f;
        rightLeg.zRot = 0.0f;
        leftLeg.zRot = 0.0f;

        float armSwing = Mth.cos(phase) * 0.8f * Math.min(1.0f, speed * 1.6f);
        arm(rightArm, dy, reach, -armSwing, armPitch + work, -inward, 0.05f);
        arm(leftArm, dy, reach, armSwing, armPitch - work * 0.6f, inward * 2.0f, -0.05f);
        float settle = (float) Math.sin(Math.PI * Mth.clamp((u - 0.82f) / 0.18f, 0.0f, 1.0f));
        rightArm.xRot -= 0.16f * settle;
        leftArm.xRot -= 0.12f * settle;
        body.xRot += 0.05f * settle;
        body.zRot = legSwing * 0.035f;
        head.zRot = -legSwing * 0.02f;
        float life = (float) WorkbenchTuning.ANIM_LIFE.get();
        float walkAmt = Math.min(1.0f, speed * 1.6f);
        float twist = Mth.cos(phase) * 0.26f * walkAmt * life;
        body.yRot = -twist;
        shRz += Mth.cos(phase) * 1.3f * walkAmt * life;   shLz -= Mth.cos(phase) * 1.3f * walkAmt * life;
        shRy += Math.abs(Mth.sin(phase)) * 0.5f * walkAmt * life;   shLy += Math.abs(Mth.sin(phase)) * 0.5f * walkAmt * life;
        head.yRot += twist * 0.7f;

        float aim = WorkbenchAssembly.aimMix();
        if (aim > 0.001f) {
            float[] a = WorkbenchAssembly.aimAngles();
            if (a != null) {
                rightArm.xRot = Mth.lerp(aim, rightArm.xRot, a[0]);
                rightArm.yRot = Mth.lerp(aim, rightArm.yRot, a[1]);
                rightArm.zRot = Mth.lerp(aim, rightArm.zRot, 0.0f);
                head.yRot = Mth.lerp(aim, head.yRot, a[1] * 0.35f);
            }
        }

        float polish = WorkbenchCraft.polishWeight();
        if (polish > 0.0f) {
            float scrub = Mth.sin((float) seconds * 16.0f);
            rightArm.xRot = Mth.lerp(polish, rightArm.xRot, armPitch - 0.15f + scrub * 0.22f);
            rightArm.yRot += polish * scrub * 0.15f;
            body.yRot += polish * scrub * 0.03f;
        }

        if (c > 0.0f) {
            float dr = WorkbenchCraft.drawerReach();
            float dl = WorkbenchCraft.drawerLeftReach();
            float carry = 4.0f * c * (1.0f - c);
            if (carry > 0.0f) {
                float step = Mth.sin((float) seconds * 10.0f) * 0.30f * Math.min(1.0f, carry);
                rightLeg.xRot += step;
                leftLeg.xRot -= step;
            }
            float pulse = WorkbenchCraft.strikePulseSmooth();
            if (WorkbenchCraft.isTinker()) {
                float t = (float) seconds;
                rightArm.xRot = Mth.lerp(c, rightArm.xRot, armPitch - 0.02f + Mth.sin(t * 9.0f) * 0.08f - 0.14f * pulse);
                rightArm.yRot = Mth.lerp(c, rightArm.yRot, -0.38f + Mth.sin(t * 5.3f) * 0.10f);
                rightArm.zRot = Mth.lerp(c, rightArm.zRot, 0.05f);
                leftArm.xRot = Mth.lerp(c, leftArm.xRot, armPitch + 0.04f + Mth.cos(t * 6.7f) * 0.05f);
                leftArm.yRot = Mth.lerp(c, leftArm.yRot, 0.42f + Mth.sin(t * 3.9f) * 0.06f);
                leftArm.zRot = Mth.lerp(c, leftArm.zRot, -0.05f);
                body.yRot = c * Mth.sin(t * 1.3f) * 0.05f;
                head.xRot += c * (0.22f + 0.03f * pulse);
                head.yRot = c * Mth.sin(t * 0.9f) * 0.10f;
                body.yRot += c * wob(t, 5.3f, 8.1f, 0.7f) * 0.09f * life;
                body.xRot += c * 0.08f * pulse * life;
                shRy += c * 0.6f * wob(t, 3.1f, 4.7f, 0.4f) * life;  shLy += c * 0.6f * wob(t, 3.7f, 5.3f, 1.9f) * life;
                shRz += c * (0.8f * wob(t, 2.3f, 3.9f, 1.1f) - 0.8f * pulse) * life;  shLz += c * 0.8f * wob(t, 2.9f, 4.3f, 2.2f) * life;
                head.yRot -= c * wob(t, 5.3f, 8.1f, 0.7f) * 0.06f * life;
            } else {
                rightArm.xRot = Mth.lerp(c, rightArm.xRot, WorkbenchCraft.hammerAngle());
                rightArm.yRot = Mth.lerp(c, rightArm.yRot, -0.18f);
                rightArm.zRot = Mth.lerp(c, rightArm.zRot, 0.08f);
                leftArm.xRot = Mth.lerp(c, leftArm.xRot, armPitch + 0.05f + Mth.sin((float) seconds * 3.1f) * 0.03f);
                leftArm.yRot = Mth.lerp(c, leftArm.yRot, 0.30f);
                body.xRot += c * 0.04f * pulse;
                body.yRot = c * (-0.05f + 0.04f * pulse);
                head.xRot += c * (0.10f + 0.05f * pulse);
                float rise = Mth.clamp((-1.05f - WorkbenchCraft.hammerAngle()) / 1.4f, 0.0f, 1.0f);
                body.xRot += c * (-0.07f * rise + 0.05f * pulse);
                head.xRot += c * (-0.05f * rise);
                leftArm.xRot -= c * 0.14f * rise;
                leftArm.zRot += c * 0.05f * rise;
                float riseBody = Mth.clamp((-1.05f - WorkbenchCraft.hammerAngleLag(0.07)) / 1.4f, 0.0f, 1.0f);
                float riseHead = Mth.clamp((-1.05f - WorkbenchCraft.hammerAngleLag(0.13)) / 1.4f, 0.0f, 1.0f);
                float r1 = WorkbenchCraft.swingRand(2), r2 = WorkbenchCraft.swingRand(3), r3 = WorkbenchCraft.swingRand(4);
                float turn = (-0.16f + 0.42f * riseBody * (1.0f + 0.25f * r1)) * life;
                body.yRot += c * turn;
                body.xRot += c * (-0.10f * riseBody + 0.10f * pulse) * life;
                float turnHead = (-0.16f + 0.42f * riseHead * (1.0f + 0.25f * r1)) * life;
                head.yRot -= c * turnHead * 0.7f;
                head.xRot += c * (0.04f * riseHead + 0.08f * pulse) * life;
                rightArm.zRot += c * (0.12f - 0.78f * riseBody * (1.0f + 0.25f * r2)) * life;
                rightArm.yRot += c * (0.08f + 0.50f * riseBody * (1.0f + 0.2f * r3)) * life;
                float shoulderR = riseBody, shoulderL = Mth.clamp((-1.05f - WorkbenchCraft.hammerAngleLag(0.17)) / 1.4f, 0.0f, 1.0f);
                shRy += c * (-1.7f * shoulderR + 1.1f * pulse) * life;
                shRz += c * (1.3f * shoulderR - 2.0f * pulse) * life;
                shRx += c * (-1.1f * shoulderR) * life;
                shLy += c * (0.7f * shoulderL - 0.5f * pulse) * life;
                shLz += c * (-0.9f * shoulderL + 1.1f * pulse) * life;
                shLx += c * (0.5f * shoulderL) * life;
                leftArm.xRot += c * 0.18f * (shoulderL - riseBody) * life;
                leftArm.xRot += c * 0.30f * pulse * life;
                leftArm.yRot -= c * 0.16f * riseBody * life; leftArm.zRot -= c * 0.14f * riseBody * life;
                rightLeg.xRot += c * (-0.12f * riseBody + 0.09f * pulse) * life;
                leftLeg.xRot += c * (0.12f * riseBody - 0.07f * pulse) * life;
                dip = c * life * (1.6f * pulse + 0.5f * (1.0f - riseBody));
                float cyc = wob(seconds, 0.9f, 1.47f, 1.3f);
                head.yRot += c * 0.16f * cyc * life;
                head.xRot += c * 0.06f * Math.max(0.0f, cyc) * life;
                leftArm.yRot += c * 0.20f * Math.max(0.0f, cyc) * life;
                leftArm.xRot -= c * 0.12f * Math.max(0.0f, cyc) * life;
            }
            if (dr > 0.0f) {
                rightArm.xRot = Mth.lerp(dr, rightArm.xRot, -0.74f);
                rightArm.yRot = Mth.lerp(dr, rightArm.yRot, -0.45f);
                rightArm.zRot = Mth.lerp(dr, rightArm.zRot, 0.0f);
            }
            if (dl > 0.0f) {
                leftArm.xRot = Mth.lerp(dl, leftArm.xRot, -0.60f - 0.18f * WorkbenchCraft.drawerOpen());
                leftArm.yRot = Mth.lerp(dl, leftArm.yRot, 0.25f);
                leftArm.zRot = Mth.lerp(dl, leftArm.zRot, 0.0f);
            }
            float look = Math.max(dr, dl);
            if (look > 0.0f) {
                head.yRot = Mth.lerp(look, head.yRot, -0.40f);
                head.xRot += look * 0.10f;
            }
            float grab = WorkbenchCraft.reachWeight();
            float raise = WorkbenchCraft.raiseWeight();
            if (grab > 0.0f) {
                rightArm.xRot = Mth.lerp(grab, rightArm.xRot, -1.10f);
                rightArm.yRot = Mth.lerp(grab, rightArm.yRot, -0.15f);
                rightArm.zRot = Mth.lerp(grab, rightArm.zRot, 0.0f);
            }
            if (raise > 0.0f) {
                float sway = Mth.sin((float) seconds * 1.7f) * 0.04f;
                rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -1.30f + sway);
                rightArm.yRot = Mth.lerp(raise, rightArm.yRot, -0.50f);
                leftArm.xRot = Mth.lerp(raise, leftArm.xRot, -0.30f);
                leftArm.yRot = Mth.lerp(raise, leftArm.yRot, 0.10f);
                head.xRot = Mth.lerp(raise, head.xRot, 0.12f);
                head.yRot = Mth.lerp(raise, head.yRot, 0.55f);
                body.xRot = Mth.lerp(raise, body.xRot, 0.04f);
                body.yRot = Mth.lerp(raise, body.yRot, 0.0f);
            }
        }

        float bt = (float) seconds * 2.1f;
        float lf = life * (1.0f - 0.5f * c);
        rightArm.zRot += lf * 0.07f * Mth.cos(bt - 0.26f);
        leftArm.zRot -= lf * 0.07f * Mth.cos(bt - 0.26f);
        rightArm.yRot += lf * 0.05f * Mth.cos(bt);
        leftArm.yRot -= lf * 0.05f * Mth.cos(bt);
        body.zRot += lf * 0.03f * Mth.sin(bt * 0.5f);
        head.zRot += lf * 0.04f * Mth.sin(bt * 0.5f + 1.0f);
        head.xRot += lf * 0.03f * Mth.sin(bt - 0.4f);
        float shoulderBob = lf * (Mth.sin(bt - 0.21f) * 0.45f);

        float[] v = {head.xRot, head.yRot, head.zRot, body.xRot, body.yRot, body.zRot,
                rightArm.xRot, rightArm.yRot, rightArm.zRot, leftArm.xRot, leftArm.yRot, leftArm.zRot,
                rightLeg.xRot, leftLeg.xRot, shRx, shRy, shRz, shLx, shLy, shLz, dip};
        float dtF = (float) (seconds - filterLast);
        boolean snap = !filterInit || dtF < 0.0f || dtF > 0.25f || u < 0.02f;
        if (snap || dtF > 1.0e-4f) {
            for (int i = 0; i < v.length; i++) {
                if (snap) filtered[i] = v[i];
                else filtered[i] += (v[i] - filtered[i]) * (1.0f - (float) Math.exp(-FILTER_RATE[i] * dtF));
            }
            filterLast = seconds;
            filterInit = true;
        }
        head.xRot = filtered[0]; head.yRot = filtered[1]; head.zRot = filtered[2];
        body.xRot = filtered[3]; body.yRot = filtered[4]; body.zRot = filtered[5];
        rightArm.xRot = filtered[6]; rightArm.yRot = filtered[7]; rightArm.zRot = filtered[8];
        leftArm.xRot = filtered[9]; leftArm.yRot = filtered[10]; leftArm.zRot = filtered[11];
        rightLeg.xRot = filtered[12]; leftLeg.xRot = filtered[13];
        shRx = filtered[14]; shRy = filtered[15]; shRz = filtered[16]; shLx = filtered[17]; shLy = filtered[18]; shLz = filtered[19];
        dip = filtered[20];

        rightLeg.y = 12.0f; leftLeg.y = 12.0f;
        rightLeg.z = 0.1f; leftLeg.z = 0.1f;
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot);
        org.joml.Vector3f hips = q.transform(new org.joml.Vector3f(0.0f, 12.0f, 0.0f));
        float px = -hips.x, py = 12.0f - hips.y + dip, pz = -hips.z;
        body.x = px; body.y = py; body.z = pz;
        head.x = hx + px; head.y = hy + py; head.z = hz + pz;
        org.joml.Vector3f rs = q.transform(new org.joml.Vector3f(rsx, rsy, rsz));
        org.joml.Vector3f ls = q.transform(new org.joml.Vector3f(lsx, lsy, lsz));
        rightArm.x = px + rs.x + shRx; rightArm.y = py + rs.y + shoulderBob + shRy; rightArm.z = pz + rs.z + shRz;
        leftArm.x = px + ls.x + shLx; leftArm.y = py + ls.y + shoulderBob + shLy; leftArm.z = pz + ls.z + shLz;
    }

    public static void restoreAnchors(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        for (ModelPart part : new ModelPart[] {head, body, rightArm, leftArm}) {
            var in = part.getInitialPose();
            part.x = in.x();
            part.y = in.y();
            part.z = in.z();
        }
    }

    private static void arm(ModelPart arm, float dy, float reach, float walkX, float restX, float restY, float restZ) {
        arm.xRot = Mth.lerp(reach, walkX, restX);
        arm.yRot = Mth.lerp(reach, 0.0f, restY);
        arm.zRot = Mth.lerp(reach, 0.0f, restZ);
    }

    private static float wob(double t, float a, float b, float phase) {
        return 0.65f * (float) Math.sin(t * a) + 0.35f * (float) Math.sin(t * b + phase);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float visualBodyYaw() {
        BlockPos pos = WorkbenchSession.tablePos();
        BlockState state = WorkbenchSession.tableState();
        if (pos == null || state == null) return 0.0f;

        float anchorYaw = WorkbenchScene.playerYaw(state);
        WorkbenchOrigin origin = WorkbenchSession.origin();
        double walk = WorkbenchSession.walkDistance();
        if (origin == null || walk < 0.2) return anchorYaw;

        double dx = WorkbenchScene.playerPosition(pos, state).x - origin.x();
        double dz = WorkbenchScene.playerPosition(pos, state).z - origin.z();
        float walkYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        float u = WorkbenchSession.blend();
        if (WorkbenchSession.phase() == WorkbenchSession.Phase.EXITING) {
            return origin.yaw();
        }
        float yaw = Mth.rotLerp(smooth(u / 0.12f), origin.yaw(), walkYaw);
        return Mth.rotLerp(smooth((u - 0.70f) / 0.30f), yaw, anchorYaw);
    }
}
