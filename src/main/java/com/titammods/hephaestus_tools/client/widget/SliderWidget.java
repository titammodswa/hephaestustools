package com.titammods.hephaestus_tools.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class SliderWidget {

    public final ElementScreen slider;
    public final ElementScreen sliderHighlighted;
    public final ElementScreen sliderDisabled;
    public final ElementScreen slideBarTop;
    public final ElementScreen slideBarBottom;
    public final ScalableElementScreen slideBar;

    protected int minValue;
    protected int maxValue;
    protected int increment;
    protected int currentValue;
    public int xPos;
    public int yPos;
    public int height;
    public int width;
    public int sliderOffset;
    protected boolean enabled;
    protected boolean hidden;
    protected boolean isScrolling;
    protected boolean isHighlighted;
    private int clickY;
    private boolean clickedBar;
    private boolean leftMouseDown = false;

    public SliderWidget(ElementScreen slider, ElementScreen sliderHighlighted, ElementScreen sliderDisabled,
                        ElementScreen slideBarTop, ElementScreen slideBarBottom, ScalableElementScreen slideBar) {
        this.slider = slider;
        this.sliderHighlighted = sliderHighlighted;
        this.sliderDisabled = sliderDisabled;
        this.slideBar = slideBar;
        this.slideBarTop = slideBarTop;
        this.slideBarBottom = slideBarBottom;
        this.height = slideBar.h;
        this.width = slideBar.w;
        this.currentValue = this.minValue = 0;
        this.maxValue = slideBar.h;
        this.increment = 1;
        this.sliderOffset = Mth.abs(slideBar.w - slider.w) / 2;
        this.enabled = true;
        this.hidden = true;
    }

    public void setPosition(int x, int y) { this.xPos = x; this.yPos = y; }
    public void setSize(int height) { this.height = height; }
    public void setSliderParameters(int min, int max, int step) {
        this.minValue = min; this.maxValue = max; this.increment = step;
        this.setSliderValue(this.currentValue);
    }
    public int getValue() { return hidden ? 0 : Math.min(maxValue, Math.max(minValue, currentValue)); }
    public void hide() { this.hidden = true; }
    public void show() { this.hidden = false; }
    public boolean isHidden() { return hidden; }
    public boolean isEnabled() { return enabled; }

    public void draw(GuiGraphics graphics) {
        if (hidden) return;
        slideBarTop.draw(graphics, xPos, yPos);
        slideBar.drawScaledY(graphics, xPos, yPos + slideBarTop.h, getUsableHeight());
        slideBarBottom.draw(graphics, xPos, yPos + height - slideBarBottom.h);
        int sx = xPos + sliderOffset;
        int sy = yPos + getSliderTop();
        if (enabled) {
            if (isScrolling) sliderDisabled.draw(graphics, sx, sy);
            else if (isHighlighted) sliderHighlighted.draw(graphics, sx, sy);
            else slider.draw(graphics, sx, sy);
        } else {
            sliderDisabled.draw(graphics, sx, sy);
        }
    }

    public void update(int mouseX, int mouseY) {
        if (!enabled || hidden) return;
        int x = mouseX - xPos;
        int y = mouseY - yPos;
        if (!leftMouseDown && clickedBar) clickedBar = false;
        if (!leftMouseDown && isScrolling) {
            isScrolling = false;
        } else if (isScrolling) {
            float d = maxValue - minValue;
            float val = (float)(y - clickY) / (float)(getUsableHeight() - slider.h) * d;
            if (val < (float)increment / 2f) setSliderValue(minValue);
            else if (val > maxValue - (float)increment / 2f) setSliderValue(maxValue);
            else setSliderValue((int)(minValue + (float)increment * Math.round(val)));
        } else if (x >= 0 && y >= getSliderTop() && x - sliderOffset <= slider.w && y <= getSliderTop() + slider.h) {
            isHighlighted = true;
            if (leftMouseDown) { isScrolling = true; clickY = y - getSliderTop(); }
        } else if (leftMouseDown && !clickedBar && x >= 0 && y >= 0 && x <= slideBar.w && y <= height) {
            if (y < getSliderTop()) decrement(); else increment();
            clickedBar = true;
        } else {
            isHighlighted = false;
        }
    }

    public void handleMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) leftMouseDown = true;
    }
    public void handleMouseReleased() { leftMouseDown = false; }

    public boolean mouseScrolled(double scrollData) {
        if (scrollData > 0) { decrement(); return true; }
        else if (scrollData < 0) { increment(); return true; }
        return true;
    }

    public int increment() { return setSliderValue(currentValue + increment); }
    public int decrement() { return setSliderValue(currentValue - increment); }

    public int setSliderValue(int val) {
        currentValue = Math.max(minValue, Math.min(maxValue, val));
        return currentValue;
    }

    private int getSliderTop() {
        if (maxValue == minValue) return slideBarTop.h;
        float d = (float)(currentValue - minValue) / (float)(maxValue - minValue);
        d *= getUsableHeight() - slider.h;
        return (int)d + slideBarTop.h;
    }

    private int getUsableHeight() {
        return height - slideBarTop.h - slideBarBottom.h;
    }
}