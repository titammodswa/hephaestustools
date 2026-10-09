package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class GhostCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector delegate;
    private final boolean ghost;
    private final float alpha, red, green, blue;
    private final int layerMask;

    private GhostCollector(SubmitNodeCollector delegate, boolean ghost, float alpha, float red, float green, float blue, int layerMask) {
        this.delegate = delegate;
        this.ghost = ghost;
        this.alpha = alpha;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.layerMask = layerMask;
    }

    static SubmitNodeCollector tinted(SubmitNodeCollector delegate, float alpha, float red, float green, float blue, int layerMask) {
        return new GhostCollector(delegate, true, alpha, red, green, blue, layerMask);
    }

    static SubmitNodeCollector masked(SubmitNodeCollector delegate, int layerMask) {
        return layerMask == -1 ? delegate : new GhostCollector(delegate, false, 1f, 1f, 1f, 1f, layerMask);
    }

    private boolean shown(BakedQuad quad) {
        int tint = quad.materialInfo().tintIndex();
        if (layerMask == -1 || tint < 0 || tint >= 31) return true;
        return (layerMask & (1 << tint)) != 0;
    }

    private static int layerColor(int[] tints, BakedQuad quad) {
        BakedQuad.MaterialInfo material = quad.materialInfo();
        if (!material.isTinted()) return -1;
        int i = material.tintIndex();
        return i >= 0 && i < tints.length ? tints[i] : -1;
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
                           int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        List<BakedQuad> kept = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) if (shown(quad)) kept.add(quad);
        if (kept.isEmpty()) return;
        if (!ghost) {
            delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, kept, foilType);
            return;
        }
        List<BakedQuad> blockQuads = new ArrayList<>(), itemQuads = new ArrayList<>();
        for (BakedQuad quad : kept) {
            if (TextureAtlas.LOCATION_BLOCKS.equals(quad.materialInfo().sprite().atlasLocation())) blockQuads.add(quad);
            else itemQuads.add(quad);
        }
        submitGhost(poseStack, Sheets.translucentItemSheet(), itemQuads, tintLayers, lightCoords, overlayCoords);
        submitGhost(poseStack, Sheets.translucentBlockItemSheet(), blockQuads, tintLayers, lightCoords, overlayCoords);
    }

    private void submitGhost(PoseStack poseStack, RenderType type, List<BakedQuad> quads, int[] tints, int light, int overlay) {
        if (quads.isEmpty()) return;
        delegate.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            QuadInstance instance = new QuadInstance();
            instance.setLightCoords(light);
            instance.setOverlayCoords(overlay);
            for (BakedQuad quad : quads) {
                int base = layerColor(tints, quad);
                int r = Math.round(ARGB.red(base) * red), g = Math.round(ARGB.green(base) * green), b = Math.round(ARGB.blue(base) * blue);
                int a = Math.round(ARGB.alpha(base) * alpha);
                instance.setColor(ARGB.color(clamp(a), clamp(r), clamp(g), clamp(b)));
                buffer.putBakedQuad(pose, quad, instance);
            }
        });
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return delegate.order(order);
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough,
                              int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
                           Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        delegate.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        delegate.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords,
                                int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor,
                                ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        delegate.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
                                @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
                                ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        delegate.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        delegate.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers,
                                 int lightCoords, int overlayCoords, int outlineColor) {
        delegate.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        delegate.submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
        delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
        delegate.submitParticleGroup(particleGroupRenderer);
    }

    @Override
    public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent,
                                           int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        delegate.submitMultiLayerBlockModel(poseStack, modelParts, translucent, tintLayers, lightCoords, overlayCoords, outlineColor);
    }
}
