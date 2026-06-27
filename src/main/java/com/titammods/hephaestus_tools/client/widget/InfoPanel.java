package com.titammods.hephaestus_tools.client.widget;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class InfoPanel {

    private static final ResourceLocation BACKGROUND_IMAGE = ResourceLocation.fromNamespaceAndPath(
            HephaestusTools.MOD_ID, "textures/gui/panel.png");

    private static final int RES_W = 118, RES_H = 75;
    private static final int TEX_W = 256, TEX_H = 256;

    private static final ElementScreen TOP_LEFT     = new ElementScreen(BACKGROUND_IMAGE, 0, 0, 4, 4, TEX_W, TEX_H);
    private static final ElementScreen TOP_RIGHT    = TOP_LEFT.move(RES_W + 4, 0, 4, 4);
    private static final ElementScreen BOTTOM_LEFT  = TOP_LEFT.move(0, RES_H + 4, 4, 4);
    private static final ElementScreen BOTTOM_RIGHT = TOP_LEFT.move(RES_W + 4, RES_H + 4, 4, 4);

    private static final ScalableElementScreen TOP    = new ScalableElementScreen(BACKGROUND_IMAGE, 4, 0, RES_W, 4, TEX_W, TEX_H);
    private static final ScalableElementScreen BOTTOM = TOP.move(4, 4 + RES_H, RES_W, 4);
    private static final ScalableElementScreen LEFT   = TOP.move(0, 4, 4, RES_H);
    private static final ScalableElementScreen RIGHT  = TOP.move(4 + RES_W, 4, 4, RES_H);
    private static final ScalableElementScreen BG     = TOP.move(4, 4, RES_W, RES_H);

    private static final ElementScreen SLIDER_NORMAL = TOP_LEFT.move(0, 83, 3, 5);
    private static final ElementScreen SLIDER_HOVER  = SLIDER_NORMAL.shift(SLIDER_NORMAL.w, 0);
    private static final ScalableElementScreen SLIDER_BAR    = TOP.move(0, 88, 3, 8);
    private static final ElementScreen SLIDER_TOP_E  = TOP_LEFT.move(3, 88, 3, 4);
    private static final ElementScreen SLIDER_BOT_E  = TOP_LEFT.move(3, 92, 3, 4);

    public int imageWidth  = RES_W + 8;
    public int imageHeight = RES_H + 8;

    public int leftPos;
    public int topPos;
    public int xOffset = 0;
    public int yOffset = 0;

    private Component caption = Component.empty();
    private final List<Component> text = new LinkedList<>();
    private float textScale = 1.0f;

    private final SliderWidget slider;

    private final boolean right;
    private final boolean bottom;

    public InfoPanel(boolean right, boolean bottom) {
        this.right = right;
        this.bottom = bottom;
        this.slider = new SliderWidget(SLIDER_NORMAL, SLIDER_HOVER, SLIDER_HOVER,
                SLIDER_TOP_E, SLIDER_BOT_E, SLIDER_BAR);
    }

    public void updatePosition(int parentX, int parentY, int parentW, int parentH) {
        if (right)  this.leftPos = parentX + parentW;
        else        this.leftPos = parentX - this.imageWidth;
        if (bottom) this.topPos  = parentY + parentH - this.imageHeight;
        else        this.topPos  = parentY;

        this.leftPos += xOffset;
        this.topPos  += yOffset;

        slider.setPosition(guiRight() - 5, topPos + 5 + 12);
        slider.setSize(imageHeight - 10 - 12);
        updateSliderParams();
    }

    public int guiRight()  { return leftPos + imageWidth; }
    public int guiBottom() { return topPos  + imageHeight; }

    public void setCaption(Component caption) {
        this.caption = caption.copy().withStyle(ChatFormatting.UNDERLINE);
        updateSliderParams();
    }

    public void setText(Component text) {
        this.text.clear();
        this.text.add(text);
        updateSliderParams();
    }

    public void setText(List<Component> list) {
        this.text.clear();
        this.text.addAll(list);
        updateSliderParams();
    }

    public void setTextScale(float scale) { this.textScale = scale; }

    private boolean hasCaption() {
        return caption != null && !caption.getString().isEmpty();
    }

    private int scaledFH(Font font) {
        return (int)Math.ceil(font.lineHeight * textScale);
    }

    private List<FormattedCharSequence> getTotalLines(Font font) {
        boolean hasSlider = !slider.isHidden();
        int w = (int)((imageWidth - 10 - (hasSlider ? slider.width + 3 : 0)) / textScale);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component comp : text) {
            if (comp.getString().isEmpty()) { lines.add(Component.empty().getVisualOrderText()); continue; }
            lines.addAll(font.split(comp, w));
        }
        return lines;
    }

    private int calcNeededHeight(Font font) {
        int h = 0;
        if (hasCaption()) { h += scaledFH(font) + 3; }
        h += (int)((font.lineHeight + 0.5f) * getTotalLines(font).size() * textScale);
        return h;
    }

    private void updateSliderParams() {
        slider.hide();
    }

    private void updateSliderWithFont(Font font) {
        int available = imageHeight - 10;
        if (calcNeededHeight(font) <= available) {
            slider.hide();
            return;
        }
        slider.show();
        int sfh = scaledFH(font);
        int hidden = (calcNeededHeight(font) - available + sfh - 1) / sfh;
        slider.setSliderParameters(0, hidden, 1);
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        updateSliderWithFont(font);

        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;

        TOP_LEFT.draw(graphics,     x,         y);
        TOP_RIGHT.draw(graphics,    x + w - 4, y);
        BOTTOM_LEFT.draw(graphics,  x,         y + h - 4);
        BOTTOM_RIGHT.draw(graphics, x + w - 4, y + h - 4);

        TOP.drawScaledX(graphics,    x + 4, y,         w - 8);
        BOTTOM.drawScaledX(graphics, x + 4, y + h - 4, w - 8);
        LEFT.drawScaledY(graphics,   x,     y + 4,     h - 8);
        RIGHT.drawScaledY(graphics,  x + w - 4, y + 4, h - 8);

        BG.drawScaled(graphics, x + 4, y + 4, w - 8, h - 8);

        int color = 0xFFF0F0F0;
        float ty = y + 5;
        float tx = x + 5;
        int sfh = scaledFH(font);

        if (hasCaption()) {
            int cx = x + w / 2 - font.width(caption) / 2;
            graphics.drawString(font, caption.getVisualOrderText(), cx, (int)ty, color, true);
            ty += sfh + 3;
        }

        if (!text.isEmpty()) {
            List<FormattedCharSequence> lines = getTotalLines(font);
            float maxY = (y + h - 5) / textScale;
            float lineH = font.lineHeight + 0.5f;

            graphics.pose().pushPose();
            graphics.pose().scale(textScale, textScale, 1f);
            float sx = tx / textScale;
            float sy = ty / textScale;

            ListIterator<FormattedCharSequence> iter = lines.listIterator(slider.getValue());
            while (iter.hasNext()) {
                if (sy + lineH - 0.5f > maxY) break;
                graphics.drawString(font, iter.next(), (int)sx, (int)sy, color, true);
                sy += lineH;
            }
            graphics.pose().popPose();
        }

        slider.update(mouseX, mouseY);
        slider.draw(graphics);
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int btn) {
        if (!slider.isEnabled() || slider.isHidden()) return false;
        if (btn == 0 && mouseX >= slider.xPos && mouseY >= slider.yPos
                && mouseX <= slider.xPos + slider.width
                && mouseY <= slider.yPos + slider.height) {
            slider.handleMouseClicked((int)mouseX, (int)mouseY, btn);
            return true;
        }
        return false;
    }

    public boolean handleMouseReleased(double mouseX, double mouseY, int state) {
        if (!slider.isEnabled() || slider.isHidden()) return false;
        slider.handleMouseReleased();
        return mouseX >= slider.xPos && mouseY >= slider.yPos
                && mouseX <= slider.xPos + slider.width
                && mouseY <= slider.yPos + slider.height;
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double delta,
                                       boolean isMouseInPanel) {
        if (!slider.isEnabled() || slider.isHidden() || !isMouseInPanel) return false;
        return slider.mouseScrolled(delta);
    }

    public InfoPanel metal() {
        shiftUVs(RES_W + 8, RES_H + 8);
        shiftSlider(12, 0);
        return this;
    }

    public InfoPanel wood() {
        shiftUVs(RES_W + 8, 0);
        shiftSlider(6, 0);
        return this;
    }

    private void shiftUVs(int xd, int yd) {
    }

    private void shiftSlider(int xd, int yd) {
    }
}