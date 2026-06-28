package com.titammods.hephaestus_tools.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tools.aoe.IAoeTool;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.lang.reflect.Field;
import java.util.List;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, value = Dist.CLIENT)
public final class HammerAoeRenderer {

    private static final int MAX_BLOCKS = 60;

    private HammerAoeRenderer() {}

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IAoeTool aoeTool)) return;

        BlockHitResult target = event.getTarget();
        if (target.getType() != HitResult.Type.BLOCK) return;

        BlockState centerState = level.getBlockState(target.getBlockPos());
        if (!aoeTool.isEffectiveOnBlock(stack, centerState, player)) return;

        List<BlockPos> extra = aoeTool.getExtraBlocks(level, target, player, stack);
        if (extra.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();

        int rendered = 0;
        for (BlockPos pos : extra) {
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            renderShapeOutline(pose, vc, shape,
                    pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z,
                    0f, 0f, 0f, 0.4f);
            if (++rendered >= MAX_BLOCKS) break;
        }
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode gm = mc.gameMode;
        if (gm == null || !gm.isDestroying()) return;

        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null || mc.getCameraEntity() == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IAoeTool aoeTool)) return;

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult brt = (BlockHitResult) result;
        BlockPos targetPos = brt.getBlockPos();

        int progress = findDestroyStage(mc, targetPos);
        if (progress < 0) return;

        BlockState centerState = level.getBlockState(targetPos);
        if (!aoeTool.isEffectiveOnBlock(stack, centerState, player)) return;

        List<BlockPos> extra = aoeTool.getExtraBlocks(level, brt, player, stack);
        if (extra.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        MultiBufferSource.BufferSource crumbling = mc.renderBuffers().crumblingBufferSource();
        VertexConsumer base = crumbling.getBuffer(ModelBakery.DESTROY_TYPES.get(progress));

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        int rendered = 0;
        for (BlockPos pos : extra) {
            pose.pushPose();
            pose.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
            PoseStack.Pose entry = pose.last();
            VertexConsumer decal = new SheetedDecalTextureGenerator(base, entry, 1.0f);
            dispatcher.renderBreakingTexture(level.getBlockState(pos), pos, level, pose, decal, ModelData.EMPTY);
            pose.popPose();
            if (++rendered >= MAX_BLOCKS) break;
        }

        pose.popPose();
        crumbling.endBatch();
    }

    private static Field destroyingBlocksField;
    private static boolean reflectFailed = false;

    @SuppressWarnings("unchecked")
    private static int findDestroyStage(Minecraft mc, BlockPos target) {
        if (reflectFailed) return -1;
        try {
            if (destroyingBlocksField == null) {
                destroyingBlocksField = net.minecraft.client.renderer.LevelRenderer.class
                        .getDeclaredField("destroyingBlocks");
                destroyingBlocksField.setAccessible(true);
            }
            Object map = destroyingBlocksField.get(mc.levelRenderer);
            if (!(map instanceof Int2ObjectMap<?> destroying)) return -1;
            for (Int2ObjectMap.Entry<?> e : ((Int2ObjectMap<BlockDestructionProgress>) destroying).int2ObjectEntrySet()) {
                BlockDestructionProgress p = (BlockDestructionProgress) e.getValue();
                if (p.getPos().equals(target)) {
                    return p.getProgress();
                }
            }
            return -1;
        } catch (Throwable t) {
            reflectFailed = true;
            return -1;
        }
    }

    private static void renderShapeOutline(PoseStack pose, VertexConsumer vc, VoxelShape shape,
                                           double ox, double oy, double oz,
                                           float r, float g, float b, float a) {
        PoseStack.Pose entry = pose.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-4f) return;
            dx /= len; dy /= len; dz /= len;
            vc.addVertex(entry, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz))
                    .setColor(r, g, b, a).setNormal(entry, dx, dy, dz);
            vc.addVertex(entry, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz))
                    .setColor(r, g, b, a).setNormal(entry, dx, dy, dz);
        });
    }
}