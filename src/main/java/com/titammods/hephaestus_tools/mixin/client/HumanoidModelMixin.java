package com.titammods.hephaestus_tools.mixin.client;

import com.titammods.hephaestus_tools.client.workbench.RemoteCraft;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchAnimation;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void hephaestusTools$workbenchPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerModel) || !(state instanceof AvatarRenderState avatar)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (avatar.id != mc.player.getId()) {
            if (RemoteCraft.isActive(avatar.id) && !RemoteCraft.isAnimated(avatar.id))
                RemoteCraft.applyPose(avatar.id, head, body, rightArm, leftArm);
            return;
        }
        float weight = WorkbenchSession.isRunning() ? WorkbenchAnimation.poseWeight() : 0.0f;
        if (weight <= 0.0f) return;
        WorkbenchAnimation.applyProceduralPose(head, body, rightArm, leftArm, rightLeg, leftLeg, weight, WorkbenchSession.sceneSeconds());
    }
}
