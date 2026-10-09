package com.titammods.hephaestus_tools.tables.blockentity;

import com.mojang.serialization.Codec;
import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.table.TableStyle;
import com.titammods.hephaestus_tools.tables.menu.ArsenalTableMenu;
import com.titammods.hephaestus_tools.tables.menu.WorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArsenalTableBlockEntity extends BlockEntity implements MenuProvider {

    private static final Codec<List<ItemStack>> STACKS_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    private final ItemStackHandler upgradeSlot = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); syncToClient(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    private final ItemStackHandler blueprintSlot = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); syncToClient(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    private final ItemStackHandler inputSlots = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); syncToClient(); }
        @Override public int getSlotLimit(int slot) { return slot == 0 ? 99 : 1; }
    };
    private final ItemStackHandler outputSlot = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); syncToClient(); }
    };

    public ArsenalTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARSENAL_TABLE_BE.get(), pos, state);
    }

    public ItemStackHandler getUpgradeSlot()   { return upgradeSlot; }
    public ItemStackHandler getBlueprintSlot() { return blueprintSlot; }
    public ItemStack getUpgradeItem()          { return upgradeSlot.getStackInSlot(0); }
    public ItemStackHandler getInputSlots()    { return inputSlots; }
    public ItemStackHandler getOutputSlot()    { return outputSlot; }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            dropContents(level, pos);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer c = new SimpleContainer(7);
        c.setItem(0, upgradeSlot.getStackInSlot(0));
        c.setItem(1, blueprintSlot.getStackInSlot(0));
        for (int i = 0; i < 4; i++) c.setItem(2 + i, inputSlots.getStackInSlot(i));
        c.setItem(6, outputSlot.getStackInSlot(0));
        Containers.dropContents(level, pos, c);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hephaestus_tools.arsenal_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        if (TableStyle.isModern(player)) return new WorkbenchMenu(containerId, inv, this);
        return new ArsenalTableMenu(containerId, inv, this);
    }

    private static List<ItemStack> snapshot(ItemStackHandler handler) {
        List<ItemStack> out = new ArrayList<>(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) out.add(handler.getStackInSlot(i).copy());
        return out;
    }

    private static void restore(ItemStackHandler handler, List<ItemStack> stacks) {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Upgrade", STACKS_CODEC, snapshot(upgradeSlot));
        output.store("Blueprint", STACKS_CODEC, snapshot(blueprintSlot));
        output.store("Inputs", STACKS_CODEC, snapshot(inputSlots));
        output.store("Output", STACKS_CODEC, snapshot(outputSlot));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("Upgrade", STACKS_CODEC).ifPresent(list -> restore(upgradeSlot, list));
        input.read("Blueprint", STACKS_CODEC).ifPresent(list -> restore(blueprintSlot, list));
        input.read("Inputs", STACKS_CODEC).ifPresent(list -> restore(inputSlots, list));
        input.read("Output", STACKS_CODEC).ifPresent(list -> restore(outputSlot, list));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
