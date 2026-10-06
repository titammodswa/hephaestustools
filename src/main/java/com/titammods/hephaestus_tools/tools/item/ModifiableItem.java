package com.titammods.hephaestus_tools.tools.item;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.event.MasteryEvents;
import com.titammods.hephaestus_tools.event.MasteryInteract;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.table.ToolXp;
import com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder;
import com.titammods.hephaestus_tools.tools.helper.ToolDurability;
import com.titammods.hephaestus_tools.tools.helper.ToolEnchantments;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public abstract class ModifiableItem extends Item {

    private static final Identifier ATTACK_DAMAGE_ID = Item.BASE_ATTACK_DAMAGE_ID;
    private static final Identifier ATTACK_SPEED_ID = Item.BASE_ATTACK_SPEED_ID;

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
    public boolean canPerformAction(ItemInstance instance, ItemAbility ability) {
        if (instance instanceof ItemStack stack
                && (!ToolStack.isInitialized(stack) || ToolStack.isBroken(stack))) {
            return false;
        }
        if (instance instanceof ItemStack stack && ability == ItemAbilities.HOE_TILL
                && MasteryInteract.isCultivator(stack)) return true;
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
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return !ToolEnchantments.isBanned(enchantment) && super.supportsEnchantment(stack, enchantment);
    }

    public boolean isEnchantable(ItemStack stack) {
        return !stack.isEmpty() && ToolStack.isInitialized(stack);
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
        return ToolStack.getProperties(stack).getHarvestTier().canHarvest(state);
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
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            ToolDurability.hurt(stack, 1, serverLevel, attacker);
        }
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
        if (!level.isClientSide()) {
            level.setBlock(pos, modified, 11);
            if (player != null && !MasteryInteract.isTilling() && level instanceof ServerLevel serverLevel) {
                ToolDurability.hurt(stack, 1, serverLevel, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (!ToolStack.isInitialized(stack)) {
            tooltip.accept(Component.translatable("tooltip.hephaestus_tools.no_material")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        appendTrait(stack, tooltip);

        int lvl = ToolXp.getLevel(stack);
        Component rank = Component.translatable(ToolXp.rankKey(lvl));
        if (lvl >= ToolXp.MAX_LEVEL) {
            tooltip.accept(Component.empty().append(rank)
                    .append(Component.literal(" (Lv." + lvl + ") - MAX"))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            int into = ToolXp.xpIntoLevel(stack);
            int need = ToolXp.xpForLevel(stack);
            tooltip.accept(Component.empty().append(rank)
                    .append(Component.literal(" (Lv." + lvl + ")"))
                    .withStyle(ChatFormatting.AQUA));
            tooltip.accept(Component.literal("XP: " + into + " / " + need)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        Map<Identifier, Integer> bonus = ToolEnchantments.bonusLevels(stack, context.registries());
        List<Component> lines = new ArrayList<>();
        if (flag.hasShiftDown()) {
            lines.addAll(ToolTooltipBuilder.stats(stack, true, bonus));
            List<Component> mods = ToolTooltipBuilder.modifiers(stack, true, bonus);
            if (!mods.isEmpty()) {
                lines.add(Component.empty());
                lines.addAll(mods);
            }
        } else if (flag.hasControlDown()) {
            lines.addAll(ToolTooltipBuilder.components(stack, this));
        } else {
            lines.addAll(ToolTooltipBuilder.defaultInfo(stack, bonus));
        }
        lines.forEach(tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
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

    private static Component materialName(MaterialId id) {
        Identifier rl = id.id();
        String key = "material." + rl.getNamespace() + "." + rl.getPath();
        return Component.translatable(key);
    }

    private static void appendTrait(ItemStack stack, Consumer<Component> tooltip) {
        for (MaterialTrait t : MaterialTrait.collect(ToolStack.getMaterials(stack))) {
            tooltip.accept(Component.translatable("trait.hephaestus_tools." + t.id())
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (ToolStack.isInitialized(stack) && stack.get(ModComponents.TOOL_PROPERTIES.get()) == null) {
            ToolStack.recalculate(stack);
        }
    }
}