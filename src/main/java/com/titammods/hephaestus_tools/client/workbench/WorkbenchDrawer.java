package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class WorkbenchDrawer {
    private WorkbenchDrawer() {}

    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath("hephaestus_tools", "block/forging_table");

    private static final float[][] PARTS = {
        {3f, 4f, 2.55f, 12f, 8f, 14.55f, 1.75f, 6.5f, 2.875f, 7f, 0f, 5.375f, 1.5f, 5.875f, 4.875f, 6.5f, 6f, 7f, 1.5f, 5.375f, 0f, 5.875f, 2.875f, 5.875f, 1.75f, 4.375f, 4.5f, 4.25f, 3.375f, 5.75f, 6.9f},
        {3f, 8f, 2.55f, 12f, 12f, 14.55f, 1.75f, 6.5f, 2.875f, 7f, 0f, 5.375f, 1.5f, 5.875f, 4.875f, 6.5f, 6f, 7f, 1.5f, 5.375f, 0f, 5.875f, 2.875f, 5.875f, 1.75f, 4.375f, 4.5f, 4.375f, 3.375f, 5.875f, 3.9f}
    };

    public static void extract(ArsenalTableBlockEntity be, ItemModelResolver resolver, int light, List<WorkbenchRenderer.Op> out) {
        if (!WorkbenchCraft.drawerEnabled()) return;
        BlockState state = be.getBlockState();
        boolean viewing = WorkbenchSession.isViewing(be.getBlockPos());
        float open = viewing ? WorkbenchCraft.drawerOpen() : 0.0f;
        Direction facing = WorkbenchScene.facing(state);
        float blockY = (facing.toYRot() + 180.0f) % 360.0f;
        ItemStackRenderState hammer = null;
        if (viewing && WorkbenchCraft.hammerInDrawer()) {
            hammer = new ItemStackRenderState();
            resolver.updateForTopItem(hammer, new ItemStack(ModItems.SLEDGE_HAMMER.get()), ItemDisplayContext.FIXED, be.getLevel(), null, 0);
        }
        final ItemStackRenderState hammerState = hammer;
        out.add((pose, collector, overlay) -> {
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(SPRITE);
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-blockY));
            pose.translate(-0.5, 0.0, -0.5);
            for (float[] p : PARTS) {
                pose.pushPose();
                pose.translate(0.0, 0.0, -open * p[30] / 16.0f);
                collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
                        (last, vc) -> box(vc, last, sprite, p, light, overlay));
                pose.popPose();
            }
            if (hammerState != null && !hammerState.isEmpty()) {
                pose.pushPose();
                pose.translate(0.47, 0.80, 0.53 - open * PARTS[1][30] / 16.0f);
                pose.mulPose(Axis.YP.rotationDegrees(-20.0f));
                pose.mulPose(Axis.XP.rotationDegrees(90.0f));
                pose.scale(0.62f, 0.62f, 0.62f);
                hammerState.submit(pose, collector, light, overlay, 0);
                pose.popPose();
            }
            pose.popPose();
        });
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, float[] p, int light, int overlay) {
        float x0 = p[0] / 16f, y0 = p[1] / 16f, z0 = p[2] / 16f, x1 = p[3] / 16f, y1 = p[4] / 16f, z1 = p[5] / 16f;
        face(vc, pose, s, light, overlay, p, 6, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        face(vc, pose, s, light, overlay, p, 10, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
        face(vc, pose, s, light, overlay, p, 14, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        face(vc, pose, s, light, overlay, p, 18, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        face(vc, pose, s, light, overlay, p, 22, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(vc, pose, s, light, overlay, p, 26, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
    }

    private static void face(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light, int overlay, float[] p, int uvAt,
                             float nx, float ny, float nz, float... c) {
        float u0 = s.getU0() + (s.getU1() - s.getU0()) * (p[uvAt] / 16f);
        float v0 = s.getV0() + (s.getV1() - s.getV0()) * (p[uvAt + 1] / 16f);
        float u1 = s.getU0() + (s.getU1() - s.getU0()) * (p[uvAt + 2] / 16f);
        float v1 = s.getV0() + (s.getV1() - s.getV0()) * (p[uvAt + 3] / 16f);
        float[] us = {u0, u0, u1, u1}, vs = {v0, v1, v1, v0};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(pose, c[i * 3], c[i * 3 + 1], c[i * 3 + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(us[i], vs[i])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
