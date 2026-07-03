package com.titammods.hephaestus_tools.tables.screen;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.client.widget.InfoPanel;
import com.titammods.hephaestus_tools.client.widget.PatternSprite;
import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.recipe.PartRecipe;
import com.titammods.hephaestus_tools.tables.blockentity.PartBuilderBlockEntity;
import com.titammods.hephaestus_tools.tables.menu.PartBuilderMenu;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            HephaestusTools.MOD_ID, "textures/gui/part_builder.png");

    private static final int HANDLE_U = 176;
    private static final int HANDLE_U_DISABLE = 188;
    private static final int SLIDER_WIDTH = 12;
    private static final int HANDLE_HEIGHT = 15;
    private static final int BAR_HEIGHT = 72;
    private static final int SCROLLABLE_AREA = BAR_HEIGHT + 2 - HANDLE_HEIGHT;
    private static final int SLIDER_LEFT = 126;
    private static final int SLIDER_TOP = 15;
    private static final int PATTERN_LEFT = 51;
    private static final int PATTERN_TOP = 15;
    private static final int MAX_PATTERN = 16;
    private static final int PATTERN_SIZE = 18;
    private static final int PATTERN_U = 176;
    private static final int PATTERN_V_START = 15;

    private static final Component INFO_TEXT = Component.translatable("gui.hephaestus_tools.part_builder.info");

    private final InfoPanel infoPanel;
    private float sliderProgress = 0.0F;
    private boolean clickedOnScrollBar;
    private int recipeIndexOffset = 0;

    public PartBuilderScreen(PartBuilderMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = this.imageHeight - 94;
        this.infoPanel = new InfoPanel(false, false);
        this.infoPanel.setTextScale(7f / 9f);
    }

    @Override
    protected void init() {
        super.init();
        this.infoPanel.updatePosition(leftPos, topPos, imageWidth, imageHeight);
    }

    private PartBuilderBlockEntity tile() { return this.menu.getBlockEntity(); }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        graphics.blit(BACKGROUND, leftPos + SLIDER_LEFT,
                topPos + SLIDER_TOP + (int) (SCROLLABLE_AREA * this.sliderProgress),
                canScroll() ? HANDLE_U : HANDLE_U_DISABLE, 0, SLIDER_WIDTH, HANDLE_HEIGHT);

        drawRecipesBackground(graphics, mouseX, mouseY, leftPos + PATTERN_LEFT, topPos + PATTERN_TOP);
        drawRecipesItems(graphics, leftPos + PATTERN_LEFT, topPos + PATTERN_TOP);

        if (!this.menu.getInputSlot().hasItem()) {
            PatternSprite.render(graphics, ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "ingot"),
                    leftPos + this.menu.getInputSlot().x, topPos + this.menu.getInputSlot().y);
        }

        updateInfoPanel();
        this.infoPanel.render(graphics, this.font, mouseX, mouseY);
    }

    private void drawRecipesBackground(GuiGraphics graphics, int mouseX, int mouseY, int left, int top) {
        int max = Math.min(this.recipeIndexOffset + MAX_PATTERN, getPartRecipeCount());
        for (int i = this.recipeIndexOffset; i < max; i++) {
            int relative = i - this.recipeIndexOffset;
            int x = left + relative % 4 * PATTERN_SIZE;
            int y = top + (relative / 4) * PATTERN_SIZE;
            int v = PATTERN_V_START;
            if (i == tile().getSelectedIndex()) {
                v += PATTERN_SIZE;
            } else if (mouseX >= x && mouseY >= y && mouseX < x + PATTERN_SIZE && mouseY < y + PATTERN_SIZE) {
                v += 2 * PATTERN_SIZE;
            }
            graphics.blit(BACKGROUND, x, y, PATTERN_U, v, PATTERN_SIZE, PATTERN_SIZE);
        }
    }

    private void drawRecipesItems(GuiGraphics graphics, int left, int top) {
        List<PartRecipe> list = tile().getSortedButtons();
        int max = Math.min(this.recipeIndexOffset + MAX_PATTERN, list.size());
        for (int i = this.recipeIndexOffset; i < max; ++i) {
            int relative = i - this.recipeIndexOffset;
            int x = left + relative % 4 * PATTERN_SIZE + 1;
            int y = top + (relative / 4) * PATTERN_SIZE + 1;
            PatternSprite.render(graphics, list.get(i).resultPart(), x, y);
        }
    }

    private int getButtonAt(int mouseX, int mouseY) {
        List<PartRecipe> buttons = tile().getSortedButtons();
        if (buttons.isEmpty()) return -1;
        int x = leftPos + PATTERN_LEFT;
        int y = topPos + PATTERN_TOP;
        int maxIndex = Math.min(this.recipeIndexOffset + MAX_PATTERN, buttons.size());
        for (int i = this.recipeIndexOffset; i < maxIndex; ++i) {
            int relative = i - this.recipeIndexOffset;
            double bx = mouseX - (double) (x + relative % 4 * PATTERN_SIZE);
            double by = mouseY - (double) (y + relative / 4 * PATTERN_SIZE);
            if (bx >= 0 && by >= 0 && bx < PATTERN_SIZE && by < PATTERN_SIZE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        int index = getButtonAt(mouseX, mouseY);
        List<PartRecipe> buttons = tile().getSortedButtons();
        if (index >= 0 && index < buttons.size()) {
            Item item = BuiltInRegistries.ITEM.get(buttons.get(index).resultPart());
            if (item != null) {
                graphics.renderTooltip(this.font, new ItemStack(item).getHoverName(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void updateInfoPanel() {
        PartRecipe recipe = tile().getSelectedRecipe();
        if (recipe == null) {
            this.infoPanel.setCaption(this.getTitle());
            this.infoPanel.setText(INFO_TEXT);
            return;
        }

        ItemStack matStack = this.menu.getInputSlot().getItem();
        MaterialId matId = matStack.isEmpty() ? null : findMaterial(matStack);
        Item resultItem = BuiltInRegistries.ITEM.get(recipe.resultPart());
        int slot = (resultItem instanceof ToolPartItem part) ? part.getPartSlot() : 0;

        if (matId == null) {
            this.infoPanel.setCaption(new ItemStack(resultItem).getHoverName());
            List<Component> lines = new ArrayList<>();
            lines.add(costLine(recipe.materialCost(), 0));
            this.infoPanel.setText(lines);
            return;
        }

        Material material = MaterialManager.getInstance().getMaterial(matId);
        MaterialStats stats = MaterialManager.getInstance().getStatsForSlot(matId, slot);

        if (matId != null && PartBuilderBlockEntity.isFoundryMaterial(matId)) {
            this.infoPanel.setCaption(new ItemStack(resultItem).getHoverName());
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.hephaestus_tools.part_builder.metal").withStyle(ChatFormatting.RED));
            this.infoPanel.setText(lines);
            return;
        }

        if (material == null || stats == null || stats == MaterialStats.EMPTY) {
            this.infoPanel.setCaption(new ItemStack(resultItem).getHoverName());
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.hephaestus_tools.part_builder.uncraftable")
                    .withStyle(ChatFormatting.RED));
            this.infoPanel.setText(lines);
            return;
        }

        Component name = Component.translatable("material.hephaestus_tools." + matId.id().getPath())
                .withStyle(s -> s.withColor(material.color()));
        this.infoPanel.setCaption(name);

        List<Component> lines = new ArrayList<>();
        lines.add(costLine(recipe.materialCost(), matStack.getCount()));
        lines.add(Component.empty());
        lines.addAll(statLines(stats, slot));
        this.infoPanel.setText(lines);
    }

    private Component costLine(int cost, int available) {
        Component c = Component.literal(available + " / " + cost);
        return available < cost ? c.copy().withStyle(ChatFormatting.RED) : c;
    }

    private List<Component> statLines(MaterialStats stats, int slot) {
        List<Component> out = new ArrayList<>();
        if (slot == 0) {
            out.add(stat("durability", String.valueOf(stats.durability())));
            out.add(stat("mining_speed", fmt(stats.miningSpeed())));
            out.add(stat("attack_damage", fmt(stats.attackDamage())));
            out.add(stat("harvest_tier", Component.translatable(
                    "tooltip.hephaestus_tools.tier." + stats.tier().name().toLowerCase(Locale.ROOT)).getString()));
        } else if (slot == 1) {
            out.add(stat("durability", pct(stats.durabilityMult())));
            out.add(stat("mining_speed", pct(stats.speedMult())));
            out.add(stat("attack_damage", pct(stats.damageMult())));
            out.add(stat("attack_speed", pct(stats.attackSpeedMult())));
        } else {
            out.add(stat("durability", String.valueOf(stats.durability())));
        }
        return out;
    }

    private static Component stat(String key, String value) {
        return Component.translatable("tooltip.hephaestus_tools." + key, value);
    }

    private static String fmt(float f) { return String.format(Locale.ROOT, "%.2f", f); }

    private static String pct(float mult) {
        int p = Math.round(mult * 100f);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    private MaterialId findMaterial(ItemStack stack) {
        for (Material m : MaterialManager.getInstance().getAllMaterials()) {
            if (m.ingredient().test(stack)) return m.id();
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.clickedOnScrollBar = false;
        List<PartRecipe> buttons = tile().getSortedButtons();
        if (!buttons.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
            int index = getButtonAt((int) mouseX, (int) mouseY);
            if (index >= 0 && this.menu.clickMenuButton(this.minecraft.player, index)) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                if (this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                }
                return true;
            }
            int x = leftPos + SLIDER_LEFT;
            int y = topPos + SLIDER_TOP;
            if (mouseX >= x && mouseX < (x + SLIDER_WIDTH) && mouseY >= y && mouseY < (y + BAR_HEIGHT)) {
                this.clickedOnScrollBar = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.clickedOnScrollBar && canScroll()) {
            int barStart = topPos + SLIDER_TOP;
            int barEnd = barStart + BAR_HEIGHT;
            this.sliderProgress = Mth.clamp(((float) mouseY - barStart - 7.5f) / (barEnd - barStart - SLIDER_TOP), 0, 1);
            this.recipeIndexOffset = Math.round(this.sliderProgress * getHiddenRows()) * 4;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        if (canScroll()) {
            int hidden = getHiddenRows();
            this.sliderProgress = Mth.clamp((float) (this.sliderProgress - deltaY / hidden), 0, 1);
            this.recipeIndexOffset = Math.round(this.sliderProgress * hidden) * 4;
            return true;
        }
        return false;
    }

    private int getPartRecipeCount() { return tile().getSortedButtons().size(); }
    private boolean canScroll() { return getPartRecipeCount() > MAX_PATTERN; }
    private int getHiddenRows() { return (getPartRecipeCount() + 3) / 4 - 4; }
}