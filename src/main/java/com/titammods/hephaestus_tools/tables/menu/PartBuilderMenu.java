package com.titammods.hephaestus_tools.tables.menu;

import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.tables.blockentity.PartBuilderBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PartBuilderMenu extends AbstractContainerMenu {

    private final PartBuilderBlockEntity blockEntity;

    private static final int PATTERN_X  = 8;
    private static final int PATTERN_Y  = 43;
    private static final int MATERIAL_X = 29;
    private static final int MATERIAL_Y = 43;
    private static final int OUTPUT_X   = 148;
    private static final int OUTPUT_Y   = 42;

    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 102;
    private static final int HOTBAR_Y     = 160;

    private final Slot patternSlot;
    private final Slot inputSlot;

    public PartBuilderMenu(int id, Inventory playerInv, PartBuilderBlockEntity be) {
        super(ModMenus.PART_BUILDER.get(), id);
        this.blockEntity = be;

        this.addSlot(new Slot(be, PartBuilderBlockEntity.MATERIAL_SLOT, MATERIAL_X, MATERIAL_Y));
        this.inputSlot = this.slots.get(this.slots.size() - 1);
        this.addSlot(new Slot(be, PartBuilderBlockEntity.PATTERN_SLOT, PATTERN_X, PATTERN_Y));
        this.patternSlot = this.slots.get(this.slots.size() - 1);
        this.addSlot(new Slot(be, PartBuilderBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                be.craft(player);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col,
                    PLAYER_INV_X + col * 18, HOTBAR_Y));
        }

        this.addDataSlot(new DataSlot() {
            @Override public int get() { return blockEntity.getSelectedIndex(); }
            @Override public void set(int value) { blockEntity.selectRecipe(value); }
        });
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        java.util.List<?> buttons = blockEntity.getSortedButtons();
        if (id >= 0 && id < buttons.size()) {
            blockEntity.selectRecipe(id);
            return true;
        }
        return false;
    }

    public Slot getPatternSlot() { return patternSlot; }
    public Slot getInputSlot()   { return inputSlot; }

    @Override
    public boolean stillValid(Player player) { return blockEntity.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int mesaSlots = PartBuilderBlockEntity.TOTAL_SLOTS;
            if (index == PartBuilderBlockEntity.OUTPUT_SLOT) {
                if (!this.moveItemStackTo(stack, mesaSlots, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onTake(player, stack);
            } else if (index < mesaSlots) {
                if (!this.moveItemStackTo(stack, mesaSlots, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, PartBuilderBlockEntity.OUTPUT_SLOT, false))
                    return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    public PartBuilderBlockEntity getBlockEntity() { return blockEntity; }
}