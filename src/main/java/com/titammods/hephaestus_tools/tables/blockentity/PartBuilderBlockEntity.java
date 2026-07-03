package com.titammods.hephaestus_tools.tables.blockentity;

import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.registry.ModRecipes;
import com.titammods.hephaestus_tools.recipe.PartRecipe;
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
            if (slot != OUTPUT_SLOT && level != null && !level.isClientSide) {
                updateOutput();
            }
        }
    };

    private ItemStack cachedResult = ItemStack.EMPTY;

    private int selectedIndex = -1;
    @Nullable private java.util.List<PartRecipe> sortedButtons = null;

    public PartBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PART_BUILDER_BE.get(), pos, state);
    }

    public java.util.List<PartRecipe> getSortedButtons() {
        if (sortedButtons == null) {
            if (level == null) return java.util.Collections.emptyList();
            sortedButtons = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.PART_BUILDER.get())
                    .stream()
                    .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                    .sorted(java.util.Comparator.comparing(r -> r.resultPart().toString()))
                    .toList();
        }
        return sortedButtons;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectRecipe(int index) {
        java.util.List<PartRecipe> buttons = getSortedButtons();
        this.selectedIndex = (index >= 0 && index < buttons.size()) ? index : -1;
        setChanged();
        if (level != null && !level.isClientSide) {
            updateOutput();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public PartRecipe getSelectedRecipe() {
        java.util.List<PartRecipe> buttons = getSortedButtons();
        return (selectedIndex >= 0 && selectedIndex < buttons.size()) ? buttons.get(selectedIndex) : null;
    }

    public void updateOutput() {
        cachedResult = computeResult();
        items.setStackInSlot(OUTPUT_SLOT, cachedResult.copy());
    }

    private ItemStack computeResult() {
        if (level == null) return ItemStack.EMPTY;
        PartRecipe recipe = getSelectedRecipe();
        if (recipe == null) return ItemStack.EMPTY;

        ItemStack mat = items.getStackInSlot(MATERIAL_SLOT);
        ItemStack pat = items.getStackInSlot(PATTERN_SLOT);
        if (mat.isEmpty() || pat.isEmpty()) return ItemStack.EMPTY;

        var patId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pat.getItem());
        if (!recipe.patternItem().equals(patId)) return ItemStack.EMPTY;

        if (mat.getCount() < recipe.materialCost()) return ItemStack.EMPTY;

        com.titammods.hephaestus_tools.materials.MaterialId matId = findMaterial(mat);
        if (matId == null) return ItemStack.EMPTY;

        if (isFoundryMaterial(matId)) return ItemStack.EMPTY;

        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(recipe.resultPart());
        if (!(item instanceof com.titammods.hephaestus_tools.tools.part.ToolPartItem part)) return ItemStack.EMPTY;
        var stats = com.titammods.hephaestus_tools.materials.MaterialManager.getInstance()
                .getStatsForSlot(matId, part.getPartSlot());
        if (stats == null || stats == com.titammods.hephaestus_tools.materials.MaterialStats.EMPTY) {
            return ItemStack.EMPTY;
        }

        return recipe.createResult(matId);
    }

    @Nullable
    private com.titammods.hephaestus_tools.materials.MaterialId findMaterial(ItemStack stack) {
        for (var m : com.titammods.hephaestus_tools.materials.MaterialManager.getInstance().getAllMaterials()) {
            if (m.ingredient().test(stack)) return m.id();
        }
        return null;
    }

    public static boolean isFoundryMaterial(com.titammods.hephaestus_tools.materials.MaterialId id) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hephaestus", "molten_" + id.id().getPath()));
    }

    public ItemStack getCachedResult() { return cachedResult; }

    public void craft(Player player) {
        PartRecipe recipe = getSelectedRecipe();
        if (recipe == null || cachedResult.isEmpty()) return;
        items.extractItem(MATERIAL_SLOT, recipe.materialCost(), false);
        items.extractItem(PATTERN_SLOT, 1, false);
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("selected", selectedIndex);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("selected", selectedIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            items.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        this.selectedIndex = tag.contains("selected") ? tag.getInt("selected") : -1;
        this.sortedButtons = null;
    }
}