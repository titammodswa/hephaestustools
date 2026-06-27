package com.titammods.hephaestus_tools.tables.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.titammods.hephaestus_tools.client.widget.PatternSprite;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.client.widget.ElementScreen;
import com.titammods.hephaestus_tools.client.widget.InfoPanel;
import com.titammods.hephaestus_tools.client.widget.ScalableElementScreen;
import com.titammods.hephaestus_tools.client.widget.TinkersAnvilButtonsWidget;
import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import com.titammods.hephaestus_tools.tables.layout.ToolLayouts;
import com.titammods.hephaestus_tools.tables.menu.TinkersAnvilMenu;
import com.titammods.hephaestus_tools.tables.menu.slot.TinkersAnvilSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TinkersAnvilScreen extends AbstractContainerScreen<TinkersAnvilMenu> {

    private static final ResourceLocation TINKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "textures/gui/tinker.png");

    private static final ResourceLocation RESULT_ICON =
            ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "gui/tinker_pattern/result");

    private static final ElementScreen ACTIVE_TEXT_FIELD =
            new ElementScreen(TINKER_TEXTURE, 0, 232, 90, 12, 256, 256);
    private static final ElementScreen ITEM_COVER        = ACTIVE_TEXT_FIELD.move(176, 18, 70, 64);
    private static final ElementScreen SLOT_BACKGROUND   = ACTIVE_TEXT_FIELD.move(176,  0, 18, 18);
    private static final ElementScreen SLOT_BORDER       = ACTIVE_TEXT_FIELD.move(194,  0, 18, 18);
    private static final ElementScreen SLOT_SPACE_TOP    = ACTIVE_TEXT_FIELD.move(0, 198, 18, 2);
    private static final ElementScreen SLOT_SPACE_BOTTOM = ACTIVE_TEXT_FIELD.move(0, 196, 18, 2);
    private static final ElementScreen PANEL_SPACE_LEFT  = ACTIVE_TEXT_FIELD.move(0, 196,  5, 4);
    private static final ElementScreen PANEL_SPACE_RIGHT = ACTIVE_TEXT_FIELD.move(9, 196,  9, 4);
    private static final ElementScreen LEFT_BEAM         = ACTIVE_TEXT_FIELD.move(  0, 202,   2, 7);
    private static final ElementScreen RIGHT_BEAM        = ACTIVE_TEXT_FIELD.move(131, 202,   2, 7);
    private static final ScalableElementScreen CENTER_BEAM =
            new ScalableElementScreen(TINKER_TEXTURE, 2, 202, 129, 7, 256, 256);

    public static final int COLUMN_COUNT = 6;

    private static final int REAL_W = 176;
    private static final int REAL_H = 184;

    private int cornerX, cornerY;

    private ElementScreen buttonDecorationTop;
    private ElementScreen buttonDecorationBot;
    private ElementScreen panelDecorationL;
    private ElementScreen panelDecorationR;
    private ElementScreen leftBeam;
    private ElementScreen rightBeam;
    private ScalableElementScreen centerBeam;

    private final InfoPanel tinkerInfo;
    private final InfoPanel modifierInfo;
    private TinkersAnvilButtonsWidget buttonsScreen;

    private ToolLayout currentLayout;
    private int maxInputs;
    private int activeInputs;

    private static final Component COMPONENTS_TEXT =
            Component.translatable("gui.hephaestus_tools.components");
    private static final Component ASCII_ANVIL = Component.literal("\n\n")
            .append("       .\n")
            .append("     /( _________\n")
            .append("     |  >:=========`\n")
            .append("     )(  \n")
            .append("     \"\"")
            .withStyle(ChatFormatting.DARK_GRAY);
    private static final Component COMPONENT_ERROR   =
            Component.translatable("gui.hephaestus_tools.error").withStyle(ChatFormatting.RED);
    private static final Component COMPONENT_WARNING =
            Component.translatable("gui.hephaestus_tools.warning").withStyle(ChatFormatting.YELLOW);

    public TinkersAnvilScreen(TinkersAnvilMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = REAL_W;
        this.imageHeight = REAL_H;
        this.currentLayout = ToolLayouts.REPAIR;
        this.maxInputs = TinkersAnvilMenu.MAX_INPUTS;
        this.activeInputs = 0;

        this.tinkerInfo   = new InfoPanel(true, false);
        this.modifierInfo = new InfoPanel(true, false);
        this.tinkerInfo.setTextScale(8f / 9f);
        this.modifierInfo.setTextScale(7f / 9f);

        metal();
    }

    private void metal() {
        this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w * 2, 0);
        this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w * 2, 0);
        this.panelDecorationL    = PANEL_SPACE_LEFT.shift(18 * 2, 0);
        this.panelDecorationR    = PANEL_SPACE_RIGHT.shift(18 * 2, 0);
        this.leftBeam            = LEFT_BEAM.shift(0, LEFT_BEAM.h);
        this.rightBeam           = RIGHT_BEAM.shift(0, RIGHT_BEAM.h);
        this.centerBeam          = CENTER_BEAM.shift(0, CENTER_BEAM.h);
    }

    @Override
    protected void init() {
        super.init();

        this.cornerX = this.leftPos;
        this.cornerY = this.topPos;

        this.tinkerInfo.xOffset = 2;
        this.tinkerInfo.yOffset = centerBeam.h + panelDecorationL.h;
        this.tinkerInfo.updatePosition(cornerX, cornerY, REAL_W, REAL_H);

        this.modifierInfo.xOffset = 2;
        this.modifierInfo.yOffset = tinkerInfo.yOffset + tinkerInfo.imageHeight + 4;
        this.modifierInfo.updatePosition(cornerX, cornerY, REAL_W, REAL_H);

        this.inventoryLabelY = 102 - 10;

        List<ToolLayout> layouts = new ArrayList<>();
        layouts.add(ToolLayouts.REPAIR);
        layouts.addAll(ToolLayouts.ALL.subList(1, ToolLayouts.ALL.size()));

        int btnLeftPos = cornerX - TinkersAnvilButtonsWidget.width(COLUMN_COUNT) - 2;
        int btnTopPos  = cornerY + centerBeam.h + buttonDecorationTop.h;

        this.buttonsScreen = new TinkersAnvilButtonsWidget(
                btnLeftPos, btnTopPos,
                COLUMN_COUNT, layouts,
                TinkersAnvilButtonsWidget.METAL_STYLE,
                currentLayout,
                this::onToolSelection
        );

        onToolSelection(ToolLayouts.REPAIR);
    }

    public void onToolSelection(ToolLayout layout) {
        this.activeInputs = Math.min(layout.inputs().size(), maxInputs);
        this.currentLayout = layout;
        this.menu.setToolSelection(layout);
        int idx = ToolLayouts.ALL.indexOf(layout);
        if (idx >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, idx);
        }
        updateDisplay();
    }

    public void updateDisplay() {
        ItemStack toolStack = menu.getSlot(TinkersAnvilMenu.TOOL_SLOT).getItem();
        if (!toolStack.isEmpty()) {
            tinkerInfo.setCaption(toolStack.getHoverName());
            tinkerInfo.setText(
                    com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.stats(toolStack, true));

            java.util.List<Component> mods =
                    com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder.modifiers(toolStack, true);
            if (mods.isEmpty()) {
                modifierInfo.setCaption(Component.empty());
                modifierInfo.setText(ASCII_ANVIL);
            } else {
                modifierInfo.setCaption(Component.translatable("gui.hephaestus_tools.modifiers"));
                modifierInfo.setText(mods);
            }
            return;
        }

        tinkerInfo.setCaption(currentLayout.displayName());
        tinkerInfo.setText(currentLayout.description());

        MutableComponent fullText = Component.literal("");
        boolean hasComponents = false;

        for (int i = 0; i < activeInputs; i++) {
            ToolLayout.InputSlotDef def = i < currentLayout.inputs().size()
                    ? currentLayout.inputs().get(i) : null;
            if (def == null || def.expectedPart() == null) continue;

            hasComponents = true;
            MutableComponent line = Component.literal(" * ");
            ItemStack slotStack = menu.getSlot(TinkersAnvilMenu.INPUT_START + i).getItem();
            boolean valid = !slotStack.isEmpty() && slotStack.getItem() == def.expectedPart();
            line.withStyle(valid ? ChatFormatting.GREEN : ChatFormatting.RED);
            line.append(Component.translatable(def.expectedPart().getDescriptionId())).append("\n");
            fullText.append(line);
        }

        if (hasComponents) {
            modifierInfo.setCaption(COMPONENTS_TEXT);
            modifierInfo.setText(fullText);
        } else {
            modifierInfo.setCaption(Component.empty());
            modifierInfo.setText(ASCII_ANVIL);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TINKER_TEXTURE, cornerX, cornerY, 0, 0, REAL_W, REAL_H, 256, 256);

        ResourceLocation layoutIcon = currentLayout.toolSlot().iconTexture();
        if (layoutIcon == null && !currentLayout.inputs().isEmpty()) {
            layoutIcon = currentLayout.inputs().get(0).iconTexture();
        }
        if (layoutIcon != null) {
            PatternSprite.renderScaled(graphics, layoutIcon, cornerX, cornerY);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 0.82f);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        ITEM_COVER.draw(graphics, cornerX + 7, cornerY + 18);

        RenderSystem.setShaderColor(1f, 1f, 1f, 0.28f);
        if (!currentLayout.toolSlot().hidden()) {
            Slot ts = menu.getSlot(TinkersAnvilMenu.TOOL_SLOT);
            SLOT_BACKGROUND.draw(graphics, cornerX + ts.x - 1, cornerY + ts.y - 1);
        }
        for (int i = 0; i < activeInputs; i++) {
            Slot slot = menu.getSlot(TinkersAnvilMenu.INPUT_START + i);
            SLOT_BACKGROUND.draw(graphics, cornerX + slot.x - 1, cornerY + slot.y - 1);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        for (int i = 0; i <= maxInputs; i++) {
            Slot slot = menu.getSlot(i);
            if (slot instanceof TinkersAnvilSlot ts) {
                if (!ts.isDormant() || slot.hasItem()) {
                    SLOT_BORDER.draw(graphics, cornerX + slot.x - 1, cornerY + slot.y - 1);
                }
            } else {
                SLOT_BORDER.draw(graphics, cornerX + slot.x - 1, cornerY + slot.y - 1);
            }
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        for (int i = 0; i <= maxInputs; i++) {
            Slot slotG = menu.getSlot(i);
            if (slotG.hasItem()) continue;
            ResourceLocation icon = getSlotIcon(i);
            if (icon != null) {
                PatternSprite.render(graphics, icon, cornerX + slotG.x, cornerY + slotG.y);
            }
        }
        Slot outSlot = menu.getSlot(TinkersAnvilMenu.OUTPUT_IDX);
        if (!outSlot.hasItem()) {
            PatternSprite.render(graphics, RESULT_ICON, cornerX + outSlot.x, cornerY + outSlot.y);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int x = buttonsScreen.getLeftPos() - leftBeam.w;
        int y = cornerY;
        leftBeam.draw(graphics, x, y);
        x += leftBeam.w;
        x += centerBeam.drawScaledX(graphics, x, y, buttonsScreen.getImageWidth());
        rightBeam.draw(graphics, x, y);

        x = tinkerInfo.leftPos - leftBeam.w;
        leftBeam.draw(graphics, x, y);
        x += leftBeam.w;
        x += centerBeam.drawScaledX(graphics, x, y, tinkerInfo.imageWidth);
        rightBeam.draw(graphics, x, y);

        List<com.titammods.hephaestus_tools.client.widget.SlotButtonItem> buttons =
                buttonsScreen.getButtons();
        for (com.titammods.hephaestus_tools.client.widget.SlotButtonItem button : buttons) {
            buttonDecorationTop.draw(graphics, button.getX(), button.getY() - buttonDecorationTop.h);
            if (button.buttonId < buttons.size() - COLUMN_COUNT) {
                buttonDecorationBot.draw(graphics, button.getX(), button.getY() + button.getHeight());
            }
        }

        panelDecorationL.draw(graphics, tinkerInfo.leftPos + 5, tinkerInfo.topPos - panelDecorationL.h);
        panelDecorationR.draw(graphics, tinkerInfo.guiRight() - 5 - panelDecorationR.w, tinkerInfo.topPos - panelDecorationR.h);
        panelDecorationL.draw(graphics, modifierInfo.leftPos + 5, modifierInfo.topPos - panelDecorationL.h);
        panelDecorationR.draw(graphics, modifierInfo.guiRight() - 5 - panelDecorationR.w, modifierInfo.topPos - panelDecorationR.h);

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        buttonsScreen.render(graphics, mouseX, mouseY, partialTick);

        tinkerInfo.render(graphics, font, mouseX, mouseY);
        modifierInfo.render(graphics, font, mouseX, mouseY);
    }

    private ItemStack lastToolStack = ItemStack.EMPTY;

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack current = menu.getSlot(TinkersAnvilMenu.TOOL_SLOT).getItem();
        if (!ItemStack.matches(current, lastToolStack)) {
            lastToolStack = current.copy();
            updateDisplay();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (tinkerInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) return false;
        if (modifierInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) return false;
        if (buttonsScreen.handleMouseClicked(mouseX, mouseY, mouseButton)) return false;
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int btn, double dx, double dy) {
        if (tinkerInfo.handleMouseClicked(mouseX, mouseY, btn)) return false;
        if (modifierInfo.handleMouseClicked(mouseX, mouseY, btn)) return false;
        return super.mouseDragged(mouseX, mouseY, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean inTinker = mouseX >= tinkerInfo.leftPos && mouseX < tinkerInfo.guiRight()
                && mouseY >= tinkerInfo.topPos && mouseY < tinkerInfo.guiBottom();
        boolean inMod = mouseX >= modifierInfo.leftPos && mouseX < modifierInfo.guiRight()
                && mouseY >= modifierInfo.topPos && mouseY < modifierInfo.guiBottom();
        if (tinkerInfo.handleMouseScrolled(mouseX, mouseY, scrollY, inTinker)) return false;
        if (modifierInfo.handleMouseScrolled(mouseX, mouseY, scrollY, inMod)) return false;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (tinkerInfo.handleMouseReleased(mouseX, mouseY, state)) return false;
        if (modifierInfo.handleMouseReleased(mouseX, mouseY, state)) return false;
        buttonsScreen.handleMouseReleased(mouseX, mouseY, state);
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, getTitle().getVisualOrderText(), 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle.getVisualOrderText(),
                inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot instanceof TinkersAnvilSlot ts && ts.isDormant() && !slot.hasItem()) return;
        super.renderSlot(graphics, slot);
    }

    @javax.annotation.Nullable
    private ResourceLocation getSlotIcon(int slotIndex) {
        if (slotIndex == TinkersAnvilMenu.TOOL_SLOT) {
            return currentLayout.toolSlot().hidden() ? null : currentLayout.toolSlot().iconTexture();
        }
        int inputIdx = slotIndex - TinkersAnvilMenu.INPUT_START;
        java.util.List<ToolLayout.InputSlotDef> inputs = currentLayout.inputs();
        if (inputIdx >= 0 && inputIdx < inputs.size()) {
            return inputs.get(inputIdx).iconTexture();
        }
        return null;
    }
}