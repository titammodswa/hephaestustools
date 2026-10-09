package com.titammods.hephaestus_tools.tables.scene;

import java.util.Locale;
import java.util.Optional;

public enum WorkbenchTuning {
    FOCUS_SIDE_OFFSET(0.50, -1.00, 1.00),
    PLAYER_DISTANCE(0.58, 0.40, 1.50),
    PLAYER_SIDE_OFFSET(-0.80, -1.60, 1.40),
    PLAYER_TURN(0.90, 0.0, 1.20),
    CAMERA_DISTANCE(1.10, 0.40, 3.00),
    CAMERA_SIDE_OFFSET(0.12, -1.50, 1.50),
    CAMERA_HEIGHT(2.20, 1.00, 3.50),
    LOOK_TARGET_HEIGHT(0.90, 0.50, 1.50),
    LOOK_FORWARD_OFFSET(0.05, -0.60, 0.80),
    LOOK_SIDE_OFFSET(0.10, -1.00, 1.00),
    CAMERA_FOV(46.0, 20.0, 100.0),
    CRAFT_BEHIND(1.75, 0.40, 3.50),
    CRAFT_HEIGHT(2.05, 1.00, 4.00),
    CRAFT_SHIFT(0.0, -1.50, 1.50),
    CRAFT_LOOK_BLEND(0.70, 0.0, 1.20),
    CRAFT_LOOK_HEIGHT(1.25, 0.50, 2.00),
    ANVIL_ACROSS(-0.50, -1.00, 1.00),
    ANVIL_FORWARD(0.03, -0.60, 0.60),
    ANVIL_TOP(1.50, 1.00, 2.00),
    CRAFT_ANVIL_SHARE(0.55, 0.0, 0.85),
    TAKE_BACK(-0.30, -1.00, 2.50),
    TAKE_SIDE(1.50, -1.50, 2.00),
    TAKE_HEIGHT(1.75, 1.00, 3.00),
    TAKE_LOOK_FORWARD(0.45, -0.50, 1.50),
    TAKE_LOOK_SIDE(0.0, -1.00, 1.00),
    TAKE_LOOK_HEIGHT(1.40, 0.80, 2.20),
    TAKE_FOV(40.0, 20.0, 90.0),
    TINKER_BACK(0.50, 0.10, 2.00),
    TINKER_SIDE(0.42, -1.50, 1.50),
    TINKER_HEIGHT(1.95, 1.00, 3.00),
    TINKER_FOV(46.0, 20.0, 90.0),
    TINKER_SECONDS(5.6, 2.00, 10.00),
    TINKER_TAP_RATE(3.0, 1.00, 6.00),
    ANIM_LIFE(1.0, 0.0, 2.5),
    CRAFT_PLAYER_ACROSS(0.0, -1.00, 1.00),
    CRAFT_TURN(0.60, 0.0, 1.0),
    TRANSITION_ARC(0.22, 0.0, 1.0),
    CRAFT_FOV(52.0, 25.0, 90.0),
    CRAFT_SECONDS(7.2, 1.50, 8.00),
    CRAFT_STRIKE_RATE(1.7, 1.00, 4.00),
    INSPECT_DISTANCE(0.95, 0.20, 2.50),
    INSPECT_HEIGHT(2.05, 1.20, 3.50),
    INSPECT_FOV(34.0, 10.0, 80.0),
    AUTO_SPIN_SPEED(28.0, 0.0, 180.0),
    FOCUS_DISTANCE(0.95, 0.20, 2.00),
    FOCUS_HEIGHT(1.95, 1.20, 3.00),
    FOCUS_SIDE_SHIFT(0.0, -1.00, 1.00),
    FOCUS_LOOK_HEIGHT(1.08, 0.80, 1.50),
    FOCUS_FOV(40.0, 15.0, 80.0),
    FOCUS_RATE(3.2, 0.5, 10.0),
    ENTER_SECONDS(1.35, 0.15, 3.00),
    EXIT_SECONDS(0.35, 0.10, 3.00),
    WALK_SPEED(2.8, 1.0, 8.0),
    ARM_PITCH(-1.05, -1.60, 0.20),
    ARM_INWARD(0.30, -0.40, 0.80),
    BODY_LEAN(0.10, 0.0, 0.70),
    PARALLAX(1.0, 0.0, 3.0);

    private static final double[] VALUES = new double[values().length];

    static {
        for (WorkbenchTuning t : values()) VALUES[t.ordinal()] = t.defaultValue;
    }

    private final double defaultValue;
    private final double min;
    private final double max;

    WorkbenchTuning(double defaultValue, double min, double max) {
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public double get() {
        return VALUES[ordinal()];
    }

    public void set(double value) {
        VALUES[ordinal()] = Math.max(min, Math.min(max, value));
    }

    public double defaultValue() {
        return defaultValue;
    }

    public static void resetAll() {
        for (WorkbenchTuning t : values()) VALUES[t.ordinal()] = t.defaultValue;
    }

    public static Optional<WorkbenchTuning> byName(String name) {
        String wanted = name.toUpperCase(Locale.ROOT);
        for (WorkbenchTuning t : values()) if (t.name().equals(wanted)) return Optional.of(t);
        return Optional.empty();
    }
}
