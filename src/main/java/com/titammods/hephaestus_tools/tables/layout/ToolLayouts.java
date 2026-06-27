package com.titammods.hephaestus_tools.tables.layout;

import com.titammods.hephaestus_tools.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ToolLayouts {

    private ToolLayouts() {}

    private static final String NS = "hephaestus_tools";

    private static ResourceLocation pat(String name) {
        return ResourceLocation.fromNamespaceAndPath(NS, "gui/tinker_pattern/" + name);
    }

    private static final ResourceLocation PICK_HEAD      = pat("pick_head");
    private static final ResourceLocation HAMMER_HEAD    = pat("hammer_head");
    private static final ResourceLocation SMALL_AXE_HEAD = pat("small_axe_head");
    private static final ResourceLocation BROAD_AXE_HEAD = pat("broad_axe_head");
    private static final ResourceLocation ADZE_HEAD      = pat("adze_head");
    private static final ResourceLocation SMALL_BLADE    = pat("small_blade");
    private static final ResourceLocation LARGE_BLADE    = pat("large_blade");
    private static final ResourceLocation BROAD_BLADE    = pat("broad_blade");
    private static final ResourceLocation TOOL_HANDLE    = pat("tool_handle");
    private static final ResourceLocation TOUGH_HANDLE   = pat("tough_handle");
    private static final ResourceLocation TOOL_BINDING   = pat("tool_binding");
    private static final ResourceLocation TOUGH_BINDING  = pat("tough_binding");
    private static final ResourceLocation LARGE_PLATE    = pat("large_plate");
    private static final ResourceLocation INGOT          = pat("ingot");
    private static final ResourceLocation GEM            = pat("gem");
    private static final ResourceLocation QUARTZ         = pat("quartz");
    private static final ResourceLocation DUST           = pat("dust");
    private static final ResourceLocation LAPIS          = pat("lapis");
    private static final ResourceLocation RESULT         = pat("result");
    private static final ResourceLocation BTN_REPAIR     = pat("button_repair");
    private static final ResourceLocation PICKAXE_ICON   = pat("pickaxe");

    private static ToolLayout make(String id, Item icon,
                                   ToolLayout.ToolSlotDef toolSlot,
                                   List<ToolLayout.InputSlotDef> inputs) {
        return new ToolLayout(id,
                Component.translatable("layout.hephaestus_tools." + id),
                Component.translatable("layout.hephaestus_tools." + id + ".description"),
                icon, toolSlot, inputs);
    }

    private static ToolLayout.InputSlotDef in(int x, int y, Item part, ResourceLocation icon) {
        return new ToolLayout.InputSlotDef(x, y, part, icon);
    }
    private static ToolLayout.InputSlotDef in(int x, int y, ResourceLocation icon) {
        return new ToolLayout.InputSlotDef(x, y, null, icon);
    }

    private static final ToolLayout.ToolSlotDef HIDDEN = new ToolLayout.ToolSlotDef(-1, -1, true, null);

    public static final ToolLayout REPAIR = make("repair", Items.EXPERIENCE_BOTTLE,
            new ToolLayout.ToolSlotDef(33, 41, false, PICKAXE_ICON),
            List.of(
                    in(15, 62, QUARTZ),
                    in(11, 37, DUST),
                    in(33, 19, LAPIS),
                    in(55, 37, INGOT),
                    in(51, 62, GEM)
            ));

    public static final ToolLayout PICKAXE = make("pickaxe", ModItems.PICKAXE.get(), HIDDEN,
            List.of(
                    in(53, 22, ModItems.PICK_HEAD.get(),    PICK_HEAD),
                    in(15, 60, ModItems.TOOL_HANDLE.get(),  TOOL_HANDLE),
                    in(33, 42, ModItems.TOOL_BINDING.get(), TOOL_BINDING)
            ));

    public static final ToolLayout SLEDGE_HAMMER = make("sledge_hammer", ModItems.SLEDGE_HAMMER.get(), HIDDEN,
            List.of(
                    in(44, 29, ModItems.HAMMER_HEAD.get(),   HAMMER_HEAD),
                    in(21, 52, ModItems.TOUGH_HANDLE.get(),  TOUGH_HANDLE),
                    in(50, 48, ModItems.TOUGH_BINDING.get(), LARGE_PLATE),
                    in(25, 20, ModItems.TOUGH_HANDLE.get(),  LARGE_PLATE)
            ));

    public static final ToolLayout VEIN_HAMMER = make("vein_hammer", ModItems.VEIN_HAMMER.get(), HIDDEN,
            List.of(
                    in(44, 29, ModItems.HAMMER_HEAD.get(),   HAMMER_HEAD),
                    in(21, 52, ModItems.TOUGH_HANDLE.get(),  TOUGH_HANDLE),
                    in(41, 49, ModItems.TOUGH_BINDING.get(), TOUGH_BINDING),
                    in(25, 20, ModItems.TOUGH_HANDLE.get(),  LARGE_PLATE)
            ));

    public static final ToolLayout MATTOCK = make("mattock", ModItems.MATTOCK.get(), HIDDEN,
            List.of(
                    in(31, 22, ModItems.SMALL_AXE_HEAD.get(), SMALL_AXE_HEAD),
                    in(22, 53, ModItems.TOOL_HANDLE.get(),    TOOL_HANDLE),
                    in(51, 34, ModItems.SMALL_AXE_HEAD.get(), ADZE_HEAD)
            ));

    public static final ToolLayout EXCAVATOR = make("excavator", ModItems.EXCAVATOR.get(), HIDDEN,
            List.of(
                    in(45, 26, ModItems.HAMMER_HEAD.get(),   LARGE_PLATE),
                    in(25, 46, ModItems.TOUGH_HANDLE.get(),  TOUGH_HANDLE),
                    in(25, 26, ModItems.TOUGH_BINDING.get(), TOUGH_BINDING),
                    in( 7, 62, ModItems.TOUGH_HANDLE.get(),  TOUGH_HANDLE)
            ));

    public static final ToolLayout HAND_AXE = make("hand_axe", ModItems.HAND_AXE.get(), HIDDEN,
            List.of(
                    in(31, 22, ModItems.SMALL_AXE_HEAD.get(), SMALL_AXE_HEAD),
                    in(22, 53, ModItems.TOOL_HANDLE.get(),    TOOL_HANDLE),
                    in(51, 34, ModItems.TOOL_BINDING.get(),   TOOL_BINDING)
            ));

    public static final ToolLayout BROAD_AXE = make("broad_axe", ModItems.BROAD_AXE.get(), HIDDEN,
            List.of(
                    in(25, 20, ModItems.BROAD_AXE_HEAD.get(), BROAD_AXE_HEAD),
                    in(21, 52, ModItems.TOUGH_HANDLE.get(),   TOUGH_HANDLE),
                    in(50, 48, ModItems.PICK_HEAD.get(),      PICK_HEAD),
                    in(44, 29, ModItems.TOUGH_BINDING.get(),  LARGE_PLATE)
            ));

    public static final ToolLayout KAMA = make("kama", ModItems.KAMA.get(), HIDDEN,
            List.of(
                    in(31, 22, ModItems.SMALL_BLADE.get(),   SMALL_BLADE),
                    in(22, 53, ModItems.TOOL_HANDLE.get(),   TOOL_HANDLE),
                    in(51, 34, ModItems.TOOL_BINDING.get(),  TOOL_BINDING)
            ));

    public static final ToolLayout SCYTHE = make("scythe", ModItems.SCYTHE.get(), HIDDEN,
            List.of(
                    in(35, 20, ModItems.LARGE_BLADE.get(),    BROAD_BLADE),
                    in(12, 55, ModItems.TOUGH_HANDLE.get(),   TOUGH_HANDLE),
                    in(50, 40, ModItems.TOUGH_BINDING.get(),  TOUGH_BINDING),
                    in(30, 40, ModItems.TOUGH_HANDLE.get(),   TOUGH_HANDLE)
            ));

    public static final ToolLayout DAGGER = make("dagger", ModItems.DAGGER.get(), HIDDEN,
            List.of(
                    in(39, 35, ModItems.SMALL_BLADE.get(),  SMALL_BLADE),
                    in(21, 53, ModItems.TOOL_HANDLE.get(),  TOOL_HANDLE)
            ));

    public static final ToolLayout SWORD = make("sword", ModItems.SWORD.get(), HIDDEN,
            List.of(
                    in(48, 26, ModItems.SMALL_BLADE.get(),  SMALL_BLADE),
                    in(12, 62, ModItems.TOOL_HANDLE.get(),  TOOL_HANDLE),
                    in(30, 44, ModItems.TOOL_HANDLE.get(),  TOOL_HANDLE)
            ));

    public static final ToolLayout CLEAVER = make("cleaver", ModItems.CLEAVER.get(), HIDDEN,
            List.of(
                    in(45, 26, ModItems.LARGE_BLADE.get(),    BROAD_BLADE),
                    in( 7, 62, ModItems.TOUGH_HANDLE.get(),   TOUGH_HANDLE),
                    in(25, 46, ModItems.TOUGH_HANDLE.get(),   TOUGH_HANDLE),
                    in(45, 46, ModItems.TOUGH_BINDING.get(),  LARGE_PLATE)
            ));

    public static final List<ToolLayout> ALL = List.of(
            REPAIR,
            PICKAXE, SLEDGE_HAMMER, VEIN_HAMMER,
            MATTOCK, EXCAVATOR,
            HAND_AXE, BROAD_AXE,
            KAMA, SCYTHE,
            DAGGER, SWORD, CLEAVER
    );
}