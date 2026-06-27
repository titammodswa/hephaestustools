package com.titammods.hephaestus_tools.tables.blockentity;

import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.tables.menu.PartBuilderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class PartBuilderBlockEntity extends BaseContainerBlockEntity implements MenuProvider {

    public static final int MATERIAL_SLOT = 0;
    public static final int PATTERN_SLOT  = 1;
    public static final int OUTPUT_SLOT   = 2;
    public static final int TOTAL_SLOTS   = 3;

    protected final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                updateOutput();
            }
        }
    };

    private ItemStack cachedResult = ItemStack.EMPTY;

    public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PART_BUILDER_BE.get(), pos, state);
    }

    public void updateOutput() {
        cachedResult = ItemStack.EMPTY;
    }

    public ItemStack getCachedResult() { return cachedResult; }

    public void craft(Player player) {
        if (cachedResult.isEmpty()) return;
        ItemStack result = cachedResult.copy();
        items.extractItem(MATERIAL_SLOT, 1, false);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        updateOutput();
    }

    public ItemStackHandler getItemHandler() { return items; }

    public void dropContents(Level level, BlockPos pos) {
        NonNullList<ItemStack> drops = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) drops.set(i, items.getStackInSlot(i));
        Containers.dropContents(level, pos, drops);
        for (int i = 0; i < TOTAL_SLOTS; i++) items.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("gui.hephaestus_tools.part_builder");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv) {
        return new PartBuilderMenu(id, playerInv, this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> list = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) list.set(i, items.getStackInSlot(i));
        return list;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> pItems) {
        for (int i = 0; i < TOTAL_SLOTS && i < pItems.size(); i++) {
            items.setStackInSlot(i, pItems.get(i));
        }
    }

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!items.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.getStackInSlot(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack s = items.getStackInSlot(slot);
        items.setStackInSlot(slot, ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < TOTAL_SLOTS; i++) items.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new PartBuilderMenu(id, playerInv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            items.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }
}