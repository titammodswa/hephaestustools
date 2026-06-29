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

    public static final DeferredItem<Item> REPAIR_KIT =
            ITEMS.register("repair_kit", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PATTERN =
            ITEMS.register("pattern", () -> new Item(new Item.Properties().stacksTo(64)));
}