package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.titammods.hephaestus_tools.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class WorkbenchHandLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final ItemStackRenderState itemState = new ItemStackRenderState();

    public WorkbenchHandLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack;
        boolean hammer;
        boolean local = state.id == mc.player.getId();
        if (local) {
            if (!WorkbenchSession.isRunning()) return;
            stack = WorkbenchCraft.handItem();
            hammer = WorkbenchCraft.holdsHammer();
        } else {
            stack = RemoteCraft.handItem(state.id);
            hammer = !stack.isEmpty() && stack.getItem() == ModItems.SLEDGE_HAMMER.get();
        }
        if (stack.isEmpty()) return;

        pose.pushPose();
        getParentModel().translateToHand(state, HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.translate(1.0f / 16.0f, 0.125f, -0.625f);
        float turn = local ? WorkbenchCraft.admireTurn() : 0.0f;
        if (turn != 0.0f) pose.mulPose(Axis.YP.rotationDegrees(turn));
        if (!hammer || !AnimHammer.submit(pose, collector, light)) {
            mc.getItemModelResolver().updateForTopItem(itemState, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    mc.level, null, state.id);
            itemState.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        pose.popPose();
    }
}
