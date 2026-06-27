package com.titammods.hephaestus_tools.tables.menu.slot;

import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import com.titammods.hephaestus_tools.tables.blockentity.TinkersAnvilBlockEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class TinkersAnvilSlot extends Slot {

    @Nullable
    private ToolLayout.InputSlotDef slotDef = null;
    private boolean active = false;

    public TinkersAnvilSlot(TinkersAnvilBlockEntity tile, int index, int x, int y) {
        super(tile, index, x, y);
    }

    public boolean isDormant() {
        return !active;
    }

    public void activate(@Nullable ToolLayout.InputSlotDef def) {
        this.slotDef = def;
        this.active = true;
    }

    public void deactivate() {
        this.slotDef = null;
        this.active = false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!active) return false;
        if (slotDef == null) return true;
        if (slotDef.expectedPart() != null) {
            return stack.getItem() == slotDef.expectedPart();
        }
        return true;
    }
}