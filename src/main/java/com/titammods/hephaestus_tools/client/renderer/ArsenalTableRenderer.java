package com.titammods.hephaestus_tools.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchDrawer;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchRenderer;
import com.titammods.hephaestus_tools.config.HephaestusConfig;
import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ArsenalTableRenderer implements BlockEntityRenderer<ArsenalTableBlockEntity, ArsenalTableRenderer.State> {

    private final ItemModelResolver itemModelResolver;

    public ArsenalTableRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final List<WorkbenchRenderer.Op> ops = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArsenalTableBlockEntity blockEntity, State state, float partialTick,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.ops.clear();
        if (!HephaestusConfig.modernInterface()) return;
        WorkbenchDrawer.extract(blockEntity, itemModelResolver, state.lightCoords, state.ops);
        WorkbenchRenderer.extractItems(blockEntity, itemModelResolver, state.lightCoords, state.ops);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        for (WorkbenchRenderer.Op op : state.ops) op.submit(poseStack, collector, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public AABB getRenderBoundingBox(ArsenalTableBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }
}
