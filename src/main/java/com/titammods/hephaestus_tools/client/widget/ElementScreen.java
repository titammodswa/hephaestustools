package com.titammods.hephaestus_tools.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ElementScreen {

    public ResourceLocation texture;
    public final int x;
    public final int y;
    public final int w;
    public final int h;
    public final int texW;
    public final int texH;

    public ElementScreen(ResourceLocation texture, int x, int y, int w, int h, int texW, int texH) {
        this.texture = texture;
        this.x = x; this.y = y;
        this.w = w; this.h = h;
        this.texW = texW; this.texH = texH;
    }

    public ElementScreen move(int x, int y, int width, int height) {
        return new ElementScreen(this.texture, x, y, width, height, this.texW, this.texH);
    }

    public ElementScreen shift(int xd, int yd) {
        return move(x + xd, y + yd, this.w, this.h);
    }

    public void draw(GuiGraphics graphics, int xPos, int yPos) {
        graphics.blit(this.texture, xPos, yPos, this.x, this.y, this.w, this.h, this.texW, this.texH);
    }
}