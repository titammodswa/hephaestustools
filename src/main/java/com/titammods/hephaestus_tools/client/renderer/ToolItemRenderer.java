package com.titammods.hephaestus_tools.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Transformation;
import com.titammods.hephaestus_tools.client.model.ModifierModelRegistry;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ToolItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Cache<TextureAtlasSprite, List<BakedQuad>> QUAD_CACHE =
            CacheBuilder.newBuilder().maximumSize(256).expireAfterWrite(5, TimeUnit.MINUTES).build();

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

    public ToolItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    private static List<BakedQuad> bakeQuadsForSprite(TextureAtlasSprite sprite) {
        var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, sprite, null);
        return UnbakedGeometryHelper.bakeElements(
                unbaked, m -> sprite, new SimpleModelState(Transformation.identity()));
    }

    private static List<BakedQuad> getQuads(TextureAtlasSprite sprite) {
        try {
            return QUAD_CACHE.get(sprite, () -> bakeQuadsForSprite(sprite));
        } catch (ExecutionException e) {
            return List.of();
        }
    }

    private static boolean isMissing(Function<ResourceLocation, TextureAtlasSprite> atlas,
                                     TextureAtlasSprite sprite) {
        return sprite == atlas.apply(
                net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
    }

    private static void renderSprite(TextureAtlasSprite sprite, VertexConsumer vc, PoseStack pose,
                                     int light, int overlay, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        if (a <= 0f) a = 1f;
        for (BakedQuad quad : getQuads(sprite)) {
            vc.putBulkData(pose.last(), quad, r, g, b, a, light, overlay);
        }
    }

    private static void expandIfNotGui(ItemDisplayContext ctx, PoseStack pose) {
        if (ctx != ItemDisplayContext.GUI) {
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.scale(1.001f, 1.001f, 1.001f);
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        Item item = stack.getItem();
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return;

        List<String> layers = TOOL_LAYERS.get(itemId.getPath());
        if (layers == null) return;

        Function<ResourceLocation, TextureAtlasSprite> atlas =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var itemColors = Minecraft.getInstance().getItemColors();

        pose.pushPose();
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

        for (int i = 0; i < layers.size(); i++) {
            TextureAtlasSprite sprite = atlas.apply(
                    ResourceLocation.fromNamespaceAndPath("hephaestus_tools", layers.get(i)));
            if (isMissing(atlas, sprite)) continue;
            int color = itemColors.getColor(stack, i);
            renderSprite(sprite, vc, pose, light, overlay, color);
        }

        if (ToolStack.isInitialized(stack)) {
            for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(stack)) {
                ResourceLocation tex = ModifierModelRegistry.getTexture(item, mod.id());
                if (tex == null) continue;
                TextureAtlasSprite sprite = atlas.apply(tex);
                if (isMissing(atlas, sprite)) continue;
                renderSprite(sprite, vc, pose, light, overlay, 0xFFFFFFFF);
            }
        }

        expandIfNotGui(ctx, pose);
        pose.popPose();
    }
}