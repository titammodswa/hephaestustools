package com.titammods.hephaestus_tools.client.widget;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SlotButtonItem extends Button {

    public static final int WIDTH  = 18;
    public static final int HEIGHT = 18;

    private static final ResourceLocation ICONS =
            ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "textures/gui/icons.png");

    private static final ElementScreen BUTTON_PRESSED_BASE = new ElementScreen(ICONS, 144,       216, WIDTH, HEIGHT, 256, 256);
    private static final ElementScreen BUTTON_NORMAL_BASE  = new ElementScreen(ICONS, 144 + 36,  216, WIDTH, HEIGHT, 256, 256);
    private static final ElementScreen BUTTON_HOVER_BASE   = new ElementScreen(ICONS, 144 + 72,  216, WIDTH, HEIGHT, 256, 256);

    private final ToolLayout layout;
    public boolean pressed;
    public final int buttonId;

    private ElementScreen pressedGui = BUTTON_PRESSED_BASE;
    private ElementScreen normalGui  = BUTTON_NORMAL_BASE;
    private ElementScreen hoverGui   = BUTTON_HOVER_BASE;

    public SlotButtonItem(int buttonId, int x, int y, ToolLayout layout, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, layout.displayName(), onPress, DEFAULT_NARRATION);
        this.layout = layout;
        this.buttonId = buttonId;
    }

    public SlotButtonItem setGraphics(ElementScreen normal, ElementScreen hover, ElementScreen pressed) {
        this.pressedGui = pressed;
        this.normalGui  = normal;
        this.hoverGui   = hover;
        return this;
    }

    public ToolLayout getLayout() { return layout; }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = getX();
        int y = getY();

        if (this.pressed) {
            pressedGui.draw(graphics, x, y);
        } else if (this.isHovered) {
            hoverGui.draw(graphics, x, y);
        } else {
            normalGui.draw(graphics, x, y);
        }

        graphics.renderItem(new ItemStack(layout.iconItem()), x + 1, y + 1);
    }
}