package com.titammods.hephaestus_tools.tables.menu;

import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.tables.blockentity.PartBuilderBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PartBuilderMenu extends AbstractContainerMenu {

    private final PartBuilderBlockEntity blockEntity;

    private static final int MATERIAL_X  = 20;
    private static final int MATERIAL_Y  = 48;
    private static final int PATTERN_X   = 47;
    private static final int PATTERN_Y   = 48;
    private static final int OUTPUT_X    = 134;
    private static final int OUTPUT_Y    = 48;

    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y     = 142;

    public PartBuilderMenu(int id, Inventory playerInv, PartBuilderBlockEntity be) {
        super(ModMenus.PART_BUILDER.get(), id);
        this.blockEntity = be;

        this.addSlot(new Slot(be, PartBuilderBlockEntity.MATERIAL_SLOT, MATERIAL_X, MATERIAL_Y));
        this.addSlot(new Slot(be, PartBuilderBlockEntity.PATTERN_SLOT,  PATTERN_X,  PATTERN_Y));
        this.addSlot(new Slot(be, PartBuilderBlockEntity.OUTPUT_SLOT,   OUTPUT_X,   OUTPUT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
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
            int mesaSlots = PartBuilderBlockEntity.TOTAL_SLOTS;
            if (index < mesaSlots) {
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