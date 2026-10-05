package com.titammods.hephaestus_tools.tools.nbt;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.item.ToolCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ToolStack {

    private ToolStack() {}

    public static ToolConstructionData getConstruction(ItemStack stack) {
        return stack.getOrDefault(ModComponents.TOOL_CONSTRUCTION.get(), ToolConstructionData.EMPTY);
    }

    public static boolean isInitialized(ItemStack stack) {
        return getConstruction(stack).isInitialized();
    }

    public static boolean isUsable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ModifiableItem
                && isInitialized(stack) && !isBroken(stack);
    }

    public static List<MaterialId> getMaterials(ItemStack stack) {
        return getConstruction(stack).materials();
    }

    public static MaterialId getMaterial(ItemStack stack, int index) {
        return getConstruction(stack).getMaterial(index);
    }

    public static List<ToolConstructionData.ModifierEntry> getModifiers(ItemStack stack) {
        return getConstruction(stack).modifiers();
    }

    public static void setConstruction(ItemStack stack, ToolConstructionData data) {
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(), data);
    }

    public static void setMaterials(ItemStack stack, List<MaterialId> materials) {
        ToolConstructionData old = getConstruction(stack);
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(),
                new ToolConstructionData(materials, old.modifiers(), old.damage(), old.broken()));
    }

    public static void addModifier(ItemStack stack, ToolConstructionData.ModifierEntry entry) {
        ToolConstructionData old = getConstruction(stack);
        List<ToolConstructionData.ModifierEntry> mods = new ArrayList<>(old.modifiers());
        mods.removeIf(e -> e.id().equals(entry.id()));
        mods.add(entry);
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(),
                new ToolConstructionData(old.materials(), List.copyOf(mods), old.damage(), old.broken()));
    }

    public static ToolPropertiesData getProperties(ItemStack stack) {
        ToolPropertiesData data = stack.get(ModComponents.TOOL_PROPERTIES.get());
        if (data != null) return data;
        recalculate(stack);
        return stack.getOrDefault(ModComponents.TOOL_PROPERTIES.get(), ToolPropertiesData.EMPTY);
    }

    public static int getDurability(ItemStack stack) {
        return getProperties(stack).getDurability();
    }

    public static float getMiningSpeed(ItemStack stack) {
        return getProperties(stack).getMiningSpeed();
    }

    public static float getAttackDamage(ItemStack stack) {
        return getProperties(stack).getAttackDamage();
    }

    public static float getAttackSpeed(ItemStack stack) {
        return getProperties(stack).getAttackSpeed();
    }

    public static int getEnchantability(ItemStack stack) {
        return getProperties(stack).getEnchantability();
    }

    public static Tiers getHarvestTier(ItemStack stack) {
        return getProperties(stack).getHarvestTier();
    }

    public static int getCurrentDamage(ItemStack stack) {
        return getConstruction(stack).damage();
    }

    public static void setDamage(ItemStack stack, int damage) {
        ToolConstructionData old = getConstruction(stack);
        int maxDurability = getDurability(stack);
        int clamped = Math.max(0, Math.min(damage, maxDurability));
        boolean broken = clamped >= maxDurability;
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(), old.withDamage(clamped).withBroken(broken));
        stack.set(DataComponents.DAMAGE, clamped);
        if (broken != old.broken()) {
            updateToolComponent(stack, getProperties(stack));
        }
    }

    public static void refreshIfStale(ItemStack stack) {
        ToolPropertiesData data = stack.get(ModComponents.TOOL_PROPERTIES.get());
        if (data == null || !isInitialized(stack)) return;
        MaterialManager.getInstance().fingerprint(getMaterials(stack)).ifPresent(hash -> {
            if (hash != data.materialsHash()) recalculate(stack);
        });
    }

    public static boolean isBroken(ItemStack stack) {
        return getConstruction(stack).broken();
    }

    public static void recalculate(ItemStack stack) {
        ToolConstructionData construction = getConstruction(stack);
        if (!construction.isInitialized()) return;

        ToolPropertiesData properties = ToolBuildHandler.calculateProperties(stack, construction)
                .withMaterialsHash(MaterialManager.getInstance().fingerprint(construction.materials()).orElse(0));

        stack.set(ModComponents.TOOL_PROPERTIES.get(), properties);

        int damage = Math.max(0, Math.min(construction.damage(), properties.getDurability()));
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(), construction.withDamage(damage)
                .withBroken(damage >= properties.getDurability()));
        stack.set(DataComponents.MAX_DAMAGE, properties.getDurability());
        stack.set(DataComponents.DAMAGE, damage);

        stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);

        updateToolComponent(stack, properties);
    }

    private static void updateToolComponent(ItemStack stack, ToolPropertiesData properties) {
        if (!(stack.getItem() instanceof ModifiableItem modItem)) return;

        if (isBroken(stack)) {
            stack.set(DataComponents.TOOL, new Tool(List.of(), 0.0f, 1));
            return;
        }

        Set<ToolCategory> categories = modItem.categories();
        Tiers tier = properties.getHarvestTier();
        float speed = Math.max(0.1f, properties.getMiningSpeed());

        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(tier.getIncorrectBlocksForDrops()));

        if (categories.contains(ToolCategory.SWORD)) {
            rules.add(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), Math.max(15.0f, speed)));
        }

        for (ToolCategory category : ToolCategory.values()) {
            if (!categories.contains(category)) continue;
            rules.add(Tool.Rule.minesAndDrops(category.tag, speed));
        }

        stack.set(DataComponents.TOOL, new Tool(List.copyOf(rules), 1.0f, 1));
    }

    public static ItemStack createTool(ItemStack template, List<MaterialId> materials) {
        ItemStack result = template.copy();
        setMaterials(result, materials);
        recalculate(result);
        return result;
    }

    public static ItemStack withUpdatedConstruction(ItemStack stack,
                                                    Function<ToolConstructionData, ToolConstructionData> updater) {
        ToolConstructionData updated = updater.apply(getConstruction(stack));
        ItemStack result = stack.copy();
        result.set(ModComponents.TOOL_CONSTRUCTION.get(), updated);
        recalculate(result);
        return result;
    }
}