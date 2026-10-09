package com.titammods.hephaestus_tools.registry;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HephaestusTools.MOD_ID);

    public static final Supplier<SoundEvent> ARSENAL_TABLE_OPEN  = register("arsenal_table_open");
    public static final Supplier<SoundEvent> ARSENAL_TABLE_CLOSE = register("arsenal_table_close");
    public static final Supplier<SoundEvent> ARSENAL_TABLE_CRAFT = register("arsenal_table_craft");
    public static final Supplier<SoundEvent> ARSENAL_TABLE_CRAFT_HOLD = register("arsenal_table_craft_hold");
    public static final Supplier<SoundEvent> WB_PART_SNAP = register("workbench.part_snap");
    public static final Supplier<SoundEvent> WB_PART_UNSNAP = register("workbench.part_unsnap");
    public static final Supplier<SoundEvent> WB_TOOL_POP = register("workbench.tool_pop");
    public static final Supplier<SoundEvent> WB_WHOOSH = register("workbench.whoosh");
    public static final Supplier<SoundEvent> WB_INFUSE = register("workbench.infuse");
    public static final Supplier<SoundEvent> WB_CRAFT_DONE = register("workbench.craft_done");
    public static final Supplier<SoundEvent> WB_TINKER_TAP = register("workbench.tinker_tap");
    public static final Supplier<SoundEvent> WB_HOLO_BLIP = register("workbench.holo_blip");

    private static Supplier<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
