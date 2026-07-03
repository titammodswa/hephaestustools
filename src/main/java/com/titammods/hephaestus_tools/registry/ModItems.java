package com.titammods.hephaestus_tools.registry;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tools.item.ToolItems;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(HephaestusTools.MOD_ID);

    private static Item.Properties toolProps() {
        return new Item.Properties()
                .stacksTo(1)
                .durability(100)
                .setNoRepair();
    }

    public static final DeferredItem<ToolItems.PickaxeItem> PICKAXE =
            ITEMS.register("pickaxe", () -> new ToolItems.PickaxeItem(toolProps()));

    public static final DeferredItem<ToolItems.SledgeHammerItem> SLEDGE_HAMMER =
            ITEMS.register("sledge_hammer", () -> new ToolItems.SledgeHammerItem(toolProps()));

    public static final DeferredItem<ToolItems.VeinHammerItem> VEIN_HAMMER =
            ITEMS.register("vein_hammer", () -> new ToolItems.VeinHammerItem(toolProps()));

    public static final DeferredItem<ToolItems.MattockItem> MATTOCK =
            ITEMS.register("mattock", () -> new ToolItems.MattockItem(toolProps()));

    public static final DeferredItem<ToolItems.ExcavatorItem> EXCAVATOR =
            ITEMS.register("excavator", () -> new ToolItems.ExcavatorItem(toolProps()));

    public static final DeferredItem<ToolItems.HandAxeItem> HAND_AXE =
            ITEMS.register("hand_axe", () -> new ToolItems.HandAxeItem(toolProps()));

    public static final DeferredItem<ToolItems.BroadAxeItem> BROAD_AXE =
            ITEMS.register("broad_axe", () -> new ToolItems.BroadAxeItem(toolProps()));

    public static final DeferredItem<ToolItems.KamaItem> KAMA =
            ITEMS.register("kama", () -> new ToolItems.KamaItem(toolProps()));

    public static final DeferredItem<ToolItems.ScytheItem> SCYTHE =
            ITEMS.register("scythe", () -> new ToolItems.ScytheItem(toolProps()));

    public static final DeferredItem<ToolItems.DaggerItem> DAGGER =
            ITEMS.register("dagger", () -> new ToolItems.DaggerItem(toolProps()));

    public static final DeferredItem<ToolItems.SwordItem> SWORD =
            ITEMS.register("sword", () -> new ToolItems.SwordItem(toolProps()));

    public static final DeferredItem<ToolItems.CleaverItem> CLEAVER =
            ITEMS.register("cleaver", () -> new ToolItems.CleaverItem(toolProps()));

    public static final DeferredItem<ToolPartItem> PICK_HEAD =
            ITEMS.register("pick_head", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> HAMMER_HEAD =
            ITEMS.register("hammer_head", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> SMALL_AXE_HEAD =
            ITEMS.register("small_axe_head", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> ADZE_HEAD =
            ITEMS.register("adze_head", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> LARGE_PLATE =
            ITEMS.register("large_plate", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> BROAD_AXE_HEAD =
            ITEMS.register("broad_axe_head", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> SMALL_BLADE =
            ITEMS.register("small_blade", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> LARGE_BLADE =
            ITEMS.register("large_blade", () -> new ToolPartItem(new Item.Properties(), 0));

    public static final DeferredItem<ToolPartItem> TOOL_HANDLE =
            ITEMS.register("tool_handle", () -> new ToolPartItem(new Item.Properties(), 1));

    public static final DeferredItem<ToolPartItem> TOUGH_HANDLE =
            ITEMS.register("tough_handle", () -> new ToolPartItem(new Item.Properties(), 1));

    public static final DeferredItem<ToolPartItem> TOOL_BINDING =
            ITEMS.register("tool_binding", () -> new ToolPartItem(new Item.Properties(), 2));

    public static final DeferredItem<ToolPartItem> TOUGH_BINDING =
            ITEMS.register("tough_binding", () -> new ToolPartItem(new Item.Properties(), 2));

    public static final DeferredItem<Item> PATTERN =
            ITEMS.register("pattern", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> PICK_HEAD_CAST =
            ITEMS.register("pick_head_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HAMMER_HEAD_CAST =
            ITEMS.register("hammer_head_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SMALL_AXE_HEAD_CAST =
            ITEMS.register("small_axe_head_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BROAD_AXE_HEAD_CAST =
            ITEMS.register("broad_axe_head_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ADZE_HEAD_CAST =
            ITEMS.register("adze_head_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LARGE_PLATE_CAST =
            ITEMS.register("large_plate_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SMALL_BLADE_CAST =
            ITEMS.register("small_blade_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LARGE_BLADE_CAST =
            ITEMS.register("large_blade_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOOL_HANDLE_CAST =
            ITEMS.register("tool_handle_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOUGH_HANDLE_CAST =
            ITEMS.register("tough_handle_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOOL_BINDING_CAST =
            ITEMS.register("tool_binding_cast", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOUGH_BINDING_CAST =
            ITEMS.register("tough_binding_cast", () -> new Item(new Item.Properties()));

    public static final java.util.List<DeferredItem<Item>> CASTS = java.util.List.of(
            PICK_HEAD_CAST, HAMMER_HEAD_CAST, SMALL_AXE_HEAD_CAST, BROAD_AXE_HEAD_CAST,
            ADZE_HEAD_CAST, LARGE_PLATE_CAST, SMALL_BLADE_CAST, LARGE_BLADE_CAST,
            TOOL_HANDLE_CAST, TOUGH_HANDLE_CAST, TOOL_BINDING_CAST, TOUGH_BINDING_CAST);
}