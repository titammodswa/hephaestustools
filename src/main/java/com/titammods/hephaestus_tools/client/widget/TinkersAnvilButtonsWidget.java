package com.titammods.hephaestus_tools.client.widget;

import com.titammods.hephaestus_tools.tables.layout.ToolLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class TinkersAnvilButtonsWidget {

    public static final int SPACING  = 4;
    public static final int MAX_ROWS = 8;

    public static final int METAL_STYLE = 1;
    public static final int WOOD_STYLE  = 2;

    private final int leftPos;
    private final int topPos;
    private final int columns;
    private final int buttonWidth;
    private final int buttonHeight;

    private final List<SlotButtonItem> buttons = new ArrayList<>();
    private Button previousPageButton;
    private Button nextPageButton;
    private int page = 0;

    private final int imageWidth;

    public TinkersAnvilButtonsWidget(int leftPos, int topPos, int columns,
                                     List<ToolLayout> layouts, int style,
                                     ToolLayout currentLayout,
                                     java.util.function.Consumer<ToolLayout> onSelection) {
        this.leftPos      = leftPos;
        this.topPos       = topPos;
        this.columns      = columns;
        this.buttonWidth  = SlotButtonItem.WIDTH;
        this.buttonHeight = SlotButtonItem.HEIGHT;
        this.imageWidth   = size(columns, buttonWidth);

        this.previousPageButton = Button.builder(Component.literal("<"),
                        b -> { if (page > 0) { page--; setButtonPositions(); } })
                .pos(leftPos, topPos - buttonHeight)
                .size(buttonWidth, buttonHeight)
                .build();

        this.nextPageButton = Button.builder(Component.literal(">"),
                        b -> { if (!isMaxPage(page)) { page++; setButtonPositions(); } })
                .pos(leftPos + (buttonWidth + SPACING) * (columns - 1), topPos - buttonHeight)
                .size(buttonWidth, buttonHeight)
                .build();

        Button.OnPress onButtonPressed = self -> {
            for (SlotButtonItem btn : buttons) btn.pressed = false;
            if (self instanceof SlotButtonItem slotBtn) {
                slotBtn.pressed = true;
                onSelection.accept(slotBtn.getLayout());
            }
        };

        for (int i = 0; i < layouts.size(); i++) {
            ToolLayout layout = layouts.get(i);
            SlotButtonItem btn = new SlotButtonItem(i, -1, -1, layout, onButtonPressed);
            applyStyle(btn, style);
            buttons.add(btn);
            if (layout == currentLayout) btn.pressed = true;
        }

        setButtonPositions();
    }

    private void applyStyle(SlotButtonItem btn, int style) {
        int yShift = -18 * style;
        ResourceLocation icons = ResourceLocation.fromNamespaceAndPath(
                com.titammods.hephaestus_tools.HephaestusTools.MOD_ID, "textures/gui/icons.png");
        btn.setGraphics(
                new ElementScreen(icons, 180,      216 + yShift, SlotButtonItem.WIDTH, SlotButtonItem.HEIGHT, 256, 256),
                new ElementScreen(icons, 216,      216 + yShift, SlotButtonItem.WIDTH, SlotButtonItem.HEIGHT, 256, 256),
                new ElementScreen(icons, 144,      216 + yShift, SlotButtonItem.WIDTH, SlotButtonItem.HEIGHT, 256, 256)
        );
    }

    public void setButtonPositions() {
        int count = buttons.size();
        boolean needsPages = count > columns * MAX_ROWS;

        previousPageButton.visible = needsPages && page > 0;
        nextPageButton.visible     = needsPages && !isMaxPage(page);

        if (needsPages) {
            previousPageButton.setX(leftPos);
            previousPageButton.setY(topPos - buttonHeight);
            nextPageButton.setX(leftPos + (buttonWidth + SPACING) * (columns - 1));
            nextPageButton.setY(topPos - buttonHeight);
        }

        int startIndex = page * columns * MAX_ROWS;
        int endIndex   = Math.min(startIndex + columns * MAX_ROWS, count);

        for (int i = 0; i < startIndex; i++) {
            buttons.get(i).setX(0); buttons.get(i).setY(0); buttons.get(i).visible = false;
        }
        for (int i = startIndex; i < endIndex; i++) {
            int col = (i - startIndex) % columns;
            int row = (i - startIndex) / columns;
            buttons.get(i).setX(leftPos + col * (buttonWidth + SPACING));
            buttons.get(i).setY(topPos  + row * (buttonHeight + SPACING));
            buttons.get(i).visible = true;
        }
        for (int i = endIndex; i < count; i++) {
            buttons.get(i).setX(0); buttons.get(i).setY(0); buttons.get(i).visible = false;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int count = buttons.size();
        if (count > columns * MAX_ROWS) {
            previousPageButton.render(graphics, mouseX, mouseY, partialTicks);
            nextPageButton.render(graphics, mouseX, mouseY, partialTicks);
        }
        int startIndex = page * columns * MAX_ROWS;
        int endIndex   = Math.min(startIndex + columns * MAX_ROWS, count);
        for (int i = startIndex; i < endIndex; i++) {
            buttons.get(i).render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int btn) {
        if (btn == 0) {
            int count = buttons.size();
            if (count > columns * MAX_ROWS) {
                if (previousPageButton.mouseClicked(mouseX, mouseY, btn)) return true;
                if (nextPageButton.mouseClicked(mouseX, mouseY, btn)) return true;
            }
            for (SlotButtonItem button : buttons) {
                if (button.mouseClicked(mouseX, mouseY, btn)) return true;
            }
        }
        return false;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
        for (SlotButtonItem button : buttons) {
            button.mouseReleased(mouseX, mouseY, state);
        }
        return false;
    }

    public List<SlotButtonItem> getButtons() { return buttons; }
    public int getLeftPos()    { return leftPos; }
    public int getImageWidth() { return imageWidth; }

    public Rect2i getArea() {
        int rows = Math.min(MAX_ROWS, (buttons.size() + columns - 1) / columns);
        return new Rect2i(leftPos, topPos,
                size(columns, buttonWidth),
                size(rows, buttonHeight));
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        Rect2i area = getArea();
        return mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight();
    }

    private boolean isMaxPage(int page) {
        return buttons.size() <= (page + 1) * columns * MAX_ROWS;
    }

    public static int size(int count, int buttonSize) {
        return buttonSize * count + SPACING * (count - 1);
    }

    public static int width(int columns) {
        return size(columns, SlotButtonItem.WIDTH);
    }
}