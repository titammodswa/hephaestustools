package com.titammods.hephaestus_tools.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.titammods.hephaestus_tools.client.workbench.RemoteCraft;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void hephaestusTools$hideInScene(PoseStack pose, SubmitNodeCollector collector, int light, ArmedEntityRenderState state,
                                             float yRot, float xRot, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && avatar.id == mc.player.getId() && WorkbenchSession.isRunning()) ci.cancel();
        else if (RemoteCraft.isActive(avatar.id)) ci.cancel();
    }
}
