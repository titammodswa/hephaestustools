package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.titammods.hephaestus_tools.client.ToolLayerMap;
import com.titammods.hephaestus_tools.client.renderer.ToolItemRenderer;
import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class WorkbenchRenderer {
    public static final int FULL_BRIGHT = 15728880;

    public interface Op {
        void submit(PoseStack pose, SubmitNodeCollector collector, int overlay);
    }

    private record ItemOp(ItemStackRenderState item, Vec3 p, float facingYaw, float tilt, float yaw, float scale, int light,
                          boolean ghost, float a, float r, float g, float b, int mask) implements Op {
        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int overlay) {
            SubmitNodeCollector out = ghost ? GhostCollector.tinted(collector, a, r, g, b, mask) : GhostCollector.masked(collector, mask);
            pose.pushPose();
            pose.translate(p.x, p.y, p.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - facingYaw));
            if (tilt != 0.0f) pose.mulPose(Axis.XP.rotationDegrees(tilt));
            pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            pose.mulPose(Axis.ZP.rotationDegrees(yaw));
            pose.scale(scale, scale, scale);
            item.submit(pose, out, light, overlay, 0);
            pose.popPose();
        }
    }

    private static final class Ctx {
        final ArsenalTableBlockEntity be;
        final ItemModelResolver resolver;
        final List<Op> out;

        Ctx(ArsenalTableBlockEntity be, ItemModelResolver resolver, List<Op> out) {
            this.be = be;
            this.resolver = resolver;
            this.out = out;
        }
    }

    private WorkbenchRenderer() {}

    public static void extractItems(ArsenalTableBlockEntity be, ItemModelResolver resolver, int light, List<Op> out) {
        if (be.getLevel() == null) return;
        Ctx ctx = new Ctx(be, resolver, out);

        boolean viewing = WorkbenchSession.isViewing(be.getBlockPos());
        int itemLight = viewing ? FULL_BRIGHT : light;
        double time = WorkbenchSession.sceneSeconds();
        float padAcross = (float) WorkbenchScene.focusAcross() + WorkbenchScene.RESULT_ACROSS;
        float padForward = WorkbenchScene.RESULT_FORWARD;

        boolean assembled = viewing && (WorkbenchAssembly.isComplete() || WorkbenchCraft.showResult());
        float strike = viewing ? WorkbenchCraft.strikePulse() : 0.0f;

        if (!assembled) {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = viewing
                        ? (i < WorkbenchSession.inputs().size() ? WorkbenchSession.inputs().get(i) : ItemStack.EMPTY)
                        : be.getInputSlots().getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (viewing && WorkbenchAssembly.isMerged(i) && Math.abs(WorkbenchAssembly.forward(i)) + Math.abs(WorkbenchAssembly.across(i)) < 0.06f) continue;
                float forward, across, lift = 0.0f, yaw = WorkbenchScene.PART_YAW[i];
                float scale = WorkbenchScene.PART_SCALE;
                if (viewing && WorkbenchAssembly.isPresent(i)) {
                    forward = padForward + WorkbenchAssembly.forward(i);
                    across = padAcross + WorkbenchAssembly.across(i);
                    if (WorkbenchAssembly.isDragging(i)) lift = 0.10f;
                    else if (WorkbenchAssembly.isHover(i)) lift = 0.035f;
                    lift += (float) Math.sin(time * 1.6 + i * 1.7) * 0.004f;
                    if (WorkbenchAssembly.isMerged(i)) {
                        yaw = 0.0f;
                        lift += 0.012f * i - 0.03f * strike;
                        scale *= 1.0f - 0.06f * strike;
                    }
                } else {
                    forward = padForward + WorkbenchAssembly.homeForward(i);
                    across = padAcross + WorkbenchAssembly.homeAcross(i);
                }
                flat(ctx, stack, forward, across, WorkbenchScene.PART_HEIGHT + lift, scale, yaw, 0.0f, i, itemLight, null, -1);
            }
        }

        ItemStack result;
        if (viewing) result = WorkbenchSession.output();
        else {
            result = be.getOutputSlot().getStackInSlot(0);
            if (result.isEmpty()) result = be.getUpgradeSlot().getStackInSlot(0);
        }
        boolean isPreview = false;
        if (result.isEmpty() && assembled) {
            result = WorkbenchSession.preview();
            isPreview = !result.isEmpty();
        }

        ItemStack ghost = viewing && result.isEmpty() && !assembled ? WorkbenchAssembly.ghost() : ItemStack.EMPTY;
        if (!ghost.isEmpty()) {
            float hover = (float) Math.sin(time * 1.8) * 0.02f;
            float spin = WorkbenchSession.viewYaw();
            float tilt = WorkbenchSession.viewTilt();
            float glow = 0.40f + 0.12f * (float) Math.sin(time * 3.0);
            int layers = ToolItemRenderer.layerCount(ghost.getItem());
            int solid = 0;
            int parts = WorkbenchAssembly.requiredCount();
            if (layers > 0 && parts > 0) {
                for (int l = 0; l < layers; l++) {
                    int part = partForLayer(ghost, l, layers, parts);
                    if (WorkbenchAssembly.isPresent(part) && WorkbenchAssembly.isMerged(part)) solid |= 1 << l;
                }
            }
            int all = layers <= 0 || layers >= 31 ? -1 : (1 << layers) - 1;
            int target = 0;
            int dragged = WorkbenchAssembly.draggingIndex();
            if (dragged >= 0 && layers > 0 && parts > 0) {
                for (int l = 0; l < layers; l++) {
                    int part = partForLayer(ghost, l, layers, parts);
                    if (part == dragged) target |= 1 << l;
                }
                target &= ~solid;
            }
            float y = WorkbenchScene.RESULT_HEIGHT + hover;
            if (target != 0) {
                float near = WorkbenchAssembly.overTool() ? 1.0f : 0.0f;
                flat(ctx, ghost, padForward, padAcross, y, WorkbenchScene.RESULT_SCALE, WorkbenchScene.RESULT_YAW + spin, tilt, 53, itemLight,
                        new float[] {0.55f + 0.35f * near + 0.1f * (float) Math.sin(time * 8.0), 0.45f, 1.0f, 0.5f}, target);
            }
            int rest = all & ~solid & ~target;
            if (rest != 0) {
                flat(ctx, ghost, padForward, padAcross, y, WorkbenchScene.RESULT_SCALE, WorkbenchScene.RESULT_YAW + spin, tilt, 51, itemLight,
                        new float[] {glow, 0.55f, 0.95f, 1.0f}, rest);
            }
            ItemStack real = WorkbenchAssembly.ghostSolid();
            if (solid != 0 && !real.isEmpty()) {
                flat(ctx, real, padForward, padAcross, y, WorkbenchScene.RESULT_SCALE, WorkbenchScene.RESULT_YAW + spin, tilt, 52, itemLight, null, solid);
            }
        }
        if (false && viewing) {
            final boolean holo = isPreview || !ghost.isEmpty();
            final boolean hasItem = !result.isEmpty();
            final boolean assembling = WorkbenchAssembly.hasParts() && !assembled;
            final BlockState state = be.getBlockState();
            out.add((pose, collector, overlay) -> renderPad(state, pose, collector, padForward, padAcross, holo, hasItem, time, assembling));
        }
        if (viewing && WorkbenchCraft.isActive()) {
            float fly = WorkbenchCraft.infuseProgress();
            ItemStack material = WorkbenchCraft.infuseItem();
            if (fly >= 0.0f && !material.isEmpty()) {
                float startF = (float) WorkbenchTuning.PLAYER_DISTANCE.get() - 0.15f;
                float to = WorkbenchCraft.flightStation();
                float ff = Mth.lerp(fly, startF, (float) WorkbenchScene.workForward(to));
                float aa = (float) WorkbenchScene.workAcross(to);
                float yy = Mth.lerp(fly, 1.25f, (float) WorkbenchScene.workTop(to) + 0.12f) + 0.35f * 4.0f * fly * (1.0f - fly);
                float sc = 0.30f * (1.0f - 0.6f * fly * fly * fly * fly);
                flat(ctx, material, ff, aa, yy, sc, fly * 540.0f, -60.0f, 60, FULL_BRIGHT, null, -1);
            }
        }

        if (viewing && WorkbenchCraft.holdsResult()) result = ItemStack.EMPTY;
        if (!result.isEmpty()) {
            float hover = viewing ? (float) Math.sin(time * 1.8) * (isPreview ? 0.02f : 0.012f) : 0.0f;
            float pop = 1.0f;
            if (isPreview) {
                float m = viewing ? WorkbenchAssembly.completeMix() : 1.0f;
                pop = 0.55f + 0.45f * easeOutBack(Mth.clamp(m, 0.0f, 1.0f));
                hover += 0.05f * strike;
            }
            float spin = viewing ? WorkbenchSession.viewYaw() : 0.0f;
            float tilt = viewing ? WorkbenchSession.viewTilt() : 0.0f;
            float f = padForward, a = padAcross, h = WorkbenchScene.RESULT_HEIGHT + hover, sc = WorkbenchScene.RESULT_SCALE * pop;
            float[] tint = null;
            if (viewing && WorkbenchCraft.isActive() && !WorkbenchCraft.showResult()) {
                float st = WorkbenchCraft.stationMix();
                f = (float) WorkbenchScene.workForward(st);
                a = (float) WorkbenchScene.workAcross(st);
                h = (float) WorkbenchScene.workTop(st) + Mth.lerp(st, 0.04f, WorkbenchScene.RESULT_HEIGHT - (float) WorkbenchScene.TABLE_TOP)
                        + 0.35f * WorkbenchCraft.carryAmount() + 0.03f * strike;
                sc = Mth.lerp(st, 0.42f, WorkbenchScene.RESULT_SCALE) * (1.0f + 0.05f * strike);
                spin = WorkbenchCraft.tinkerTurn();
                tilt = 0.0f;
                if (WorkbenchCraft.isTinker()) h += 0.04f;
                float heat = WorkbenchCraft.heat();
                if (heat > 0.02f) tint = new float[] {1.0f, 1.0f, 1.0f - 0.40f * heat, 1.0f - 0.62f * heat};
            }
            float morph = viewing && !WorkbenchCraft.isActive() ? WorkbenchCraft.morph() : 0.0f;
            if (morph > 0.01f) {
                sc *= 1.0f + 0.14f * morph;
                h += 0.04f * morph;
                tint = new float[] {1.0f, 1.0f, 1.0f - 0.22f * morph, 1.0f - 0.55f * morph};
            }
            flat(ctx, result, f, a, h, sc, WorkbenchScene.RESULT_YAW + spin, tilt, 50, itemLight, tint, -1);
        }
    }

    public static void renderGlow(BlockState state, PoseStack pose, SubmitNodeCollector collector,
                                  float forward, float across, float y, float size, float r, float g, float b, float a) {
        Vec3 p = WorkbenchScene.localPoint(state, forward, across, y);
        float half = size * 0.5f;
        pose.pushPose();
        pose.translate(p.x, p.y, p.z);
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (last, vc) -> {
            Matrix4f m = last.pose();
            quad(vc, m, -half, -half, half, half, 0.0f, r, g, b, a);
            quad(vc, m, -half * 0.55f, -half * 0.55f, half * 0.55f, half * 0.55f, 0.001f, 1.0f, 0.75f, 0.35f, a * 0.8f);
        });
        pose.popPose();
    }

    private static int partForLayer(ItemStack tool, int layer, int layers, int parts) {
        int m = ToolLayerMap.materialIndexForLayer(tool.getItem(), layer);
        if (m >= 0 && m < parts) return m;
        return layers == parts ? layer : Math.min(parts - 1, layer * parts / Math.max(1, layers));
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1.0f;
        float x = t - 1.0f;
        return 1.0f + c3 * x * x * x + c1 * x * x;
    }

    private static void renderPad(BlockState state, PoseStack pose, SubmitNodeCollector collector,
                                  float forward, float across, boolean hologram, boolean hasItem, double time,
                                  boolean assembling) {
        Direction facing = WorkbenchScene.facing(state);
        Vec3 p = WorkbenchScene.localPoint(state, forward, across, WorkbenchScene.TABLE_TOP + 0.03);
        float half = WorkbenchScene.PAD_SIZE * 0.5f;
        float r, g, b;
        if (hologram) { r = 0.30f; g = 0.80f; b = 1.00f; }
        else if (hasItem) { r = 1.00f; g = 0.72f; b = 0.30f; }
        else if (assembling) { r = 0.55f; g = 0.85f; b = 1.00f; }
        else { r = 0.60f; g = 0.65f; b = 0.70f; }
        float pulse = hologram || assembling ? 0.5f + 0.5f * (float) Math.sin(time * 3.0) : 0.6f;
        pose.pushPose();
        pose.translate(p.x, p.y, p.z);
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - facing.toYRot() + WorkbenchScene.RESULT_YAW));
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (last, vc) -> {
            Matrix4f m = last.pose();
            quad(vc, m, -half, -half, half, half, 0.0f, r, g, b, 0.10f + 0.06f * pulse);
            float len = half * 0.38f;
            float t = 0.012f;
            float a = 0.55f + 0.35f * pulse;
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    float cx = sx * half, cz = sz * half;
                    quad(vc, m, Math.min(cx, cx - sx * len), Math.min(cz, cz - sz * t),
                            Math.max(cx, cx - sx * len), Math.max(cz, cz - sz * t), 0.001f, r, g, b, a);
                    quad(vc, m, Math.min(cx, cx - sx * t), Math.min(cz, cz - sz * len),
                            Math.max(cx, cx - sx * t), Math.max(cz, cz - sz * len), 0.001f, r, g, b, a);
                }
            }
        });
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float z0, float x1, float z1, float y,
                             float r, float g, float b, float a) {
        int ir = (int) (r * 255), ig = (int) (g * 255), ib = (int) (b * 255), ia = (int) (a * 255);
        vc.addVertex(m, x0, y, z0).setColor(ir, ig, ib, ia);
        vc.addVertex(m, x0, y, z1).setColor(ir, ig, ib, ia);
        vc.addVertex(m, x1, y, z1).setColor(ir, ig, ib, ia);
        vc.addVertex(m, x1, y, z0).setColor(ir, ig, ib, ia);
    }

    private static void flat(Ctx ctx, ItemStack stack, float forward, float across, float y, float scale, float yawDegrees,
                             float tiltDegrees, int seed, int light, float[] tint, int mask) {
        BlockState state = ctx.be.getBlockState();
        Direction facing = WorkbenchScene.facing(state);
        Vec3 p = WorkbenchScene.localPoint(state, forward, across, y);
        ItemStackRenderState item = new ItemStackRenderState();
        ctx.resolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, ctx.be.getLevel(), null,
                ctx.be.getBlockPos().hashCode() + seed);
        if (item.isEmpty()) return;
        boolean ghost = tint != null;
        ctx.out.add(new ItemOp(item, p, facing.toYRot(), tiltDegrees, yawDegrees, scale, light, ghost,
                ghost ? tint[0] : 1f, ghost ? tint[1] : 1f, ghost ? tint[2] : 1f, ghost ? tint[3] : 1f, mask));
    }
}
