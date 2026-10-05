package com.titammods.hephaestus_tools.mixin;

import com.titammods.hephaestus_tools.tools.aoe.PlayerBlockBreaks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void hephaestusTools$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerBlockBreaks.begin(player, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void hephaestusTools$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerBlockBreaks.finish(player, pos, cir.getReturnValueZ());
    }
}