package com.titammods.hephaestus_tools.tools.nbt;

import com.titammods.hephaestus_tools.api.event.ToolRecalculatedEvent;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.tier.TierManager;
import com.titammods.hephaestus_tools.materials.trait.ToolTraits;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import com.titammods.hephaestus_tools.tools.item.ToolCategory;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.stat.HarvestTier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
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

    public static HarvestTier getHarvestTier(ItemStack stack) {
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
            ToolPropertiesData properties = getProperties(stack);
            updateAttributeModifiers(stack, properties);
            updateToolComponent(stack, properties);
        }
    }

    public static void refreshIfStale(ItemStack stack) {
        ToolPropertiesData data = stack.get(ModComponents.TOOL_PROPERTIES.get());
        if (data == null || !isInitialized(stack)) return;
        OptionalInt hash = fingerprint(stack.getItem(), getConstruction(stack));
        if (hash.isPresent() && hash.getAsInt() != data.materialsHash()) recalculate(stack);
    }

    public static void refresh(ItemStack stack) {
        if (!(stack.getItem() instanceof ModifiableItem) || !isInitialized(stack)) return;
        if (stack.get(ModComponents.TOOL_PROPERTIES.get()) == null) recalculate(stack);
        else refreshIfStale(stack);
    }

    private static OptionalInt fingerprint(Item tool, ToolConstructionData construction) {
        List<MaterialId> materials = construction.materials();
        OptionalInt hash = MaterialManager.getInstance().fingerprint(materials);
        if (hash.isEmpty()) return hash;
        int value = 31 * hash.getAsInt() + ToolTraits.fingerprint(tool, materials);
        value = 31 * value + TierManager.getInstance().fingerprint();
        value = 31 * value + BuiltInRegistries.ITEM.getKey(tool).hashCode();
        for (ToolConstructionData.ModifierEntry modifier : construction.modifiers()) {
            value = 31 * value + modifier.id().hashCode();
            value = 31 * value + modifier.level();
        }
        return OptionalInt.of(value);
    }

    public static boolean isBroken(ItemStack stack) {
        return getConstruction(stack).broken();
    }

    public static void recalculate(ItemStack stack) {
        ToolConstructionData construction = getConstruction(stack);
        if (!construction.isInitialized()) return;

        ToolPropertiesData properties = ToolBuildHandler.calculateProperties(stack, construction)
                .withMaterialsHash(fingerprint(stack.getItem(), construction).orElse(0));

        stack.set(ModComponents.TOOL_PROPERTIES.get(), properties);

        int damage = Math.max(0, Math.min(construction.damage(), properties.getDurability()));
        stack.set(ModComponents.TOOL_CONSTRUCTION.get(), construction.withDamage(damage)
                .withBroken(damage >= properties.getDurability()));
        stack.set(DataComponents.DAMAGE, damage);
        updateAttributeModifiers(stack, properties);

        updateToolComponent(stack, properties);
        stack.set(DataComponents.MAX_DAMAGE, properties.getDurability());
        stack.set(DataComponents.ENCHANTABLE,
                new Enchantable(
                        Math.max(1, properties.getEnchantability())));
        NeoForge.EVENT_BUS.post(new ToolRecalculatedEvent(stack));
    }

    private static void updateAttributeModifiers(ItemStack stack, ToolPropertiesData properties) {
        float attackDamage = isBroken(stack) ? 0f : Math.max(0f, properties.getAttackDamage());
        float attackSpeed = properties.getAttackSpeed() - 4.0f;

        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();

        List<ItemAttributeModifiers.Entry> entries = new ArrayList<>(
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers());
        entries.removeIf(entry -> entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)
                || entry.modifier().id().equals(Item.BASE_ATTACK_SPEED_ID));
        entries.addAll(modifiers.modifiers());
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(List.copyOf(entries)));
    }

    private static final Lazy<HolderGetter<Block>> BLOCK_LOOKUP =
            Lazy.of(() -> VanillaRegistries.createLookup().lookupOrThrow(Registries.BLOCK));

    private static void updateToolComponent(ItemStack stack, ToolPropertiesData properties) {
        if (!(stack.getItem() instanceof ModifiableItem modItem)) return;

        Set<ToolCategory> categories = modItem.categories();
        boolean sword = categories.contains(ToolCategory.SWORD);

        if (isBroken(stack)) {
            stack.set(DataComponents.TOOL, new Tool(List.of(), 0.0f, 1, !sword));
            return;
        }

        HarvestTier tier = properties.getHarvestTier();
        float speed = Math.max(0.1f, properties.getMiningSpeed());
        HolderGetter<Block> blocks = BLOCK_LOOKUP.get();

        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(blocks.getOrThrow(tier.incorrectForTag())));

        if (sword) {
            rules.add(Tool.Rule.minesAndDrops(
                    HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), Math.max(15.0f, speed)));
            rules.add(Tool.Rule.overrideSpeed(
                    blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
        }

        for (ToolCategory category : ToolCategory.values()) {
            if (!categories.contains(category)) continue;
            rules.add(Tool.Rule.minesAndDrops(blocks.getOrThrow(category.tag), speed));
        }

        stack.set(DataComponents.TOOL, new Tool(List.copyOf(rules), 1.0f, 1, !sword));
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
