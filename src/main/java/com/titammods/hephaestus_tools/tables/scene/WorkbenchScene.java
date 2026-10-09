package com.titammods.hephaestus_tools.tables.scene;

import com.titammods.hephaestus_tools.tables.block.ArsenalTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class WorkbenchScene {
    private WorkbenchScene() {}

    public static final double TABLE_TOP = 1.0;
    public static final float PLAYER_PITCH = 18.0f;

    public static final float[] PART_ACROSS = {0.86f, 0.86f, 0.86f, 0.86f};
    public static final float[] PART_FORWARD = {-0.33f, -0.11f, 0.11f, 0.33f};
    public static final float[] PART_YAW = {-4.0f, 3.0f, -2.0f, 5.0f};
    public static final float PART_HEIGHT = 1.075f;
    public static final float PART_SCALE = 0.30f;
    public static final float TRAY_SIZE = 0.20f;
    public static final float TRAY_HEIGHT = 0.05f;
    public static final float PAD_SIZE = 0.50f;
    public static final float RESULT_ACROSS = 0.0f;
    public static final float RESULT_FORWARD = 0.0f;
    public static final float RESULT_HEIGHT = 1.12f;
    public static final float RESULT_SCALE = 0.70f;
    public static final float RESULT_YAW = -18.0f;

    public static double focusAcross() {
        return WorkbenchTuning.FOCUS_SIDE_OFFSET.get();
    }

    public static Direction facing(BlockState state) {
        return state.hasProperty(ArsenalTableBlock.FACING)
                ? state.getValue(ArsenalTableBlock.FACING)
                : Direction.NORTH;
    }

    public static Direction acrossDir(BlockState state) {
        return facing(state).getCounterClockWise();
    }

    public static Vec3 localPoint(BlockState state, double forward, double across, double y) {
        Direction f = facing(state);
        Direction s = acrossDir(state);
        return new Vec3(
                0.5 + s.getStepX() * (0.5 + across) + f.getStepX() * forward,
                y,
                0.5 + s.getStepZ() * (0.5 + across) + f.getStepZ() * forward);
    }

    public static Vec3 worldPoint(BlockPos pos, BlockState state, double forward, double across, double y) {
        return localPoint(state, forward, across, y).add(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 playerPosition(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                WorkbenchTuning.PLAYER_DISTANCE.get(),
                focusAcross() + WorkbenchTuning.PLAYER_SIDE_OFFSET.get(),
                0.0);
    }

    public static float playerYaw(BlockState state) {
        double across = WorkbenchTuning.PLAYER_SIDE_OFFSET.get();
        double forward = WorkbenchTuning.PLAYER_DISTANCE.get();
        double toCenter = Math.toDegrees(Math.atan2(-across, forward));
        return facing(state).getOpposite().toYRot() + (float) (toCenter * WorkbenchTuning.PLAYER_TURN.get());
    }

    public static Vec3 cameraPosition(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                WorkbenchTuning.CAMERA_DISTANCE.get(),
                focusAcross() + WorkbenchTuning.CAMERA_SIDE_OFFSET.get(),
                WorkbenchTuning.CAMERA_HEIGHT.get());
    }

    public static Vec3 focusCameraPosition(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                WorkbenchTuning.FOCUS_DISTANCE.get(),
                focusAcross() + RESULT_ACROSS + WorkbenchTuning.FOCUS_SIDE_SHIFT.get(),
                WorkbenchTuning.FOCUS_HEIGHT.get());
    }

    public static Vec3 focusLookTarget(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                RESULT_FORWARD,
                focusAcross() + RESULT_ACROSS,
                WorkbenchTuning.FOCUS_LOOK_HEIGHT.get());
    }

    public static Vec3 inspectCameraPosition(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                WorkbenchTuning.INSPECT_DISTANCE.get(),
                focusAcross() + RESULT_ACROSS + WorkbenchTuning.FOCUS_SIDE_SHIFT.get(),
                WorkbenchTuning.INSPECT_HEIGHT.get());
    }

    public static double workForward(double station) {
        return WorkbenchTuning.ANVIL_FORWARD.get() + (RESULT_FORWARD - WorkbenchTuning.ANVIL_FORWARD.get()) * station;
    }

    public static double workAcross(double station) {
        double anvil = WorkbenchTuning.ANVIL_ACROSS.get();
        return anvil + (focusAcross() + RESULT_ACROSS - anvil) * station;
    }

    public static double workTop(double station) {
        double anvil = WorkbenchTuning.ANVIL_TOP.get();
        return anvil + (TABLE_TOP - anvil) * station;
    }

    public static Vec3 workPoint(BlockPos pos, BlockState state, double station, double lift) {
        return worldPoint(pos, state, workForward(station), workAcross(station), workTop(station) + lift);
    }

    public static Vec3 craftPlayerPosition(BlockPos pos, BlockState state, double station) {
        return worldPoint(pos, state, WorkbenchTuning.PLAYER_DISTANCE.get(), WorkbenchTuning.CRAFT_PLAYER_ACROSS.get(), 0.0);
    }

    public static float craftPlayerYaw(BlockState state) {
        return facing(state).getOpposite().toYRot();
    }

    public static float craftTurnYaw(BlockPos pos, BlockState state, double station) {
        Vec3 p = craftPlayerPosition(pos, state, station);
        Vec3 w = workPoint(pos, state, station, 0.0);
        float toWork = (float) Math.toDegrees(Math.atan2(-(w.x - p.x), w.z - p.z));
        return Mth.rotLerp((float) WorkbenchTuning.CRAFT_TURN.get(), craftPlayerYaw(state), toWork);
    }

    private static Vec3 backFrom(BlockPos pos, BlockState state, double station) {
        Vec3 p = craftPlayerPosition(pos, state, station);
        Vec3 w = workPoint(pos, state, station, 0.0);
        Vec3 d = new Vec3(p.x - w.x, 0.0, p.z - w.z);
        double len = d.length();
        if (len < 1.0e-4) {
            Direction f = facing(state);
            return new Vec3(f.getStepX(), 0.0, f.getStepZ());
        }
        return d.scale(1.0 / len);
    }

    public static Vec3 craftCameraPosition(BlockPos pos, BlockState state, double station) {
        Vec3 player = craftPlayerPosition(pos, state, station);
        Direction f = facing(state);
        Vec3 back = new Vec3(f.getStepX(), 0.0, f.getStepZ());
        Vec3 side = new Vec3(back.z, 0.0, -back.x);
        return player.add(back.scale(WorkbenchTuning.CRAFT_BEHIND.get()))
                .add(side.scale(WorkbenchTuning.CRAFT_SHIFT.get()))
                .add(0.0, WorkbenchTuning.CRAFT_HEIGHT.get(), 0.0);
    }

    public static Vec3 craftLookTarget(BlockPos pos, BlockState state, double station) {
        Vec3 work = workPoint(pos, state, station, 0.0);
        Vec3 player = craftPlayerPosition(pos, state, station);
        double k = WorkbenchTuning.CRAFT_LOOK_BLEND.get();
        return new Vec3(
                work.x + (player.x - work.x) * k,
                pos.getY() + WorkbenchTuning.CRAFT_LOOK_HEIGHT.get(),
                work.z + (player.z - work.z) * k);
    }

    public static Vec3 takeCameraPosition(BlockPos pos, BlockState state) {
        Vec3 player = craftPlayerPosition(pos, state, 1.0);
        Vec3 back = backFrom(pos, state, 1.0);
        Vec3 right = new Vec3(back.z, 0.0, -back.x);
        return player.add(back.scale(WorkbenchTuning.TAKE_BACK.get()))
                .add(right.scale(WorkbenchTuning.TAKE_SIDE.get()))
                .add(0.0, WorkbenchTuning.TAKE_HEIGHT.get(), 0.0);
    }

    public static Vec3 takeLookTarget(BlockPos pos, BlockState state) {
        Vec3 player = craftPlayerPosition(pos, state, 1.0);
        Vec3 back = backFrom(pos, state, 1.0);
        Vec3 right = new Vec3(back.z, 0.0, -back.x);
        return player.add(back.scale(-WorkbenchTuning.TAKE_LOOK_FORWARD.get()))
                .add(right.scale(WorkbenchTuning.TAKE_LOOK_SIDE.get()))
                .add(0.0, WorkbenchTuning.TAKE_LOOK_HEIGHT.get(), 0.0);
    }

    public static Vec3 tinkerCameraPosition(BlockPos pos, BlockState state) {
        Vec3 player = craftPlayerPosition(pos, state, 1.0);
        Vec3 back = backFrom(pos, state, 1.0);
        Vec3 right = new Vec3(back.z, 0.0, -back.x);
        return player.add(back.scale(WorkbenchTuning.TINKER_BACK.get()))
                .add(right.scale(WorkbenchTuning.TINKER_SIDE.get()))
                .add(0.0, WorkbenchTuning.TINKER_HEIGHT.get(), 0.0);
    }

    public static Vec3 tinkerLookTarget(BlockPos pos, BlockState state) {
        return workPoint(pos, state, 1.0, RESULT_HEIGHT - TABLE_TOP + 0.02);
    }

    public static Vec3 lookTarget(BlockPos pos, BlockState state) {
        return worldPoint(pos, state,
                WorkbenchTuning.LOOK_FORWARD_OFFSET.get(),
                focusAcross() + WorkbenchTuning.LOOK_SIDE_OFFSET.get(),
                WorkbenchTuning.LOOK_TARGET_HEIGHT.get());
    }
}
