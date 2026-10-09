package com.titammods.hephaestus_tools.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.client.ToolColorHandler;
import com.titammods.hephaestus_tools.client.model.ModifierModelRegistry;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ToolItemRenderer implements SpecialModelRenderer<List<Identifier>> {

    public static final Identifier RENDERER_ID =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "tool_modifiers");

    private static final Map<String, List<String>> TOOL_LAYERS = Map.ofEntries(
            Map.entry("pickaxe", List.of(
                    "item/tool/pickaxe/head", "item/tool/pickaxe/handle", "item/tool/pickaxe/binding")),
            Map.entry("sledge_hammer", List.of(
                    "item/tool/sledge_hammer/handle", "item/tool/sledge_hammer/head",
                    "item/tool/sledge_hammer/front", "item/tool/sledge_hammer/back")),
            Map.entry("vein_hammer", List.of(
                    "item/tool/vein_hammer/handle", "item/tool/vein_hammer/head",
                    "item/tool/vein_hammer/front", "item/tool/vein_hammer/grip")),
            Map.entry("mattock", List.of(
                    "item/tool/mattock/axe", "item/tool/pickaxe/handle", "item/tool/mattock/pick")),
            Map.entry("excavator", List.of(
                    "item/tool/excavator/head", "item/tool/excavator/grip",
                    "item/tool/excavator/handle", "item/tool/excavator/binding")),
            Map.entry("hand_axe", List.of(
                    "item/tool/hand_axe/head", "item/tool/pickaxe/handle", "item/tool/hand_axe/binding")),
            Map.entry("broad_axe", List.of(
                    "item/tool/broad_axe/handle", "item/tool/broad_axe/blade",
                    "item/tool/broad_axe/back", "item/tool/broad_axe/binding")),
            Map.entry("kama", List.of(
                    "item/tool/kama/head", "item/tool/pickaxe/handle", "item/tool/kama/binding")),
            Map.entry("scythe", List.of(
                    "item/tool/scythe/head", "item/tool/scythe/accessory",
                    "item/tool/scythe/handle", "item/tool/scythe/binding")),
            Map.entry("dagger", List.of(
                    "item/tool/dagger/blade", "item/tool/dagger/guard",
                    "item/tool/dagger/crossguard", "item/tool/dagger/handle")),
            Map.entry("sword", List.of(
                    "item/tool/sword/blade", "item/tool/sword/guard", "item/tool/sword/handle")),
            Map.entry("cleaver", List.of(
                    "item/tool/cleaver/head", "item/tool/cleaver/shield",
                    "item/tool/cleaver/guard", "item/tool/cleaver/handle"))
    );

    private static final Map<String, List<String>> WORKBENCH_LAYERS = Map.ofEntries(
            Map.entry("hand_axe", List.of(
                    "item/tool/workbench/hand_axe/head", "item/tool/pickaxe/handle", "item/tool/workbench/hand_axe/binding")),
            Map.entry("cleaver", List.of(
                    "item/tool/workbench/cleaver/head", "item/tool/cleaver/shield",
                    "item/tool/cleaver/guard", "item/tool/cleaver/handle"))
    );

    public static List<String> workbenchLayersFor(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        List<String> layers = WORKBENCH_LAYERS.get(id.getPath());
        return layers != null ? layers : TOOL_LAYERS.get(id.getPath());
    }

    public static Map<String, List<String>> toolLayers() {
        return TOOL_LAYERS;
    }

    public static int layerCount(Item item) {
        List<String> layers = layersFor(item);
        return layers == null ? 0 : layers.size();
    }

    public static List<String> layersFor(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return null;
        return TOOL_LAYERS.get(id.getPath());
    }

    private static Identifier png(String path) {
        return Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "textures/" + path + ".png");
    }

    private static Identifier pngOf(Identifier texture) {
        return Identifier.fromNamespaceAndPath(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
    }

    public static void renderGuiPreview(GuiGraphicsExtractor graphics,
                                        ItemStack stack, int x, int y, int size, float alpha) {
        List<String> layers = layersFor(stack.getItem());
        if (layers == null) return;
        int alphaBits = Math.max(0, Math.min(255, Math.round(alpha * 255f))) << 24;
        for (int i = 0; i < layers.size(); i++) {
            int color = (ToolColorHandler.INSTANCE.getColor(stack, i) & 0x00FFFFFF) | alphaBits;
            graphics.blit(RenderPipelines.GUI_TEXTURED, png(layers.get(i)), x, y,
                    0f, 0f, size, size, 16, 16, 16, 16, color);
        }
        for (Identifier overlay : modifierTextures(stack)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, overlay, x, y,
                    0f, 0f, size, size, 16, 16, 16, 16, 0x00FFFFFF | alphaBits);
        }
    }

    public static void renderGuiPreviewTinted(GuiGraphicsExtractor graphics,
                                              ItemStack stack, int x, int y, int size, int argb) {
        List<String> layers = workbenchLayersFor(stack.getItem());
        if (layers == null) return;
        int ta = argb >>> 24 & 255, tr = argb >> 16 & 255, tg = argb >> 8 & 255, tb = argb & 255;
        for (int i = 0; i < layers.size(); i++) {
            int base = ToolColorHandler.INSTANCE.getColor(stack, i);
            int r = (base >> 16 & 255) * tr / 255, g = (base >> 8 & 255) * tg / 255, b = (base & 255) * tb / 255;
            int color = ta << 24 | r << 16 | g << 8 | b;
            graphics.blit(RenderPipelines.GUI_TEXTURED, png(layers.get(i)), x, y,
                    0f, 0f, size, size, 16, 16, 16, 16, color);
        }
        for (Identifier overlay : modifierTextures(stack)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, overlay, x, y,
                    0f, 0f, size, size, 16, 16, 16, 16, argb);
        }
    }

    private static final Map<Identifier, Boolean> TEXTURE_EXISTS = new ConcurrentHashMap<>();

    private static boolean exists(Identifier png) {
        return TEXTURE_EXISTS.computeIfAbsent(png,
                path -> Minecraft.getInstance().getResourceManager().getResource(path).isPresent());
    }

    public static void clearTextureCache() {
        TEXTURE_EXISTS.clear();
    }

    private static List<Identifier> modifierTextures(ItemStack stack) {
        List<Identifier> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;
        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(stack)) {
            if (mod.level() <= 0) continue;
            Identifier tex = ModifierModelRegistry.getTexture(stack.getItem(), mod.id());
            if (tex == null) continue;
            Identifier png = pngOf(tex);
            if (exists(png)) out.add(png);
        }
        return out;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<List<Identifier>> {

        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<List<Identifier>> bake(SpecialModelRenderer.BakingContext ctx) {
            return new ToolItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<List<Identifier>>> type() {
            return CODEC;
        }
    }

    private static final float FRONT_Z = 8.5f / 16f + 1f / 512f;
    private static final float BACK_Z = 7.5f / 16f - 1f / 512f;

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0f, 0f, BACK_Z));
        output.accept(new Vector3f(1f, 0f, BACK_Z));
        output.accept(new Vector3f(0f, 1f, BACK_Z));
        output.accept(new Vector3f(1f, 1f, BACK_Z));
        output.accept(new Vector3f(0f, 0f, FRONT_Z));
        output.accept(new Vector3f(1f, 0f, FRONT_Z));
        output.accept(new Vector3f(0f, 1f, FRONT_Z));
        output.accept(new Vector3f(1f, 1f, FRONT_Z));
    }

    @Override
    public @Nullable List<Identifier> extractArgument(ItemStack stack) {
        List<Identifier> textures = modifierTextures(stack);
        return textures.isEmpty() ? null : textures;
    }

    @Override
    public void submit(@Nullable List<Identifier> textures, PoseStack poseStack,
                       SubmitNodeCollector collector,
                       int packedLight, int packedOverlay,
                       boolean hasFoil, int outlineColor) {
        if (textures == null || textures.isEmpty()) return;

        int sky = packedLight >> 16 & 0xFFFF;
        int block = packedLight & 0xFFFF;

        poseStack.pushPose();
        for (Identifier texture : textures) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                    (pose, buf) -> overlayQuads(buf, pose.pose(), packedOverlay, sky, block));
        }
        poseStack.popPose();
    }

    private static void overlayQuads(VertexConsumer buf, Matrix4f m, int packedOverlay, int sky, int block) {
        v(buf, m, 0f, 0f, FRONT_Z, 0f, 1f, 1f, packedOverlay, sky, block);
        v(buf, m, 1f, 0f, FRONT_Z, 1f, 1f, 1f, packedOverlay, sky, block);
        v(buf, m, 1f, 1f, FRONT_Z, 1f, 0f, 1f, packedOverlay, sky, block);
        v(buf, m, 0f, 1f, FRONT_Z, 0f, 0f, 1f, packedOverlay, sky, block);

        v(buf, m, 0f, 0f, BACK_Z, 0f, 1f, -1f, packedOverlay, sky, block);
        v(buf, m, 0f, 1f, BACK_Z, 0f, 0f, -1f, packedOverlay, sky, block);
        v(buf, m, 1f, 1f, BACK_Z, 1f, 0f, -1f, packedOverlay, sky, block);
        v(buf, m, 1f, 0f, BACK_Z, 1f, 1f, -1f, packedOverlay, sky, block);
    }

    private static void v(VertexConsumer buf, Matrix4f m,
                          float x, float y, float z, float u, float vv, float normalZ,
                          int packedOverlay, int sky, int block) {
        buf.addVertex(m, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, vv)
                .setOverlay(packedOverlay)
                .setUv2(block, sky)
                .setNormal(0f, 0f, normalZ);
    }
}