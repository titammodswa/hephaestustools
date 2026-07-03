package com.titammods.hephaestus_tools.client.widget;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class PatternSprite {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HephaestusTools.MOD_ID, "textures/gui/tinker.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private static final Map<String, int[]> UV = new HashMap<>();
    static {
        UV.put("pick_head",       new int[]{  0, 216});
        UV.put("hammer_head",     new int[]{ 16, 216});
        UV.put("small_axe_head",  new int[]{ 32, 216});
        UV.put("broad_axe_head",  new int[]{ 48, 216});
        UV.put("small_blade",     new int[]{ 64, 216});
        UV.put("large_blade",     new int[]{ 80, 216});
        UV.put("tool_handle",     new int[]{ 96, 216});
        UV.put("tough_handle",    new int[]{112, 216});
        UV.put("tool_binding",    new int[]{128, 216});
        UV.put("tough_binding",   new int[]{144, 216});
        UV.put("ingot",           new int[]{160, 216});
        UV.put("gem",             new int[]{176, 216});
        UV.put("nugget",          new int[]{192, 216});
        UV.put("result",          new int[]{208, 216});
        UV.put("button_repair",   new int[]{224, 216});
        UV.put("rod",             new int[]{240, 216});
        UV.put("quartz",          new int[]{ 96, 232});
        UV.put("dust",            new int[]{112, 232});
        UV.put("lapis",           new int[]{128, 232});
        UV.put("adze_head",       new int[]{144, 232});
        UV.put("large_plate",     new int[]{160, 232});
        UV.put("broad_blade",     new int[]{176, 232});
        UV.put("pickaxe",         new int[]{208, 232});
    }

    private static String name(ResourceLocation rl) {
        if (rl == null) return null;
        String path = rl.getPath();
        return path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
    }

    public static void render(GuiGraphics graphics, ResourceLocation pattern, int x, int y) {
        int[] uv = UV.get(name(pattern));
        if (uv == null) return;
        graphics.blit(TEXTURE, x, y, uv[0], uv[1], 16, 16, TEX_W, TEX_H);
    }

    public static void renderScaled(GuiGraphics graphics, ResourceLocation pattern,
                                    int cornerX, int cornerY) {
        int[] uv = UV.get(name(pattern));
        if (uv == null) return;
        final float scale = 3.7f;
        final float xOff = 12.5f;
        final float yOff = 22f;
        graphics.pose().pushPose();
        graphics.pose().translate(xOff, yOff, 0f);
        graphics.pose().scale(scale, scale, 1f);
        graphics.blit(TEXTURE, (int)(cornerX / scale), (int)(cornerY / scale),
                uv[0], uv[1], 16, 16, TEX_W, TEX_H);
        graphics.pose().popPose();
    }

    private PatternSprite() {}
}