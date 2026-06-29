package com.titammods.hephaestus_tools.tools.item;

import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public abstract class ModifiableItem extends Item {

    private static final ResourceLocation ATTACK_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath("hephaestus_tools", "tool_attack_damage");
    private static final ResourceLocation ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("hephaestus_tools", "tool_attack_speed");

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static volatile long lastMineLog = 0L;

    public ModifiableItem(Properties properties) {
        super(properties);
    }

    public abstract TagKey<Block> getToolBlockTag();

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return !stack.isEmpty() && ToolStack.isInitialized(stack);
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    public int getEnchantmentValue(ItemStack stack) {
        return ToolStack.getEnchantability(stack);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ToolStack.getDurability(stack);
    }

    @Override
    public int getDamage(ItemStack stack) {
        return ToolStack.getCurrentDamage(stack);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        ToolStack.setDamage(stack, damage);
        int maxDurability = ToolStack.getDurability(stack);
        int clamped = Math.max(0, Math.min(damage, maxDurability));
        stack.set(net.minecraft.core.component.DataComponents.DAMAGE, clamped);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return ToolStack.isInitialized(stack) && !ToolStack.isBroken(stack);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!ToolStack.isInitialized(stack)) {
            return ItemAttributeModifiers.EMPTY;
        }

        ToolPropertiesData props = ToolStack.getProperties(stack);
        float attackDamage = props.getAttackDamage();
        float attackSpeed  = props.getAttackSpeed();

        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, attackSpeed - 4.0f,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!ToolStack.isInitialized(stack)) return 1.0f;
        if (ToolStack.isBroken(stack)) return 0.3f;
        return state.is(getToolBlockTag()) ? ToolStack.getMiningSpeed(stack) : 1.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!ToolStack.isInitialized(stack) || ToolStack.isBroken(stack)) return false;
        if (!state.is(getToolBlockTag())) return false;
        return hasCorrectTier(state, ToolStack.getProperties(stack).getHarvestTier());
    }

    private static boolean hasCorrectTier(BlockState state, Tiers tier) {
        int level = switch (tier) {
            case WOOD, GOLD -> 0;
            case STONE      -> 1;
            case IRON       -> 2;
            case DIAMOND    -> 3;
            case NETHERITE  -> 4;
        };
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return level >= 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL))    return level >= 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL))   return level >= 1;
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) > 0) {
            stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!ToolStack.isInitialized(stack)) {
            tooltip.add(Component.translatable("tooltip.hephaestus_tools.no_material")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.addAll(com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.stats(stack, true));
            List<Component> mods =
                    com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.modifiers(stack, true);
            if (!mods.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.addAll(mods);
            }
        } else if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            tooltip.addAll(com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.components(stack, this));
        } else {
            tooltip.addAll(com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.defaultInfo(stack));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        Component baseName = super.getName(stack);
        if (!ToolStack.isInitialized(stack)) {
            return baseName;
        }

        java.util.List<com.titammods.hephaestus_tools.materials.MaterialId> materials =
                ToolStack.getMaterials(stack);
        if (materials.isEmpty()) {
            return baseName;
        }

        java.util.LinkedHashSet<com.titammods.hephaestus_tools.materials.MaterialId> unique =
                new java.util.LinkedHashSet<>();
        for (var mat : materials) {
            if (mat != null && !mat.isEmpty()) {
                unique.add(mat);
            }
        }
        if (unique.isEmpty()) {
            return baseName;
        }

        if (unique.size() == 1) {
            Component matName = materialName(unique.iterator().next());
            return Component.translatable("item.hephaestus_tools.tool.format", matName, baseName);
        }

        net.minecraft.network.chat.MutableComponent combined = Component.literal("");
        java.util.Iterator<com.titammods.hephaestus_tools.materials.MaterialId> iter = unique.iterator();
        combined.append(materialName(iter.next()));
        while (iter.hasNext()) {
            combined.append(Component.translatable("item.hephaestus_tools.tool.material_separator"))
                    .append(materialName(iter.next()));
        }
        return Component.translatable("item.hephaestus_tools.tool.format", combined, baseName);
    }

    private static Component materialName(com.titammods.hephaestus_tools.materials.MaterialId id) {
        ResourceLocation rl = id.id();
        String key = "material." + rl.getNamespace() + "." + rl.getPath();
        return Component.translatable(key);
    }


    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (ToolStack.isInitialized(stack) && stack.get(
                com.titammods.hephaestus_tools.registry.ModComponents.TOOL_PROPERTIES.get()) == null) {
            ToolStack.recalculate(stack);
        }
    }
}