package com.titammods.hephaestus_tools.tables.menu;

import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.tables.blockentity.TinkersAnvilBlockEntity;
import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import com.titammods.hephaestus_tools.tables.layout.ToolLayouts;
import com.titammods.hephaestus_tools.tables.menu.slot.TinkersAnvilSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TinkersAnvilMenu extends AbstractContainerMenu {

    public static final int TOOL_SLOT   = 0;
    public static final int INPUT_START = 1;
    public static final int MAX_INPUTS  = 6;
    public static final int OUTPUT_IDX  = INPUT_START + MAX_INPUTS;
    public static final int TOTAL_SLOTS = OUTPUT_IDX + 1; 

    public static final int OUTPUT_X = 114;
    public static final int OUTPUT_Y = 38;

    private static final int STILL_FILLED_X       = 112;
    private static final int STILL_FILLED_Y       = 62;
    private static final int STILL_FILLED_SPACING = 18;

    private static final int INV_X_OFFSET = 8;
    private static final int INV_Y_OFFSET = 102;

    private final TinkersAnvilBlockEntity blockEntity;
    private final List<TinkersAnvilSlot> inputSlots = new ArrayList<>();
    private ToolLayout currentLayout;
    private int activeInputs;

    public TinkersAnvilMenu(int id, Inventory playerInv, TinkersAnvilBlockEntity be) {
        super(ModMenus.TINKERS_ANVIL.get(), id);
        this.blockEntity = be;
        this.currentLayout = ToolLayouts.REPAIR;

        this.addSlot(new TinkersAnvilSlot(be, TinkersAnvilBlockEntity.TOOL_SLOT, 0, 0));

        for (int i = 0; i < MAX_INPUTS; i++) {
            TinkersAnvilSlot slot = new TinkersAnvilSlot(be,
                    TinkersAnvilBlockEntity.INPUT_START + i, 0, 0);
            inputSlots.add(slot);
            this.addSlot(slot);
        }

        Slot outputSlot = new Slot(be, TinkersAnvilBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                be.craft(player);
                super.onTake(player, stack);
            }
        };
        this.addSlot(outputSlot);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        INV_X_OFFSET + col * 18, INV_Y_OFFSET + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col,
                    INV_X_OFFSET + col * 18, INV_Y_OFFSET + 58));
        }

        setToolSelection(currentLayout);
    }

    public void setToolSelection(ToolLayout layout) {
        this.currentLayout = layout;
        this.activeInputs = Math.min(layout.inputs().size(), MAX_INPUTS);

        int stillFilled = 0;

        Slot toolSlot = this.slots.get(TOOL_SLOT);
        ToolLayout.ToolSlotDef toolDef = layout.toolSlot();
        if (toolDef.hidden()) {
            setSlotPos(toolSlot, -9999, -9999);
            if (toolSlot instanceof TinkersAnvilSlot ts) ts.deactivate();
        } else {
            setSlotPos(toolSlot, toolDef.x(), toolDef.y());
            if (toolSlot instanceof TinkersAnvilSlot ts) ts.activate(null);
        }
        for (int i = 0; i < MAX_INPUTS; i++) {
            TinkersAnvilSlot slot = inputSlots.get(i);
            if (i < layout.inputs().size()) {
                ToolLayout.InputSlotDef def = layout.inputs().get(i);
                setSlotPos(slot, def.x(), def.y());
                slot.activate(def);
            } else {
                if (slot.hasItem()) {
                    setSlotPos(slot, STILL_FILLED_X - STILL_FILLED_SPACING * stillFilled, STILL_FILLED_Y);
                    stillFilled++;
                } else {
                    setSlotPos(slot, STILL_FILLED_X, STILL_FILLED_Y);
                }
                slot.deactivate();
            }
        }
    }

    public static void setSlotPos(Slot slot, int x, int y) {
        try {
            java.lang.reflect.Field fx = Slot.class.getDeclaredField("x");
            java.lang.reflect.Field fy = Slot.class.getDeclaredField("y");
            fx.setAccessible(true);
            fy.setAccessible(true);
            fx.setInt(slot, x);
            fy.setInt(slot, y);
        } catch (Exception ignored) {}
    }

    public ToolLayout getCurrentLayout()          { return currentLayout; }
    public int getActiveInputs()                  { return activeInputs; }
    public List<TinkersAnvilSlot> getInputSlots() { return inputSlots; }
    public TinkersAnvilBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= ToolLayouts.ALL.size()) return false;
        ToolLayout layout = ToolLayouts.ALL.get(id);
        setToolSelection(layout);
        blockEntity.setCurrentLayout(layout);
        return true;
    }

    @Override
    public boolean stillValid(Player player) { return blockEntity.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == OUTPUT_IDX) {
                if (!this.moveItemStackTo(stack, TOTAL_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onTake(player, stack);
            } else if (index < TOTAL_SLOTS) {
                if (!this.moveItemStackTo(stack, TOTAL_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, OUTPUT_IDX, false))
                    return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}