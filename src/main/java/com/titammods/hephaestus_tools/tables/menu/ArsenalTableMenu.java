package com.titammods.hephaestus_tools.tables.menu;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.table.ArsenalTableLayout;
import com.titammods.hephaestus_tools.table.ToolAssembly;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import java.util.ArrayList;
import java.util.List;

public class ArsenalTableMenu extends AbstractContainerMenu {
    public static final int SELECT_BASE = 100, REPAIR = 150, ASSEMBLE = 160;
    public static final int MACHINE_SLOTS = 6;
    public static final int TAB_BASE = 10, APPLY_BASE = 200;
    public static final int MASTERY_SELECT_BASE = 300;
    private int activeTab = 0;
    public static final int REPAIR_DIVISOR = 4;
    private final ArsenalTableBlockEntity blockEntity;
    private int selectedTool = 0;

    public ArsenalTableMenu(int id, Inventory inv, ArsenalTableBlockEntity be) {
        super(ModMenus.ARSENAL_TABLE.get(), id);
        blockEntity = be;
        ToolAssembly.init();
        addSlot(new SlotItemHandler(be.getUpgradeSlot(), 0, 112, 56) {
            @Override public boolean isActive() { return activeTab != 0; }
            @Override public boolean mayPlace(ItemStack s) {
                return isActive() && s.getItem() instanceof ModifiableItem && ToolStack.isInitialized(s);
            }
        });
        int[][] positions = ArsenalTableLayout.hexSlotPositions();
        for (int i = 0; i < 4; i++) {
            final int part = i;
            addSlot(new SlotItemHandler(be.getInputSlots(), i,
                    positions[i][0] + ArsenalTableLayout.SLOT_ITEM_INSET,
                    positions[i][1] + ArsenalTableLayout.SLOT_ITEM_INSET_Y) {
                @Override public boolean isActive() { return activeTab == 0 && (isRepair() ? part == 0 : part < parts().size()); }
                @Override public boolean mayPlace(ItemStack s) { return isActive() && accepts(part, s); }
            });
        }
        addSlot(new SlotItemHandler(be.getOutputSlot(), 0,
                ArsenalTableLayout.OUTPUT_X + ArsenalTableLayout.OUTPUT_ITEM_INSET,
                ArsenalTableLayout.OUTPUT_Y + ArsenalTableLayout.OUTPUT_ITEM_INSET) {
            @Override public boolean isActive() { return activeTab == 0; }
            @Override public boolean mayPlace(ItemStack s) {
                return isActive() && getItem().isEmpty() && s.getItem() instanceof ModifiableItem && ToolStack.isInitialized(s);
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col + row * 9 + 9, ArsenalTableLayout.INV_X + col * 18,
                    ArsenalTableLayout.INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, ArsenalTableLayout.INV_X + col * 18, ArsenalTableLayout.INV_Y + 58));
        addDataSlot(new DataSlot() {
            @Override public int get() { return selectedTool; }
            @Override public void set(int value) { selectedTool = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return activeTab; }
            @Override public void set(int value) { activeTab = value; }
        });
    }

    public int getActiveTab() { return activeTab; }
    public int getSelectedTool() { return selectedTool; }
    public boolean isRepair() { return activeTab == 0 && !output().isEmpty() && ToolStack.isInitialized(output()); }
    public ArsenalTableBlockEntity getBlockEntity() { return blockEntity; }
    public List<Item> parts() { return isRepair() ? List.of() : assembly().parts(); }
    public ToolAssembly assembly() { return ToolAssembly.REGISTRY.get(Math.max(0, selectedTool)); }
    public ItemStack input(int i) { return slots.get(i + 1).getItem(); }
    public ItemStack tool() { return activeTab == 0 ? output() : slots.get(0).getItem(); }
    public ItemStack output() { return slots.get(5).getItem(); }

    private boolean accepts(int part, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isRepair()) {
            if (part != 0 || !ToolStack.isInitialized(tool())) return false;
            if (swapIndexFor(tool(), stack) >= 0) return true;
            var material = MaterialManager.getInstance().getMaterial(ToolStack.getMaterial(tool(), 0));
            return material != null && material.ingredient().test(stack);
        }
        return part < parts().size() && stack.is(parts().get(part))
                && stack.getItem() instanceof ToolPartItem p && !p.getMaterial(stack).isEmpty()
                && MaterialManager.getInstance().getMaterial(p.getMaterial(stack)) != null;
    }

    private int swapIndexFor(ItemStack tool, ItemStack stack) {
        if (tool.isEmpty() || stack.isEmpty()) return -1;
        if (!(stack.getItem() instanceof ToolPartItem incoming)) return -1;
        MaterialId material = incoming.getMaterial(stack);
        if (material == null || material.isEmpty()) return -1;
        if (MaterialManager.getInstance().getMaterial(material) == null) return -1;

        List<Item> layout = ToolBuildHandler.getToolParts(tool.getItem());
        List<MaterialId> materials = ToolStack.getMaterials(tool);
        int count = Math.min(layout.size(), materials.size());
        for (int i = 0; i < count; i++) {
            if (layout.get(i) == stack.getItem() && !material.equals(materials.get(i))) return i;
        }
        return -1;
    }

    private ItemStack swapResult(ItemStack tool, ItemStack stack) {
        int index = swapIndexFor(tool, stack);
        if (index < 0) return ItemStack.EMPTY;

        List<MaterialId> materials = new ArrayList<>(ToolStack.getMaterials(tool));
        materials.set(index, ((ToolPartItem) stack.getItem()).getMaterial(stack));

        ItemStack result = tool.copyWithCount(1);
        int damage = ToolStack.getCurrentDamage(result);
        boolean broken = ToolStack.isBroken(result);
        ToolStack.setMaterials(result, materials);
        ToolStack.recalculate(result);
        int durability = ToolStack.getDurability(result);
        result.setDamageValue(broken ? damage : Math.min(damage, Math.max(0, durability - 1)));
        return result;
    }

    public ItemStack preview() {
        if (activeTab != 0) return ItemStack.EMPTY;
        if (isRepair()) {
            if (!ToolStack.isInitialized(tool()) || !(tool().getItem() instanceof ModifiableItem)) return ItemStack.EMPTY;
            for (int i = 1; i < 4; i++) if (!input(i).isEmpty()) return ItemStack.EMPTY;
            ItemStack swapped = swapResult(tool(), input(0));
            if (!swapped.isEmpty()) return swapped;
            if (ToolStack.getCurrentDamage(tool()) <= 0 || !accepts(0, input(0))) return ItemStack.EMPTY;
            ItemStack result = tool().copyWithCount(1);
            int restored = Math.max(1, (ToolStack.getDurability(result) + REPAIR_DIVISOR - 1) / REPAIR_DIVISOR);
            result.setDamageValue(Math.max(0, ToolStack.getCurrentDamage(result) - restored));
            return result;
        }
        if (!tool().isEmpty()) return ItemStack.EMPTY;
        List<MaterialId> materials = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i < parts().size()) {
                if (!accepts(i, input(i))) return ItemStack.EMPTY;
                materials.add(((ToolPartItem) input(i).getItem()).getMaterial(input(i)));
            } else if (!input(i).isEmpty()) return ItemStack.EMPTY;
        }
        return ToolStack.createTool(new ItemStack(assembly().result()), materials);
    }

    public boolean canAssemble() { return output().isEmpty() && !preview().isEmpty(); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (id >= TAB_BASE && id < TAB_BASE + 3) {
            int nextTab = id - TAB_BASE;
            if (nextTab == activeTab) return true;
            for (int i = 1; i <= 4; i++) returnSlot(player, i);
            int source = activeTab == 0 ? 5 : 0;
            int target = nextTab == 0 ? 5 : 0;
            if (source != target && !slots.get(source).getItem().isEmpty()) {
                if (!slots.get(target).getItem().isEmpty()) returnSlot(player, target);
                slots.get(target).set(slots.get(source).remove(slots.get(source).getItem().getCount()));
            }
            activeTab = nextTab;
            blockEntity.setChanged();
            broadcastChanges();
            return true;
        }
        if (activeTab == 2 && id >= MASTERY_SELECT_BASE && id < MASTERY_SELECT_BASE + 3) {
            ItemStack chosen = tool().copy();
            if (!com.titammods.hephaestus_tools.table.ToolMastery.choose(chosen,id-MASTERY_SELECT_BASE)) return false;
            blockEntity.getUpgradeSlot().setStackInSlot(0,chosen);
            blockEntity.setChanged();
            broadcastChanges();
            return true;
        }
        if (activeTab == 1 && id >= APPLY_BASE && id < MASTERY_SELECT_BASE) {
            var upgrades = com.titammods.hephaestus_tools.table.ToolUpgrades.availableFor(tool().getItem());
            int index = id - APPLY_BASE;
            if (index < 0 || index >= upgrades.size() || !ToolStack.isInitialized(tool())) return false;
            ItemStack upgraded = tool().copyWithCount(1);
            if (!com.titammods.hephaestus_tools.table.ToolUpgrades.apply(player, upgraded, upgrades.get(index))) return false;
            blockEntity.getUpgradeSlot().setStackInSlot(0, upgraded);
            blockEntity.setChanged();
            broadcastChanges();
            return true;
        }
        if (activeTab != 0) return false;
        int selection = id == REPAIR ? -1 : id - SELECT_BASE;
        if (id == REPAIR || (selection >= 0 && selection < ToolAssembly.REGISTRY.size())) {
            if (selection != selectedTool) {
                for (int i = 0; i < 6; i++) {
                    ItemStack old = slots.get(i).remove(slots.get(i).getItem().getCount());
                    if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
                }
                selectedTool = selection;
            }
            broadcastChanges();
            return true;
        }
        return false;
    }

    private void returnSlot(Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack old = slot.remove(slot.getItem().getCount());
        if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
    }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (slotId >= 0 && slotId < MACHINE_SLOTS && !slots.get(slotId).isActive()) return;
        if (slotId == 5 && output().isEmpty()) {
            ItemStack result = preview();
            boolean pickup = type == ClickType.PICKUP && (button == 0 || button == 1)
                    && getCarried().isEmpty();
            boolean quickMove = type == ClickType.QUICK_MOVE && hasInventoryRoom(result);
            boolean swap = type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)
                    && player.getInventory().getItem(button).isEmpty();
            if (!result.isEmpty() && (pickup || quickMove || swap)) {
                if (isRepair()) {
                    blockEntity.getUpgradeSlot().extractItem(0, 1, false);
                    blockEntity.getInputSlots().extractItem(0, 1, false);
                } else for (int i = 0; i < parts().size(); i++)
                    blockEntity.getInputSlots().extractItem(i, 1, false);
                blockEntity.getOutputSlot().setStackInSlot(0, result);
                blockEntity.setChanged();
            }
        }
        super.clicked(slotId, button, type, player);
        if (slotId == 1 && type == ClickType.PICKUP && isRepair()
                && !attemptPartSwap(player)) attemptRepair();
    }

    private boolean attemptPartSwap(Player player) {
        ItemStack current = output();
        ItemStack incoming = input(0);
        if (current.isEmpty() || incoming.isEmpty()) return false;

        int index = swapIndexFor(current, incoming);
        if (index < 0) return false;

        MaterialId previous = ToolStack.getMaterial(current, index);
        ItemStack swapped = swapResult(current, incoming);
        if (swapped.isEmpty()) return false;

        ItemStack returned = ((ToolPartItem) incoming.getItem()).withMaterial(previous);

        blockEntity.getOutputSlot().setStackInSlot(0, swapped);
        blockEntity.getInputSlots().extractItem(0, 1, false);
        if (!returned.isEmpty() && !player.getInventory().add(returned)) player.drop(returned, false);
        blockEntity.setChanged();
        broadcastChanges();
        return true;
    }

    private void attemptRepair() {
        ItemStack current = output();
        ItemStack material = input(0);
        if (current.isEmpty() || material.isEmpty() || ToolStack.getCurrentDamage(current) <= 0 || !accepts(0, material)) return;
        ItemStack repaired = current.copyWithCount(1);
        int restored = Math.max(1, (ToolStack.getDurability(repaired) + REPAIR_DIVISOR - 1) / REPAIR_DIVISOR);
        repaired.setDamageValue(Math.max(0, ToolStack.getCurrentDamage(repaired) - restored));
        blockEntity.getOutputSlot().setStackInSlot(0, repaired);
        blockEntity.getInputSlots().extractItem(0, 1, false);
        blockEntity.setChanged();
        broadcastChanges();
    }

    private boolean hasInventoryRoom(ItemStack result) {
        if (result.isEmpty()) return false;
        for (int i = MACHINE_SLOTS; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack current = slot.getItem();
            if (slot.mayPlace(result) && (current.isEmpty()
                    || ItemStack.isSameItemSameComponents(current, result)
                    && current.getCount() + result.getCount() <= slot.getMaxStackSize(result))) return true;
        }
        return false;
    }

    @Override public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(activeTab == 0 ? moveItemStackTo(stack, 1, 6, false)
                : moveItemStackTo(stack, 0, 1, false))) {
            int hotbar = MACHINE_SLOTS + 27;
            if (index < hotbar ? !moveItemStackTo(stack, hotbar, slots.size(), false)
                    : !moveItemStackTo(stack, MACHINE_SLOTS, hotbar, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}