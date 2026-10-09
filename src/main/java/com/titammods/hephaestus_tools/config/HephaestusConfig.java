package com.titammods.hephaestus_tools.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HephaestusConfig {

    public static final String FILE_NAME = "hephaestusconfig.toml";

    public enum TableInterface { MODERN, CLASSIC }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<TableInterface> TABLE_INTERFACE;
    public static final ModConfigSpec.BooleanValue SKIP_CINEMATICS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("hephaestus_tools.configuration.forge_table").push("forge_table");
        TABLE_INTERFACE = builder
                .comment(" Forge Table interface")
                .translation("hephaestus_tools.configuration.table_interface")
                .defineEnum("table_interface", TableInterface.MODERN);
        SKIP_CINEMATICS = builder
                .comment(" Skip the craft cinematics (MODERN only)")
                .translation("hephaestus_tools.configuration.skip_cinematics")
                .define("skip_cinematics", false);
        builder.pop();
        SPEC = builder.build();
    }

    private HephaestusConfig() {}

    public static boolean modernInterface() {
        try {
            return TABLE_INTERFACE.get() == TableInterface.MODERN;
        } catch (IllegalStateException e) {
            return TABLE_INTERFACE.getDefault() == TableInterface.MODERN;
        }
    }

    public static boolean skipCinematics() {
        try {
            return SKIP_CINEMATICS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static void setSkipCinematics(boolean value) {
        try {
            SKIP_CINEMATICS.set(value);
            SKIP_CINEMATICS.save();
        } catch (IllegalStateException ignored) {
        }
    }
}