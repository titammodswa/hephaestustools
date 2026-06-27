package com.titammods.hephaestus_tools.tools.nbt;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ToolStack {

    private ToolStack() {}


    public static ToolConstructionData getConstruction(ItemStack stack) {
        return stack.getOrDefault(ModComponents.TOOL_CONSTRUCTION.get(), ToolConstructionData.EMPTY);
    }

    public static boolean isInitialized(ItemStack stack) {
        return getConstruction(stack).isInitialized();
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
    }

    public static boolean isBroken(ItemStack stack) {
        return getConstruction(stack).broken();
    }

    public static void recalculate(ItemStack stack) {
        ToolConstructionData construction = getConstruction(stack);
        if (!construction.isInitialized()) return;

        ToolPropertiesData properties = com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler
                .calculateProperties(stack, construction);

        stack.set(ModComponents.TOOL_PROPERTIES.get(), properties);

        stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);

        updateToolComponent(stack, properties);
        stack.set(DataComponents.MAX_DAMAGE, properties.getDurability());
    }

    private static void updateToolComponent(ItemStack stack, ToolPropertiesData properties) {
        if (!(stack.getItem() instanceof com.titammods.hephaestus_tools.tools.item.ModifiableItem modItem)) return;

        Tiers tier = properties.getHarvestTier();
        float speed = properties.getMiningSpeed();

        net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> incorrectTag = switch (tier) {
            case STONE     -> net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL;
            case IRON      -> net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL;
            case GOLD      -> net.minecraft.tags.BlockTags.INCORRECT_FOR_GOLD_TOOL;
            case DIAMOND   -> net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            case NETHERITE -> net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
            default        -> net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        };

        Tool toolComponent = new Tool(
                List.of(
                        Tool.Rule.deniesDrops(incorrectTag),
                        Tool.Rule.minesAndDrops(modItem.getToolBlockTag(), speed)
                ),
                1.0f,
                1
        );
        stack.set(DataComponents.TOOL, toolComponent);
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