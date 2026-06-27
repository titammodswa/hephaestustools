package com.titammods.hephaestus_tools.tables.blockentity;

import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.tables.menu.TinkersAnvilMenu;
import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import com.titammods.hephaestus_tools.tables.layout.ToolLayouts;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.materials.MaterialId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.modifier.UpgradeRecipes;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TinkersAnvilBlockEntity extends BaseContainerBlockEntity implements MenuProvider {

    public static final int TOOL_SLOT   = 0;
    public static final int INPUT_START = 1;
    public static final int INPUT_COUNT = 6;
    public static final int OUTPUT_SLOT = 7;
    public static final int TOTAL_SLOTS = 8;

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

    private ToolLayout currentLayout = ToolLayouts.REPAIR;

    public TinkersAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TINKERS_ANVIL_BE.get(), pos, state);
    }

    public void setCurrentLayout(ToolLayout layout) {
        this.currentLayout = layout;
        if (level != null && !level.isClientSide) {
            updateOutput();
        }
    }

    public ToolLayout getCurrentLayout() { return currentLayout; }

    public void updateOutput() {
        if (currentLayout != null && currentLayout.toolSlot().hidden()) {
            cachedResult = computeBuildResult();
        } else {
            ItemStack tool = items.getStackInSlot(TOOL_SLOT);
            if (!tool.isEmpty() && hasPartInInputs()) {
                cachedResult = computeSwapResult(tool);
            } else {
                cachedResult = computeUpgradeResult();
            }
        }
        items.setStackInSlot(OUTPUT_SLOT, cachedResult);
    }

    private boolean hasPartInInputs() {
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            if (items.getStackInSlot(i).getItem() instanceof ToolPartItem) return true;
        }
        return false;
    }

    private ItemStack computeSwapResult(ItemStack tool) {
        if (!(tool.getItem() instanceof ModifiableItem)) return ItemStack.EMPTY;
        if (!ToolStack.isInitialized(tool)) return ItemStack.EMPTY;

        java.util.List<Item> parts = ToolBuildHandler.getToolParts(tool.getItem());
        if (parts.isEmpty()) return ItemStack.EMPTY;

        java.util.List<MaterialId> materials = new java.util.ArrayList<>(ToolStack.getMaterials(tool));
        boolean anySwap = false;

        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            ItemStack input = items.getStackInSlot(i);
            if (input.isEmpty()) continue;
            if (!(input.getItem() instanceof ToolPartItem partItem)) return ItemStack.EMPTY;
            int idx = parts.indexOf(partItem);
            if (idx < 0 || idx >= materials.size()) return ItemStack.EMPTY;
            MaterialId newMat = partItem.getMaterial(input);
            if (newMat.isEmpty()) return ItemStack.EMPTY;
            if (!newMat.equals(materials.get(idx))) {
                materials.set(idx, newMat);
                anySwap = true;
            }
        }
        if (!anySwap) return ItemStack.EMPTY;

        ItemStack result = ToolBuildHandler.buildTool(tool.getItem(), materials);
        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(tool)) {
            ToolStack.addModifier(result, mod);
        }
        ToolStack.recalculate(result);
        int dmg = Math.min(ToolStack.getCurrentDamage(tool), ToolStack.getDurability(result) - 1);
        if (dmg > 0) ToolStack.setDamage(result, dmg);
        return result;
    }

    private ItemStack computeBuildResult() {
        var inputs = currentLayout.inputs();
        if (inputs.isEmpty()) return ItemStack.EMPTY;

        java.util.List<MaterialId> materials = new java.util.ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            ToolLayout.InputSlotDef def = inputs.get(i);
            if (def.expectedPart() == null) return ItemStack.EMPTY;

            ItemStack slotStack = items.getStackInSlot(INPUT_START + i);
            if (slotStack.isEmpty()) return ItemStack.EMPTY;
            if (slotStack.getItem() != def.expectedPart()) return ItemStack.EMPTY;
            if (!(slotStack.getItem() instanceof ToolPartItem partItem)) return ItemStack.EMPTY;

            MaterialId mat = partItem.getMaterial(slotStack);
            if (mat.isEmpty()) return ItemStack.EMPTY;
            materials.add(mat);
        }
        return ToolBuildHandler.buildTool(currentLayout.iconItem(), materials);
    }

    private ItemStack computeUpgradeResult() {
        ItemStack tool = items.getStackInSlot(TOOL_SLOT);
        if (tool.isEmpty() || !(tool.getItem() instanceof ModifiableItem)) {
            return ItemStack.EMPTY;
        }

        Item upgradeItem = null;
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            ItemStack input = items.getStackInSlot(i);
            if (input.isEmpty()) continue;
            if (UpgradeRecipes.forItem(input.getItem()) == null) return ItemStack.EMPTY;
            if (upgradeItem == null) upgradeItem = input.getItem();
            else if (upgradeItem != input.getItem()) return ItemStack.EMPTY;
        }
        if (upgradeItem == null) return ItemStack.EMPTY;

        UpgradeRecipes.UpgradeRecipe recipe = UpgradeRecipes.forItem(upgradeItem);
        int levels = computeUpgradeLevels(tool, recipe);
        if (levels < 1) return ItemStack.EMPTY;

        int currentLevel = currentModifierLevel(tool, recipe.modifierId());
        ItemStack result = tool.copy();
        ToolStack.addModifier(result,
                new ToolConstructionData.ModifierEntry(recipe.modifierId(), currentLevel + levels));
        ToolStack.recalculate(result);
        return result;
    }

    private static int currentModifierLevel(ItemStack tool, net.minecraft.resources.ResourceLocation modifierId) {
        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(tool)) {
            if (mod.id().equals(modifierId)) return mod.level();
        }
        return 0;
    }

    private int computeUpgradeLevels(ItemStack tool, UpgradeRecipes.UpgradeRecipe recipe) {
        int currentLevel = currentModifierLevel(tool, recipe.modifierId());
        if (currentLevel >= recipe.maxLevel()) return 0;
        int totalCount = 0;
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            ItemStack input = items.getStackInSlot(i);
            if (!input.isEmpty()) totalCount += input.getCount();
        }
        int possible = totalCount / recipe.neededPerLevel();
        return Math.min(possible, recipe.maxLevel() - currentLevel);
    }

    public ItemStack getCachedResult() { return cachedResult; }

    public void craft(Player player) {
        if (cachedResult.isEmpty()) return;

        ItemStack tool = items.getStackInSlot(TOOL_SLOT);
        boolean buildMode = currentLayout != null && currentLayout.toolSlot().hidden();
        boolean swapMode  = !buildMode && !tool.isEmpty() && hasPartInInputs();
        boolean upgradeMode = !buildMode && !swapMode && !tool.isEmpty();
        if (swapMode) {
            returnOldParts(player, tool);
        }

        if (upgradeMode) {
            consumeUpgradeInputs(tool);
        } else {
            for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
                if (!items.getStackInSlot(i).isEmpty()) {
                    items.extractItem(i, 1, false);
                }
            }
        }
        items.setStackInSlot(TOOL_SLOT, ItemStack.EMPTY);
        updateOutput();
    }

    private void consumeUpgradeInputs(ItemStack tool) {
        Item upgradeItem = null;
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            ItemStack in = items.getStackInSlot(i);
            if (!in.isEmpty()) { upgradeItem = in.getItem(); break; }
        }
        if (upgradeItem == null) return;
        UpgradeRecipes.UpgradeRecipe recipe = UpgradeRecipes.forItem(upgradeItem);
        if (recipe == null) {
            for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++)
                if (!items.getStackInSlot(i).isEmpty()) items.extractItem(i, 1, false);
            return;
        }
        int toConsume = computeUpgradeLevels(tool, recipe) * recipe.neededPerLevel();
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT && toConsume > 0; i++) {
            ItemStack in = items.getStackInSlot(i);
            if (in.isEmpty()) continue;
            int take = Math.min(toConsume, in.getCount());
            items.extractItem(i, take, false);
            toConsume -= take;
        }
    }

    private void returnOldParts(Player player, ItemStack tool) {
        java.util.List<Item> parts = ToolBuildHandler.getToolParts(tool.getItem());
        java.util.List<MaterialId> oldMaterials = ToolStack.getMaterials(tool);
        for (int i = INPUT_START; i < INPUT_START + INPUT_COUNT; i++) {
            ItemStack input = items.getStackInSlot(i);
            if (input.isEmpty() || !(input.getItem() instanceof ToolPartItem partItem)) continue;
            int idx = parts.indexOf(partItem);
            if (idx < 0 || idx >= oldMaterials.size()) continue;
            ItemStack oldPart = partItem.withMaterial(oldMaterials.get(idx));
            if (!player.getInventory().add(oldPart)) {
                player.drop(oldPart, false);
            }
        }
    }

    public ItemStackHandler getItemHandler() { return items; }

    public void dropContents(Level level, BlockPos pos) {
        NonNullList<ItemStack> drops = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            drops.set(i, items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, drops);
        for (int i = 0; i < TOTAL_SLOTS; i++) items.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("gui.hephaestus_tools.tinkers_anvil");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv) {
        return new TinkersAnvilMenu(id, playerInv, this);
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
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
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
        return new TinkersAnvilMenu(id, playerInv, this);
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