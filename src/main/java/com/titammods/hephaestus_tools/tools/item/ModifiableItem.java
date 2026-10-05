package com.titammods.hephaestus_tools.tools.item;

import com.titammods.hephaestus_tools.event.MasteryEvents;
import com.titammods.hephaestus_tools.event.MasteryInteract;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.tools.helper.ToolDurability;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public abstract class ModifiableItem extends Item {

    private static final ResourceLocation ATTACK_DAMAGE_ID = Item.BASE_ATTACK_DAMAGE_ID;
    private static final ResourceLocation ATTACK_SPEED_ID = Item.BASE_ATTACK_SPEED_ID;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static volatile long lastMineLog = 0L;

    public ModifiableItem(Properties properties) {
        super(properties);
    }

    public abstract Set<ToolCategory> categories();

    public TagKey<Block> getToolBlockTag() {
        for (ToolCategory c : ToolCategory.values()) {
            if (categories().contains(c)) return c.tag;
        }
        return BlockTags.MINEABLE_WITH_PICKAXE;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (!ToolStack.isInitialized(stack) || ToolStack.isBroken(stack)) return false;
        if (ability == ItemAbilities.HOE_TILL && MasteryInteract.isCultivator(stack)) return true;
        for (ToolCategory c : categories()) {
            if (c.ability != null && c.ability == ability) return true;
        }
        return false;
    }

    protected boolean effectiveOn(BlockState state) {
        if (categories().contains(ToolCategory.SWORD)
                && (state.is(Blocks.COBWEB) || state.is(BlockTags.SWORD_EFFICIENT))) return true;
        for (ToolCategory c : categories()) if (state.is(c.tag)) return true;
        return false;
    }

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
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return ToolStack.isInitialized(stack);
    }

    public float getAttackDamageBonus() { return 0f; }

    public float getAttackDamageMultiplier() { return 1.0f; }

    public float getBaseAttackSpeed() { return -1f; }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!ToolStack.isInitialized(stack)) {
            return ItemAttributeModifiers.EMPTY;
        }

        ToolPropertiesData props = ToolStack.getProperties(stack);
        float attackDamage = ToolStack.isBroken(stack) ? 0f : Math.max(0f, props.getAttackDamage());
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
        if (ToolStack.isBroken(stack)) return 0.0f;
        if (categories().contains(ToolCategory.SWORD) && state.is(Blocks.COBWEB))
            return Math.max(15.0f, ToolStack.getMiningSpeed(stack));
        return effectiveOn(state) ? ToolStack.getMiningSpeed(stack) : 1.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!ToolStack.isInitialized(stack) || ToolStack.isBroken(stack)) return false;
        if (!effectiveOn(state)) return false;
        return hasCorrectTier(state, ToolStack.getProperties(stack).getHarvestTier());
    }

    private static boolean hasCorrectTier(BlockState state, Tiers tier) {
        return !state.is(tier.getIncorrectBlocksForDrops());
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && state.getDestroySpeed(level, pos) > 0
                && !MasteryEvents.sparesAoeWear(stack, entity)) {
            ToolDurability.hurt(stack, 1, serverLevel, entity);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            ToolDurability.hurt(stack, 1, serverLevel, attacker);
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return MasteryInteract.useFirst(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        if (!ToolStack.isInitialized(stack) || ToolStack.isBroken(stack)) return InteractionResult.PASS;

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        BlockState modified = null;
        ItemAbility ability = null;
        if (MasteryInteract.isCultivator(stack) && ctx.getClickedFace() != Direction.DOWN) {
            modified = state.getToolModifiedState(ctx, ItemAbilities.HOE_TILL, false);
            if (modified != null) ability = ItemAbilities.HOE_TILL;
        }
        for (ToolCategory c : ToolCategory.values()) {
            if (modified != null) break;
            if (!categories().contains(c) || !c.modifiesBlockOnUse()) continue;
            ItemAbility a = c.ability;
            if (a != ItemAbilities.AXE_STRIP
                    && (ctx.getClickedFace() == Direction.DOWN || !level.getBlockState(pos.above()).isAir())) continue;
            BlockState mod = state.getToolModifiedState(ctx, a, false);
            if (mod != null) { modified = mod; ability = a; break; }
        }
        if (modified == null) return InteractionResult.PASS;

        Player player = ctx.getPlayer();
        level.playSound(player, pos,
                ability == ItemAbilities.AXE_STRIP ? SoundEvents.AXE_STRIP
                        : ability == ItemAbilities.HOE_TILL ? SoundEvents.HOE_TILL : SoundEvents.SHOVEL_FLATTEN,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        if (!level.isClientSide) {
            level.setBlock(pos, modified, 11);
            if (player != null && !MasteryInteract.isTilling() && level instanceof ServerLevel serverLevel) {
                ToolDurability.hurt(stack, 1, serverLevel, player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!ToolStack.isInitialized(stack)) {
            tooltip.add(Component.translatable("tooltip.hephaestus_tools.no_material")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        appendTrait(stack, tooltip);


        int lvl = com.titammods.hephaestus_tools.table.ToolXp.getLevel(stack);
        net.minecraft.network.chat.Component rank =
                net.minecraft.network.chat.Component.translatable(com.titammods.hephaestus_tools.table.ToolXp.rankKey(lvl));
        if (lvl >= com.titammods.hephaestus_tools.table.ToolXp.MAX_LEVEL) {
            tooltip.add(net.minecraft.network.chat.Component.empty().append(rank)
                    .append(net.minecraft.network.chat.Component.literal(" (Lv." + lvl + ") - MAX"))
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else {
            int into = com.titammods.hephaestus_tools.table.ToolXp.xpIntoLevel(stack);
            int need = com.titammods.hephaestus_tools.table.ToolXp.xpForLevel(stack);
            tooltip.add(net.minecraft.network.chat.Component.empty().append(rank)
                    .append(net.minecraft.network.chat.Component.literal(" (Lv." + lvl + ")"))
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
            tooltip.add(net.minecraft.network.chat.Component.literal("XP: " + into + " / " + need)
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
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
    public void inventoryTick(ItemStack stack, Level level,
                              net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;
        ToolStack.refreshIfStale(stack);
        if (!(entity instanceof Player player) || !ToolStack.isInitialized(stack)) return;
        for (MaterialTrait t : MaterialTrait.collect(ToolStack.getMaterials(stack))) {
            if (ToolStack.isUsable(stack) || t == MaterialTrait.CULTIVATED) {
                t.onInventoryTick(stack, level, player);
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack);
    }

    private static Component materialName(com.titammods.hephaestus_tools.materials.MaterialId id) {
        ResourceLocation rl = id.id();
        String key = "material." + rl.getNamespace() + "." + rl.getPath();
        return Component.translatable(key);
    }

    private static void appendTrait(ItemStack stack, List<Component> tooltip) {
        for (var t : com.titammods.hephaestus_tools.materials.trait.MaterialTrait.collect(ToolStack.getMaterials(stack))) {
            tooltip.add(Component.translatable("trait.hephaestus_tools." + t.id())
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
    }


    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (ToolStack.isInitialized(stack) && stack.get(
                com.titammods.hephaestus_tools.registry.ModComponents.TOOL_PROPERTIES.get()) == null) {
            ToolStack.recalculate(stack);
        }
    }
}