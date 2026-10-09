package com.titammods.hephaestus_tools.mixin.client;

import com.titammods.hephaestus_tools.client.workbench.WorkbenchCamera;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchSession;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void setPosition(Vec3 position);

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void hephaestusTools$workbenchCamera(float partialTicks, CallbackInfo ci) {
        if (!WorkbenchSession.isRunning()) return;
        Camera self = (Camera) (Object) this;
        WorkbenchCamera.Pose pose = WorkbenchCamera.update(self.position(), self.yRot(), self.xRot());
        if (pose == null) return;
        setPosition(pose.position());
        setRotation(pose.yaw(), pose.pitch());
    }
}
